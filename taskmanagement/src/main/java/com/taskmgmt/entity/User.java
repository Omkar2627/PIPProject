package com.taskmgmt.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    private Role role; // ADMIN or USER

    // Tasks created by this user
    @OneToMany(mappedBy = "createdBy", cascade = CascadeType.ALL)
    private List<Task> createdTasks;

    // Tasks assigned to this user (via TaskAssignee)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<TaskAssignee> assignments;
}
