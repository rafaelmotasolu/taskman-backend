package com.solutis.projeto.taskman_backend.dto.ai;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatPromptResponseDTO(
        UUID sessionId,
        String message,
        LocalDateTime timestamp
) {
}

