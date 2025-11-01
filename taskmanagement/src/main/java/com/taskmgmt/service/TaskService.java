package com.taskmgmt.service;

import com.taskmgmt.dto.AssigneeDto;
import com.taskmgmt.dto.TaskRequestDto;
import com.taskmgmt.dto.TaskResponseDto;
import com.taskmgmt.entity.*;
import com.taskmgmt.repository.TaskAssigneeRepository;
import com.taskmgmt.repository.TaskRepository;
import com.taskmgmt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskAssigneeRepository taskAssigneeRepository;

    /**
     * Admin creates a new task and assigns it to a user
     */
    public TaskResponseDto createTaskByAdmin(TaskRequestDto dto, String adminEmail) {
        User assignee = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        User adminUser = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        Task task = Task.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .dueDate(dto.getDueDate())
                .status(TaskStatus.TODO)
                .createdBy(adminUser)
                .build();

        Task savedTask = taskRepository.save(task);

        TaskAssignee assignment = TaskAssignee.builder()
                .task(savedTask)
                .user(assignee)
                .build();
        taskAssigneeRepository.save(assignment);

        return TaskResponseDto.fromEntity(savedTask);
    }

    /**
     * Get tasks for a specific user (via email)
     */
    public List<TaskResponseDto> getTasksForUser(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<TaskAssignee> assignments = taskAssigneeRepository.findByUser(user);
        List<TaskResponseDto> dtos = new ArrayList<>();

        for (TaskAssignee a : assignments) {
            dtos.add(TaskResponseDto.fromEntity(a.getTask()));
        }
        return dtos;
    }

    /**
     * Get all tasks (Admin only)
     */
    public List<TaskResponseDto> getAllTasks() {
        List<Task> tasks = taskRepository.findAll();
        List<TaskResponseDto> dtos = new ArrayList<>();
        for (Task task : tasks) {
            dtos.add(TaskResponseDto.fromEntity(task));
        }
        return dtos;
    }

    /**
     * Update task status (Admin or User)
     */
    public TaskResponseDto updateTaskStatus(Long taskId, TaskStatus newStatus, String loggedInUserEmail) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        User loggedInUser = userRepository.findByEmail(loggedInUserEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isAdmin = loggedInUser.getRole() == Role.ADMIN;
        boolean isTaskOwner = task.getCreatedBy().equals(loggedInUser);
        boolean isAssignee = taskAssigneeRepository.findByTaskAndUser(task, loggedInUser).isPresent();

        if (!isAdmin && !isTaskOwner && !isAssignee) {
            throw new RuntimeException("Only task owner, assignee, or admin can update status!");
        }

        task.setStatus(newStatus);
        Task updated = taskRepository.save(task);

        return TaskResponseDto.fromEntity(updated);
    }


    /**
     * Assign a new user to an existing task (collaboration)
     */

    public List<AssigneeDto> assignUsersToTask(Long taskId, List<Long> assigneeIds, String loggedInUserEmail) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        User loggedInUser = userRepository.findByEmail(loggedInUserEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!task.getCreatedBy().equals(loggedInUser.getId())
                && loggedInUser.getRole() != Role.ADMIN) {
            throw new RuntimeException("Only task owner or admin can assign users!");
        }


        List<AssigneeDto> assignedUsers = new ArrayList<>();
        for (Long userId : assigneeIds) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

            boolean alreadyAssigned = taskAssigneeRepository.existsByTaskAndUser(task, user);
            if (!alreadyAssigned) {
                TaskAssignee assignment = new TaskAssignee();
                assignment.setTask(task);
                assignment.setUser(user);
                taskAssigneeRepository.save(assignment);
            }

            assignedUsers.add(AssigneeDto.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .build());
        }

        return assignedUsers;
    }

}
