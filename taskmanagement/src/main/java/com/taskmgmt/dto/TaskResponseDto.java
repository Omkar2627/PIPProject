package com.taskmgmt.dto;

import com.taskmgmt.entity.Task;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskResponseDto {
    private Long id;
    private String title;
    private String description;
    private String status;
    private LocalDate dueDate;
    private Long createdById;
    private String createdByName;

    public static TaskResponseDto fromEntity(Task task) {
        return new TaskResponseDto(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus().name(),
                task.getDueDate(),
                task.getCreatedBy().getId(),
                task.getCreatedBy().getName()
        );
    }
}
