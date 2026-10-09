package com.solutis.projeto.taskman_backend.service;

import com.solutis.projeto.taskman_backend.domain.entity.Task;
import com.solutis.projeto.taskman_backend.domain.entity.User;
import com.solutis.projeto.taskman_backend.domain.enums.TaskPriority;
import com.solutis.projeto.taskman_backend.domain.enums.TaskStatus;
import com.solutis.projeto.taskman_backend.dto.task.TaskCreateDTO;
import com.solutis.projeto.taskman_backend.dto.task.TaskDashboardDTO;
import com.solutis.projeto.taskman_backend.dto.task.TaskResponseDTO;
import com.solutis.projeto.taskman_backend.dto.task.TaskStatusUpdateDTO;
import com.solutis.projeto.taskman_backend.dto.task.TaskSummaryDTO;
import com.solutis.projeto.taskman_backend.dto.task.TaskUpdateDTO;
import com.solutis.projeto.taskman_backend.exception.ResourceNotFoundException;
import com.solutis.projeto.taskman_backend.repository.TaskRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    @Transactional
    public TaskResponseDTO createTask(TaskCreateDTO dto, User currentUser) {
        Task task = Task.builder()
                .title(dto.title())
                .description(dto.description())
                .status(TaskStatus.TODO)
                .priority(dto.priority() != null ? dto.priority() : TaskPriority.MEDIUM)
                .dueDate(dto.dueDate())
                .user(currentUser)
                .parentTask(null)
                .build();

        Task saved = taskRepository.save(task);
        return toResponseDTO(saved);
    }

    @Transactional
    public TaskResponseDTO createSubtask(UUID parentId, TaskCreateDTO dto, User currentUser) {
        Task parent = taskRepository.findByIdAndUserId(parentId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa pai não encontrada com o ID: " + parentId));

        Task subtask = Task.builder()
                .title(dto.title())
                .description(dto.description())
                .status(TaskStatus.TODO)
                .priority(dto.priority() != null ? dto.priority() : parent.getPriority())
                .dueDate(dto.dueDate())
                .user(currentUser)
                .parentTask(parent)
                .build();

        parent.addSubtask(subtask);
        Task saved = taskRepository.save(subtask);
        return toResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public Page<TaskSummaryDTO> listTasks(
            User currentUser,
            TaskStatus status,
            TaskPriority priority,
            Boolean rootOnly,
            Pageable pageable
    ) {
        Specification<Task> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("user").get("id"), currentUser.getId()));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }

            if (Boolean.TRUE.equals(rootOnly)) {
                predicates.add(cb.isNull(root.get("parentTask")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return taskRepository.findAll(spec, pageable).map(this::toSummaryDTO);
    }

    @Transactional(readOnly = true)
    public TaskResponseDTO getTaskById(UUID id, User currentUser) {
        Task task = taskRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada com o ID: " + id));

        return toResponseDTO(task);
    }

    @Transactional
    public TaskResponseDTO updateTask(UUID id, TaskUpdateDTO dto, User currentUser) {
        Task task = taskRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada com o ID: " + id));

        task.setTitle(dto.title());
        task.setDescription(dto.description());
        if (dto.status() != null) {
            validateAndHandleSubtaskCompletion(task, dto.status(), dto.completeSubtasks());
            task.setStatus(dto.status());
        }
        if (dto.priority() != null) {
            task.setPriority(dto.priority());
        }
        task.setDueDate(dto.dueDate());

        Task updated = taskRepository.save(task);
        return toResponseDTO(updated);
    }

    @Transactional
    public TaskResponseDTO updateTaskStatus(UUID id, TaskStatusUpdateDTO dto, User currentUser) {
        Task task = taskRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada com o ID: " + id));

        validateAndHandleSubtaskCompletion(task, dto.status(), dto.completeSubtasks());
        task.setStatus(dto.status());
        Task updated = taskRepository.save(task);
        return toResponseDTO(updated);
    }

    private void validateAndHandleSubtaskCompletion(Task task, TaskStatus newStatus, Boolean completeSubtasks) {
        if (newStatus == TaskStatus.DONE && task.getSubtasks() != null && !task.getSubtasks().isEmpty()) {
            boolean hasPendingSubtasks = task.getSubtasks().stream()
                    .anyMatch(s -> s.getStatus() != TaskStatus.DONE);

            if (hasPendingSubtasks) {
                if (Boolean.TRUE.equals(completeSubtasks)) {
                    for (Task subtask : task.getSubtasks()) {
                        subtask.setStatus(TaskStatus.DONE);
                    }
                } else {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Não é possível concluir a tarefa pois existem subtarefas pendentes."
                    );
                }
            }
        }
    }

    @Transactional
    public void deleteTask(UUID id, User currentUser) {
        Task task = taskRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada com o ID: " + id));

        if (task.getParentTask() != null) {
            task.getParentTask().removeSubtask(task);
        }
        taskRepository.delete(task);
    }

    @Transactional
    public void deleteSubtask(UUID parentId, UUID subtaskId, User currentUser) {
        Task parent = taskRepository.findByIdAndUserId(parentId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa principal não encontrada com o ID: " + parentId));

        Task subtask = taskRepository.findByIdAndUserId(subtaskId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Subtarefa não encontrada com o ID: " + subtaskId));

        if (subtask.getParentTask() == null || !subtask.getParentTask().getId().equals(parent.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A subtarefa não pertence à tarefa informada.");
        }

        parent.removeSubtask(subtask);
        taskRepository.delete(subtask);
    }

    @Transactional(readOnly = true)
    public TaskDashboardDTO getDashboardMetrics(User currentUser) {
        UUID userId = currentUser.getId();
        long total = taskRepository.countByUserId(userId);
        long todo = taskRepository.countByUserIdAndStatus(userId, TaskStatus.TODO);
        long inProgress = taskRepository.countByUserIdAndStatus(userId, TaskStatus.IN_PROGRESS);
        long done = taskRepository.countByUserIdAndStatus(userId, TaskStatus.DONE);
        long highPriority = taskRepository.countByUserIdAndPriority(userId, TaskPriority.HIGH);
        long overdue = taskRepository.countOverdueTasks(userId, LocalDateTime.now(), TaskStatus.DONE);

        return new TaskDashboardDTO(total, todo, inProgress, done, highPriority, overdue);
    }

    public TaskResponseDTO toResponseDTO(Task task) {
        List<TaskSummaryDTO> subtaskSummaries = task.getSubtasks() != null
                ? task.getSubtasks().stream().map(this::toSummaryDTO).toList()
                : List.of();

        return new TaskResponseDTO(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getParentTask() != null ? task.getParentTask().getId() : null,
                subtaskSummaries,
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }

    public TaskSummaryDTO toSummaryDTO(Task task) {
        int subtaskCount = task.getSubtasks() != null ? task.getSubtasks().size() : 0;
        int completedCount = task.getSubtasks() != null
                ? (int) task.getSubtasks().stream().filter(s -> s.getStatus() == TaskStatus.DONE).count()
                : 0;

        return new TaskSummaryDTO(
                task.getId(),
                task.getTitle(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                subtaskCount,
                completedCount
        );
    }
}

