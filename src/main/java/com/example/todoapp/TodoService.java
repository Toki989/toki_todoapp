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
            boolean showCompleted, int page) {
        int offset = (page - 1) * PAGE_SIZE;
        return todoMapper.searchPage(keyword, category, order, showCompleted, offset);
    }

    public int countPages(String keyword, String category, boolean showCompleted) {
        int count = todoMapper.count(keyword, category, showCompleted);
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

    public void delete(Long id) {
        todoMapper.deleteById(id);
        log.info("削除 id={}", id);
    }
}
