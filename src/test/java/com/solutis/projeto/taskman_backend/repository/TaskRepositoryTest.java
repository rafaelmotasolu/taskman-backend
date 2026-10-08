package com.solutis.projeto.taskman_backend.repository;

import com.solutis.projeto.taskman_backend.domain.entity.Task;
import com.solutis.projeto.taskman_backend.domain.entity.User;
import com.solutis.projeto.taskman_backend.domain.enums.TaskPriority;
import com.solutis.projeto.taskman_backend.domain.enums.TaskStatus;
import com.solutis.projeto.taskman_backend.domain.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .name("Task Owner")
                .email("taskowner." + UUID.randomUUID() + "@example.com")
                .password("encoded_pass")
                .role(UserRole.ROLE_USER)
                .build());
    }

    @Test
    @DisplayName("Should create root task with subtasks hierarchy and cascade save")
    void shouldCreateRootTaskWithSubtasksHierarchy() {
        Task rootTask = Task.builder()
                .title("Root Project Task")
                .description("Parent task description")
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.HIGH)
                .dueDate(LocalDateTime.now().plusDays(5))
                .user(testUser)
                .build();

        Task subtask1 = Task.builder()
                .title("Subtask 1 - Setup")
                .status(TaskStatus.DONE)
                .priority(TaskPriority.MEDIUM)
                .user(testUser)
                .build();

        Task subtask2 = Task.builder()
                .title("Subtask 2 - Implementation")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.HIGH)
                .user(testUser)
                .build();

        rootTask.addSubtask(subtask1);
        rootTask.addSubtask(subtask2);

        Task savedRoot = taskRepository.save(rootTask);

        assertThat(savedRoot.getId()).isNotNull();
        assertThat(savedRoot.getCreatedAt()).isNotNull();
        assertThat(savedRoot.getUpdatedAt()).isNotNull();
        assertThat(savedRoot.getSubtasks()).hasSize(2);

        // Fetch root tasks only
        List<Task> rootTasks = taskRepository.findByUserIdAndParentTaskIsNull(testUser.getId());
        assertThat(rootTasks).extracting(t -> t.getTitle()).contains("Root Project Task");

        // Fetch subtasks by parent id
        List<Task> subtasks = taskRepository.findByUserIdAndParentTaskId(testUser.getId(), savedRoot.getId());
        assertThat(subtasks).hasSize(2)
                .extracting(t -> t.getTitle())
                .containsExactlyInAnyOrder("Subtask 1 - Setup", "Subtask 2 - Implementation");
    }

    @Test
    @DisplayName("Should query pending and overdue tasks")
    void shouldQueryPendingAndOverdueTasks() {
        // Pending task in the future
        Task futurePending = taskRepository.save(Task.builder()
                .title("Future Pending Task")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.LOW)
                .dueDate(LocalDateTime.now().plusDays(2))
                .user(testUser)
                .build());

        // Overdue task (due date in the past, status TODO)
        Task overdueTask = taskRepository.save(Task.builder()
                .title("Overdue Task")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.HIGH)
                .dueDate(LocalDateTime.now().minusDays(1))
                .user(testUser)
                .build());

        // Completed task in the past (not overdue because it is DONE)
        Task completedPastTask = taskRepository.save(Task.builder()
                .title("Completed Past Task")
                .status(TaskStatus.DONE)
                .priority(TaskPriority.MEDIUM)
                .dueDate(LocalDateTime.now().minusDays(3))
                .user(testUser)
                .build());

        // Query pending tasks (status != DONE)
        List<Task> pendingTasks = taskRepository.findByUserIdAndStatusNot(testUser.getId(), TaskStatus.DONE);
        assertThat(pendingTasks).extracting(t -> t.getId())
                .contains(futurePending.getId(), overdueTask.getId())
                .doesNotContain(completedPastTask.getId());

        // Query overdue tasks
        List<Task> overdueList = taskRepository.findOverdueTasks(testUser.getId(), LocalDateTime.now(), TaskStatus.DONE);
        assertThat(overdueList).extracting(t -> t.getId())
                .contains(overdueTask.getId())
                .doesNotContain(futurePending.getId(), completedPastTask.getId());
    }

    @Test
    @DisplayName("Should find task by id and user id")
    void shouldFindByIdAndUserId() {
        Task task = taskRepository.save(Task.builder()
                .title("Private Task")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .user(testUser)
                .build());

        Optional<Task> found = taskRepository.findByIdAndUserId(task.getId(), testUser.getId());
        assertThat(found).isPresent();

        Optional<Task> notFoundOtherUser = taskRepository.findByIdAndUserId(task.getId(), UUID.randomUUID());
        assertThat(notFoundOtherUser).isEmpty();
    }
}

