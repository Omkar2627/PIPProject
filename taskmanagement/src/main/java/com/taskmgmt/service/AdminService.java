//package com.taskmgmt.service;
//
//import com.taskmgmt.entity.Task;
//import com.taskmgmt.entity.User;
//import com.taskmgmt.entity.Role;
//import com.taskmgmt.repository.UserRepository;
//import org.springframework.stereotype.Service;
//
//import java.util.Set;
//
//@Service
//public class AdminService {
//    private final UserRepository userRepo;
//    private final UserService userService;
//    public AdminService(UserRepository userRepo, UserService userService) {
//        this.userRepo = userRepo; this.userService = userService;
//    }
//
//    public User createAdmin(String name, String email, String password) {
//        return userService.createUser(name, email, password, Set.of(Role.ADMIN));
//    }
//
//    public void deleteUser(Long userId) { userRepo.deleteById(userId); }
//}
