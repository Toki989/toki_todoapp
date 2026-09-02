package com.example.todoapp;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TodoMapper {

    List<Todo> search(@Param("keyword") String keyword,
            @Param("category") String category,
            @Param("order") String order,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("showCompleted") boolean showCompleted);

    List<Todo> searchPage(@Param("keyword") String keyword,
            @Param("category") String category,
            @Param("order") String order,
            @Param("showCompleted") boolean showCompleted,
            @Param("trash") boolean trash,
            @Param("offset") int offset);

    int count(@Param("keyword") String keyword,
            @Param("category") String category,
            @Param("showCompleted") boolean showCompleted,
            @Param("trash") boolean trash);

    Todo findById(Long id);

    void insert(Todo todo);

    void update(Todo todo);

    void markDeleted(Long id);

    void restore(Long id);
}
