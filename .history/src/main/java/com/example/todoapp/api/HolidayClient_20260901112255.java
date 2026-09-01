package com.example.todoapp.api;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HolidayClient {

    private static final String HOLIDAY_API_URL = "https://holidays-jp.github.io/api/v1/date.json";

    private final RestClient restClient;

    public HolidayClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    public HolidayFetchResult getHolidays() {
        try {
            Map<String, String> holidays = restClient.get()
                    .uri(HOLIDAY_API_URL)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            return new HolidayFetchResult(holidays == null ? Map.of() : holidays, false);
        } catch (RestClientException exception) {
            return new HolidayFetchResult(Map.of(), true);
        }
    }

    public record HolidayFetchResult(Map<String, String> holidays, boolean unavailable) {
    }
}
