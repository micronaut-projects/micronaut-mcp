package example.micronaut.weather;

import java.util.List;

public record ForecastProperties(
    List<Period> periods) {
}
