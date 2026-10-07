package com.solutis.projeto.taskman_backend.dto.task;

import com.solutis.projeto.taskman_backend.domain.enums.TaskPriority;
import com.solutis.projeto.taskman_backend.domain.enums.TaskStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TaskResponseDTO(
        UUID id,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDateTime dueDate,
        UUID parentId,
        List<TaskSummaryDTO> subtasks,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

