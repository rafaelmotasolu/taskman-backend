package com.solutis.projeto.taskman_backend.dto.ai;

import jakarta.validation.constraints.NotBlank;

public record TaskImproveRequestDTO(
        @NotBlank(message = "O título é obrigatório")
        String title,

        String description
) {
}

