// NotificationRepository.java
package com.taskmgmt.repository;

import com.taskmgmt.entity.Notification;
import com.taskmgmt.entity.Task;
import com.taskmgmt.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByTaskAndUserAndType(Task task, User user, String type);
    Optional<Notification> findByTaskAndUserAndType(Task task, User user, String type);
    boolean existsByTaskAndUser(Task task, User user);

}
