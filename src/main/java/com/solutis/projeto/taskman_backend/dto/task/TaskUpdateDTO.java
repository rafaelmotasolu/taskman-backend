package com.solutis.projeto.taskman_backend.dto.task;

import com.solutis.projeto.taskman_backend.domain.enums.TaskPriority;
import com.solutis.projeto.taskman_backend.domain.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record TaskUpdateDTO(
        @NotBlank(message = "O título da tarefa é obrigatório")
        @Size(max = 200, message = "O título deve ter no máximo 200 caracteres")
        String title,

        String description,

        TaskStatus status,

        TaskPriority priority,

        LocalDateTime dueDate
) {
}

