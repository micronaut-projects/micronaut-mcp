package example.micronaut.moon;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.jsonschema.JsonSchema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 *
 * @param date the date in format yyyy-MM-dd
 */
@JsonSchema
@Introspected
public record MoonPhaseRequest(@NonNull @NotNull LocalDate date) {
}
