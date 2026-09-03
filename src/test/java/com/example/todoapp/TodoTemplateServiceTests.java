package com.example.todoapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

class TodoTemplateServiceTests {
    private final TodoTemplateMapper templateMapper = mock(TodoTemplateMapper.class);
    private final TodoMapper todoMapper = mock(TodoMapper.class);
    private final TodoTemplateService service = new TodoTemplateService(templateMapper, todoMapper);

    @Test
    void createsParentAndItemsInSubmittedOrder() {
        TodoTemplateForm form = form("朝の準備", "起きる", "出発する");
        doAnswer(invocation -> {
            invocation.<TodoTemplate>getArgument(0).setId(10L);
            return null;
        }).when(templateMapper).insertTemplate(any());

        assertThat(service.create(form)).isEqualTo(10L);

        ArgumentCaptor<TodoTemplateItem> items = ArgumentCaptor.forClass(TodoTemplateItem.class);
        verify(templateMapper, org.mockito.Mockito.times(2)).insertItem(items.capture());
        assertThat(items.getAllValues()).extracting(TodoTemplateItem::getDisplayOrder)
                .containsExactly(1, 2);
        assertThat(items.getAllValues()).extracting(TodoTemplateItem::getTodoTemplateId)
                .containsOnly(10L);
    }

    @Test
    void updateReplacesItemsAfterUpdatingParent() {
        when(templateMapper.updateTemplate(any())).thenReturn(1);
        TodoTemplateForm form = form("更新", "一", "二");

        assertThat(service.update(3L, form)).isTrue();

        InOrder order = inOrder(templateMapper);
        order.verify(templateMapper).updateTemplate(any());
        order.verify(templateMapper).deleteItemsByTemplateId(3L);
        order.verify(templateMapper, org.mockito.Mockito.times(2)).insertItem(any());
    }

    @Test
    void applyCopiesEveryItemAsIndependentIncompleteUnpinnedTodo() {
        TodoTemplate template = new TodoTemplate();
        template.setId(4L);
        when(templateMapper.findById(4L)).thenReturn(template);
        TodoTemplateItem first = item("設計", "メモ", "デザイン", 1,
                LocalDate.of(2026, 9, 10), 1);
        TodoTemplateItem second = item("実装", null, "プログラミング", 2, null, 2);
        when(templateMapper.findItemsByTemplateId(4L)).thenReturn(List.of(first, second));
        doAnswer(invocation -> {
            Todo todo = invocation.getArgument(0);
            todo.setId((long) (todo.getTitle().equals("設計") ? 20 : 21));
            return null;
        }).when(todoMapper).insert(any());

        assertThat(service.apply(4L)).isEqualTo(2);

        ArgumentCaptor<Todo> todos = ArgumentCaptor.forClass(Todo.class);
        verify(todoMapper, org.mockito.Mockito.times(2)).insert(todos.capture());
        assertThat(todos.getAllValues()).extracting(Todo::getTitle).containsExactly("設計", "実装");
        assertThat(todos.getAllValues()).allSatisfy(todo -> {
            assertThat(todo.getCompleted()).isFalse();
            assertThat(todo.getPinned()).isFalse();
            assertThat(todo.getCompletedAt()).isNull();
            assertThat(todo.getDeletedAt()).isNull();
        });
        assertThat(todos.getAllValues().getFirst().getDueDate())
                .isEqualTo(LocalDate.of(2026, 9, 10));
    }

    @Test
    void doesNotApplyMissingOrEmptyTemplate() {
        when(templateMapper.findById(99L)).thenReturn(null);
        assertThat(service.apply(99L)).isNull();
        verify(todoMapper, never()).insert(any());

        TodoTemplate empty = new TodoTemplate();
        when(templateMapper.findById(1L)).thenReturn(empty);
        when(templateMapper.findItemsByTemplateId(1L)).thenReturn(List.of());
        assertThat(service.apply(1L)).isZero();
        verify(todoMapper, never()).insert(any());
    }

    @Test
    void deleteOnlyTouchesTemplateMapper() {
        when(templateMapper.deleteTemplate(7L)).thenReturn(1);
        assertThat(service.delete(7L)).isTrue();
        verify(templateMapper).deleteTemplate(7L);
        verify(todoMapper, never()).markDeleted(any());
    }

    private TodoTemplateForm form(String name, String... titles) {
        TodoTemplateForm form = new TodoTemplateForm();
        form.setName(name);
        form.setItems(java.util.Arrays.stream(titles).map(title -> {
            TodoTemplateItemForm item = new TodoTemplateItemForm();
            item.setTitle(title);
            item.setCategory("プログラミング");
            item.setPriority(2);
            return item;
        }).toList());
        return form;
    }

    private TodoTemplateItem item(String title, String detail, String category,
            int priority, LocalDate dueDate, int displayOrder) {
        TodoTemplateItem item = new TodoTemplateItem();
        item.setTitle(title);
        item.setDetail(detail);
        item.setCategory(category);
        item.setPriority(priority);
        item.setDueDate(dueDate);
        item.setDisplayOrder(displayOrder);
        return item;
    }
}
