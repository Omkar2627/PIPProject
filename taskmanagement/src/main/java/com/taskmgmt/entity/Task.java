package com.taskmgmt.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private TaskStatus status; // TODO, IN_PROGRESS, DONE

    private LocalDate dueDate;

    private Instant createdAt = Instant.now();




    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;


    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL)
    private List<TaskAssignee> assignees;


    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL)
    private List<Notification> notifications;
}
