package com.solutis.projeto.taskman_backend.dto.ai;

import java.util.List;

public record TaskDecompositionResponseDTO(
        List<SubtaskItemDTO> subtasks
) {
}

