package example.micronaut.weather.model;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record Properties(
    String id,
    String areaDesc,
    String event,
    String severity,
    String description,
    String instruction) {
}
