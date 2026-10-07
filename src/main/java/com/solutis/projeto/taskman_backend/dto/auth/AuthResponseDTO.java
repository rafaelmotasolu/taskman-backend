package com.solutis.projeto.taskman_backend.dto.auth;

import java.util.UUID;

public record AuthResponseDTO(
        String token,
        UUID userId,
        String name,
        String email,
        String role
) {
}

