package com.solutis.projeto.taskman_backend.repository;

import com.solutis.projeto.taskman_backend.domain.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findByUserIdAndSessionIdOrderByCreatedAtAsc(UUID userId, String sessionId);

    void deleteByUserIdAndSessionId(UUID userId, String sessionId);

    @Query("SELECT DISTINCT m.sessionId FROM ChatMessage m WHERE m.user.id = :userId")
    List<String> findDistinctSessionIdsByUserId(@Param("userId") UUID userId);
}

