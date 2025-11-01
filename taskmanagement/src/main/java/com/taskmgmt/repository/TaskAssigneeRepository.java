package com.taskmgmt.repository;

import com.taskmgmt.entity.TaskAssignee;
import com.taskmgmt.entity.Task;
import com.taskmgmt.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskAssigneeRepository extends JpaRepository<TaskAssignee, Long> {
    List<TaskAssignee> findByTask(Task task);
    List<TaskAssignee> findByTaskId(Long taskId);
    List<TaskAssignee> findByUser(User user);
    boolean existsByTaskAndUser(Task task, User user);
    Optional<TaskAssignee> findByTaskAndUser(Task task, User user);

}
