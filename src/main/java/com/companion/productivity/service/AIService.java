package com.companion.productivity.service;

import com.companion.productivity.model.*;
import com.companion.productivity.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AIService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private HabitRepository habitRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    /**
     * Parse natural language command to extract task fields.
     * E.g. "Draft proposal by tomorrow at 5pm priority high category Work workload 3h"
     */
    public Task parseTaskFromText(String text, User user) {
        Task task = new Task();
        task.setUser(user);
        task.setStatus("TODO");

        String lowerText = text.toLowerCase();

        // 1. Extract Priority
        if (lowerText.contains("priority high") || lowerText.contains("high priority") || lowerText.contains("urgently")) {
            task.setPriority("HIGH");
        } else if (lowerText.contains("priority low") || lowerText.contains("low priority")) {
            task.setPriority("LOW");
        } else {
            task.setPriority("MEDIUM");
        }

        // 2. Extract Category
        if (lowerText.contains("category personal") || lowerText.contains("personal")) {
            task.setCategory("Personal");
        } else if (lowerText.contains("category health") || lowerText.contains("health") || lowerText.contains("gym") || lowerText.contains("workout")) {
            task.setCategory("Health");
        } else if (lowerText.contains("category finance") || lowerText.contains("finance") || lowerText.contains("budget") || lowerText.contains("pay")) {
            task.setCategory("Finance");
        } else {
            task.setCategory("Work"); // default
        }

        // 3. Extract Workload (Hours)
        Pattern workloadPattern = Pattern.compile("workload (\\d+(\\.\\d+)?)\\s*(h|hr|hrs|hour|hours)?");
        Matcher workloadMatcher = workloadPattern.matcher(lowerText);
        if (workloadMatcher.find()) {
            try {
                task.setWorkloadHours(Double.parseDouble(workloadMatcher.group(1)));
            } catch (NumberFormatException e) {
                task.setWorkloadHours(1.0);
            }
        } else {
            task.setWorkloadHours(1.5); // Default workload
        }

        // 4. Extract Due Date
        LocalDateTime dueDate = LocalDateTime.now().plusDays(2).withHour(17).withMinute(0).withSecond(0).withNano(0); // Default due date (in 2 days)
        
        if (lowerText.contains("today")) {
            dueDate = LocalDateTime.now().withHour(18).withMinute(0).withSecond(0).withNano(0);
        } else if (lowerText.contains("tomorrow")) {
            dueDate = LocalDateTime.now().plusDays(1).withHour(17).withMinute(0).withSecond(0).withNano(0);
        } else if (lowerText.contains("next week")) {
            dueDate = LocalDateTime.now().plusWeeks(1).withHour(12).withMinute(0).withSecond(0).withNano(0);
        } else if (lowerText.contains("by monday")) {
            dueDate = getNextDayOfWeek(DayOfWeek.MONDAY).atTime(17, 0);
        } else if (lowerText.contains("by tuesday")) {
            dueDate = getNextDayOfWeek(DayOfWeek.TUESDAY).atTime(17, 0);
        } else if (lowerText.contains("by wednesday")) {
            dueDate = getNextDayOfWeek(DayOfWeek.WEDNESDAY).atTime(17, 0);
        } else if (lowerText.contains("by thursday")) {
            dueDate = getNextDayOfWeek(DayOfWeek.THURSDAY).atTime(17, 0);
        } else if (lowerText.contains("by friday")) {
            dueDate = getNextDayOfWeek(DayOfWeek.FRIDAY).atTime(17, 0);
        }
        
        task.setDueDate(dueDate);

        // 5. Clean Title (remove instructions like "by tomorrow", "priority high", "workload 2h" to leave clean task description)
        String title = text;
        title = title.replaceAll("(?i)\\b(by|at|due)\\s+(today|tomorrow|next week|monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b", "");
        title = title.replaceAll("(?i)\\b(priority|category|workload)\\s+\\w+\\b", "");
        title = title.replaceAll("(?i)\\b\\d+\\s*(h|hr|hrs|hour|hours)\\b", "");
        title = title.trim();
        
        if (title.isEmpty()) {
            title = "New NLP Task";
        }
        task.setTitle(title);

        return task;
    }

    private LocalDate getNextDayOfWeek(DayOfWeek dayOfWeek) {
        LocalDate today = LocalDate.now();
        int daysToAdd = (dayOfWeek.getValue() - today.getDayOfWeek().getValue() + 7) % 7;
        if (daysToAdd == 0) daysToAdd = 7;
        return today.plusDays(daysToAdd);
    }

    /**
     * Dynamically prioritize all incomplete tasks for a user
     */
    public List<Task> prioritizeUserTasks(User user) {
        List<Task> tasks = taskRepository.findByUser(user);
        List<Task> incompleteTasks = tasks.stream()
                .filter(t -> !"COMPLETED".equals(t.getStatus()))
                .collect(Collectors.toList());

        LocalDateTime now = LocalDateTime.now();

        for (Task task : incompleteTasks) {
            int score = 40; // Base score

            // 1. Explicit Priority weight
            if ("HIGH".equals(task.getPriority())) score += 30;
            else if ("MEDIUM".equals(task.getPriority())) score += 15;
            else score += 5;

            // 2. Due Date Urgency
            if (task.getDueDate() != null) {
                long hoursRemaining = ChronoUnit.HOURS.between(now, task.getDueDate());
                if (hoursRemaining < 0) {
                    score += 30; // Overdue
                } else if (hoursRemaining <= 24) {
                    score += 25; // Due today
                } else if (hoursRemaining <= 48) {
                    score += 15; // Due tomorrow
                } else if (hoursRemaining <= 168) {
                    score += 8;  // Due this week
                }
            }

            // 3. Workload scaling
            if (task.getWorkloadHours() != null && task.getWorkloadHours() > 4.0) {
                score += 5; // Demands more effort, prioritize earlier
            }

            task.setAiPriorityScore(Math.min(100, Math.max(0, score)));
            
            // Recalculate deadline prediction
            task.setPredictedDeadlineStatus(predictDeadlineRisk(task, incompleteTasks));
            taskRepository.save(task);
        }

        return incompleteTasks.stream()
                .sorted(Comparator.comparing(Task::getAiPriorityScore).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Heuristically determines if a task is at risk of missing its deadline.
     */
    private String predictDeadlineRisk(Task task, List<Task> incompleteTasks) {
        if ("COMPLETED".equals(task.getStatus())) return "ON_TIME";
        if (task.getDueDate() == null) return "ON_TIME";

        LocalDateTime now = LocalDateTime.now();
        if (task.getDueDate().isBefore(now)) return "OVERDUE";

        // Calculate hours remaining to complete before due date
        double hoursLeft = ChronoUnit.HOURS.between(now, task.getDueDate());

        // Sum up workload hours of all other tasks due BEFORE or AT the same time
        double totalWorkloadAhead = incompleteTasks.stream()
                .filter(t -> t.getDueDate() != null && !t.getDueDate().isAfter(task.getDueDate()))
                .mapToDouble(t -> t.getWorkloadHours() != null ? t.getWorkloadHours() : 1.0)
                .sum();

        // Assume user works max 6 productive hours per day
        double daysNeeded = totalWorkloadAhead / 6.0;
        double daysAvailable = hoursLeft / 24.0;

        if (daysNeeded > daysAvailable) {
            return "RISK_OF_DELAY";
        }

        return "ON_TIME";
    }

    /**
     * AI Scheduling logic: Automatically updates task due dates to fit inside the calendar slots.
     */
    public List<Task> runSmartSchedule(User user) {
        List<Task> tasks = taskRepository.findByUser(user);
        List<Task> unscheduled = tasks.stream()
                .filter(t -> !"COMPLETED".equals(t.getStatus()))
                .sorted(Comparator.comparing((Task t) -> "HIGH".equals(t.getPriority()) ? 1 : ("MEDIUM".equals(t.getPriority()) ? 2 : 3)))
                .collect(Collectors.toList());

        LocalDate startDay = LocalDate.now();
        Map<LocalDate, Double> dailyWorkload = new HashMap<>();

        for (Task task : unscheduled) {
            // Find a day starting from today that has enough capacity (limit 6 hours/day)
            LocalDate scheduledDay = startDay;
            double taskWorkload = task.getWorkloadHours() != null ? task.getWorkloadHours() : 1.5;

            while (true) {
                double currentLoad = dailyWorkload.getOrDefault(scheduledDay, 0.0);
                if (currentLoad + taskWorkload <= 6.0) {
                    dailyWorkload.put(scheduledDay, currentLoad + taskWorkload);
                    break;
                }
                scheduledDay = scheduledDay.plusDays(1);
            }

            task.setDueDate(scheduledDay.atTime(17, 0)); // Set schedule to 5:00 PM on that day
            taskRepository.save(task);
        }

        // Return prioritized sorted list
        return prioritizeUserTasks(user);
    }

    /**
     * Analyze user goals, habits, and tasks to generate personalized daily insights.
     */
    public List<String> generateProductivityInsights(User user) {
        List<String> insights = new ArrayList<>();
        List<Task> tasks = taskRepository.findByUser(user);
        List<Habit> habits = habitRepository.findByUser(user);
        List<Goal> goals = goalRepository.findByUser(user);

        List<Task> incomplete = tasks.stream().filter(t -> !"COMPLETED".equals(t.getStatus())).collect(Collectors.toList());
        long overdueCount = incomplete.stream().filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(LocalDateTime.now())).count();
        long atRiskCount = incomplete.stream().filter(t -> "RISK_OF_DELAY".equals(t.getPredictedDeadlineStatus())).count();

        // 1. Workload Insights
        if (overdueCount > 0) {
            insights.add("⚠️ You have " + overdueCount + " overdue task" + (overdueCount > 1 ? "s" : "") + ". We highly recommend using AI Prioritize to rank them and tackling them first.");
        }
        if (atRiskCount > 0) {
            insights.add("⏰ AI Alert: " + atRiskCount + " task" + (atRiskCount > 1 ? "s are" : " is") + " at risk of delay due to calendar congestion. Consider rescheduling lower priority items.");
        }

        // 2. Habits Insights
        if (!habits.isEmpty()) {
            Habit highestStreak = habits.stream().max(Comparator.comparingInt(Habit::getStreak)).orElse(null);
            if (highestStreak != null && highestStreak.getStreak() > 0) {
                insights.add("🔥 Stellar streak! You have kept your habit '" + highestStreak.getName() + "' active for " + highestStreak.getStreak() + " days. Keep it up!");
            } else {
                insights.add("🌱 Consistency check: You don't have any active habit streaks today. Pick one small habit and complete it to get the ball rolling!");
            }
        }

        // 3. Goal progress insights
        if (!goals.isEmpty()) {
            Goal closestGoal = goals.stream()
                    .filter(g -> g.getCurrentValue() < g.getTargetValue())
                    .max(Comparator.comparingDouble(g -> (double) g.getCurrentValue() / g.getTargetValue()))
                    .orElse(null);
            if (closestGoal != null) {
                double pct = ((double) closestGoal.getCurrentValue() / closestGoal.getTargetValue()) * 100;
                insights.add("🎯 Almost there! Your goal '" + closestGoal.getTitle() + "' is " + String.format("%.0f", pct) + "% complete. You can cross this line this week!");
            }
        }

        // General fallback
        if (insights.isEmpty()) {
            insights.add("✨ Your calendar is well balanced and you are on track! Great job keeping your workload in control.");
        }

        return insights;
    }

    /**
     * AI Chatbot handler to execute instructions and return chat dialog responses.
     */
    public Map<String, Object> handleChatbotQuery(String query, User user) {
        Map<String, Object> response = new HashMap<>();
        String lowerQuery = query.toLowerCase();
        String reply;

        if (lowerQuery.contains("prioritize") || lowerQuery.contains("rank")) {
            prioritizeUserTasks(user);
            reply = "I have scanned all your active tasks and ran my prioritization algorithm. They are now sorted in the Task Manager based on urgency and priority weight. Let me know if you need help with anything else!";
            response.put("action", "REFRESH_TASKS");
        } else if (lowerQuery.contains("schedule") || lowerQuery.contains("auto plan") || lowerQuery.contains("plan my day")) {
            runSmartSchedule(user);
            reply = "Done! I have restructured your calendar and auto-slotted your tasks to prevent daily workload from exceeding 6 hours. Check your calendar for details.";
            response.put("action", "REFRESH_CALENDAR");
        } else if (lowerQuery.contains("workload") || lowerQuery.contains("insights") || lowerQuery.contains("analysis")) {
            List<String> insights = generateProductivityInsights(user);
            reply = "Here is my workload analysis for you:\n\n" + String.join("\n\n", insights);
            response.put("action", "SHOW_INSIGHTS");
        } else if (lowerQuery.startsWith("add task") || lowerQuery.startsWith("create task") || lowerQuery.contains("remind me to")) {
            // NLP Add
            String taskText = query.replaceFirst("(?i)^add task\\s+", "")
                                   .replaceFirst("(?i)^create task\\s+", "")
                                   .replaceFirst("(?i)^remind me to\\s+", "");
            Task task = parseTaskFromText(taskText, user);
            taskRepository.save(task);
            prioritizeUserTasks(user); // Reprioritize now that we added a task
            reply = "I've added the task: **\"" + task.getTitle() + "\"** to your list. It is scheduled for " 
                    + task.getDueDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a")) 
                    + " with " + task.getPriority() + " priority (" + task.getWorkloadHours() + " hours workload estimate).";
            response.put("action", "REFRESH_TASKS");
        } else if (lowerQuery.contains("status") || lowerQuery.contains("progress") || lowerQuery.contains("score")) {
            int score = user.getProductivityScore();
            reply = "Your current **Productivity Score** is **" + score + "/100**. This is calculated from your completed tasks, goal progression, and habit streaks. You are performing well! Let's keep completing tasks to boost it higher.";
            response.put("action", "REFRESH_SCORE");
        } else {
            // General Conversational Agent
            reply = "Hi! I am your AI Productivity Companion. I can help you with:\n" +
                    "- **Prioritizing Tasks**: Try saying *'prioritize my tasks'*\n" +
                    "- **Auto-Scheduling**: Try saying *'schedule my day'*\n" +
                    "- **Workload Analysis**: Try saying *'analyze my workload'*\n" +
                    "- **Adding Tasks**: Try saying *'add task Review document due tomorrow workload 2 hours priority high'*\n" +
                    "\nWhat would you like to achieve today?";
        }

        response.put("reply", reply);
        return response;
    }

    /**
     * Generates a structural daily productivity report
     */
    public Map<String, Object> generateProductivityReport(User user) {
        Map<String, Object> report = new HashMap<>();
        List<Task> tasks = taskRepository.findByUser(user);
        List<Habit> habits = habitRepository.findByUser(user);

        long completed = tasks.stream().filter(t -> "COMPLETED".equals(t.getStatus())).count();
        long active = tasks.stream().filter(t -> !"COMPLETED".equals(t.getStatus())).count();
        int totalHabits = habits.size();
        int completedHabits = (int) habits.stream().filter(h -> LocalDate.now().equals(h.getLastCompletedDate())).count();

        report.put("completedTasksCount", completed);
        report.put("activeTasksCount", active);
        report.put("productivityScore", user.getProductivityScore());
        report.put("totalHabits", totalHabits);
        report.put("completedHabitsToday", completedHabits);

        // Daily Workload distribution for chart
        Map<String, Double> workloadByDay = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE");
        
        for (int i = 0; i < 7; i++) {
            LocalDate date = LocalDate.now().plusDays(i - 3); // center around today
            workloadByDay.put(date.format(formatter), 0.0);
        }

        for (Task task : tasks) {
            if (task.getDueDate() != null && !"COMPLETED".equals(task.getStatus())) {
                String dayName = task.getDueDate().format(formatter);
                if (workloadByDay.containsKey(dayName)) {
                    double hours = task.getWorkloadHours() != null ? task.getWorkloadHours() : 1.0;
                    workloadByDay.put(dayName, workloadByDay.get(dayName) + hours);
                }
            }
        }

        report.put("workloadChartData", workloadByDay);
        return report;
    }
}
