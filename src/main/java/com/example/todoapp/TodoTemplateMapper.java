package com.example.todoapp;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TodoTemplateMapper {
    List<TodoTemplate> findAll();
    TodoTemplate findById(Long id);
    List<TodoTemplateItem> findItemsByTemplateId(Long todoTemplateId);
    void insertTemplate(TodoTemplate template);
    void insertItem(TodoTemplateItem item);
    int updateTemplate(TodoTemplate template);
    void deleteItemsByTemplateId(Long todoTemplateId);
    int deleteTemplate(Long id);
}
