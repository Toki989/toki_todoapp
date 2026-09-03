package com.example.todoapp;

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class TodoTemplateControllerTests {
    private TodoTemplateService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = org.mockito.Mockito.mock(TodoTemplateService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new TodoTemplateController(service))
                .setValidator(validator).build();
    }

    @Test
    void displaysListAndNewFormWithOneItem() throws Exception {
        when(service.findAll()).thenReturn(List.of());
        mockMvc.perform(get("/todo-templates"))
                .andExpect(status().isOk()).andExpect(view().name("todo-templates/list"))
                .andExpect(model().attribute("templates", hasSize(0)));
        mockMvc.perform(get("/todo-templates/new"))
                .andExpect(status().isOk()).andExpect(view().name("todo-templates/create"))
                .andExpect(model().attribute("todoTemplateForm", hasProperty("items", hasSize(1))));
    }

    @Test
    void validNestedInputMovesToConfirmation() throws Exception {
        mockMvc.perform(validPost("/todo-templates/confirm"))
                .andExpect(status().isOk())
                .andExpect(view().name("todo-templates/create-confirm"))
                .andExpect(model().hasNoErrors());
    }

    @Test
    void invalidNestedInputStaysOnFormWithErrors() throws Exception {
        mockMvc.perform(post("/todo-templates/confirm")
                .param("name", "　")
                .param("items[0].title", " ")
                .param("items[0].category", "不正")
                .param("items[0].priority", "4"))
                .andExpect(status().isOk())
                .andExpect(view().name("todo-templates/create"))
                .andExpect(model().attributeHasFieldErrors("todoTemplateForm", "name",
                        "items[0].title", "items[0].category", "items[0].priority"));
    }

    @Test
    void rewriteKeepsValues() throws Exception {
        mockMvc.perform(validPost("/todo-templates/new"))
                .andExpect(status().isOk()).andExpect(view().name("todo-templates/create"))
                .andExpect(model().attribute("todoTemplateForm", hasProperty("name",
                        org.hamcrest.Matchers.is("定型作業"))));
    }

    @Test
    void loadsEditDeleteAndApplyScreens() throws Exception {
        TodoTemplate template = template(5L);
        when(service.findById(5L)).thenReturn(template);
        when(service.toForm(template)).thenReturn(form());

        mockMvc.perform(get("/todo-templates/5/edit"))
                .andExpect(view().name("todo-templates/edit"));
        mockMvc.perform(get("/todo-templates/5/delete"))
                .andExpect(view().name("todo-templates/delete"));
        mockMvc.perform(get("/todo-templates/5/apply"))
                .andExpect(view().name("todo-templates/apply"));
        mockMvc.perform(post("/todo-templates/5/apply/confirm"))
                .andExpect(view().name("todo-templates/apply-confirm"));
    }

    @Test
    void editConfirmationRewriteUpdateAndDeleteComplete() throws Exception {
        TodoTemplate template = template(5L);
        when(service.findById(5L)).thenReturn(template);
        when(service.update(org.mockito.ArgumentMatchers.eq(5L), any())).thenReturn(true);
        when(service.delete(5L)).thenReturn(true);

        mockMvc.perform(validPost("/todo-templates/5/confirm"))
                .andExpect(status().isOk()).andExpect(view().name("todo-templates/edit-confirm"));
        mockMvc.perform(validPost("/todo-templates/5/edit"))
                .andExpect(status().isOk()).andExpect(view().name("todo-templates/edit"));
        mockMvc.perform(validPost("/todo-templates/5"))
                .andExpect(redirectedUrl("/todo-templates"))
                .andExpect(flash().attribute("message", "テンプレートを保存しました"));
        mockMvc.perform(post("/todo-templates/5/delete"))
                .andExpect(redirectedUrl("/todo-templates"))
                .andExpect(flash().attribute("message", "テンプレートを削除しました"));
    }

    @Test
    void completionRedirectsWithMessages() throws Exception {
        when(service.apply(5L)).thenReturn(2);
        mockMvc.perform(validPost("/todo-templates"))
                .andExpect(redirectedUrl("/todo-templates"))
                .andExpect(flash().attribute("message", "テンプレートを登録しました"));
        mockMvc.perform(post("/todo-templates/5/apply"))
                .andExpect(redirectedUrl("/todo-templates"))
                .andExpect(flash().attribute("message", "2件のToDoを作成しました"));
    }

    @Test
    void finalPostRevalidatesHiddenValues() throws Exception {
        mockMvc.perform(post("/todo-templates").param("name", "不正").param("items[0].title", ""))
                .andExpect(status().isOk()).andExpect(view().name("todo-templates/create"))
                .andExpect(model().attributeHasFieldErrors("todoTemplateForm", "items[0].title",
                        "items[0].category", "items[0].priority"));
        verify(service, never()).create(any());
    }

    @Test
    void missingIdAndDisappearedBeforeFinalPostReturnSafely() throws Exception {
        when(service.findById(404L)).thenReturn(null);
        mockMvc.perform(get("/todo-templates/404/edit"))
                .andExpect(redirectedUrl("/todo-templates"))
                .andExpect(flash().attribute("message", "見つかりませんでした"));
        when(service.apply(404L)).thenReturn(null);
        mockMvc.perform(post("/todo-templates/404/apply"))
                .andExpect(redirectedUrl("/todo-templates"))
                .andExpect(flash().attribute("message", "見つかりませんでした"));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder validPost(String url) {
        return post(url).param("name", "定型作業")
                .param("items[0].title", "設計")
                .param("items[0].detail", "確認")
                .param("items[0].category", "デザイン")
                .param("items[0].priority", "1")
                .param("items[0].dueDate", "2026-09-10");
    }

    private TodoTemplate template(Long id) {
        TodoTemplate template = new TodoTemplate();
        template.setId(id);
        template.setName("定型作業");
        template.setItems(List.of());
        return template;
    }

    private TodoTemplateForm form() {
        TodoTemplateForm form = new TodoTemplateForm();
        form.setName("定型作業");
        form.setItems(List.of(new TodoTemplateItemForm()));
        return form;
    }
}
