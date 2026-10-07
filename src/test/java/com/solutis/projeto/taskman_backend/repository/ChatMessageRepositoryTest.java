package com.solutis.projeto.taskman_backend.repository;

import com.solutis.projeto.taskman_backend.domain.entity.ChatMessage;
import com.solutis.projeto.taskman_backend.domain.entity.User;
import com.solutis.projeto.taskman_backend.domain.enums.MessageRole;
import com.solutis.projeto.taskman_backend.domain.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ChatMessageRepositoryTest {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .name("Chat User")
                .email("chat." + UUID.randomUUID() + "@example.com")
                .password("encoded_pass")
                .role(UserRole.ROLE_USER)
                .build());
    }

    @Test
    @DisplayName("Should save chat messages and retrieve ordered by createdAt ASC for a session")
    void shouldSaveAndRetrieveOrderedMessages() throws InterruptedException {
        String sessionId = "session-" + UUID.randomUUID();

        ChatMessage msg1 = chatMessageRepository.save(ChatMessage.builder()
                .sessionId(sessionId)
                .role(MessageRole.USER)
                .content("Como posso priorizar minhas tarefas de hoje?")
                .user(testUser)
                .build());

        Thread.sleep(10);

        ChatMessage msg2 = chatMessageRepository.save(ChatMessage.builder()
                .sessionId(sessionId)
                .role(MessageRole.ASSISTANT)
                .content("Sugiro começar pelas tarefas com prioridade HIGH e prazo mais próximo.")
                .user(testUser)
                .build());

        List<ChatMessage> history = chatMessageRepository.findByUserIdAndSessionIdOrderByCreatedAtAsc(
                testUser.getId(),
                sessionId
        );

        assertThat(history).hasSize(2);
        assertThat(history.get(0).getContent()).isEqualTo("Como posso priorizar minhas tarefas de hoje?");
        assertThat(history.get(0).getRole()).isEqualTo(MessageRole.USER);
        assertThat(history.get(1).getContent()).isEqualTo("Sugiro começar pelas tarefas com prioridade HIGH e prazo mais próximo.");
        assertThat(history.get(1).getRole()).isEqualTo(MessageRole.ASSISTANT);

        List<String> sessions = chatMessageRepository.findDistinctSessionIdsByUserId(testUser.getId());
        assertThat(sessions).contains(sessionId);
    }
}

