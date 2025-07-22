package example.micronaut.weather;

public record Properties(
    String id,
    String areaDesc,
    String event,
    String severity,
    String description,
    String instruction) {
}
