package example.micronaut.weather.model;

import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

@Serdeable
public record Alerts(
    List<String> context,
    String type,
    List<Feature> features,
    String title,
    String updated) {
}
