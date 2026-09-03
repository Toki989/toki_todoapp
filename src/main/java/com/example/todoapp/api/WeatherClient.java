package com.example.todoapp.api;

import java.net.http.HttpClient;
import java.net.URI;
import java.time.Duration;
import java.util.List;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Component
public class WeatherClient {

    private static final String WEATHER_API_URL = "https://api.open-meteo.com/v1/forecast"
            + "?latitude=35.6895&longitude=139.6917"
            + "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max"
            + "&timezone=Asia%2FTokyo&forecast_days=14";

    private final RestClient restClient;

    public WeatherClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    public WeatherFetchResult getWeather() {
        try {
            WeatherResponse weather = restClient.get()
                    .uri(URI.create(WEATHER_API_URL))
                    .retrieve()
                    .body(WeatherResponse.class);
            return new WeatherFetchResult(weather == null ? WeatherResponse.empty() : weather, false);
        } catch (RestClientException exception) {
            return new WeatherFetchResult(WeatherResponse.empty(), true);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WeatherResponse(Daily daily) {

        public static WeatherResponse empty() {
            return new WeatherResponse(new Daily(List.of(), List.of(), List.of(), List.of(), List.of()));
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Daily(
            List<String> time,
            @JsonProperty("weather_code") List<Integer> weatherCode,
            @JsonProperty("temperature_2m_max") List<Double> temperatureMax,
            @JsonProperty("temperature_2m_min") List<Double> temperatureMin,
            @JsonProperty("precipitation_probability_max") List<Integer> precipitationProbability) {
    }

    public record WeatherFetchResult(WeatherResponse weather, boolean unavailable) {
    }
}
