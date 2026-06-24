package com.companion.productivity.service;

import com.companion.productivity.model.*;
import com.companion.productivity.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class GoalHabitService {

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private HabitRepository habitRepository;

    @Autowired
    private UserService userService;

    // Goals CRUD
    public List<Goal> getGoals(User user) {
        return goalRepository.findByUser(user);
    }

    public Goal saveGoal(Goal goal) {
        Goal saved = goalRepository.save(goal);
        userService.calculateProductivityScore(goal.getUser());
        return saved;
    }

    public void deleteGoal(Long id, User user) {
        goalRepository.findById(id).ifPresent(goal -> {
            if (goal.getUser().getId().equals(user.getId())) {
                goalRepository.delete(goal);
                userService.calculateProductivityScore(user);
            }
        });
    }

    // Habits CRUD
    public List<Habit> getHabits(User user) {
        return habitRepository.findByUser(user);
    }

    public Habit saveHabit(Habit habit) {
        Habit saved = habitRepository.save(habit);
        userService.calculateProductivityScore(habit.getUser());
        return saved;
    }

    public void deleteHabit(Long id, User user) {
        habitRepository.findById(id).ifPresent(habit -> {
            if (habit.getUser().getId().equals(user.getId())) {
                habitRepository.delete(habit);
                userService.calculateProductivityScore(user);
            }
        });
    }

    /**
     * Mark a habit as completed for today, incrementing the streak if completed consecutively.
     */
    public Habit completeHabitToday(Long id, User user) {
        Habit habit = habitRepository.findById(id).orElseThrow(() -> new RuntimeException("Habit not found"));
        if (!habit.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        LocalDate today = LocalDate.now();
        if (habit.getLastCompletedDate() != null && habit.getLastCompletedDate().equals(today)) {
            // Already completed today
            return habit;
        }

        if (habit.getLastCompletedDate() != null && habit.getLastCompletedDate().equals(today.minusDays(1))) {
            // Consecutive day completion: increment streak
            habit.setStreak(habit.getStreak() + 1);
        } else {
            // Broken streak or first completion: set to 1
            habit.setStreak(1);
        }

        habit.setLastCompletedDate(today);
        Habit saved = habitRepository.save(habit);
        userService.calculateProductivityScore(user);
        return saved;
    }
}
