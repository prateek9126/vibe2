package com.companion.productivity.controller;

import com.companion.productivity.model.*;
import com.companion.productivity.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserService userService;

    @Autowired
    private AIService aiService;

    private User getAuthenticatedUser(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return null;
        return userService.getUserById(userId);
    }

    @GetMapping
    public ResponseEntity<?> getTasks(HttpSession session) {
        User user = getAuthenticatedUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }
        return ResponseEntity.ok(taskService.getTasks(user));
    }

    @PostMapping
    public ResponseEntity<?> createTask(@RequestBody Map<String, Object> body, HttpSession session) {
        User user = getAuthenticatedUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        Task task;
        if (body.containsKey("nlpText")) {
            // Natural language parsing request
            String nlpText = (String) body.get("nlpText");
            task = aiService.parseTaskFromText(nlpText, user);
        } else {
            // Standard creation request
            String title = (String) body.get("title");
            String description = (String) body.get("description");
            String priority = (String) body.get("priority");
            String category = (String) body.get("category");
            String status = (String) body.get("status");
            String dueDateStr = (String) body.get("dueDate");
            Double workload = body.get("workloadHours") != null ? ((Number) body.get("workloadHours")).doubleValue() : 1.0;

            if (title == null || title.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Title is required"));
            }

            LocalDateTime dueDate = dueDateStr != null ? LocalDateTime.parse(dueDateStr) : LocalDateTime.now().plusDays(1);

            task = new Task(title, description, status != null ? status : "TODO", 
                            priority != null ? priority : "MEDIUM", 
                            category != null ? category : "Work", 
                            dueDate, workload, user);
        }

        Task saved = taskService.saveTask(task);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTask(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpSession session) {
        User user = getAuthenticatedUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        Task task = taskService.getTaskById(id);
        if (task == null || !task.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Task not found"));
        }

        if (body.containsKey("title")) task.setTitle((String) body.get("title"));
        if (body.containsKey("description")) task.setDescription((String) body.get("description"));
        if (body.containsKey("status")) task.setStatus((String) body.get("status"));
        if (body.containsKey("priority")) task.setPriority((String) body.get("priority"));
        if (body.containsKey("category")) task.setCategory((String) body.get("category"));
        if (body.containsKey("workloadHours")) task.setWorkloadHours(((Number) body.get("workloadHours")).doubleValue());
        if (body.containsKey("dueDate")) {
            String dueStr = (String) body.get("dueDate");
            task.setDueDate(dueStr != null ? LocalDateTime.parse(dueStr) : null);
        }

        Task saved = taskService.saveTask(task);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable Long id, HttpSession session) {
        User user = getAuthenticatedUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        taskService.deleteTask(id, user);
        return ResponseEntity.ok(Map.of("message", "Task deleted successfully"));
    }

    @PostMapping("/prioritize")
    public ResponseEntity<?> prioritizeTasks(HttpSession session) {
        User user = getAuthenticatedUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        List<Task> prioritized = aiService.prioritizeUserTasks(user);
        return ResponseEntity.ok(prioritized);
    }

    @PostMapping("/schedule")
    public ResponseEntity<?> scheduleTasks(HttpSession session) {
        User user = getAuthenticatedUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        List<Task> scheduled = aiService.runSmartSchedule(user);
        return ResponseEntity.ok(scheduled);
    }
}
