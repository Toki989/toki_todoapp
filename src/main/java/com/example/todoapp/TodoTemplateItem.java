package com.example.todoapp;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class TodoTemplateItem {
    private Long id;
    private Long todoTemplateId;
    private String title;
    private String detail;
    private String category;
    private Integer priority;
    private LocalDate dueDate;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
