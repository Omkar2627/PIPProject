package com.taskmgmt.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class BulkStatusUpdateRequest {
    private List<Long> taskIds;
    private String status;
    // getters & setters
}

