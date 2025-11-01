package com.taskmgmt.dto;

import lombok.Data;

@Data
public class UpdateStatusRequest {
    private String status;  // TODO, IN_PROGRESS, DONE
}
