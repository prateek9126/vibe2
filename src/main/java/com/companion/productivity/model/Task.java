package com.companion.productivity.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    private String status = "TODO"; // TODO, IN_PROGRESS, COMPLETED

    private String priority = "MEDIUM"; // LOW, MEDIUM, HIGH

    private String category = "Work"; // Work, Personal, Health, Finance, etc.

    private LocalDateTime dueDate;

    private LocalDateTime completedDate;

    private Integer aiPriorityScore = 50; // AI calculated score (0-100)

    private Double workloadHours = 1.0; // Task duration estimate in hours

    private String predictedDeadlineStatus = "ON_TIME"; // ON_TIME, RISK_OF_DELAY, OVERDUE

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Task() {
    }

    public Task(String title, String description, String status, String priority, String category, LocalDateTime dueDate, Double workloadHours, User user) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.category = category;
        this.dueDate = dueDate;
        this.workloadHours = workloadHours != null ? workloadHours : 1.0;
        this.user = user;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDateTime getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(LocalDateTime completedDate) {
        this.completedDate = completedDate;
    }

    public Integer getAiPriorityScore() {
        return aiPriorityScore;
    }

    public void setAiPriorityScore(Integer aiPriorityScore) {
        this.aiPriorityScore = aiPriorityScore;
    }

    public Double getWorkloadHours() {
        return workloadHours;
    }

    public void setWorkloadHours(Double workloadHours) {
        this.workloadHours = workloadHours;
    }

    public String getPredictedDeadlineStatus() {
        return predictedDeadlineStatus;
    }

    public void setPredictedDeadlineStatus(String predictedDeadlineStatus) {
        this.predictedDeadlineStatus = predictedDeadlineStatus;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
