package com.solutis.projeto.taskman_backend.controller;

import com.solutis.projeto.taskman_backend.domain.entity.ChatMessage;
import com.solutis.projeto.taskman_backend.domain.entity.User;
import com.solutis.projeto.taskman_backend.dto.ai.ChatPromptRequestDTO;
import com.solutis.projeto.taskman_backend.dto.ai.ChatPromptResponseDTO;
import com.solutis.projeto.taskman_backend.dto.ai.TaskAnalysisResponseDTO;
import com.solutis.projeto.taskman_backend.dto.ai.TaskDecompositionResponseDTO;
import com.solutis.projeto.taskman_backend.dto.ai.TaskImproveRequestDTO;
import com.solutis.projeto.taskman_backend.dto.ai.TaskImprovementResponseDTO;
import com.solutis.projeto.taskman_backend.dto.task.TaskResponseDTO;
import com.solutis.projeto.taskman_backend.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Tag(name = "Inteligência Artificial", description = "Endpoints com IA para análise, aprimoramento, decomposição e assistente conversacional")
public class AiTaskController {

    private final AiService aiService;

    @PostMapping("/tasks/improve")
    @Operation(summary = "Aprimorar título e descrição de uma tarefa com IA")
    public ResponseEntity<TaskImprovementResponseDTO> improveTask(
            @Valid @RequestBody TaskImproveRequestDTO request,
            @AuthenticationPrincipal User currentUser
    ) {
        TaskImprovementResponseDTO response = aiService.improveTask(request.title(), request.description(), currentUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/tasks/{id}/analyze")
    @Operation(summary = "Analisar complexidade, prioridade e esforço de uma tarefa existente")
    public ResponseEntity<TaskAnalysisResponseDTO> analyzeTask(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        TaskAnalysisResponseDTO response = aiService.analyzeTask(id, currentUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/tasks/{id}/decompose")
    @Operation(summary = "Decompor tarefa existente em lista de subtarefas")
    public ResponseEntity<TaskDecompositionResponseDTO> decomposeTask(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        TaskDecompositionResponseDTO response = aiService.decomposeTask(id, currentUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/tasks/{id}/apply-subtasks")
    @Operation(summary = "Decompor tarefa e persistir as subtarefas geradas diretamente no banco")
    public ResponseEntity<List<TaskResponseDTO>> applySubtasks(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        List<TaskResponseDTO> createdSubtasks = aiService.applySubtasks(id, currentUser);
        return ResponseEntity.ok(createdSubtasks);
    }

    @PostMapping("/chat")
    @Operation(summary = "Conversar com o assistente inteligente usando histórico e ferramentas de tarefas")
    public ResponseEntity<ChatPromptResponseDTO> chat(
            @Valid @RequestBody ChatPromptRequestDTO request,
            @AuthenticationPrincipal User currentUser
    ) {
        ChatPromptResponseDTO response = aiService.chat(request.sessionId(), request.message(), currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/chat/{sessionId}/history")
    @Operation(summary = "Recuperar histórico de mensagens de uma sessão de chat")
    public ResponseEntity<List<ChatMessage>> getChatHistory(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal User currentUser
    ) {
        List<ChatMessage> history = aiService.getChatHistory(sessionId, currentUser);
        return ResponseEntity.ok(history);
    }
}

