package example.micronaut.weather.model;

import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

@Serdeable
public record ForecastProperties(
    List<Period> periods) {
}
