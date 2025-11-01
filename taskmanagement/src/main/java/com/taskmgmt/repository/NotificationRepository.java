// NotificationRepository.java
package com.taskmgmt.repository;

import com.taskmgmt.entity.Notification;
import com.taskmgmt.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUser(User user);
}
