package com.cinekeep.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CurrentUserService currentUserService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserLoadsUserFromJwtSubject() {
        User user = new User("david@example.com", "dtoro", "password-hash");
        authenticateWithSubject("7");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        assertThat(currentUserService.getCurrentUser()).isSameAs(user);
    }

    @Test
    void getCurrentUserRejectsMissingAuthentication() {
        assertThatThrownBy(() -> currentUserService.getCurrentUser())
                .isInstanceOf(UserNotAuthenticatedException.class);
    }

    @Test
    void getCurrentUserRejectsNonJwtAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("someone", "password", List.of())
        );

        assertThatThrownBy(() -> currentUserService.getCurrentUser())
                .isInstanceOf(UserNotAuthenticatedException.class);
    }

    @Test
    void getCurrentUserRejectsTokenOfDeletedUser() {
        authenticateWithSubject("7");
        when(userRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> currentUserService.getCurrentUser())
                .isInstanceOf(UserNotAuthenticatedException.class);
    }

    private static void authenticateWithSubject(String subject) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject(subject)
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
