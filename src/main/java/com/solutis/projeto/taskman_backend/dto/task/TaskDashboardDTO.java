package com.solutis.projeto.taskman_backend.dto.task;

public record TaskDashboardDTO(
        long totalTasks,
        long todoTasks,
        long inProgressTasks,
        long doneTasks,
        long highPriorityTasks,
        long overdueTasks
) {
}

