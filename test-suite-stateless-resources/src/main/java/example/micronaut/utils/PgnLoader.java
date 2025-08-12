package example.micronaut.utils;

import io.micronaut.core.annotation.NonNull;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Optional;

public interface PgnLoader {
    @NonNull
    Optional<String> loadPgn(@NonNull @NotNull @Positive Integer round);
}
