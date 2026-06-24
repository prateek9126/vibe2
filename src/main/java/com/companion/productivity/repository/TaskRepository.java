package com.companion.productivity.repository;

import com.companion.productivity.model.Task;
import com.companion.productivity.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByUser(User user);
    List<Task> findByUserAndStatus(User user, String status);
    List<Task> findByUserAndDueDateBetween(User user, LocalDateTime start, LocalDateTime end);
    List<Task> findByUserAndCategory(User user, String category);
}
