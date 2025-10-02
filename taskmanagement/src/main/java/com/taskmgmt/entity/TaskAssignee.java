package com.taskmgmt.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "task_assignees")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskAssignee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
