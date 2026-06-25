package com.companion.productivity.controller;

import com.companion.productivity.model.*;
import com.companion.productivity.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class GoalHabitController {

    @Autowired
    private GoalHabitService goalHabitService;

    @Autowired
    private UserService userService;

    private User getAuthenticatedUser(HttpSession session) {
        // Temporarily bypass session check for demo mode
        return userService.getOrCreateDefaultUser();
        /*
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return null;
        return userService.getUserById(userId);
        */
    }

    // --- GOALS API ---

    @GetMapping("/goals")
    public ResponseEntity<?> getGoals(HttpSession session) {
        User user = getAuthenticatedUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }
        return ResponseEntity.ok(goalHabitService.getGoals(user));
    }

    @PostMapping("/goals")
    public ResponseEntity<?> createGoal(@RequestBody Map<String, Object> body, HttpSession session) {
        User user = getAuthenticatedUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        String title = (String) body.get("title");
        String description = (String) body.get("description");
        Integer targetVal = body.get("targetValue") != null ? ((Number) body.get("targetValue")).intValue() : 100;
        Integer currentVal = body.get("currentValue") != null ? ((Number) body.get("currentValue")).intValue() : 0;
        String unit = (String) body.get("unit");
        String dateStr = (String) body.get("targetDate");

        if (title == null || title.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Title is required"));
        }

        LocalDateTime targetDate = dateStr != null ? LocalDateTime.parse(dateStr) : LocalDateTime.now().plusWeeks(4);

        Goal goal = new Goal(title, description, targetDate, targetVal, currentVal, unit, user);
        Goal saved = goalHabitService.saveGoal(goal);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/goals/{id}")
    public ResponseEntity<?> updateGoal(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpSession session) {
        User user = getAuthenticatedUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        Goal goal = goalHabitService.getGoals(user).stream()
                .filter(g -> g.getId().equals(id))
                .findFirst().orElse(null);

        if (goal == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Goal not found"));
        }

        if (body.containsKey("title")) goal.setTitle((String) body.get("title"));
        if (body.containsKey("description")) goal.setDescription((String) body.get("description"));
        if (body.containsKey("targetValue")) goal.setTargetValue(((Number) body.get("targetValue")).intValue());
        if (body.containsKey("currentValue")) goal.setCurrentValue(((Number) body.get("currentValue")).intValue());
        if (body.containsKey("unit")) goal.setUnit((String) body.get("unit"));
        if (body.containsKey("targetDate")) {
            String dateStr = (String) body.get("targetDate");
            goal.setTargetDate(dateStr != null ? LocalDateTime.parse(dateStr) : null);
        }

        Goal saved = goalHabitService.saveGoal(goal);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/goals/{id}")
    public ResponseEntity<?> deleteGoal(@PathVariable Long id, HttpSession session) {
        User user = getAuthenticatedUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        goalHabitService.deleteGoal(id, user);
        return ResponseEntity.ok(Map.of("message", "Goal deleted successfully"));
    }

    // --- HABITS API ---

    @GetMapping("/habits")
    public ResponseEntity<?> getHabits(HttpSession session) {
        User user = getAuthenticatedUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }
        return ResponseEntity.ok(goalHabitService.getHabits(user));
    }

    @PostMapping("/habits")
    public ResponseEntity<?> createHabit(@RequestBody Map<String, String> body, HttpSession session) {
        User user = getAuthenticatedUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        String name = body.get("name");
        String freq = body.get("frequency");

        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Habit name is required"));
        }

        Habit habit = new Habit(name, freq, user);
        Habit saved = goalHabitService.saveHabit(habit);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/habits/{id}/complete")
    public ResponseEntity<?> completeHabit(@PathVariable Long id, HttpSession session) {
        User user = getAuthenticatedUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        try {
            Habit completed = goalHabitService.completeHabitToday(id, user);
            return ResponseEntity.ok(completed);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/habits/{id}")
    public ResponseEntity<?> deleteHabit(@PathVariable Long id, HttpSession session) {
        User user = getAuthenticatedUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        goalHabitService.deleteHabit(id, user);
        return ResponseEntity.ok(Map.of("message", "Habit deleted successfully"));
    }
}
