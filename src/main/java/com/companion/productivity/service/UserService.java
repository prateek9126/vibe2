package com.companion.productivity.service;

import com.companion.productivity.model.User;
import com.companion.productivity.model.Task;
import com.companion.productivity.model.Goal;
import com.companion.productivity.model.Habit;
import com.companion.productivity.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private HabitRepository habitRepository;

    public User registerUser(String username, String password, String fullName) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        String hashedPassword = hashPassword(password);
        User user = new User(username, hashedPassword, fullName);
        return userRepository.save(user);
    }

    public Optional<User> loginUser(String username, String password) {
        String hashedPassword = hashPassword(password);
        return userRepository.findByUsername(username)
                .filter(user -> user.getPassword().equals(hashedPassword));
    }

    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    /**
     * Dynamically calculates and updates a user's productivity score (0 - 100).
     * Score breakdown:
     * - Base Score: 40
     * - Task Completion Rate: Up to 35 points
     * - Habit Streaks Consistency: Up to 15 points
     * - Goal Progress: Up to 10 points
     */
    public int calculateProductivityScore(User user) {
        int baseScore = 40;
        int taskPoints = 0;
        int habitPoints = 0;
        int goalPoints = 0;

        // 1. Task Completion Rate
        List<Task> tasks = taskRepository.findByUser(user);
        if (!tasks.isEmpty()) {
            long completed = tasks.stream().filter(t -> "COMPLETED".equals(t.getStatus())).count();
            double ratio = (double) completed / tasks.size();
            taskPoints = (int) (ratio * 35);
        }

        // 2. Habit Streaks
        List<Habit> habits = habitRepository.findByUser(user);
        if (!habits.isEmpty()) {
            int totalStreak = habits.stream().mapToInt(Habit::getStreak).sum();
            // Up to 15 points, 3 points per streak increment, capped at 15
            habitPoints = Math.min(15, totalStreak * 3);
        }

        // 3. Goal Progress
        List<Goal> goals = goalRepository.findByUser(user);
        if (!goals.isEmpty()) {
            double totalProgress = 0;
            for (Goal goal : goals) {
                if (goal.getTargetValue() > 0) {
                    double progress = (double) goal.getCurrentValue() / goal.getTargetValue();
                    totalProgress += Math.min(1.0, progress); // Cap individual goal progress at 100%
                }
            }
            double averageProgress = totalProgress / goals.size();
            goalPoints = (int) (averageProgress * 10);
        }

        int finalScore = baseScore + taskPoints + habitPoints + goalPoints;
        finalScore = Math.min(100, Math.max(0, finalScore)); // Ensure bounds

        if (user.getProductivityScore() == null || user.getProductivityScore() != finalScore) {
            user.setProductivityScore(finalScore);
            userRepository.save(user);
        }

        return finalScore;
    }

    // Secure SHA-256 password hashing utility
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}
