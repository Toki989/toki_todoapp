package com.example.todoapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfSystemProperty(named = "mysql.integration", matches = "true")
class TodoTemplateMySqlIntegrationTests {
    private static final String PREFIX = "__template_it__";

    @Autowired
    private TodoTemplateService service;

    @Autowired
    private TodoTemplateMapper mapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.execute("DROP TRIGGER IF EXISTS trg_test_todos_fail");
        jdbcTemplate.execute("DROP TRIGGER IF EXISTS trg_test_template_items_fail");
        jdbcTemplate.update("DELETE FROM todos WHERE title LIKE ?", PREFIX + "%");
        jdbcTemplate.update("DELETE FROM todo_templates WHERE name LIKE ?", PREFIX + "%");
    }

    @Test
    void mapperPersistsChildrenInOrderAndCascadeDoesNotTouchTodos() {
        int todosBefore = count("SELECT COUNT(*) FROM todos");
        Long id = service.create(form(PREFIX + "mapper", "b", "a"));

        assertThat(mapper.findItemsByTemplateId(id))
                .extracting(TodoTemplateItem::getTitle).containsExactly(PREFIX + "b", PREFIX + "a");
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO todo_template_items
                    (todo_template_id, title, category, priority, display_order)
                VALUES (?, ?, 'デザイン', 2, 1)
                """, id, PREFIX + "duplicate"))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO todo_template_items
                    (todo_template_id, title, category, priority, display_order)
                VALUES (?, ?, 'デザイン', 2, 0)
                """, id, PREFIX + "zero"))
                .isInstanceOf(DataAccessException.class);

        assertThat(service.delete(id)).isTrue();
        assertThat(count("SELECT COUNT(*) FROM todo_template_items WHERE todo_template_id = " + id))
                .isZero();
        assertThat(count("SELECT COUNT(*) FROM todos")).isEqualTo(todosBefore);
    }

    @Test
    void applyCommitsAllCopiesAndTheySurviveTemplateChangesAndDeletion() {
        Long id = service.create(form(PREFIX + "apply", "one", "two"));
        assertThat(service.apply(id)).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM todos WHERE title LIKE '" + PREFIX + "%'"))
                .isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM todos WHERE title LIKE '" + PREFIX
                + "%' AND completed = FALSE AND pinned = FALSE AND completed_at IS NULL AND deleted_at IS NULL"))
                .isEqualTo(2);

        service.update(id, form(PREFIX + "changed", "new"));
        service.delete(id);
        assertThat(count("SELECT COUNT(*) FROM todos WHERE title LIKE '" + PREFIX + "%'"))
                .isEqualTo(2);
    }

    @Test
    void applyRollsBackEarlierTodoWhenLaterInsertFails() {
        Long id = service.create(form(PREFIX + "rollback", "ok", "fail"));
        jdbcTemplate.execute("""
                CREATE TRIGGER trg_test_todos_fail BEFORE INSERT ON todos
                FOR EACH ROW BEGIN
                    IF NEW.title = '__template_it__fail' THEN
                        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'intentional test failure';
                    END IF;
                END
                """);

        assertThatThrownBy(() -> service.apply(id)).isInstanceOf(DataAccessException.class);
        assertThat(count("SELECT COUNT(*) FROM todos WHERE title LIKE '" + PREFIX + "%'"))
                .isZero();
    }

    @Test
    void updateRollsBackParentAndDeletedChildrenWhenReplacementInsertFails() {
        Long id = service.create(form(PREFIX + "before", "old-one", "old-two"));
        jdbcTemplate.execute("""
                CREATE TRIGGER trg_test_template_items_fail BEFORE INSERT ON todo_template_items
                FOR EACH ROW BEGIN
                    IF NEW.title = '__template_it__fail-update' THEN
                        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'intentional test failure';
                    END IF;
                END
                """);

        assertThatThrownBy(() -> service.update(id,
                form(PREFIX + "after", "replacement", "fail-update")))
                .isInstanceOf(DataAccessException.class);

        TodoTemplate unchanged = service.findById(id);
        assertThat(unchanged.getName()).isEqualTo(PREFIX + "before");
        assertThat(unchanged.getItems()).extracting(TodoTemplateItem::getTitle)
                .containsExactly(PREFIX + "old-one", PREFIX + "old-two");
    }

    @Test
    void rendersListInputConfirmAndReadOnlyPages() throws Exception {
        Long id = service.create(form(PREFIX + "screens", "screen-item"));

        mockMvc.perform(get("/todo-templates"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(PREFIX + "screens")));
        mockMvc.perform(get("/todo-templates/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("items[0].title")));
        mockMvc.perform(post("/todo-templates/confirm")
                .param("name", PREFIX + "confirm")
                .param("items[0].title", PREFIX + "confirm-item")
                .param("items[0].category", "デザイン")
                .param("items[0].priority", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("テンプレート登録確認")));
        mockMvc.perform(get("/todo-templates/" + id + "/edit")).andExpect(status().isOk());
        mockMvc.perform(get("/todo-templates/" + id + "/delete")).andExpect(status().isOk());
        mockMvc.perform(get("/todo-templates/" + id + "/apply")).andExpect(status().isOk());
    }

    private TodoTemplateForm form(String name, String... suffixes) {
        TodoTemplateForm form = new TodoTemplateForm();
        form.setName(name);
        form.setItems(java.util.Arrays.stream(suffixes).map(suffix -> {
            TodoTemplateItemForm item = new TodoTemplateItemForm();
            item.setTitle(PREFIX + suffix);
            item.setDetail("detail");
            item.setCategory("プログラミング");
            item.setPriority(2);
            item.setDueDate(LocalDate.of(2026, 9, 30));
            return item;
        }).toList());
        return form;
    }

    private int count(String sql) {
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
}
