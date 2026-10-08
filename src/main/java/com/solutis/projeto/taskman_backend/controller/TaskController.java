package com.solutis.projeto.taskman_backend.controller;

import com.solutis.projeto.taskman_backend.domain.entity.User;
import com.solutis.projeto.taskman_backend.domain.enums.TaskPriority;
import com.solutis.projeto.taskman_backend.domain.enums.TaskStatus;
import com.solutis.projeto.taskman_backend.dto.task.TaskCreateDTO;
import com.solutis.projeto.taskman_backend.dto.task.TaskDashboardDTO;
import com.solutis.projeto.taskman_backend.dto.task.TaskResponseDTO;
import com.solutis.projeto.taskman_backend.dto.task.TaskStatusUpdateDTO;
import com.solutis.projeto.taskman_backend.dto.task.TaskSummaryDTO;
import com.solutis.projeto.taskman_backend.dto.task.TaskUpdateDTO;
import com.solutis.projeto.taskman_backend.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@Tag(name = "Tarefas", description = "Endpoints para gerenciamento de tarefas e subtarefas")
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @Operation(summary = "Criar nova tarefa raiz")
    public ResponseEntity<TaskResponseDTO> createTask(
            @Valid @RequestBody TaskCreateDTO dto,
            @AuthenticationPrincipal User currentUser
    ) {
        TaskResponseDTO created = taskService.createTask(dto, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{id}/subtasks")
    @Operation(summary = "Criar subtarefa vinculada a uma tarefa pai existente")
    public ResponseEntity<TaskResponseDTO> createSubtask(
            @PathVariable("id") UUID id,
            @Valid @RequestBody TaskCreateDTO dto,
            @AuthenticationPrincipal User currentUser
    ) {
        TaskResponseDTO created = taskService.createSubtask(id, dto, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "Listar tarefas do usuário com filtros e paginação")
    public ResponseEntity<Page<TaskSummaryDTO>> listTasks(
            @RequestParam(name = "status", required = false) TaskStatus status,
            @RequestParam(name = "priority", required = false) TaskPriority priority,
            @RequestParam(name = "rootOnly", required = false) Boolean rootOnly,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal User currentUser
    ) {
        Page<TaskSummaryDTO> tasks = taskService.listTasks(currentUser, status, priority, rootOnly, pageable);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter detalhes de uma tarefa por ID")
    public ResponseEntity<TaskResponseDTO> getTaskById(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        TaskResponseDTO task = taskService.getTaskById(id, currentUser);
        return ResponseEntity.ok(task);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar dados cadastrais da tarefa")
    public ResponseEntity<TaskResponseDTO> updateTask(
            @PathVariable("id") UUID id,
            @Valid @RequestBody TaskUpdateDTO dto,
            @AuthenticationPrincipal User currentUser
    ) {
        TaskResponseDTO updated = taskService.updateTask(id, dto, currentUser);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Atualizar apenas o status da tarefa")
    public ResponseEntity<TaskResponseDTO> updateTaskStatus(
            @PathVariable("id") UUID id,
            @Valid @RequestBody TaskStatusUpdateDTO dto,
            @AuthenticationPrincipal User currentUser
    ) {
        TaskResponseDTO updated = taskService.updateTaskStatus(id, dto, currentUser);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir tarefa e suas etapas em cascata")
    public ResponseEntity<Void> deleteTask(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        taskService.deleteTask(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Obter métricas e estatísticas das tarefas do usuário")
    public ResponseEntity<TaskDashboardDTO> getDashboard(
            @AuthenticationPrincipal User currentUser
    ) {
        TaskDashboardDTO dashboard = taskService.getDashboardMetrics(currentUser);
        return ResponseEntity.ok(dashboard);
    }
}
