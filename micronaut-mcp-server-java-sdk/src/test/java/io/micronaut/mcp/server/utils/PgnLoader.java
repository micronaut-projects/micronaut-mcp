package io.micronaut.mcp.server.utils;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.io.ResourceLoader;
import jakarta.inject.Singleton;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Singleton
public class PgnLoader {
    private static final Logger LOG = LoggerFactory.getLogger(PgnLoader.class);
    private final ResourceLoader resourceLoader;

    PgnLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @NonNull
    public Optional<String> loadPgn(@NonNull @NotNull @Positive Integer round) {
        Optional<InputStream> roundPgnInputStreamOptional = resourceLoader.getResourceAsStream("classpath:fidewwc2024/round_" + round + ".pgn");
        if (roundPgnInputStreamOptional.isEmpty()) {
            return Optional.empty();
        }
        InputStream inputStream = roundPgnInputStreamOptional.get();
        try {
            return Optional.of(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error(e.getMessage(), e);
            }
            return Optional.empty();
        }
    }
}
