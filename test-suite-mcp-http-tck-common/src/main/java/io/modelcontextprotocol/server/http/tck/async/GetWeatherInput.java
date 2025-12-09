package io.modelcontextprotocol.server.http.tck.async;

import org.jspecify.annotations.NonNull;
import io.micronaut.jsonschema.JsonSchema;
import io.micronaut.serde.annotation.Serdeable;

/**
 * @param location City name or zip code
 */
@JsonSchema
@Serdeable
public record GetWeatherInput(@NonNull String location) {
}
