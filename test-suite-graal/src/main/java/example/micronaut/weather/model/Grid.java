package example.micronaut.weather.model;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record Grid(String id, String x, String y) {
}
