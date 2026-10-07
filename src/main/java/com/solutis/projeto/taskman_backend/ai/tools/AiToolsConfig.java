package com.solutis.projeto.taskman_backend.ai.tools;

import com.solutis.projeto.taskman_backend.domain.entity.User;
import com.solutis.projeto.taskman_backend.domain.enums.TaskPriority;
import com.solutis.projeto.taskman_backend.domain.enums.TaskStatus;
import com.solutis.projeto.taskman_backend.dto.task.TaskSummaryDTO;
import com.solutis.projeto.taskman_backend.repository.TaskRepository;
import com.solutis.projeto.taskman_backend.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Configuration
@RequiredArgsConstructor
public class AiToolsConfig {

    private final TaskRepository taskRepository;
    private final TaskService taskService;

    public record EmptyRequest() {
    }

    public record PriorityRequest(TaskPriority priority) {
    }

    public record TaskSearchRequest(String identifierOrTitle) {
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }

    @Bean
    @Description("Retorna a lista de tarefas pendentes do usuário logado (status diferente de DONE)")
    public Function<EmptyRequest, List<TaskSummaryDTO>> getPendingTasksFunction() {
        return request -> {
            User user = getAuthenticatedUser();
            if (user == null) return List.of();
            return taskRepository.findByUserIdAndStatusNot(user.getId(), TaskStatus.DONE)
                    .stream()
                    .map(taskService::toSummaryDTO)
                    .toList();
        };
    }

    @Bean
    @Description("Retorna as tarefas com prazo expirado e não concluídas do usuário logado")
    public Function<EmptyRequest, List<TaskSummaryDTO>> getOverdueTasksFunction() {
        return request -> {
            User user = getAuthenticatedUser();
            if (user == null) return List.of();
            return taskRepository.findOverdueTasks(user.getId(), LocalDateTime.now(), TaskStatus.DONE)
                    .stream()
                    .map(taskService::toSummaryDTO)
                    .toList();
        };
    }

    @Bean
    @Description("Filtra as tarefas do usuário logado pela prioridade informada (LOW, MEDIUM, HIGH)")
    public Function<PriorityRequest, List<TaskSummaryDTO>> getTasksByPriorityFunction() {
        return request -> {
            User user = getAuthenticatedUser();
            if (user == null || request.priority() == null) return List.of();
            return taskRepository.findAll((root, query, cb) -> cb.and(
                    cb.equal(root.get("user").get("id"), user.getId()),
                    cb.equal(root.get("priority"), request.priority())
            )).stream().map(taskService::toSummaryDTO).toList();
        };
    }

    @Bean
    @Description("Busca tarefas do usuário logado por UUID exato ou por termo contido no título")
    public Function<TaskSearchRequest, List<TaskSummaryDTO>> getTaskDetailsFunction() {
        return request -> {
            User user = getAuthenticatedUser();
            if (user == null || request.identifierOrTitle() == null || request.identifierOrTitle().isBlank()) {
                return List.of();
            }
            String term = request.identifierOrTitle().trim();

            try {
                UUID id = UUID.fromString(term);
                return taskRepository.findByIdAndUserId(id, user.getId())
                        .map(taskService::toSummaryDTO)
                        .map(List::of)
                        .orElse(List.of());
            } catch (IllegalArgumentException e) {
                return taskRepository.findAll((root, query, cb) -> cb.and(
                        cb.equal(root.get("user").get("id"), user.getId()),
                        cb.like(cb.lower(root.get("title")), "%" + term.toLowerCase() + "%")
                )).stream().map(taskService::toSummaryDTO).toList();
            }
        };
    }
}

