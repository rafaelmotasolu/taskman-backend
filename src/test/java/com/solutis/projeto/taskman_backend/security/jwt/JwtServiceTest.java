package com.solutis.projeto.taskman_backend.security.jwt;

import com.solutis.projeto.taskman_backend.domain.entity.User;
import com.solutis.projeto.taskman_backend.domain.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    @Test
    @DisplayName("Should generate token, extract username and userId, and validate successfully")
    void shouldGenerateAndValidateToken() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .name("Alice")
                .email("alice@example.com")
                .password("secret")
                .role(UserRole.ROLE_USER)
                .build();

        String token = jwtService.generateToken(user, userId);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("alice@example.com");
        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }
}

