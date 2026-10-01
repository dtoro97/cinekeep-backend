package com.cinekeep.user;

import com.cinekeep.auth.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@WithMockUser
class UserControllerTest {
    @Autowired
    private MockMvcTester mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void getCurrentUserReturnsProfile() {
        when(userService.getCurrentUserProfile()).thenReturn(
                new UserResponse(1L, "david@example.com", "dtoro", Instant.parse("2026-10-01T10:00:00Z"))
        );

        assertThat(mockMvc.get().uri("/api/users/me"))
                .hasStatusOk()
                .bodyJson()
                .isStrictlyEqualTo("""
                        {
                          "id": 1,
                          "email": "david@example.com",
                          "username": "dtoro",
                          "createdAt": "2026-10-01T10:00:00Z"
                        }
                        """);
    }
}
