package com.solutis.projeto.taskman_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.solutis.projeto.taskman_backend.domain.entity.User;
import com.solutis.projeto.taskman_backend.domain.enums.TaskPriority;
import com.solutis.projeto.taskman_backend.domain.enums.TaskStatus;
import com.solutis.projeto.taskman_backend.domain.enums.UserRole;
import com.solutis.projeto.taskman_backend.dto.task.TaskCreateDTO;
import com.solutis.projeto.taskman_backend.dto.task.TaskStatusUpdateDTO;
import com.solutis.projeto.taskman_backend.dto.task.TaskUpdateDTO;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private User userA;
    private String tokenA;

    private User userB;
    private String tokenB;

    @BeforeEach
    void setUp() {
        userA = userRepository.save(User.builder()
                .name("User A")
                .email("usera." + UUID.randomUUID() + "@example.com")
                .password("encoded_pass")
                .role(UserRole.ROLE_USER)
                .build());
        tokenA = jwtService.generateToken(userA, userA.getId());

        userB = userRepository.save(User.builder()
                .name("User B")
                .email("userb." + UUID.randomUUID() + "@example.com")
                .password("encoded_pass")
                .role(UserRole.ROLE_USER)
                .build());
        tokenB = jwtService.generateToken(userB, userB.getId());
    }

    @Test
    @DisplayName("Should create root task, add subtask, list, update status, and fetch dashboard")
    void shouldExecuteCompleteTaskLifecycle() throws Exception {
        // 1. Create root task
        TaskCreateDTO createRoot = new TaskCreateDTO(
                "Projeto Backend Taskman",
                "Construir toda a infraestrutura e CRUD",
                TaskPriority.HIGH,
                LocalDateTime.now().plusDays(7)
        );

        MvcResult createResult = mockMvc.perform(post("/tasks")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRoot)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Projeto Backend Taskman"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andReturn();

        String rootId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        // 2. Create subtask
        TaskCreateDTO createSub = new TaskCreateDTO(
                "Subtarefa - Autenticação JWT",
                "Implementar filtros e endpoints de auth",
                TaskPriority.MEDIUM,
                LocalDateTime.now().plusDays(2)
        );

        mockMvc.perform(post("/tasks/" + rootId + "/subtasks")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createSub)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Subtarefa - Autenticação JWT"))
                .andExpect(jsonPath("$.parentId").value(rootId));

        // 3. Get task details (with subtasks)
        mockMvc.perform(get("/tasks/" + rootId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Projeto Backend Taskman"))
                .andExpect(jsonPath("$.subtasks.length()").value(1));

        // 4. Update task details
        TaskUpdateDTO updateDTO = new TaskUpdateDTO(
                "Projeto Backend Taskman - Atualizado",
                "Descrição atualizada",
                TaskStatus.IN_PROGRESS,
                TaskPriority.HIGH,
                LocalDateTime.now().plusDays(10)
        );

        mockMvc.perform(put("/tasks/" + rootId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Projeto Backend Taskman - Atualizado"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // 5. Patch status to DONE (with completeSubtasks = true)
        TaskStatusUpdateDTO statusDTO = new TaskStatusUpdateDTO(TaskStatus.DONE, true);
        mockMvc.perform(patch("/tasks/" + rootId + "/status")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));

        // 6. List tasks
        mockMvc.perform(get("/tasks")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("status", "DONE")
                        .param("rootOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Projeto Backend Taskman - Atualizado"));

        // 7. Get dashboard metrics (both root and subtask are DONE)
        mockMvc.perform(get("/tasks/dashboard")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTasks").value(2))
                .andExpect(jsonPath("$.doneTasks").value(2))
                .andExpect(jsonPath("$.todoTasks").value(0));

        // 8. Delete root task (cascades to subtasks)
        mockMvc.perform(delete("/tasks/" + rootId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNoContent());

        // 9. Verify task is gone
        mockMvc.perform(get("/tasks/" + rootId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should prevent user B from accessing or modifying user A's task")
    void shouldEnforceUserIsolation() throws Exception {
        TaskCreateDTO createRoot = new TaskCreateDTO(
                "Tarefa Confidencial User A",
                "Dados sensíveis",
                TaskPriority.HIGH,
                LocalDateTime.now().plusDays(3)
        );

        MvcResult result = mockMvc.perform(post("/tasks")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRoot)))
                .andExpect(status().isCreated())
                .andReturn();

        String taskId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        // User B cannot get User A's task
        mockMvc.perform(get("/tasks/" + taskId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // User B cannot update User A's task
        TaskUpdateDTO updateDTO = new TaskUpdateDTO("Hacked", "Hacked", TaskStatus.DONE, TaskPriority.LOW, null);
        mockMvc.perform(put("/tasks/" + taskId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isNotFound());

        // User B cannot delete User A's task
        mockMvc.perform(delete("/tasks/" + taskId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 400 Bad Request on validation errors")
    void shouldValidateTaskCreation() throws Exception {
        TaskCreateDTO invalidDTO = new TaskCreateDTO(
                "",
                "Description",
                TaskPriority.LOW,
                LocalDateTime.now().minusDays(1) // past date
        );

        mockMvc.perform(post("/tasks")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.title").isNotEmpty())
                .andExpect(jsonPath("$.fields.dueDate").isNotEmpty());
    }

    @Test
    @DisplayName("Should block task completion when subtasks are pending and allow when confirmed or all done")
    void shouldBlockCompletionWhenSubtasksPendingAndAllowWhenConfirmed() throws Exception {
        // 1. Create root task
        TaskCreateDTO createRoot = new TaskCreateDTO(
                "Tarefa Principal com Subtarefas",
                "Descrição principal",
                TaskPriority.HIGH,
                LocalDateTime.now().plusDays(5)
        );

        MvcResult rootResult = mockMvc.perform(post("/tasks")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRoot)))
                .andExpect(status().isCreated())
                .andReturn();
        String rootId = objectMapper.readTree(rootResult.getResponse().getContentAsString()).get("id").asText();

        // 2. Create subtask
        TaskCreateDTO createSub = new TaskCreateDTO(
                "Subtarefa 1",
                "Descrição etapa 1",
                TaskPriority.MEDIUM,
                LocalDateTime.now().plusDays(3)
        );

        MvcResult subResult = mockMvc.perform(post("/tasks/" + rootId + "/subtasks")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createSub)))
                .andExpect(status().isCreated())
                .andReturn();
        String subId = objectMapper.readTree(subResult.getResponse().getContentAsString()).get("id").asText();

        // 3. Try to complete parent task without completing subtask (should be blocked with 409 Conflict)
        TaskStatusUpdateDTO failDTO = new TaskStatusUpdateDTO(TaskStatus.DONE, false);
        mockMvc.perform(patch("/tasks/" + rootId + "/status")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(failDTO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Não é possível concluir a tarefa pois existem subtarefas pendentes."));

        // 4. Complete subtask first
        TaskStatusUpdateDTO subDoneDTO = new TaskStatusUpdateDTO(TaskStatus.DONE);
        mockMvc.perform(patch("/tasks/" + subId + "/status")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(subDoneDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));

        // 5. Now all subtasks are DONE -> parent should complete directly without error
        mockMvc.perform(patch("/tasks/" + rootId + "/status")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(failDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));

        // 6. Test with another task: completeSubtasks = true should complete both parent and subtasks
        MvcResult root2Result = mockMvc.perform(post("/tasks")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRoot)))
                .andExpect(status().isCreated())
                .andReturn();
        String root2Id = objectMapper.readTree(root2Result.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/tasks/" + root2Id + "/subtasks")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createSub)))
                .andExpect(status().isCreated());

        TaskStatusUpdateDTO cascadeDTO = new TaskStatusUpdateDTO(TaskStatus.DONE, true);
        mockMvc.perform(patch("/tasks/" + root2Id + "/status")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cascadeDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.subtasks[0].status").value("DONE"));
    }

    @Test
    @DisplayName("Should allow deleting a subtask and remove it from parent task")
    void shouldAllowDeletingSubtask() throws Exception {
        TaskCreateDTO createRoot = new TaskCreateDTO("Root Task for Subtask Deletion", "Desc", TaskPriority.MEDIUM, null);
        MvcResult rootResult = mockMvc.perform(post("/tasks")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRoot)))
                .andExpect(status().isCreated())
                .andReturn();
        String rootId = objectMapper.readTree(rootResult.getResponse().getContentAsString()).get("id").asText();

        TaskCreateDTO createSub = new TaskCreateDTO("Subtask to Delete", "Desc", TaskPriority.LOW, null);
        MvcResult subResult = mockMvc.perform(post("/tasks/" + rootId + "/subtasks")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createSub)))
                .andExpect(status().isCreated())
                .andReturn();
        String subId = objectMapper.readTree(subResult.getResponse().getContentAsString()).get("id").asText();

        // Verify subtask is present
        mockMvc.perform(get("/tasks/" + rootId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtasks.length()").value(1));

        // Delete the subtask
        mockMvc.perform(delete("/tasks/" + rootId + "/subtasks/" + subId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNoContent());

        // Verify subtask is gone from parent
        mockMvc.perform(get("/tasks/" + rootId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtasks.length()").value(0));
    }
}

