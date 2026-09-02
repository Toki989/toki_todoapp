package com.example.todoapp;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
public class HomeController {

    private final TodoService todoService;

    public HomeController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("title", "やること管理");
        return "index";
    }

    @GetMapping("/todos")
    public String todos(@RequestParam(name = "keyword", defaultValue = "") String keyword,
            @RequestParam(name = "category", defaultValue = "") String category,
            @RequestParam(name = "order", defaultValue = "asc") String order,
            @RequestParam(name = "showCompleted", defaultValue = "false") boolean showCompleted,
            @RequestParam(name = "trash", defaultValue = "0") String trashValue,
            @RequestParam(name = "page", defaultValue = "1") int page,
            Model model) {
        if (!order.equals("desc")) {
            order = "asc";
        }

        boolean trash = trashValue.equals("1");
        if (trash) {
            showCompleted = true;
        }
        int totalPages = todoService.countPages(keyword, category, showCompleted, trash);
        int currentPage = Math.max(1, Math.min(page, Math.max(totalPages, 1)));

        model.addAttribute("todos",
                todoService.search(keyword, category, order, showCompleted, trash, currentPage));
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("keyword", keyword);
        model.addAttribute("category", category);
        model.addAttribute("order", order);
        model.addAttribute("showCompleted", showCompleted);
        model.addAttribute("trash", trash);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        return "todos";
    }

    @GetMapping("/api/todos.csv")
    public ResponseEntity<byte[]> exportTodos(
            @RequestParam(name = "keyword", defaultValue = "") String keyword,
            @RequestParam(name = "category", defaultValue = "") String category,
            @RequestParam(name = "order", defaultValue = "asc") String order,
            @RequestParam(name = "showCompleted", defaultValue = "false") boolean showCompleted,
            @RequestParam(name = "trash", defaultValue = "0") String trashValue) {
        order = order.equals("desc") ? "desc" : "asc";
        boolean trash = trashValue.equals("1");
        if (trash) {
            showCompleted = true;
        }

        List<Todo> todos = todoService.searchAll(keyword, category, order, showCompleted, trash);
        byte[] csv = createCsv(todos, !trash).getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment().filename("todos.csv").build());
        return ResponseEntity.ok().headers(headers).body(csv);
    }

    private String createCsv(List<Todo> todos, boolean includePinned) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        List<String> headers = new ArrayList<>();
        if (includePinned) {
            headers.add("印");
        }
        headers.addAll(List.of("やること", "メモ", "ジャンル", "優先度", "期限", "状態"));
        appendCsvRow(csv, headers);

        for (Todo todo : todos) {
            List<String> row = new ArrayList<>();
            if (includePinned) {
                row.add(Boolean.TRUE.equals(todo.getPinned()) ? "★" : "☆");
            }
            row.add(todo.getTitle());
            row.add(todo.getDetail());
            row.add(todo.getCategory());
            row.add(priorityLabel(todo.getPriority()));
            row.add(todo.getDueDate() == null ? "" : todo.getDueDate().toString());
            row.add(completedLabel(todo));
            appendCsvRow(csv, row);
        }
        return csv.toString();
    }

    private void appendCsvRow(StringBuilder csv, List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(csvCell(values.get(i)));
        }
        csv.append("\r\n");
    }

    private String csvCell(String value) {
        String safeValue = value == null ? "" : value;
        if (!safeValue.isEmpty() && "=+-@\t\r".indexOf(safeValue.charAt(0)) >= 0) {
            safeValue = "'" + safeValue;
        }
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }

    private String priorityLabel(Integer priority) {
        if (Integer.valueOf(1).equals(priority)) {
            return "高";
        }
        if (Integer.valueOf(2).equals(priority)) {
            return "中";
        }
        return "低";
    }

    private String completedLabel(Todo todo) {
        if (!Boolean.TRUE.equals(todo.getCompleted())) {
            return "未完了";
        }
        if (todo.getCompletedAt() == null) {
            return "完了";
        }
        return "完了（" + todo.getCompletedAt().toLocalDate() + "）";
    }

    @GetMapping("/todos/new")
    public String create(Model model) {
        model.addAttribute("todo", new Todo());
        return "create";
    }

    @PostMapping("/todos/confirm")
    public String confirm(@Valid @ModelAttribute Todo todo, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "create";
        }

        return "create-confirm";
    }

    @PostMapping("/todos/new")
    public String rewrite(@ModelAttribute Todo todo) {
        return "create";
    }

    @GetMapping("/todos/{id}/edit")
    public String edit(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Todo todo = todoService.findById(id);

        if (todo == null) {
            redirectAttributes.addFlashAttribute("message", "見つかりませんでした");
            return "redirect:/todos";
        }

        model.addAttribute("todo", todo);
        return "edit";
    }

    @PostMapping("/todos/{id}/confirm")
    public String editConfirm(@PathVariable Long id, @Valid @ModelAttribute Todo todo,
            BindingResult bindingResult) {
        todo.setId(id);

        if (bindingResult.hasErrors()) {
            return "edit";
        }

        return "edit-confirm";
    }

    @PostMapping("/todos/{id}/edit")
    public String editRewrite(@PathVariable Long id, @ModelAttribute Todo todo) {
        todo.setId(id);
        return "edit";
    }

    @GetMapping("/todos/{id}/delete")
    public String deleteConfirm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Todo todo = todoService.findById(id);

        if (todo == null) {
            redirectAttributes.addFlashAttribute("message", "見つかりませんでした");
            return "redirect:/todos";
        }

        model.addAttribute("todo", todo);
        return "delete";
    }

    @PostMapping("/todos/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        todoService.delete(id);
        redirectAttributes.addFlashAttribute("message", "削除しました");
        return "redirect:/todos";
    }

    @PostMapping("/todos/{id}/restore")
    public String restore(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        todoService.restore(id);
        redirectAttributes.addFlashAttribute("message", "戻しました");
        return "redirect:/todos?trash=1";
    }

    @PostMapping("/todos/{id}/pin")
    public String togglePinned(@PathVariable Long id,
            @RequestParam(name = "keyword", defaultValue = "") String keyword,
            @RequestParam(name = "category", defaultValue = "") String category,
            @RequestParam(name = "order", defaultValue = "asc") String order,
            @RequestParam(name = "showCompleted", defaultValue = "false") boolean showCompleted,
            @RequestParam(name = "page", defaultValue = "1") int page,
            RedirectAttributes redirectAttributes) {
        todoService.togglePinned(id);
        redirectAttributes.addAttribute("keyword", keyword);
        redirectAttributes.addAttribute("category", category);
        redirectAttributes.addAttribute("order", order.equals("desc") ? "desc" : "asc");
        redirectAttributes.addAttribute("showCompleted", showCompleted);
        redirectAttributes.addAttribute("page", Math.max(page, 1));
        return "redirect:/todos";
    }

    @PostMapping("/todos")
    public String insert(@ModelAttribute Todo todo, RedirectAttributes redirectAttributes) {
        todoService.create(todo);
        redirectAttributes.addFlashAttribute("message", "登録しました");
        return "redirect:/todos";
    }

    @PostMapping("/todos/{id}")
    public String update(@PathVariable Long id, @ModelAttribute Todo todo,
            RedirectAttributes redirectAttributes) {
        todo.setId(id);
        todoService.update(todo);
        redirectAttributes.addFlashAttribute("message", "保存しました");
        return "redirect:/todos";
    }
}
