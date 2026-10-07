package com.solutis.projeto.taskman_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solutis.projeto.taskman_backend.dto.auth.LoginRequestDTO;
import com.solutis.projeto.taskman_backend.dto.auth.RegisterRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Should register new user successfully with 201 Created and return JWT token")
    void shouldRegisterUserSuccessfully() throws Exception {
        String uniqueEmail = "newuser." + UUID.randomUUID() + "@example.com";
        RegisterRequestDTO registerDTO = new RegisterRequestDTO(
                "New User",
                uniqueEmail,
                "password123"
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.name").value("New User"))
                .andExpect(jsonPath("$.email").value(uniqueEmail))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    @DisplayName("Should return 409 Conflict when registering with duplicate email")
    void shouldReturnConflictForDuplicateEmail() throws Exception {
        String email = "dup." + UUID.randomUUID() + "@example.com";
        RegisterRequestDTO registerDTO = new RegisterRequestDTO("User A", email, "password123");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Should authenticate registered user and return 200 OK with JWT token")
    void shouldLoginSuccessfully() throws Exception {
        String email = "login." + UUID.randomUUID() + "@example.com";
        RegisterRequestDTO registerDTO = new RegisterRequestDTO("Login User", email, "secretPass99");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().isCreated());

        LoginRequestDTO loginDTO = new LoginRequestDTO(email, "secretPass99");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when password does not match")
    void shouldFailLoginWithIncorrectPassword() throws Exception {
        String email = "badpass." + UUID.randomUUID() + "@example.com";
        RegisterRequestDTO registerDTO = new RegisterRequestDTO("User", email, "correctPassword");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().isCreated());

        LoginRequestDTO loginDTO = new LoginRequestDTO(email, "wrongPassword");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when validation fails")
    void shouldReturnBadRequestOnValidationFailure() throws Exception {
        RegisterRequestDTO invalidDTO = new RegisterRequestDTO("", "not-an-email", "123");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.name").isNotEmpty())
                .andExpect(jsonPath("$.fields.email").isNotEmpty())
                .andExpect(jsonPath("$.fields.password").isNotEmpty());
    }

    @Test
    @DisplayName("Should reject unauthenticated access to protected routes")
    void shouldRejectUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/tasks"))
                .andExpect(status().isForbidden());
    }
}
