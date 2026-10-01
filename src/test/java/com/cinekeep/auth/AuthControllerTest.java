package com.cinekeep.auth;

import com.cinekeep.user.UserResponse;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, RefreshTokenCookies.class})
class AuthControllerTest {
    private static final String REGISTER_REQUEST = """
            { "email": "david@example.com", "username": "dtoro", "password": "correct horse" }
            """;
    private static final String LOGIN_REQUEST = """
            { "email": "david@example.com", "password": "correct horse" }
            """;

    @Autowired
    private MockMvcTester mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void registerReturnsTokensAndSetsRefreshCookie() {
        when(authService.register(any())).thenReturn(authResult("refresh-token"));

        MvcTestResult result = mockMvc.post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REGISTER_REQUEST)
                .exchange();

        assertThat(setCookieHeader(result))
                .startsWith("cinekeep_refresh_token=refresh-token;")
                .contains("Path=/api/auth", "Max-Age=2592000", "HttpOnly", "SameSite=Strict");
        assertThat(result)
                .hasStatus(201)
                .bodyJson()
                .hasPathSatisfying("$.accessToken", value -> value.assertThat().isEqualTo("access-token"))
                .hasPathSatisfying("$.tokenType", value -> value.assertThat().isEqualTo("Bearer"))
                .hasPathSatisfying("$.user.username", value -> value.assertThat().isEqualTo("dtoro"));
    }

    @Test
    void registerRejectsInvalidRequest() {
        assertThat(mockMvc.post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "email": "not-an-email", "username": "a b", "password": "short" }
                        """))
                .hasStatus(400)
                .bodyJson()
                .hasPathSatisfying("$.message", value -> value.assertThat().asString()
                        .contains("email").contains("username").contains("password"));

        verifyNoInteractions(authService);
    }

    @Test
    void registerReturnsConflictForTakenEmail() {
        when(authService.register(any())).thenThrow(new EmailAlreadyExistsException());

        assertThat(mockMvc.post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REGISTER_REQUEST))
                .hasStatus(409)
                .bodyJson()
                .isStrictlyEqualTo("""
                        { "status": 409, "message": "Email is already registered" }
                        """);
    }

    @Test
    void loginReturnsTokens() {
        when(authService.login(any())).thenReturn(authResult("refresh-token"));

        assertThat(mockMvc.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(LOGIN_REQUEST))
                .hasStatusOk()
                .containsHeader(HttpHeaders.SET_COOKIE)
                .bodyJson()
                .hasPathSatisfying("$.expiresIn", value -> value.assertThat().isEqualTo(900));
    }

    @Test
    void loginReturnsUnauthorizedForInvalidCredentials() {
        when(authService.login(any())).thenThrow(new InvalidCredentialsException());

        assertThat(mockMvc.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(LOGIN_REQUEST))
                .hasStatus(401)
                .bodyJson()
                .isStrictlyEqualTo("""
                        { "status": 401, "message": "Invalid email or password" }
                        """);
    }

    @Test
    void refreshUsesTokenFromCookie() {
        when(authService.refresh("old-refresh-token")).thenReturn(authResult("new-refresh-token"));

        MvcTestResult result = mockMvc.post().uri("/api/auth/refresh")
                .cookie(new Cookie("cinekeep_refresh_token", "old-refresh-token"))
                .exchange();

        assertThat(result).hasStatusOk();
        assertThat(setCookieHeader(result)).startsWith("cinekeep_refresh_token=new-refresh-token;");
    }

    @Test
    void refreshWithoutCookieReturnsUnauthorized() {
        when(authService.refresh(null)).thenThrow(new InvalidRefreshTokenException());

        assertThat(mockMvc.post().uri("/api/auth/refresh"))
                .hasStatus(401)
                .bodyJson()
                .isStrictlyEqualTo("""
                        { "status": 401, "message": "Invalid or expired refresh token" }
                        """);
    }

    @Test
    void logoutRevokesTokenAndClearsCookie() {
        MvcTestResult result = mockMvc.post().uri("/api/auth/logout")
                .cookie(new Cookie("cinekeep_refresh_token", "refresh-token"))
                .exchange();

        assertThat(result).hasStatus(204);
        assertThat(setCookieHeader(result)).startsWith("cinekeep_refresh_token=;").contains("Max-Age=0");
        verify(authService).logout("refresh-token");
    }

    private static String setCookieHeader(MvcTestResult result) {
        return result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
    }

    private static AuthResult authResult(String refreshToken) {
        UserResponse user = new UserResponse(1L, "david@example.com", "dtoro", Instant.parse("2026-10-01T10:00:00Z"));
        return new AuthResult(new AuthResponse("access-token", "Bearer", 900, user), refreshToken);
    }
}
