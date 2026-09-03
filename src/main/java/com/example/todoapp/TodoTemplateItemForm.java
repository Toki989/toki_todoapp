package com.example.todoapp;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TodoTemplateItemForm {
    @NotBlank(message = "タイトルを入力してください")
    @Size(max = 255, message = "タイトルは255文字以内で入力してください")
    private String title;

    @Size(max = 255, message = "メモは255文字以内で入力してください")
    private String detail;

    @NotBlank(message = "ジャンルを選んでください")
    @Pattern(regexp = "デザイン|マーケティング|プログラミング|資格|就職活動",
            message = "ジャンルを選んでください")
    private String category;

    @NotNull(message = "優先度を選んでください")
    @Min(value = 1, message = "優先度を選んでください")
    @Max(value = 3, message = "優先度を選んでください")
    private Integer priority;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;
}
