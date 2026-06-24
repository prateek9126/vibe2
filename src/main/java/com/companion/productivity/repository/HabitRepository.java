package com.companion.productivity.repository;

import com.companion.productivity.model.Habit;
import com.companion.productivity.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HabitRepository extends JpaRepository<Habit, Long> {
    List<Habit> findByUser(User user);
}
