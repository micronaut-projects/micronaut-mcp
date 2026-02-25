package example.micronaut.moon;

import io.micronaut.core.annotation.Introspected;
import org.jspecify.annotations.NonNull;
import io.micronaut.jsonschema.JsonSchema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

/**
 *
 * @param date the date in format yyyy-MM-dd
 */
@JsonSchema
@Introspected
public record MoonPhaseRequest(@NonNull @NotNull @PastOrPresent LocalDate date) {
}
