package com.taskmgmt.controller;

import com.taskmgmt.dto.*;
import com.taskmgmt.entity.Role;
import com.taskmgmt.entity.TaskStatus;
import com.taskmgmt.entity.User;
import com.taskmgmt.repository.UserRepository;
import com.taskmgmt.service.TaskService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TaskControllerTest {

    @Mock
    private TaskService taskService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TaskController taskController;

    private User adminUser;
    private User normalUser;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        adminUser = new User();
        adminUser.setEmail("admin@test.com");
        adminUser.setRole(Role.ADMIN);

        normalUser = new User();
        normalUser.setEmail("user@test.com");
        normalUser.setRole(Role.USER);
    }


    // GET /api/tasks (Success & Error Cases)

    @Test
    void testGetTasks_Admin_ShouldReturnAllTasks() {
        when(authentication.getName()).thenReturn("admin@test.com");
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));

        List<TaskResponseDto> mockTasks = List.of(new TaskResponseDto());
        when(taskService.getAllTasks()).thenReturn(mockTasks);

        ResponseEntity<List<TaskResponseDto>> response = taskController.getTasks(null, null, null, authentication);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testGetTasks_User_ShouldReturnUserTasks() {
        when(authentication.getName()).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(normalUser));

        List<TaskResponseDto> mockTasks = List.of(new TaskResponseDto());
        when(taskService.getTasksForUser("user@test.com")).thenReturn(mockTasks);

        ResponseEntity<List<TaskResponseDto>> response = taskController.getTasks(null, null, null, authentication);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testGetTasks_WithFilters_ShouldCallFilteredService() {
        when(authentication.getName()).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(normalUser));

        List<TaskResponseDto> mockTasks = List.of(new TaskResponseDto());
        when(taskService.getFilteredTasks(TaskStatus.TODO, LocalDate.now(), "other@test.com"))
                .thenReturn(mockTasks);

        ResponseEntity<List<TaskResponseDto>> response = taskController.getTasks(
                TaskStatus.TODO, LocalDate.now(), "other@test.com", authentication);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void testGetTasks_UserNotFound_ShouldThrowException() {
        when(authentication.getName()).thenReturn("unknown@test.com");
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                taskController.getTasks(null, null, null, authentication)
        );
    }


    // POST /api/tasks (Create Task)


    @Test
    void testCreateTask_Success() {
        TaskRequestDto request = new TaskRequestDto();
        TaskResponseDto responseDto = new TaskResponseDto();

        when(authentication.getName()).thenReturn("admin@test.com");
        when(taskService.createTaskByAdmin(request, "admin@test.com")).thenReturn(responseDto);

        ResponseEntity<TaskResponseDto> response = taskController.createTask(request, authentication);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
    }

    @Test
    void testCreateTask_Failure_UnauthorizedUser() {
        when(authentication.getName()).thenReturn("user@test.com");

        TaskRequestDto dto = new TaskRequestDto();
        when(taskService.createTaskByAdmin(dto, "user@test.com"))
                .thenThrow(new RuntimeException("Only admin can create tasks"));

        assertThrows(RuntimeException.class, () ->
                taskController.createTask(dto, authentication)
        );
    }


    // PUT /api/tasks/{id} (Update Status)

    @Test
    void testUpdateTaskStatus_Success() {
        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus("IN_PROGRESS");

        TaskResponseDto mockResponse = new TaskResponseDto();

        when(authentication.getName()).thenReturn("user@test.com");
        when(taskService.updateTaskStatus(1L, TaskStatus.IN_PROGRESS, "user@test.com"))
                .thenReturn(mockResponse);

        ResponseEntity<TaskResponseDto> response = taskController.updateTaskStatus(1L, request, authentication);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void testUpdateTaskStatus_InvalidStatus_ShouldThrow() {
        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus("INVALID_STATUS");

        when(authentication.getName()).thenReturn("user@test.com");

        assertThrows(IllegalArgumentException.class, () ->
                taskController.updateTaskStatus(1L, request, authentication)
        );
    }

    @Test
    void testUpdateTaskStatus_TaskNotFound() {
        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus("DONE");

        when(authentication.getName()).thenReturn("user@test.com");

        when(taskService.updateTaskStatus(1L, TaskStatus.DONE, "user@test.com"))
                .thenThrow(new RuntimeException("Task not found"));

        assertThrows(RuntimeException.class, () ->
                taskController.updateTaskStatus(1L, request, authentication)
        );
    }


    // POST /api/tasks/{id}/assignees


    @Test
    void testAssignUsers_Success() {
        AssignUsersRequest req = new AssignUsersRequest();
        req.setAssigneeIds(List.of(1L, 2L));

        List<AssigneeDto> mockAssignees = List.of(new AssigneeDto());

        when(authentication.getName()).thenReturn("admin@test.com");
        when(taskService.assignUsersToTask(10L, req.getAssigneeIds(), "admin@test.com"))
                .thenReturn(mockAssignees);

        ResponseEntity<List<AssigneeDto>> response =
                taskController.assignUsersToTask(10L, req, authentication);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testAssignUsers_Failure_InvalidUserId() {
        AssignUsersRequest req = new AssignUsersRequest();
        req.setAssigneeIds(List.of(99L)); // invalid id

        when(authentication.getName()).thenReturn("admin@test.com");

        when(taskService.assignUsersToTask(10L, req.getAssigneeIds(), "admin@test.com"))
                .thenThrow(new RuntimeException("User ID not found"));

        assertThrows(RuntimeException.class, () ->
                taskController.assignUsersToTask(10L, req, authentication)
        );
    }


    // GET /api/tasks/overdue


    @Test
    void testGetOverdueTasks_Success() {
        when(authentication.getName()).thenReturn("user@test.com");
        List<TaskResponseDto> mockOverdue = List.of(new TaskResponseDto());

        when(taskService.getOverdueTasks("user@test.com")).thenReturn(mockOverdue);

        ResponseEntity<List<TaskResponseDto>> response = taskController.getOverdueTasks(authentication);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testGetOverdueTasks_NoTasksFound() {
        when(authentication.getName()).thenReturn("user@test.com");
        when(taskService.getOverdueTasks("user@test.com")).thenReturn(Collections.emptyList());

        ResponseEntity<List<TaskResponseDto>> response = taskController.getOverdueTasks(authentication);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void testGetOverdueTasks_UserNotFound() {
        when(authentication.getName()).thenReturn("ghost@test.com");

        when(taskService.getOverdueTasks("ghost@test.com"))
                .thenThrow(new RuntimeException("User not found"));

        assertThrows(RuntimeException.class, () ->
                taskController.getOverdueTasks(authentication)
        );
    }
}
