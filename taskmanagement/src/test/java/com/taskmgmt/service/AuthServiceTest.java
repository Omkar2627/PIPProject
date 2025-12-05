package com.taskmgmt.service;

import com.taskmgmt.dto.*;
import com.taskmgmt.entity.Role;
import com.taskmgmt.entity.User;
import com.taskmgmt.repository.UserRepository;
import com.taskmgmt.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }



    // 1. REGISTER USER - SUCCESS

    @Test
    void testRegister_Success() {
        RegisterRequestDto dto = new RegisterRequestDto();
        dto.setName("John");
        dto.setEmail("john@test.com");
        dto.setPassword("1234");

        when(passwordEncoder.encode("1234")).thenReturn("encoded");

        String result = authService.register(dto);

        assertEquals("User registered successfully", result);
        verify(userRepository, times(1)).save(any(User.class));
    }



    // 2. LOGIN - SUCCESS

    @Test
    void testLogin_Success() {

        LoginRequestDto dto = new LoginRequestDto();
        dto.setEmail("user@test.com");
        dto.setPassword("12345");

        User user = User.builder()
                .email("user@test.com")
                .password("encoded")
                .role(Role.USER)
                .build();

        // Authentication successful
        doNothing().when(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("user@test.com", "12345")
        );

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken("user@test.com", "USER"))
                .thenReturn("token123");

        AuthResponseDto response = authService.login(dto);

        assertNotNull(response);
        assertEquals("token123", response.getAccessToken());
        assertEquals("token123", response.getRefreshToken());
        assertEquals("USER", response.getRole());
    }



    // 3. LOGIN - AUTHENTICATION FAILED

    @Test
    void testLogin_AuthenticationFailed() {

        LoginRequestDto dto = new LoginRequestDto();
        dto.setEmail("wrong@test.com");
        dto.setPassword("bad");

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager)
                .authenticate(any());

        assertThrows(BadCredentialsException.class, () -> authService.login(dto));
        verify(userRepository, never()).findByEmail(any());
    }



    // 4. LOGIN - USER NOT FOUND

    @Test
    void testLogin_UserNotFound() {

        LoginRequestDto dto = new LoginRequestDto();
        dto.setEmail("missing@test.com");
        dto.setPassword("123");

        doNothing().when(authenticationManager).authenticate(any());

        when(userRepository.findByEmail("missing@test.com"))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.login(dto));
    }



    // 5. REFRESH TOKEN - SUCCESS

    @Test
    void testRefreshAccessToken_Success() {

        String refreshToken = "ref123";

        when(jwtUtil.validateToken(refreshToken)).thenReturn(true);
        when(jwtUtil.extractUsername(refreshToken)).thenReturn("user@test.com");

        User user = User.builder()
                .email("user@test.com")
                .role(Role.USER)
                .build();

        when(userRepository.findByEmail("user@test.com"))
                .thenReturn(Optional.of(user));

        when(jwtUtil.generateToken("user@test.com", "USER"))
                .thenReturn("newAccess123");

        String result = authService.refreshAccessToken(refreshToken);

        assertEquals("newAccess123", result);
    }



    // 6. REFRESH TOKEN - INVALID TOKEN

    @Test
    void testRefreshAccessToken_InvalidToken() {

        String refreshToken = "invalid";

        when(jwtUtil.validateToken(refreshToken)).thenReturn(false);

        assertThrows(RuntimeException.class,
                () -> authService.refreshAccessToken(refreshToken)
        );
    }



    // 7. REFRESH TOKEN - USER NOT FOUND

    @Test
    void testRefreshAccessToken_UserNotFound() {

        String refreshToken = "valid";

        when(jwtUtil.validateToken(refreshToken)).thenReturn(true);
        when(jwtUtil.extractUsername(refreshToken)).thenReturn("missing@test.com");
        when(userRepository.findByEmail("missing@test.com"))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> authService.refreshAccessToken(refreshToken)
        );
    }
}
