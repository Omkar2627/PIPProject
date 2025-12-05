package com.taskmgmt.controller;

import com.taskmgmt.dto.UserResponseDto;
import com.taskmgmt.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AdminController adminController;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    // ---------------------------------------------------------------------
    // TEST 1 – Get All Users (Success)
    // ---------------------------------------------------------------------
    @Test
    void testGetAllUsers_ReturnsUserList() {
        // Arrange
        UserResponseDto user1 = new UserResponseDto(1L, "John", "john@gmail.com", "USER");
        UserResponseDto user2 = new UserResponseDto(2L, "Admin", "admin@gmail.com", "ADMIN");

        List<UserResponseDto> mockUsers = Arrays.asList(user1, user2);

        when(userService.getAllUsers()).thenReturn(mockUsers);

        // Act
        ResponseEntity<List<UserResponseDto>> response = adminController.getAllUsers();

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals(2, response.getBody().size());
        assertEquals("John", response.getBody().get(0).getName());

        verify(userService, times(1)).getAllUsers();
    }

    // ---------------------------------------------------------------------
    // TEST 2 – Get All Users (Empty List)
    // ---------------------------------------------------------------------
    @Test
    void testGetAllUsers_ReturnsEmptyList() {
        when(userService.getAllUsers()).thenReturn(Collections.emptyList());

        ResponseEntity<List<UserResponseDto>> response = adminController.getAllUsers();

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isEmpty());

        verify(userService, times(1)).getAllUsers();
    }

    // ---------------------------------------------------------------------
    // TEST 3 – Get User by ID (Success)
    // ---------------------------------------------------------------------
    @Test
    void testGetUserById_ReturnsUser() {
        UserResponseDto user =
                new UserResponseDto(10L, "Test User", "test@gmail.com", "USER");

        when(userService.getUserById(10L)).thenReturn(user);

        ResponseEntity<UserResponseDto> response = adminController.getUserById(10L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Test User", response.getBody().getName());
        assertEquals(10L, response.getBody().getId());

        verify(userService, times(1)).getUserById(10L);
    }

    // ---------------------------------------------------------------------
    // TEST 4 – Get User by ID (User Not Found)
    // ---------------------------------------------------------------------
    @Test
    void testGetUserById_ThrowsException_WhenUserNotFound() {
        when(userService.getUserById(999L))
                .thenThrow(new RuntimeException("User not found"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            adminController.getUserById(999L);
        });

        assertEquals("User not found", exception.getMessage());
        verify(userService, times(1)).getUserById(999L);
    }
}
