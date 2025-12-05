package com.taskmgmt.controller;

import com.taskmgmt.dto.AuthResponseDto;
import com.taskmgmt.dto.LoginRequestDto;
import com.taskmgmt.dto.RegisterRequestDto;
import com.taskmgmt.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }


    //  TEST 1 – Register User

    @Test
    void testRegister_ReturnsSuccessMessage() {
        // Arrange
        RegisterRequestDto request = new RegisterRequestDto();
        request.setEmail("test@gmail.com");
        request.setPassword("123456");
        request.setName("Test User");

        when(authService.register(request)).thenReturn("User registered successfully");

        // Act
        ResponseEntity<String> response = authController.register(request);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals("User registered successfully", response.getBody());
        verify(authService, times(1)).register(request);
    }


    //  TEST 2 – Login User

    @Test
    void testLogin_ReturnsTokenResponse() {
        LoginRequestDto request = new LoginRequestDto();
        request.setEmail("test@gmail.com");
        request.setPassword("123456");

        AuthResponseDto authResponse =
                new AuthResponseDto("ACCESS-TOKEN", "REFRESH-TOKEN", "USER");

        when(authService.login(request)).thenReturn(authResponse);

        ResponseEntity<AuthResponseDto> response = authController.login(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("ACCESS-TOKEN", response.getBody().getAccessToken());
        assertEquals("REFRESH-TOKEN", response.getBody().getRefreshToken());
        assertEquals("USER", response.getBody().getRole());

        verify(authService, times(1)).login(request);
    }

    @Test
    void testLogin_ThrowsException_WhenInvalidCredentials() {
        LoginRequestDto request = new LoginRequestDto();
        request.setEmail("wrong@gmail.com");
        request.setPassword("invalid");

        when(authService.login(any(LoginRequestDto.class)))
                .thenThrow(new RuntimeException("Invalid email or password"));


        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authController.login(request);
        });

        assertEquals("Invalid email or password", exception.getMessage());
        verify(authService, times(1)).login(request);
    }
    @Test
    void testRegister_ThrowsException_WhenServiceFails() {
        RegisterRequestDto request = new RegisterRequestDto();
        request.setEmail("test@gmail.com");

        when(authService.register(any(RegisterRequestDto.class)))
                .thenThrow(new RuntimeException("Email already exists"));


        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authController.register(request);
        });

        assertEquals("Email already exists", exception.getMessage());
        verify(authService, times(1)).register(request);
    }

}
