package com.solutis.projeto.taskman_backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solutis.projeto.taskman_backend.domain.entity.ChatMessage;
import com.solutis.projeto.taskman_backend.domain.entity.Task;
import com.solutis.projeto.taskman_backend.domain.entity.User;
import com.solutis.projeto.taskman_backend.domain.enums.MessageRole;
import com.solutis.projeto.taskman_backend.domain.enums.TaskPriority;
import com.solutis.projeto.taskman_backend.domain.enums.TaskStatus;
import com.solutis.projeto.taskman_backend.dto.ai.ChatPromptResponseDTO;
import com.solutis.projeto.taskman_backend.dto.ai.SubtaskItemDTO;
import com.solutis.projeto.taskman_backend.dto.ai.TaskAnalysisResponseDTO;
import com.solutis.projeto.taskman_backend.dto.ai.TaskDecompositionResponseDTO;
import com.solutis.projeto.taskman_backend.dto.ai.TaskImprovementResponseDTO;
import com.solutis.projeto.taskman_backend.dto.task.TaskResponseDTO;
import com.solutis.projeto.taskman_backend.exception.ResourceNotFoundException;
import com.solutis.projeto.taskman_backend.repository.ChatMessageRepository;
import com.solutis.projeto.taskman_backend.repository.TaskRepository;
import com.solutis.projeto.taskman_backend.service.AiService;
import com.solutis.projeto.taskman_backend.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Service
public class SpringAiServiceImpl implements AiService {

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${spring.ai.ollama.chat.options.model:llama3.2}")
    private String ollamaModel;

