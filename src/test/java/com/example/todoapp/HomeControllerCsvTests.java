package com.example.todoapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class HomeControllerCsvTests {

    @Test
    void exportsFilteredTodosAsSafeUtf8Csv() {
        TodoService todoService = mock(TodoService.class);
        Todo todo = new Todo();
        todo.setTitle("=SUM(1,2)");
        todo.setDetail("+メモ\"改行\nあり");
        todo.setCategory("プログラミング");
        todo.setPriority(1);
        todo.setDueDate(LocalDate.of(2026, 9, 10));
        todo.setCompleted(true);
        todo.setCompletedAt(LocalDateTime.of(2026, 9, 2, 12, 0));
        todo.setPinned(true);
        when(todoService.searchAll("検索", "プログラミング", "desc", true, false))
                .thenReturn(List.of(todo));

        HomeController controller = new HomeController(todoService);
        ResponseEntity<byte[]> response = controller.exportTodos(
                "検索", "プログラミング", "desc", true, "0");

        assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo("todos.csv");
        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("text/csv;charset=UTF-8");
        String csv = new String(response.getBody(), StandardCharsets.UTF_8);
        assertThat(csv).startsWith("\uFEFF\"印\",\"やること\",\"メモ\",\"ジャンル\",\"優先度\",\"期限\",\"状態\"\r\n");
        assertThat(csv).contains("\"★\",\"'=SUM(1,2)\",\"'+メモ\"\"改行\nあり\"");
        assertThat(csv).endsWith("\"高\",\"2026-09-10\",\"完了（2026-09-02）\"\r\n");
        verify(todoService).searchAll("検索", "プログラミング", "desc", true, false);
    }

    @Test
    void trashExportUsesTrashFilterAndOmitsPinnedColumn() {
        TodoService todoService = mock(TodoService.class);
        when(todoService.searchAll("", "", "asc", true, true)).thenReturn(List.of());

        HomeController controller = new HomeController(todoService);
        ResponseEntity<byte[]> response = controller.exportTodos("", "", "invalid", false, "1");

        String csv = new String(response.getBody(), StandardCharsets.UTF_8);
        assertThat(csv).isEqualTo("\uFEFF\"やること\",\"メモ\",\"ジャンル\",\"優先度\",\"期限\",\"状態\"\r\n");
        verify(todoService).searchAll("", "", "asc", true, true);
    }
}
