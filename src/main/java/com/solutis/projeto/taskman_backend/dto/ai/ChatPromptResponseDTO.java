package com.solutis.projeto.taskman_backend.dto.ai;

import com.solutis.projeto.taskman_backend.dto.task.TaskResponseDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ChatPromptResponseDTO(
        UUID sessionId,
        String message,
        LocalDateTime timestamp,
        List<TaskResponseDTO> createdTasks
) {
    public ChatPromptResponseDTO(UUID sessionId, String message, LocalDateTime timestamp) {
        this(sessionId, message, timestamp, List.of());
    }
}