    private final TaskRepository taskRepository;
    private final TaskService taskService;
    private final ChatMessageRepository chatMessageRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public SpringAiServiceImpl(
            TaskRepository taskRepository,
            TaskService taskService,
            ChatMessageRepository chatMessageRepository
    ) {
        this.taskRepository = taskRepository;
        this.taskService = taskService;
        this.chatMessageRepository = chatMessageRepository;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public TaskImprovementResponseDTO improveTask(String title, String description, User user) {
        String systemPrompt = """
                Você é um especialista em produtividade e metodologia ágil.
                Sua tarefa é aprimorar o título e a descrição de uma tarefa, tornando-a clara, objetiva, acionável e técnica.
                Responda estritamente em formato JSON com as chaves: "title" e "description".
                Não inclua explicações fora do JSON.
                """;

        String userPrompt = String.format("""
                Título original: %s
                Descrição original: %s
                """, title, description != null ? description : "");

        try {
            String rawJson = callOllama(systemPrompt, userPrompt, true);
            return parseJsonOrFallback(rawJson, TaskImprovementResponseDTO.class, () ->
                    buildDefaultImprovement(title, description));
        } catch (Exception e) {
            log.warn("Falha na chamada ao Ollama para improveTask: {}. Aplicando fallback.", e.getMessage());
            return buildDefaultImprovement(title, description);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TaskAnalysisResponseDTO analyzeTask(UUID taskId, User user) {
        Task task = taskRepository.findByIdAndUserId(taskId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada com ID: " + taskId));

        String systemPrompt = """
                Você é um analista de projetos e engenheiro de software sênior. Avalie a tarefa e forneça uma estimativa técnica estruturada.
                Responda estritamente em formato JSON com as seguintes chaves:
                - "priority": prioridade sugerida ("LOW", "MEDIUM" ou "HIGH")
                - "complexity": nível de complexidade ("BAIXA", "MEDIA" ou "ALTA")
                - "estimatedHours": número inteiro com a estimativa de horas
                - "reason": justificativa técnica concisa e bem embasada
                Não adicione texto antes ou depois do JSON.
                """;

        String userPrompt = String.format("""
                Título da Tarefa: %s
                Descrição: %s
                Prioridade Atual: %s
                Prazo: %s
                """, task.getTitle(), task.getDescription(), task.getPriority(), task.getDueDate());

        try {
            String rawJson = callOllama(systemPrompt, userPrompt, true);
            return parseAnalysisJson(rawJson, task);
        } catch (Exception e) {
            log.warn("Falha na chamada ao Ollama para analyzeTask: {}. Aplicando fallback.", e.getMessage());
            return buildDefaultAnalysis(task);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TaskDecompositionResponseDTO decomposeTask(UUID taskId, User user) {
        Task task = taskRepository.findByIdAndUserId(taskId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada com ID: " + taskId));

        String systemPrompt = """
                Você é um arquiteto de software e Scrum Master experiente.
                Decomponha a tarefa principal em uma lista sequencial de 3 a 5 subtarefas práticas e bem definidas.
                Responda estritamente em formato JSON com o campo "subtasks", contendo uma lista de objetos com "title" e "description".
                Exemplo:
                {
                  "subtasks": [
                    { "title": "Passo 1", "description": "Descrição detalhada do passo 1" },
                    { "title": "Passo 2", "description": "Descrição detalhada do passo 2" }
                  ]
                }
                Não inclua explicações fora do JSON.
                """;

        String userPrompt = String.format("""
                Tarefa Principal: %s
                Descrição: %s
                """, task.getTitle(), task.getDescription() != null ? task.getDescription() : "");

        try {
            String rawJson = callOllama(systemPrompt, userPrompt, true);
            return parseDecompositionJson(rawJson, task);
        } catch (Exception e) {
            log.warn("Falha na chamada ao Ollama para decomposeTask: {}. Aplicando fallback.", e.getMessage());
            return buildDefaultDecomposition(task);
        }
    }

    @Override
    @Transactional
    public List<TaskResponseDTO> applySubtasks(UUID taskId, User user) {
        TaskDecompositionResponseDTO decomposition = decomposeTask(taskId, user);
        if (decomposition != null && decomposition.subtasks() != null) {
            return applyApprovedSubtasks(taskId, decomposition.subtasks(), user);
        }
        return List.of();
    }

    @Override
    @Transactional
    public List<TaskResponseDTO> applyApprovedSubtasks(UUID taskId, List<SubtaskItemDTO> approvedSubtasks, User user) {
        Task parent = taskRepository.findByIdAndUserId(taskId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa principal não encontrada com ID: " + taskId));

        List<TaskResponseDTO> createdSubtasks = new ArrayList<>();
        if (approvedSubtasks != null && !approvedSubtasks.isEmpty()) {
            for (SubtaskItemDTO item : approvedSubtasks) {
                Task subtask = Task.builder()
                        .title(item.title())
                        .description(item.description())
                        .status(TaskStatus.TODO)
                        .priority(parent.getPriority() != null ? parent.getPriority() : TaskPriority.MEDIUM)
                        .dueDate(parent.getDueDate())
                        .user(user)
                        .parentTask(parent)
                        .build();

                parent.addSubtask(subtask);
                Task saved = taskRepository.save(subtask);
                createdSubtasks.add(taskService.toResponseDTO(saved));
            }
        }

        return createdSubtasks;
    }

    @Override
    @Transactional
    public ChatPromptResponseDTO chat(UUID sessionId, String userMessage, User user) {
        String sessionKey = sessionId.toString();

        // 1. Salva mensagem do usuário
        chatMessageRepository.save(ChatMessage.builder()
                .sessionId(sessionKey)
                .role(MessageRole.USER)
                .content(userMessage)
                .user(user)
                .build());

        // 2. Coleta contexto das tarefas do usuário para o agente
        List<Task> userTasks = taskRepository.findByUserIdAndParentTaskIsNull(user.getId());
        long totalTasks = userTasks.size();
        long todoCount = userTasks.stream().filter(t -> t.getStatus() == TaskStatus.TODO).count();
        long inProgressCount = userTasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count();
        long doneCount = userTasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();

        StringBuilder tasksContext = new StringBuilder();
        tasksContext.append(String.format("Resumo do usuário %s: Total=%d, A Fazer=%d, Em Andamento=%d, Concluídas=%d.\n",
                user.getName(), totalTasks, todoCount, inProgressCount, doneCount));
        tasksContext.append("Lista de tarefas atuais do usuário:\n");
        for (Task t : userTasks) {
            tasksContext.append(String.format("- [%s] %s (Prioridade: %s, Prazo: %s)\n",
                    t.getStatus(), t.getTitle(), t.getPriority(), t.getDueDate() != null ? t.getDueDate() : "Não definido"));
        }

        String systemInstructions = String.format("""
                Você é o assistente inteligente oficial do Taskman.
                Seu objetivo é ajudar o usuário a planejar, priorizar e acompanhar suas tarefas.
                
                DADOS REAIS DAS TAREFAS DO USUÁRIO NO BANCO:
                %s
                
                REGRAS:
                1. NUNCA invente tarefas que não estão na lista acima.
                2. Quando o usuário perguntar sobre suas tarefas, prazos ou pendências, use os dados acima como verdade absoluta.
                3. Seja prestativo, claro, motivador e responda sempre em português.
                """, tasksContext);

        // 3. Monta mensagens de histórico para o Ollama
        List<ChatMessage> previousMessages = chatMessageRepository
                .findByUserIdAndSessionIdOrderByCreatedAtAsc(user.getId(), sessionKey);

        List<Map<String, String>> messagesPayload = new ArrayList<>();
        messagesPayload.add(Map.of("role", "system", "content", systemInstructions));

        // Pega as últimas 8 mensagens para manter a conversa fluida
        int startIdx = Math.max(0, previousMessages.size() - 8);
        for (int i = startIdx; i < previousMessages.size(); i++) {
            ChatMessage msg = previousMessages.get(i);
            String roleStr = msg.getRole() == MessageRole.USER ? "user" : "assistant";
            messagesPayload.add(Map.of("role", roleStr, "content", msg.getContent()));
        }

        String assistantResponse;
        try {
            assistantResponse = callOllamaWithMessages(messagesPayload, false);
            if (assistantResponse == null || assistantResponse.isBlank()) {
                assistantResponse = "Entendi sua mensagem. Como posso ajudar com suas tarefas hoje?";
            }
        } catch (Exception e) {
            log.warn("Falha no chat com Ollama: {}. Usando resposta assistida.", e.getMessage());
            assistantResponse = String.format("Olá, %s! No momento você tem %d tarefas no seu painel (%d a fazer e %d em andamento). O que gostaria de priorizar agora?",
                    user.getName(), totalTasks, todoCount, inProgressCount);
        }

        // 4. Salva resposta do assistente
        chatMessageRepository.save(ChatMessage.builder()
                .sessionId(sessionKey)
                .role(MessageRole.ASSISTANT)
                .content(assistantResponse)
                .user(user)
                .build());

        return new ChatPromptResponseDTO(sessionId, assistantResponse, LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessage> getChatHistory(UUID sessionId, User user) {
        return chatMessageRepository.findByUserIdAndSessionIdOrderByCreatedAtAsc(user.getId(), sessionId.toString());
    }

    private String callOllama(String systemPrompt, String userPrompt, boolean jsonFormat) throws Exception {
        List<Map<String, String>> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        messages.add(Map.of("role", "user", "content", userPrompt));
        return callOllamaWithMessages(messages, jsonFormat);
    }

    private String callOllamaWithMessages(List<Map<String, String>> messages, boolean jsonFormat) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", ollamaModel != null && !ollamaModel.isBlank() ? ollamaModel : "llama3.2");
        requestBody.put("stream", false);
        if (jsonFormat) {
            requestBody.put("format", "json");
        }
        requestBody.put("messages", messages);

        String jsonPayload = objectMapper.writeValueAsString(requestBody);

        String endpoint = ollamaBaseUrl != null && !ollamaBaseUrl.isBlank()
                ? (ollamaBaseUrl.endsWith("/") ? ollamaBaseUrl + "api/chat" : ollamaBaseUrl + "/api/chat")
                : "http://localhost:11434/api/chat";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                .build();

        log.info("Enviando requisição para Ollama: {} com modelo {}", endpoint, requestBody.get("model"));
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            JsonNode rootNode = objectMapper.readTree(response.body());
            JsonNode messageNode = rootNode.path("message");
            String content = messageNode.path("content").asText();
            log.info("Resposta recebida com sucesso do Ollama (tamanho: {} chars)", content.length());
            return content;
        } else {
            throw new RuntimeException("Ollama respondeu status " + response.statusCode() + ": " + response.body());
        }
    }

    private TaskAnalysisResponseDTO parseAnalysisJson(String rawJson, Task task) {
        try {
            String clean = extractCleanJson(rawJson);
            JsonNode root = objectMapper.readTree(clean);

            String priorityStr = root.path("priority").asText("MEDIUM").toUpperCase();
            if (!List.of("LOW", "MEDIUM", "HIGH").contains(priorityStr)) {
                priorityStr = task.getPriority() != null ? task.getPriority().name() : "MEDIUM";
            }

            String complexityStr = root.path("complexity").asText("MEDIA").toUpperCase();
            int hours = root.path("estimatedHours").asInt(4);
            if (hours <= 0) hours = 4;

            String reason = root.path("reason").asText("Análise técnica calculada pelo assistente local.");

            return new TaskAnalysisResponseDTO(priorityStr, complexityStr, hours, reason);
        } catch (Exception e) {
            log.warn("Erro ao fazer parse da análise do Ollama: {}. Usando fallback.", e.getMessage());
            return buildDefaultAnalysis(task);
        }
    }

    private TaskDecompositionResponseDTO parseDecompositionJson(String rawJson, Task task) {
        try {
            String clean = extractCleanJson(rawJson);
            JsonNode root = objectMapper.readTree(clean);

            List<SubtaskItemDTO> subtasks = new ArrayList<>();
            JsonNode arrayNode = root.has("subtasks") ? root.get("subtasks")
                    : (root.has("etapas") ? root.get("etapas")
                    : (root.has("tasks") ? root.get("tasks")
                    : (root.isArray() ? root : null)));

            if (arrayNode != null && arrayNode.isArray()) {
                for (JsonNode item : arrayNode) {
                    // Trata caso de arrays aninhados
                    if (item.has("subtasks") && item.get("subtasks").isArray()) {
                        for (JsonNode nested : item.get("subtasks")) {
                            String t = nested.path("title").asText(nested.path("titulo").asText("Etapa"));
                            String d = nested.path("description").asText(nested.path("descricao").asText(""));
                            subtasks.add(new SubtaskItemDTO(t, d));
                        }
                    } else {
                        String t = item.path("title").asText(item.path("titulo").asText(item.path("name").asText("Etapa")));
                        String d = item.path("description").asText(item.path("descricao").asText(""));
                        subtasks.add(new SubtaskItemDTO(t, d));
                    }
                }
            }

            if (!subtasks.isEmpty()) {
                return new TaskDecompositionResponseDTO(subtasks);
            }
        } catch (Exception e) {
            log.warn("Erro ao processar JSON de decomposição do Ollama: {}. Usando fallback.", e.getMessage());
        }

        return buildDefaultDecomposition(task);
    }

    private <T> T parseJsonOrFallback(String rawContent, Class<T> clazz, Supplier<T> fallbackSupplier) {
        if (rawContent == null || rawContent.isBlank()) {
            return fallbackSupplier.get();
        }
        try {
            String cleanJson = extractCleanJson(rawContent);
            return objectMapper.readValue(cleanJson, clazz);
        } catch (Exception e) {
            log.debug("Não foi possível parsear resposta do LLM: {}", e.getMessage());
            return fallbackSupplier.get();
        }
    }

    private String extractCleanJson(String rawContent) {
        String clean = rawContent.trim();
        if (clean.startsWith("```json")) {
            clean = clean.substring(7);
        } else if (clean.startsWith("```")) {
            clean = clean.substring(3);
        }
        if (clean.endsWith("```")) {
            clean = clean.substring(0, clean.length() - 3);
        }
        return clean.trim();
    }

    private TaskImprovementResponseDTO buildDefaultImprovement(String title, String description) {
        String improvedTitle = title.trim();
        if (!improvedTitle.endsWith(".")) {
            improvedTitle = "[Ação] " + improvedTitle;
        }
        String improvedDescription = (description != null && !description.isBlank())
                ? "Objetivo detalhado: " + description.trim() + "\nCritérios de aceitação: Definir escopo e validação técnica."
                : "Definir os requisitos e critérios de entrega para a conclusão bem-sucedida desta tarefa.";

        return new TaskImprovementResponseDTO(improvedTitle, improvedDescription);
    }

    private TaskAnalysisResponseDTO buildDefaultAnalysis(Task task) {
        TaskPriority priority = task.getPriority() != null ? task.getPriority() : TaskPriority.MEDIUM;
        String complexity = priority == TaskPriority.HIGH ? "ALTA" : (priority == TaskPriority.LOW ? "BAIXA" : "MEDIA");
        int hours = priority == TaskPriority.HIGH ? 8 : (priority == TaskPriority.LOW ? 2 : 4);
        String reason = "Estimativa calculada com base na prioridade " + priority + " e escopo da tarefa.";

        return new TaskAnalysisResponseDTO(priority.name(), complexity, hours, reason);
    }

    private TaskDecompositionResponseDTO buildDefaultDecomposition(Task task) {
        List<SubtaskItemDTO> subtasks = List.of(
                new SubtaskItemDTO("Planejamento e Requisitos", "Levantamento das necessidades e escopo para " + task.getTitle()),
                new SubtaskItemDTO("Execução e Implementação", "Desenvolvimento da atividade principal de " + task.getTitle()),
                new SubtaskItemDTO("Validação e Conclusão", "Testes, revisão e encerramento de " + task.getTitle())
        );
        return new TaskDecompositionResponseDTO(subtasks);
    }
}
