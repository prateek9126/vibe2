package com.companion.productivity.repository;

import com.companion.productivity.model.Goal;
import com.companion.productivity.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findByUser(User user);
}
