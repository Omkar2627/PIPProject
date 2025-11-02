package com.taskmgmt.controller;

import com.taskmgmt.dto.*;
import com.taskmgmt.entity.Role;
import com.taskmgmt.entity.TaskStatus;
import com.taskmgmt.entity.User;
import com.taskmgmt.repository.UserRepository;
import com.taskmgmt.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final UserRepository userRepository;

    // Get tasks for logged-in user
    @GetMapping
    public ResponseEntity<List<TaskResponseDto>> getTasks(Authentication authentication) {
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<TaskResponseDto> tasks;

        if (user.getRole() == Role.ADMIN) {
            tasks = taskService.getAllTasks(); // Admin sees all tasks
        } else {
            tasks = taskService.getTasksForUser(email); // User sees assigned tasks
        }

        return ResponseEntity.ok(tasks);
    }

    // Create a new task
    @PostMapping
    public ResponseEntity<TaskResponseDto> createTask(
            @RequestBody TaskRequestDto dto,
            Authentication authentication
    ) {
        String adminEmail = authentication.getName();
        TaskResponseDto response = taskService.createTaskByAdmin(dto, adminEmail);
        return ResponseEntity.ok(response);
    }

    // Update task status (Admin or assigned user)
    @PutMapping("/{id}")
    public ResponseEntity<TaskResponseDto> updateTaskStatus(
            @PathVariable("id") Long taskId,
            @RequestBody UpdateStatusRequest request,
            Authentication authentication
    ) {
        String loggedInUserEmail = authentication.getName();
        TaskResponseDto response = taskService.updateTaskStatus(
                taskId,
                TaskStatus.valueOf(request.getStatus()),
                loggedInUserEmail
        );
        return ResponseEntity.ok(response);
    }



    // Assign user to existing task (collaboration)
    @PostMapping("/{taskId}/assignees")
    public ResponseEntity<List<AssigneeDto>> assignUsersToTask(
            @PathVariable Long taskId,
            @RequestBody AssignUsersRequest request,
            Authentication authentication) {

        List<AssigneeDto> assignedUsers = taskService.assignUsersToTask(taskId, request.getAssigneeIds(), authentication.getName());
        return ResponseEntity.ok(assignedUsers);
    }

}
