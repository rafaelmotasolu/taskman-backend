package com.solutis.projeto.taskman_backend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solutis.projeto.taskman_backend.domain.entity.ChatMessage;
import com.solutis.projeto.taskman_backend.domain.entity.Task;
import com.solutis.projeto.taskman_backend.domain.entity.User;
import com.solutis.projeto.taskman_backend.domain.enums.MessageRole;
import com.solutis.projeto.taskman_backend.domain.enums.TaskPriority;
import com.solutis.projeto.taskman_backend.dto.ai.ChatPromptResponseDTO;
import com.solutis.projeto.taskman_backend.dto.ai.SubtaskItemDTO;
import com.solutis.projeto.taskman_backend.dto.ai.TaskAnalysisResponseDTO;
import com.solutis.projeto.taskman_backend.dto.ai.TaskDecompositionResponseDTO;
import com.solutis.projeto.taskman_backend.dto.ai.TaskImprovementResponseDTO;
import com.solutis.projeto.taskman_backend.dto.task.TaskCreateDTO;
import com.solutis.projeto.taskman_backend.dto.task.TaskResponseDTO;
import com.solutis.projeto.taskman_backend.exception.ResourceNotFoundException;
import com.solutis.projeto.taskman_backend.repository.ChatMessageRepository;
import com.solutis.projeto.taskman_backend.repository.TaskRepository;
import com.solutis.projeto.taskman_backend.service.AiService;
import com.solutis.projeto.taskman_backend.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Service
public class SpringAiServiceImpl implements AiService {

    private final ChatClient chatClient;
    private final TaskRepository taskRepository;
    private final TaskService taskService;
    private final ChatMessageRepository chatMessageRepository;
    private final ObjectMapper objectMapper;

