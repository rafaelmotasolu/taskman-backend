package com.solutis.projeto.taskman_backend.repository;

import com.solutis.projeto.taskman_backend.domain.entity.Task;
import com.solutis.projeto.taskman_backend.domain.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findByUserId(UUID userId);

    Optional<Task> findByIdAndUserId(UUID id, UUID userId);

    List<Task> findByUserIdAndStatus(UUID userId, TaskStatus status);

    List<Task> findByUserIdAndParentTaskIsNull(UUID userId);

    List<Task> findByUserIdAndStatusNot(UUID userId, TaskStatus status);

    @Query("SELECT t FROM Task t WHERE t.user.id = :userId AND t.dueDate < :now AND t.status != :doneStatus")
    List<Task> findOverdueTasks(
            @Param("userId") UUID userId,
            @Param("now") LocalDateTime now,
            @Param("doneStatus") TaskStatus doneStatus
    );

    List<Task> findByUserIdAndParentTaskId(UUID userId, UUID parentId);
}

