package com.solutis.projeto.taskman_backend.dto.task;

import com.solutis.projeto.taskman_backend.domain.enums.TaskPriority;
import com.solutis.projeto.taskman_backend.domain.enums.TaskStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record TaskSummaryDTO(
        UUID id,
        String title,
        TaskStatus status,
        TaskPriority priority,
        LocalDateTime dueDate,
        int subtaskCount,
        int completedSubtaskCount
) {
}

