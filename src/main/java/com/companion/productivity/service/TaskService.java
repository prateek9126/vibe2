package com.companion.productivity.service;

import com.companion.productivity.model.*;
import com.companion.productivity.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private AIService aiService;

    public List<Task> getTasks(User user) {
        return taskRepository.findByUser(user);
    }

    public Task saveTask(Task task) {
        boolean isCompleted = "COMPLETED".equals(task.getStatus());
        if (isCompleted && task.getCompletedDate() == null) {
            task.setCompletedDate(LocalDateTime.now());
        } else if (!isCompleted) {
            task.setCompletedDate(null);
        }

        Task saved = taskRepository.save(task);

        // Auto-run AI prioritize and recalculate productivity score
        aiService.prioritizeUserTasks(task.getUser());
        userService.calculateProductivityScore(task.getUser());

        return saved;
    }

    public Task getTaskById(Long id) {
        return taskRepository.findById(id).orElse(null);
    }

    public void deleteTask(Long id, User user) {
        taskRepository.findById(id).ifPresent(task -> {
            if (task.getUser().getId().equals(user.getId())) {
                taskRepository.delete(task);
                aiService.prioritizeUserTasks(user);
                userService.calculateProductivityScore(user);
            }
        });
    }
}
