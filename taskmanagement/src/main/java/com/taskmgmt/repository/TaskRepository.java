package com.taskmgmt.repository;

import com.taskmgmt.entity.Task;
import com.taskmgmt.entity.TaskStatus;
import com.taskmgmt.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByStatus(com.taskmgmt.entity.TaskStatus status);
    List<Task> findByDueDateBeforeAndStatusNot(LocalDate date, com.taskmgmt.entity.TaskStatus done); // for overdue
    List<Task> findByCreatedBy(User user);
}
