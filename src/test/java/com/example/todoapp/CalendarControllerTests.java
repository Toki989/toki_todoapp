package com.example.todoapp;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CalendarControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void weekViewStartsOnSundayAndEndsSevenDaysLater() throws Exception {
        mockMvc.perform(get("/calendar")
                .param("view", "week")
                .param("date", "2026-09-02"))
                .andExpect(status().isOk())
                .andExpect(view().name("calendar"))
                .andExpect(model().attribute("firstDay", LocalDate.of(2026, 8, 30)))
                .andExpect(model().attribute("lastDay", LocalDate.of(2026, 9, 5)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "data-from=\"2026-08-30\" data-to=\"2026-09-05\" data-view=\"week\"")));
    }

    @Test
    void monthViewCanSwitchToWeekView() throws Exception {
        mockMvc.perform(get("/calendar")
                .param("year", "2026")
                .param("month", "9"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "href=\"/calendar?view=week&amp;date=2026-09-01\"")));
    }
}
