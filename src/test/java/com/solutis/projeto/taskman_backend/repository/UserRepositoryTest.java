package com.solutis.projeto.taskman_backend.repository;

import com.solutis.projeto.taskman_backend.domain.entity.User;
import com.solutis.projeto.taskman_backend.domain.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should save user and find by email successfully")
    void shouldSaveAndFindByEmail() {
        String email = "test." + System.currentTimeMillis() + "@example.com";
        User user = User.builder()
                .name("Test User")
                .email(email)
                .password("encoded_secret_password")
                .role(UserRole.ROLE_USER)
                .build();

        User savedUser = userRepository.save(user);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getAuthorities()).hasSize(1);
        assertThat(savedUser.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");

        Optional<User> found = userRepository.findByEmail(email);
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test User");
        assertThat(found.get().getUsername()).isEqualTo(email);
    }
}

