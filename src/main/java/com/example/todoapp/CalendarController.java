package com.example.todoapp;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CalendarController {

    @GetMapping("/calendar")
    public String calendar(@RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month, Model model) {
        YearMonth currentMonth = YearMonth.now();
        int displayYear = year != null ? year : currentMonth.getYear();
        int displayMonth = month != null ? month : currentMonth.getMonthValue();
        YearMonth displayedMonth = YearMonth.of(displayYear, displayMonth);

        LocalDate firstDay = displayedMonth.atDay(1);
        LocalDate lastDay = displayedMonth.atEndOfMonth();

        model.addAttribute("year", displayYear);
        model.addAttribute("month", displayMonth);
        model.addAttribute("firstDay", firstDay);
        model.addAttribute("lastDay", lastDay);
        model.addAttribute("weeks", createWeeks(displayedMonth));
        model.addAttribute("previousMonth", displayedMonth.minusMonths(1));
        model.addAttribute("nextMonth", displayedMonth.plusMonths(1));

        return "calendar";
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