    public SpringAiServiceImpl(
            ChatModel chatModel,
            TaskRepository taskRepository,
            TaskService taskService,
            ChatMessageRepository chatMessageRepository
    ) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.taskRepository = taskRepository;
        this.taskService = taskService;
        this.chatMessageRepository = chatMessageRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public TaskImprovementResponseDTO improveTask(String title, String description, User user) {
        String systemPrompt = """
                Você é um especialista em produtividade e metodologia ágil.
                Sua tarefa é aprimorar o título e a descrição de uma tarefa, tornando-a clara, objetiva, acionável e técnica.
                Responda estritamente em formato JSON com as chaves: "title" e "description".
                """;

        String userPrompt = String.format("""
                Título original: %s
                Descrição original: %s
                """, title, description != null ? description : "");

        try {
            String rawContent = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();

            return parseJsonOrFallback(rawContent, TaskImprovementResponseDTO.class, () ->
                    buildDefaultImprovement(title, description));
        } catch (Exception e) {
            log.warn("Falha na chamada ao LLM para improveTask, aplicando fallback: {}", e.getMessage());
            return buildDefaultImprovement(title, description);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TaskAnalysisResponseDTO analyzeTask(UUID taskId, User user) {
        Task task = taskRepository.findByIdAndUserId(taskId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada com ID: " + taskId));

        String systemPrompt = """
                Você é um analista de projetos experiente. Avalie a tarefa e forneça uma estimativa técnica estruturada.
                Responda em formato JSON com as seguintes chaves:
                - "priority": prioridade sugerida ("LOW", "MEDIUM" ou "HIGH")
                - "complexity": complexidade ("BAIXA", "MEDIA" ou "ALTA")
                - "estimatedHours": número inteiro com a estimativa de horas para conclusão
                - "reason": justificativa concisa da análise técnica
                """;

        String userPrompt = String.format("""
                Título: %s
                Descrição: %s
                Prioridade Atual: %s
                Prazo: %s
                """, task.getTitle(), task.getDescription(), task.getPriority(), task.getDueDate());

        try {
            String rawContent = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();

            return parseJsonOrFallback(rawContent, TaskAnalysisResponseDTO.class, () ->
                    buildDefaultAnalysis(task));
        } catch (Exception e) {
            log.warn("Falha na chamada ao LLM para analyzeTask, aplicando fallback: {}", e.getMessage());
            return buildDefaultAnalysis(task);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TaskDecompositionResponseDTO decomposeTask(UUID taskId, User user) {
        Task task = taskRepository.findByIdAndUserId(taskId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada com ID: " + taskId));

        String systemPrompt = """
                Você é um arquiteto de software e gerente ágil. Decomponha a tarefa principal em uma lista sequencial de 3 a 5 subtarefas práticas e bem definidas.
                Responda estritamente em formato JSON com o campo "subtasks", contendo uma lista de objetos com "title" e "description".
                """;

        String userPrompt = String.format("""
                Tarefa Principal: %s
                Descrição: %s
                """, task.getTitle(), task.getDescription());

        try {
            String rawContent = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();

            return parseJsonOrFallback(rawContent, TaskDecompositionResponseDTO.class, () ->
                    buildDefaultDecomposition(task));
        } catch (Exception e) {
            log.warn("Falha na chamada ao LLM para decomposeTask, aplicando fallback: {}", e.getMessage());
            return buildDefaultDecomposition(task);
        }
    }

    @Override
    @Transactional
    public List<TaskResponseDTO> applySubtasks(UUID taskId, User user) {
        Task parent = taskRepository.findByIdAndUserId(taskId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa principal não encontrada com ID: " + taskId));

        TaskDecompositionResponseDTO decomposition = decomposeTask(taskId, user);

        List<TaskResponseDTO> createdSubtasks = new ArrayList<>();
        if (decomposition != null && decomposition.subtasks() != null) {
            for (SubtaskItemDTO item : decomposition.subtasks()) {
                TaskCreateDTO subtaskDTO = new TaskCreateDTO(
                        item.title(),
                        item.description(),
                        parent.getPriority(),
                        parent.getDueDate()
                );
                createdSubtasks.add(taskService.createSubtask(parent.getId(), subtaskDTO, user));
            }
        }

        return createdSubtasks;
    }

    @Override
    @Transactional
    public ChatPromptResponseDTO chat(UUID sessionId, String userMessage, User user) {
        String sessionKey = sessionId.toString();

        // 1. Salva a mensagem do usuário no histórico
        chatMessageRepository.save(ChatMessage.builder()
                .sessionId(sessionKey)
                .role(MessageRole.USER)
                .content(userMessage)
                .user(user)
                .build());

        // 2. Recupera o histórico anterior
        List<ChatMessage> previousMessages = chatMessageRepository
                .findByUserIdAndSessionIdOrderByCreatedAtAsc(user.getId(), sessionKey);

        List<Message> historyPromptMessages = new ArrayList<>();
        for (ChatMessage msg : previousMessages) {
            if (msg.getRole() == MessageRole.USER) {
                historyPromptMessages.add(new UserMessage(msg.getContent()));
            } else if (msg.getRole() == MessageRole.ASSISTANT) {
                historyPromptMessages.add(new AssistantMessage(msg.getContent()));
            }
        }

        String systemInstructions = """
                Você é o assistente inteligente oficial do Taskman.
                Seu objetivo é ajudar o usuário a planejar, priorizar e acompanhar suas tarefas diárias.
                REGRAS OBRIGATÓRIAS:
                1. NUNCA invente ou alucine tarefas que o usuário não possui.
                2. Quando o usuário fizer perguntas sobre suas tarefas, pendências ou prazos, invoque as ferramentas disponíveis (getPendingTasksFunction, getOverdueTasksFunction, getTasksByPriorityFunction, getTaskDetailsFunction) para consultar os dados reais antes de responder.
                3. Seja prestativo, claro, empático e responda sempre em português.
                """;

        String assistantResponse;
        try {
            assistantResponse = chatClient.prompt()
                    .system(systemInstructions)
                    .messages(historyPromptMessages)
                    .tools(
                            "getPendingTasksFunction",
                            "getOverdueTasksFunction",
                            "getTasksByPriorityFunction",
                            "getTaskDetailsFunction"
                    )
                    .call()
                    .content();

            if (assistantResponse == null || assistantResponse.isBlank()) {
                assistantResponse = "Entendi sua mensagem. Como posso ajudar com suas tarefas hoje?";
            }
        } catch (Exception e) {
            log.warn("Falha no processamento do chat com Spring AI: {}", e.getMessage());
            assistantResponse = "O assistente de IA está temporariamente indisponível no momento. Suas tarefas continuam seguras e você pode consultá-las diretamente pelo painel.";
        }

        // 3. Salva a resposta do assistente no histórico
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

    private <T> T parseJsonOrFallback(String rawContent, Class<T> clazz, Supplier<T> fallbackSupplier) {
        if (rawContent == null || rawContent.isBlank()) {
            return fallbackSupplier.get();
        }
        try {
            String cleanJson = rawContent.trim();
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.substring(7);
            } else if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.substring(3);
            }
            if (cleanJson.endsWith("```")) {
                cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
            }
            cleanJson = cleanJson.trim();
            return objectMapper.readValue(cleanJson, clazz);
        } catch (Exception e) {
            log.debug("Não foi possível parsear resposta do LLM como JSON: {}", e.getMessage());
            return fallbackSupplier.get();
        }
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

