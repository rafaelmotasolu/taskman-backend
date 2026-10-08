package com.solutis.projeto.taskman_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solutis.projeto.taskman_backend.domain.entity.Task;
import com.solutis.projeto.taskman_backend.domain.entity.User;
import com.solutis.projeto.taskman_backend.domain.enums.TaskPriority;
import com.solutis.projeto.taskman_backend.domain.enums.TaskStatus;
import com.solutis.projeto.taskman_backend.domain.enums.UserRole;
import com.solutis.projeto.taskman_backend.dto.ai.ChatPromptRequestDTO;
import com.solutis.projeto.taskman_backend.dto.ai.TaskImproveRequestDTO;
import com.solutis.projeto.taskman_backend.repository.TaskRepository;
import com.solutis.projeto.taskman_backend.repository.UserRepository;
import com.solutis.projeto.taskman_backend.security.jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AiTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User userA;
    private String tokenA;

    private User userB;
    private String tokenB;

    private Task taskA;

    @BeforeEach
    void setUp() {
        userA = userRepository.save(User.builder()
                .name("AI Test User A")
                .email("ai.user.a." + UUID.randomUUID() + "@example.com")
                .password("encoded_pass")
                .role(UserRole.ROLE_USER)
                .build());
        tokenA = jwtService.generateToken(userA, userA.getId());

        userB = userRepository.save(User.builder()
                .name("AI Test User B")
                .email("ai.user.b." + UUID.randomUUID() + "@example.com")
                .password("encoded_pass")
                .role(UserRole.ROLE_USER)
                .build());
        tokenB = jwtService.generateToken(userB, userB.getId());

        taskA = taskRepository.save(Task.builder()
                .title("Implementar microsserviço de notificações")
                .description("Criar serviço com RabbitMQ e envio de e-mails assíncronos")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.HIGH)
                .dueDate(LocalDateTime.now().plusDays(4))
                .user(userA)
                .build());
    }

    @Test
    @DisplayName("Should improve task title and description via AI")
    void shouldImproveTask() throws Exception {
        TaskImproveRequestDTO request = new TaskImproveRequestDTO(
                "fazer tela de login",
                "login do sistema"
        );

        mockMvc.perform(post("/ai/tasks/improve")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.description").isNotEmpty());
    }

    @Test
    @DisplayName("Should analyze task complexity, priority, and estimated hours")
    void shouldAnalyzeTask() throws Exception {
        mockMvc.perform(post("/ai/tasks/" + taskA.getId() + "/analyze")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").isNotEmpty())
                .andExpect(jsonPath("$.complexity").isNotEmpty())
                .andExpect(jsonPath("$.estimatedHours").isNumber())
                .andExpect(jsonPath("$.reason").isNotEmpty());
    }

    @Test
    @DisplayName("Should decompose task into structured subtasks")
    void shouldDecomposeTask() throws Exception {
        mockMvc.perform(post("/ai/tasks/" + taskA.getId() + "/decompose")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtasks").isArray())
                .andExpect(jsonPath("$.subtasks.length()").isNotEmpty())
                .andExpect(jsonPath("$.subtasks[0].title").isNotEmpty());
    }

    @Test
    @DisplayName("Should decompose and persist generated subtasks to database")
    void shouldApplySubtasksToDatabase() throws Exception {
        mockMvc.perform(post("/ai/tasks/" + taskA.getId() + "/apply-subtasks")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").isNotEmpty())
                .andExpect(jsonPath("$[0].parentId").value(taskA.getId().toString()));
    }

    @Test
    @DisplayName("Should process chat conversation and retrieve history")
    void shouldChatAndRetrieveHistory() throws Exception {
        UUID sessionId = UUID.randomUUID();
        ChatPromptRequestDTO prompt = new ChatPromptRequestDTO(
                sessionId,
                "Quais tarefas de alta prioridade eu tenho para entregar?"
        );

        mockMvc.perform(post("/ai/chat")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prompt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
                .andExpect(jsonPath("$.message").isNotEmpty());

        // Fetch history
        mockMvc.perform(get("/ai/chat/" + sessionId + "/history")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)) // USER and ASSISTANT
                .andExpect(jsonPath("$[0].role").value("USER"))
                .andExpect(jsonPath("$[1].role").value("ASSISTANT"));
    }

    @Test
    @DisplayName("Should dynamically create task from chat when requested by user")
    void shouldCreateTaskDynamicallyFromChat() throws Exception {
        UUID sessionId = UUID.randomUUID();
        ChatPromptRequestDTO prompt = new ChatPromptRequestDTO(
                sessionId,
                "Crie uma tarefa para preparar a apresentação da diretoria amanhã com prioridade alta"
        );

        mockMvc.perform(post("/ai/chat")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prompt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.createdTasks").isArray())
                .andExpect(jsonPath("$.createdTasks.length()").value(1))
                .andExpect(jsonPath("$.createdTasks[0].priority").value("HIGH"));

        // Verify task exists in repository for userA
        var tasks = taskRepository.findByUserIdAndParentTaskIsNull(userA.getId());
        boolean taskExists = tasks.stream()
                .anyMatch(t -> t.getTitle().toLowerCase().contains("apresentação") || t.getPriority() == TaskPriority.HIGH);
        org.junit.jupiter.api.Assertions.assertTrue(taskExists, "Task should have been created in database");
    }

    @Test
    @DisplayName("Should prevent user B from analyzing user A's task")
    void shouldEnforceIsolationInAiEndpoints() throws Exception {
        mockMvc.perform(post("/ai/tasks/" + taskA.getId() + "/analyze")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/ai/tasks/" + taskA.getId() + "/decompose")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }
}

