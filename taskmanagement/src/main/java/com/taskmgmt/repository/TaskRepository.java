package com.taskmgmt.repository;

import com.taskmgmt.entity.Task;
import com.taskmgmt.entity.TaskStatus;
import com.taskmgmt.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByStatus(com.taskmgmt.entity.TaskStatus status);
    List<Task> findByDueDateBeforeAndStatusNot(LocalDate date, com.taskmgmt.entity.TaskStatus done); // for overdue
    List<Task> findByCreatedBy(User user);

    @Query("""
        SELECT DISTINCT t
        FROM Task t
        LEFT JOIN TaskAssignee ta ON ta.task = t
        LEFT JOIN User u ON ta.user = u
        WHERE (:status IS NULL OR t.status = :status)
          AND (:dueDate IS NULL OR t.dueDate = :dueDate)
          AND (:assigneeEmail IS NULL OR u.email = :assigneeEmail)
        """)
    List<Task> findFilteredTasks(
            @Param("status") TaskStatus status,
            @Param("dueDate") LocalDate dueDate,
            @Param("assigneeEmail") String assigneeEmail
    );
}
