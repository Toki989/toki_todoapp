package com.example.todoapp;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TodoTemplateForm {
    @NotBlank(message = "テンプレート名を入力してください")
    @Size(max = 255, message = "テンプレート名は255文字以内で入力してください")
    private String name;

    @NotNull
    @Size(min = 1, message = "項目を1件以上入力してください")
    @Valid
    private List<TodoTemplateItemForm> items = new ArrayList<>();
}
