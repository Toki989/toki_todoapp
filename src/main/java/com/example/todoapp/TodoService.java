package com.example.todoapp;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TodoService {

    private static final int PAGE_SIZE = 10;

    private final TodoMapper todoMapper;

    public TodoService(TodoMapper todoMapper) {
        this.todoMapper = todoMapper;
    }

    public List<Todo> search(String keyword, String category, String order,
            boolean showCompleted, boolean trash, int page) {
        int offset = (page - 1) * PAGE_SIZE;
        return todoMapper.searchPage(keyword, category, order, showCompleted, trash, offset);
    }

    public List<Todo> searchAll(String keyword, String category, String order,
            boolean showCompleted, boolean trash) {
        return todoMapper.searchAll(keyword, category, order, showCompleted, trash);
    }

    public int countPages(String keyword, String category, boolean showCompleted, boolean trash) {
        int count = todoMapper.count(keyword, category, showCompleted, trash);
        return (count + PAGE_SIZE - 1) / PAGE_SIZE;
    }

    public List<Todo> search(String keyword, String category, String order,
            LocalDate from, LocalDate to) {
        return todoMapper.search(keyword, category, order, from, to, true);
    }

    public Todo findById(Long id) {
        return todoMapper.findById(id);
    }

    public void create(Todo todo) {
        todoMapper.insert(todo);
        log.info("登録 id={}", todo.getId());
    }

    public void update(Todo todo) {
        todoMapper.update(todo);
        log.info("編集 id={}", todo.getId());
    }

    public void togglePinned(Long id) {
        todoMapper.togglePinned(id);
    }

    public void delete(Long id) {
        todoMapper.markDeleted(id);
        log.info("削除 id={}", id);
    }

    public void restore(Long id) {
        todoMapper.restore(id);
        log.info("復元 id={}", id);
    }
}
