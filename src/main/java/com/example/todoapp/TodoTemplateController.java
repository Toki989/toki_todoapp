package com.example.todoapp;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/todo-templates")
public class TodoTemplateController {
    private final TodoTemplateService service;

    public TodoTemplateController(TodoTemplateService service) {
        this.service = service;
    }

    @ModelAttribute("categories")
    public List<String> categories() {
        return List.of("デザイン", "マーケティング", "プログラミング", "資格", "就職活動");
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("templates", service.findAll());
        return "todo-templates/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        if (!model.containsAttribute("todoTemplateForm")) {
            TodoTemplateForm form = new TodoTemplateForm();
            form.getItems().add(new TodoTemplateItemForm());
            model.addAttribute("todoTemplateForm", form);
        }
        return "todo-templates/create";
    }

    @PostMapping("/confirm")
    public String createConfirm(@Valid @ModelAttribute TodoTemplateForm todoTemplateForm,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "todo-templates/create";
        }
        return "todo-templates/create-confirm";
    }

    @PostMapping("/new")
    public String createRewrite(@ModelAttribute TodoTemplateForm todoTemplateForm) {
        ensureOneItem(todoTemplateForm);
        return "todo-templates/create";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute TodoTemplateForm todoTemplateForm,
            BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "todo-templates/create";
        }
        service.create(todoTemplateForm);
        redirectAttributes.addFlashAttribute("message", "テンプレートを登録しました");
        return "redirect:/todo-templates";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        TodoTemplate template = service.findById(id);
        if (template == null) {
            return notFound(redirectAttributes);
        }
        model.addAttribute("todoTemplateForm", service.toForm(template));
        model.addAttribute("templateId", id);
        return "todo-templates/edit";
    }

    @PostMapping("/{id}/confirm")
    public String editConfirm(@PathVariable Long id,
            @Valid @ModelAttribute TodoTemplateForm todoTemplateForm,
            BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (service.findById(id) == null) {
            return notFound(redirectAttributes);
        }
        model.addAttribute("templateId", id);
        if (bindingResult.hasErrors()) {
            return "todo-templates/edit";
        }
        return "todo-templates/edit-confirm";
    }

    @PostMapping("/{id}/edit")
    public String editRewrite(@PathVariable Long id,
            @ModelAttribute TodoTemplateForm todoTemplateForm,
            Model model, RedirectAttributes redirectAttributes) {
        if (service.findById(id) == null) {
            return notFound(redirectAttributes);
        }
        ensureOneItem(todoTemplateForm);
        model.addAttribute("templateId", id);
        return "todo-templates/edit";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
            @Valid @ModelAttribute TodoTemplateForm todoTemplateForm,
            BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (service.findById(id) == null) {
            return notFound(redirectAttributes);
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("templateId", id);
            return "todo-templates/edit";
        }
        if (!service.update(id, todoTemplateForm)) {
            return notFound(redirectAttributes);
        }
        redirectAttributes.addFlashAttribute("message", "テンプレートを保存しました");
        return "redirect:/todo-templates";
    }

    @GetMapping("/{id}/delete")
    public String deleteConfirm(@PathVariable Long id, Model model,
            RedirectAttributes redirectAttributes) {
        TodoTemplate template = service.findById(id);
        if (template == null) {
            return notFound(redirectAttributes);
        }
        model.addAttribute("template", template);
        return "todo-templates/delete";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (!service.delete(id)) {
            return notFound(redirectAttributes);
        }
        redirectAttributes.addFlashAttribute("message", "テンプレートを削除しました");
        return "redirect:/todo-templates";
    }

    @GetMapping("/{id}/apply")
    public String apply(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return showReadOnly(id, model, redirectAttributes, "todo-templates/apply");
    }

    @PostMapping("/{id}/apply/confirm")
    public String applyConfirm(@PathVariable Long id, Model model,
            RedirectAttributes redirectAttributes) {
        return showReadOnly(id, model, redirectAttributes, "todo-templates/apply-confirm");
    }

    @PostMapping("/{id}/apply")
    public String applyTemplate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Integer count = service.apply(id);
        if (count == null) {
            return notFound(redirectAttributes);
        }
        if (count == 0) {
            redirectAttributes.addFlashAttribute("message", "項目がないテンプレートは適用できません");
        } else {
            redirectAttributes.addFlashAttribute("message", count + "件のToDoを作成しました");
        }
        return "redirect:/todo-templates";
    }

    private String showReadOnly(Long id, Model model, RedirectAttributes redirectAttributes,
            String view) {
        TodoTemplate template = service.findById(id);
        if (template == null) {
            return notFound(redirectAttributes);
        }
        model.addAttribute("template", template);
        return view;
    }

    private String notFound(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("message", "見つかりませんでした");
        return "redirect:/todo-templates";
    }

    private void ensureOneItem(TodoTemplateForm form) {
        if (form.getItems() == null || form.getItems().isEmpty()) {
            form.setItems(new java.util.ArrayList<>());
            form.getItems().add(new TodoTemplateItemForm());
        }
    }
}
