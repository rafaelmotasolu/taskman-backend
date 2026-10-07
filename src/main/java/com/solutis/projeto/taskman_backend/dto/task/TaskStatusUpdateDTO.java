package com.solutis.projeto.taskman_backend.dto.task;

import com.solutis.projeto.taskman_backend.domain.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record TaskStatusUpdateDTO(
        @NotNull(message = "O status da tarefa é obrigatório")
        TaskStatus status
) {
}

