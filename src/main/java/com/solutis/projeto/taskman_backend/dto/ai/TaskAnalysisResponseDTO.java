package com.solutis.projeto.taskman_backend.dto.ai;

public record TaskAnalysisResponseDTO(
        String priority,
        String complexity,
        Integer estimatedHours,
        String reason
) {
}

