package com.solutis.projeto.taskman_backend.dto.task;

import com.solutis.projeto.taskman_backend.domain.enums.TaskPriority;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record TaskCreateDTO(
        @NotBlank(message = "O título da tarefa é obrigatório")
        @Size(max = 200, message = "O título deve ter no máximo 200 caracteres")
        String title,

        String description,

        TaskPriority priority,

        @FutureOrPresent(message = "O prazo não pode ser uma data no passado")
        LocalDateTime dueDate
) {
}

