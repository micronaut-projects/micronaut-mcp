package example.micronaut.weather;

public record Feature(
    String id,
    String type,
    Object geometry,
    Properties properties) {
}
