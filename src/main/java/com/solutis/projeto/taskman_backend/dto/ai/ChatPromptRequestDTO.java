package com.solutis.projeto.taskman_backend.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ChatPromptRequestDTO(
        @NotNull(message = "O ID da sessão é obrigatório")
        UUID sessionId,

        @NotBlank(message = "A mensagem do usuário é obrigatória")
        String message
) {
}

