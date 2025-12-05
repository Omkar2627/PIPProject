package com.taskmgmt.service;


import com.taskmgmt.entity.Notification;
import com.taskmgmt.entity.Task;
import com.taskmgmt.entity.User;
import com.taskmgmt.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationService {
    @Autowired
     NotificationRepository notificationRepository;
    @Autowired
    EmailService emailService;

    @Transactional
    public Notification createIfNotExists(Task task, User user, String type, String message, boolean sendEmail) {
        boolean exists = notificationRepository.existsByTaskAndUserAndType(task, user, type);
        if (exists) {
            return notificationRepository.findByTaskAndUserAndType(task, user, type).orElse(null);
        }
        Notification n = Notification.builder()
                .task(task)
                .user(user)
                //.type(type)
                .message(message)
                .createdAt(LocalDateTime.now())
                .sent(false)
                .build();
        Notification saved = notificationRepository.save(n);
        if (sendEmail && user.getEmail() != null && !user.getEmail().isBlank()) {
            try {
                emailService.sendSimpleEmail(user.getEmail(), "Task Reminder: " + task.getTitle(), message);
                saved.setSent(true);
                saved.setSentAt(LocalDateTime.now());
                notificationRepository.save(saved);
            } catch (Exception ex) {
                // log and continue; don't roll back notification creation
                ex.printStackTrace();
            }
        }
        return saved;
    }
}


