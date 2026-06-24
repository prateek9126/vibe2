package com.companion.productivity.controller;

import com.companion.productivity.model.User;
import com.companion.productivity.service.AIService;
import com.companion.productivity.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    @Autowired
    private AIService aiService;

    @Autowired
    private UserService userService;

    private User getAuthenticatedUser(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return null;
        return userService.getUserById(userId);
    }

    @PostMapping("/chat")
    public ResponseEntity<?> chatbotResponse(@RequestBody Map<String, String> body, HttpSession session) {
        User user = getAuthenticatedUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        String query = body.get("query");
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Query is required"));
        }

        Map<String, Object> reply = aiService.handleChatbotQuery(query, user);
        return ResponseEntity.ok(reply);
    }

    @GetMapping("/insights")
    public ResponseEntity<?> getInsights(HttpSession session) {
        User user = getAuthenticatedUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        List<String> insights = aiService.generateProductivityInsights(user);
        return ResponseEntity.ok(insights);
    }

    @GetMapping("/report")
    public ResponseEntity<?> getReport(HttpSession session) {
        User user = getAuthenticatedUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        Map<String, Object> report = aiService.generateProductivityReport(user);
        return ResponseEntity.ok(report);
    }
}
