package com.solutis.projeto.taskman_backend.service;

import com.solutis.projeto.taskman_backend.domain.entity.ChatMessage;
import com.solutis.projeto.taskman_backend.domain.entity.User;
import com.solutis.projeto.taskman_backend.dto.ai.ChatPromptResponseDTO;
import com.solutis.projeto.taskman_backend.dto.ai.SubtaskItemDTO;
import com.solutis.projeto.taskman_backend.dto.ai.TaskAnalysisResponseDTO;
import com.solutis.projeto.taskman_backend.dto.ai.TaskDecompositionResponseDTO;
import com.solutis.projeto.taskman_backend.dto.ai.TaskImprovementResponseDTO;
import com.solutis.projeto.taskman_backend.dto.task.TaskResponseDTO;

import java.util.List;
import java.util.UUID;

public interface AiService {

    TaskImprovementResponseDTO improveTask(String title, String description, User user);

    TaskAnalysisResponseDTO analyzeTask(UUID taskId, User user);

    TaskDecompositionResponseDTO decomposeTask(UUID taskId, User user);

    List<TaskResponseDTO> applySubtasks(UUID taskId, User user);

    List<TaskResponseDTO> applyApprovedSubtasks(UUID taskId, List<SubtaskItemDTO> approvedSubtasks, User user);

    ChatPromptResponseDTO chat(UUID sessionId, String userMessage, User user);

    List<ChatMessage> getChatHistory(UUID sessionId, User user);
}

