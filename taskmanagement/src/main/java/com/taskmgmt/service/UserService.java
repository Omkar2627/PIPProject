package com.taskmgmt.service;

import com.taskmgmt.dto.UserResponseDto;
import com.taskmgmt.entity.User;
import com.taskmgmt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // Get all users (Admin only)
    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserResponseDto::fromEntity) // Use static method
                .collect(Collectors.toList());
    }

    // Get single user by ID (Admin only)
    public UserResponseDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return UserResponseDto.fromEntity(user); // Use static method
    }
}
