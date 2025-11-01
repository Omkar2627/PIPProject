package com.taskmgmt.service;

import com.taskmgmt.dto.*;
import com.taskmgmt.entity.Role;
import com.taskmgmt.entity.User;
import com.taskmgmt.repository.UserRepository;
import com.taskmgmt.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    // Register a new user
    public String register(RegisterRequestDto dto) {
        User user = User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .role(Role.USER)
                .build();
        userRepository.save(user);
        return "User registered successfully";
    }

    // Login and return access token (you can also return refresh token if needed)
    public AuthResponseDto login(LoginRequestDto dto) {
        // Authenticate user
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword())
        );

        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate access token
        String accessToken = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        // For now, reuse same token as refresh token or implement a separate refresh token if needed
        String refreshToken = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        return new AuthResponseDto(accessToken, refreshToken, user.getRole().name());
    }

    // Refresh access token using refresh token
    public String refreshAccessToken(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid or expired refresh token");
        }
        String username = jwtUtil.extractUsername(refreshToken);
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return jwtUtil.generateToken(user.getEmail(), user.getRole().name());
    }
}
