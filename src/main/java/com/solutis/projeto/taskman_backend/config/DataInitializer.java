package com.solutis.projeto.taskman_backend.config;

import com.solutis.projeto.taskman_backend.domain.entity.Task;
import com.solutis.projeto.taskman_backend.domain.entity.User;
import com.solutis.projeto.taskman_backend.domain.enums.TaskPriority;
import com.solutis.projeto.taskman_backend.domain.enums.TaskStatus;
import com.solutis.projeto.taskman_backend.domain.enums.UserRole;
import com.solutis.projeto.taskman_backend.repository.TaskRepository;
import com.solutis.projeto.taskman_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Inicializando dados padrão de teste e demonstração...");

        User admin = getOrCreateUser("Admin", "admin@taskman.com", "admin123", UserRole.ROLE_ADMIN);
        User dev = getOrCreateUser("Lucas Silva (Dev)", "dev@taskman.com", "admin123", UserRole.ROLE_USER);
        User gestor = getOrCreateUser("Mariana Costa (Gestora)", "gestor@taskman.com", "admin123", UserRole.ROLE_USER);

        seedTasksForDev(dev);
        seedTasksForAdmin(admin);
        seedTasksForGestor(gestor);

        log.info("Dados padrão inicializados com sucesso.");
    }

    private User getOrCreateUser(String name, String email, String rawPassword, UserRole role) {
        return userRepository.findByEmail(email).map(user -> {
            // Garante que a senha seja atualizada para o padrão caso necessário
            user.setPassword(passwordEncoder.encode(rawPassword));
            return userRepository.save(user);
        }).orElseGet(() ->
                userRepository.save(User.builder()
                        .name(name)
                        .email(email)
                        .password(passwordEncoder.encode(rawPassword))
                        .role(role)
                        .createdAt(LocalDateTime.now())
                        .build())
        );
    }

    private void seedTasksForDev(User dev) {
        if (taskRepository.findByUserIdAndParentTaskIsNull(dev.getId()).isEmpty()) {
            taskRepository.save(Task.builder()
                    .title("Implementar autenticação JWT e segurança")
                    .description("Configurar JwtService, JwtAuthenticationFilter e SecurityConfig com stateless session")
                    .status(TaskStatus.DONE)
                    .priority(TaskPriority.HIGH)
                    .dueDate(LocalDateTime.now().minusDays(1))
                    .user(dev)
                    .build());

            Task task2 = Task.builder()
                    .title("Construir tela de Kanban interativa com filtros")
                    .description("Desenvolver visualização Kanban com colunas TODO, IN_PROGRESS e DONE, busca e filtros")
                    .status(TaskStatus.IN_PROGRESS)
                    .priority(TaskPriority.HIGH)
                    .dueDate(LocalDateTime.now().plusDays(2))
                    .user(dev)
                    .build();

            Task subtask1 = Task.builder()
                    .title("Estruturar layout das colunas de status")
                    .description("Criar containers visuais para cada status")
                    .status(TaskStatus.DONE)
                    .priority(TaskPriority.MEDIUM)
                    .user(dev)
                    .build();

            Task subtask2 = Task.builder()
                    .title("Adicionar transição rápida de status com dropdown")
                    .description("Permitir mover tarefas entre colunas rapidamente")
                    .status(TaskStatus.IN_PROGRESS)
                    .priority(TaskPriority.HIGH)
                    .user(dev)
                    .build();

            task2.addSubtask(subtask1);
            task2.addSubtask(subtask2);
            taskRepository.save(task2);

            taskRepository.save(Task.builder()
                    .title("Integrar Spring AI para decomposição de tarefas")
                    .description("Utilizar Ollama para quebrar tarefas complexas em subtarefas acionáveis")
                    .status(TaskStatus.TODO)
                    .priority(TaskPriority.MEDIUM)
                    .dueDate(LocalDateTime.now().plusDays(4))
                    .user(dev)
                    .build());

            taskRepository.save(Task.builder()
                    .title("Revisão de performance das queries JPA")
                    .description("Validar se índices e consultas de dashboard estão otimizadas")
                    .status(TaskStatus.TODO)
                    .priority(TaskPriority.LOW)
                    .dueDate(LocalDateTime.now().plusDays(7))
                    .user(dev)
                    .build());
        }
    }

    private void seedTasksForAdmin(User admin) {
        if (taskRepository.findByUserIdAndParentTaskIsNull(admin.getId()).isEmpty()) {
            taskRepository.save(Task.builder()
                    .title("Auditoria de segurança e expiração de tokens")
                    .description("Verificar tempo de expiração do JWT e políticas de CORS na infraestrutura")
                    .status(TaskStatus.TODO)
                    .priority(TaskPriority.HIGH)
                    .dueDate(LocalDateTime.now().plusDays(1))
                    .user(admin)
                    .build());

            taskRepository.save(Task.builder()
                    .title("Monitoramento de métricas e saúde do Docker")
                    .description("Acompanhar consumo de memória dos containers e logs do PostgreSQL")
                    .status(TaskStatus.IN_PROGRESS)
                    .priority(TaskPriority.MEDIUM)
                    .dueDate(LocalDateTime.now().plusDays(3))
                    .user(admin)
                    .build());
        }
    }

    private void seedTasksForGestor(User gestor) {
        if (taskRepository.findByUserIdAndParentTaskIsNull(gestor.getId()).isEmpty()) {
            taskRepository.save(Task.builder()
                    .title("Planejamento da Sprint 1 e priorização de backlog")
                    .description("Alinhar entregas da arquitetura e definição de escopo das tarefas")
                    .status(TaskStatus.DONE)
                    .priority(TaskPriority.HIGH)
                    .dueDate(LocalDateTime.now().minusDays(2))
                    .user(gestor)
                    .build());

            taskRepository.save(Task.builder()
                    .title("Apresentação de demonstração do Taskman")
                    .description("Demonstrar login rápido, métricas de dashboard e recursos de IA para a equipe")
                    .status(TaskStatus.IN_PROGRESS)
                    .priority(TaskPriority.HIGH)
                    .dueDate(LocalDateTime.now().plusDays(1))
                    .user(gestor)
                    .build());
        }
    }
}

