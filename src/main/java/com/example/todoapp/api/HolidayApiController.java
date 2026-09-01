package com.example.todoapp.api;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/holidays")
public class HolidayApiController {

    private final HolidayClient holidayClient;

    public HolidayApiController(HolidayClient holidayClient) {
        this.holidayClient = holidayClient;
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> getHolidays(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        HolidayClient.HolidayFetchResult result = holidayClient.getHolidays();

        Map<String, String> filteredHolidays = new LinkedHashMap<>();
        result.holidays().forEach((dateText, name) -> {
            LocalDate date = LocalDate.parse(dateText);
            if ((from == null || !date.isBefore(from))
                    && (to == null || !date.isAfter(to))) {
                filteredHolidays.put(dateText, name);
            }
        });

        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        if (result.unavailable()) {
            response.header("X-Holidays-Unavailable", "true");
        }
        return response.body(filteredHolidays);
    }
}
