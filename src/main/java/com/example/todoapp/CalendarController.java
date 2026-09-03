package com.example.todoapp;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CalendarController {

    @GetMapping("/calendar")
    public String calendar(@RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(defaultValue = "month") String view,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {
        YearMonth currentMonth = YearMonth.now();
        int displayYear = year != null ? year : currentMonth.getYear();
        int displayMonth = month != null ? month : currentMonth.getMonthValue();
        YearMonth displayedMonth = YearMonth.of(displayYear, displayMonth);

        boolean weekView = "week".equals(view);
        LocalDate selectedDate = date != null
                ? date
                : (year != null || month != null ? displayedMonth.atDay(1) : LocalDate.now());
        LocalDate firstDay = weekView
                ? selectedDate.minusDays(selectedDate.getDayOfWeek().getValue() % 7)
                : displayedMonth.atDay(1);
        LocalDate lastDay = weekView ? firstDay.plusDays(6) : displayedMonth.atEndOfMonth();

        model.addAttribute("year", displayYear);
        model.addAttribute("month", displayMonth);
        model.addAttribute("view", weekView ? "week" : "month");
        model.addAttribute("firstDay", firstDay);
        model.addAttribute("lastDay", lastDay);
        model.addAttribute("weeks", weekView
                ? List.of(createWeek(firstDay))
                : createWeeks(displayedMonth));
        model.addAttribute("previousMonth", displayedMonth.minusMonths(1));
        model.addAttribute("nextMonth", displayedMonth.plusMonths(1));
        model.addAttribute("previousWeek", firstDay.minusWeeks(1));
        model.addAttribute("nextWeek", firstDay.plusWeeks(1));

        return "calendar";
    }

    private List<LocalDate> createWeek(LocalDate firstDay) {
        List<LocalDate> week = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            week.add(firstDay.plusDays(i));
        }
        return week;
    }

    private List<List<LocalDate>> createWeeks(YearMonth displayedMonth) {
        List<LocalDate> cells = new ArrayList<>();
        LocalDate firstDay = displayedMonth.atDay(1);
        int emptyCellsBeforeFirstDay = firstDay.getDayOfWeek().getValue() % 7;

        for (int i = 0; i < emptyCellsBeforeFirstDay; i++) {
            cells.add(null);
        }

        for (int day = 1; day <= displayedMonth.lengthOfMonth(); day++) {
            cells.add(displayedMonth.atDay(day));
        }

        while (cells.size() % 7 != 0) {
            cells.add(null);
        }

        List<List<LocalDate>> weeks = new ArrayList<>();
        for (int i = 0; i < cells.size(); i += 7) {
            weeks.add(new ArrayList<>(cells.subList(i, i + 7)));
        }
        return weeks;
    }
}
