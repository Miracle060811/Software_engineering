package com.travelmate.service.impl;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TravelWeatherServiceTests {

    @Test
    void parsesForecastByExactDate() throws Exception {
        TravelWeatherService service = new TravelWeatherService();
        LocalDate date = LocalDate.of(2026, 8, 26);
        TravelPlaceService.VerifiedPlace place = new TravelPlaceService.VerifiedPlace(
                "杭州", "杭州", "杭州市, 浙江省, 中国", "CN", 30.25, 120.16, "OpenStreetMap Nominatim");
        String response = """
                {"daily":{"time":["2026-08-26"],"weather_code":[61],
                 "temperature_2m_max":[31.2],"temperature_2m_min":[24.1],
                 "precipitation_probability_max":[70]}}
                """;

        TravelWeatherService.WeatherSnapshot result = service.parseForecast(place, date, response);
        assertEquals("有雨", result.condition());
        assertEquals(24.1, result.minTemperature());
        assertEquals(31.2, result.maxTemperature());
        assertEquals(70, result.precipitationProbability());
    }
}
