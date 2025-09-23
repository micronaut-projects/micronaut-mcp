package io.modelcontextprotocol.server.http.tck.async;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.jsonschema.JsonSchema;
import io.micronaut.serde.annotation.Serdeable;

/**
 * @param location City name or zip code
 */
@JsonSchema
@Serdeable
public record GetWeatherInput(@NonNull String location) {
}
