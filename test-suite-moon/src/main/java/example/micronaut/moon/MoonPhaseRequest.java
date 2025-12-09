package example.micronaut.moon;

import org.jspecify.annotations.NonNull;
import io.micronaut.jsonschema.JsonSchema;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

/**
 *
 * @param date the date in format yyyy-MM-dd
 */
@JsonSchema
@Serdeable
public record MoonPhaseRequest(@NonNull @NotNull @PastOrPresent LocalDate date) {
}
