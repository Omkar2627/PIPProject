package com.taskmgmt.scheduler;

import com.taskmgmt.config.ReminderProperties;
import com.taskmgmt.entity.Task;
import com.taskmgmt.entity.TaskStatus;
import com.taskmgmt.entity.User;
import com.taskmgmt.repository.TaskRepository;
import com.taskmgmt.service.NotificationService;
import com.taskmgmt.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;


@Component
@RequiredArgsConstructor
public class DueDateReminderScheduler {

    private final TaskRepository taskRepository;
    private final TaskService taskService;
    private final NotificationService notificationService;
    private final ReminderProperties properties;

    @Scheduled(cron = "${reminder.cron:0 0 8 * * *}")
    @Transactional
    public void runReminderJob() {
        LocalDate today = LocalDate.now();

        // 1) Overdue tasks
        List<Task> overdueTasks = taskRepository.findByDueDateBeforeAndStatusNot(today, TaskStatus.DONE);

        for (Task t : overdueTasks) {
            List<User> assignees = taskService.findAssigneeUsers(t);

            String msg = String.format("Task '%s' was due on %s and is now OVERDUE.",
                    t.getTitle(), t.getDueDate());

            for (User u : assignees) {
                notificationService.createIfNotExists(t, u, "OVERDUE", msg, true);
            }

        }
        // 2) Upcoming tasks (due within next N days)
        LocalDate start = today;
        LocalDate end = today.plusDays(7);
        List<Task> upcomingTasks = taskRepository.findByDueDateBetweenAndStatusNot(start, end, TaskStatus.DONE);
        for (Task t : upcomingTasks) {
            // skip tasks already due today if you don't want duplicate messages (configurable)
            List<User> assignees = taskService.findAssigneeUsers(t);
            String msg = String.format("Reminder: Task '%s' is due on %s.", t.getTitle(), t.getDueDate());
            for (User u : assignees) {
                notificationService.createIfNotExists(t, u, "UPCOMING", msg, false);
            }
        }

    }
    }
