package com.taskmgmt.service;

import com.taskmgmt.dto.UserResponseDto;
import com.taskmgmt.entity.Role;
import com.taskmgmt.entity.User;
import com.taskmgmt.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User user1;
    private User user2;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        user1 = User.builder()
                .id(1L)
                .name("User One")
                .email("one@test.com")
                .role(Role.USER)
                .build();

        user2 = User.builder()
                .id(2L)
                .name("User Two")
                .email("two@test.com")
                .role(Role.ADMIN)
                .build();
    }


    //TEST getAllUsers()


    @Test
    void testGetAllUsers_ReturnsUserList() {
        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));

        List<UserResponseDto> users = userService.getAllUsers();

        assertEquals(2, users.size());
        assertEquals("User One", users.get(0).getName());
        assertEquals("User Two", users.get(1).getName());

        verify(userRepository, times(1)).findAll();
    }

    @Test
    void testGetAllUsers_ReturnsEmptyList() {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        List<UserResponseDto> users = userService.getAllUsers();

        assertTrue(users.isEmpty());
        verify(userRepository, times(1)).findAll();
    }



    //                 TEST getUserById(id)

    @Test
    void testGetUserById_ReturnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));

        UserResponseDto dto = userService.getUserById(1L);

        assertEquals("User One", dto.getName());
        assertEquals("one@test.com", dto.getEmail());

        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void testGetUserById_ThrowsException_WhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                userService.getUserById(99L)
        );

        assertEquals("User not found", ex.getMessage());

        verify(userRepository, times(1)).findById(99L);
    }
}
