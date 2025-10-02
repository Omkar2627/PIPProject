// TaskRequestDto.java
package com.taskmgmt.dto;

import com.taskmgmt.entity.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskRequestDto {
    private String title;
    private String description;
    private LocalDate dueDate;
    private TaskStatus status;
    private Long userId;
    private Long createdById;


}
