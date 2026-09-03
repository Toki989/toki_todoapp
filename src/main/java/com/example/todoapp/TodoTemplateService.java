package com.example.todoapp;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TodoTemplateService {
    private final TodoTemplateMapper templateMapper;
    private final TodoMapper todoMapper;

    public TodoTemplateService(TodoTemplateMapper templateMapper, TodoMapper todoMapper) {
        this.templateMapper = templateMapper;
        this.todoMapper = todoMapper;
    }

    public List<TodoTemplate> findAll() {
        List<TodoTemplate> templates = templateMapper.findAll();
        templates.forEach(template ->
                template.setItems(templateMapper.findItemsByTemplateId(template.getId())));
        return templates;
    }

    public TodoTemplate findById(Long id) {
        TodoTemplate template = templateMapper.findById(id);
        if (template != null) {
            template.setItems(templateMapper.findItemsByTemplateId(id));
        }
        return template;
    }

    @Transactional
    public Long create(TodoTemplateForm form) {
        TodoTemplate template = new TodoTemplate();
        template.setName(form.getName());
        templateMapper.insertTemplate(template);
        insertItems(template.getId(), form.getItems());
        return template.getId();
    }

    @Transactional
    public boolean update(Long id, TodoTemplateForm form) {
        TodoTemplate template = new TodoTemplate();
        template.setId(id);
        template.setName(form.getName());
        if (templateMapper.updateTemplate(template) == 0) {
            return false;
        }
        templateMapper.deleteItemsByTemplateId(id);
        insertItems(id, form.getItems());
        return true;
    }

    @Transactional
    public boolean delete(Long id) {
        return templateMapper.deleteTemplate(id) > 0;
    }

    @Transactional
    public Integer apply(Long id) {
        if (templateMapper.findById(id) == null) {
            return null;
        }
        List<TodoTemplateItem> items = templateMapper.findItemsByTemplateId(id);
        if (items.isEmpty()) {
            return 0;
        }

        List<Long> createdIds = new ArrayList<>();
        for (TodoTemplateItem item : items) {
            Todo todo = new Todo();
            todo.setTitle(item.getTitle());
            todo.setDetail(item.getDetail());
            todo.setCategory(item.getCategory());
            todo.setPriority(item.getPriority());
            todo.setDueDate(item.getDueDate());
            todo.setCompleted(false);
            todo.setPinned(false);
            todoMapper.insert(todo);
            createdIds.add(todo.getId());
        }
        log.info("テンプレート適用 templateId={}, count={}, todoIds={}", id, createdIds.size(), createdIds);
        return createdIds.size();
    }

    public TodoTemplateForm toForm(TodoTemplate template) {
        TodoTemplateForm form = new TodoTemplateForm();
        form.setName(template.getName());
        List<TodoTemplateItemForm> itemForms = template.getItems().stream().map(item -> {
            TodoTemplateItemForm itemForm = new TodoTemplateItemForm();
            itemForm.setTitle(item.getTitle());
            itemForm.setDetail(item.getDetail());
            itemForm.setCategory(item.getCategory());
            itemForm.setPriority(item.getPriority());
            itemForm.setDueDate(item.getDueDate());
            return itemForm;
        }).toList();
        form.setItems(new ArrayList<>(itemForms));
        return form;
    }

    private void insertItems(Long templateId, List<TodoTemplateItemForm> forms) {
        for (int i = 0; i < forms.size(); i++) {
            TodoTemplateItemForm form = forms.get(i);
            TodoTemplateItem item = new TodoTemplateItem();
            item.setTodoTemplateId(templateId);
            item.setTitle(form.getTitle());
            item.setDetail(form.getDetail());
            item.setCategory(form.getCategory());
            item.setPriority(form.getPriority());
            item.setDueDate(form.getDueDate());
            item.setDisplayOrder(i + 1);
            templateMapper.insertItem(item);
        }
    }
}
