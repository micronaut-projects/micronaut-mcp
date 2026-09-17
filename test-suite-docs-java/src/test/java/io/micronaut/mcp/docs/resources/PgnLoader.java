package io.micronaut.mcp.docs.resources;

import io.micronaut.core.io.ResourceLoader;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Loads the PGN of a round of the World Chess Championship 2024 from the classpath.
 */
@Singleton
public class PgnLoader {
    private static final Logger LOG = LoggerFactory.getLogger(PgnLoader.class);

    private final ResourceLoader resourceLoader;

    PgnLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public Optional<String> loadPgn(Integer round) {
        Optional<InputStream> pgnInputStream = resourceLoader.getResourceAsStream("classpath:fidewwc2024/round_" + round + ".pgn");
        if (pgnInputStream.isEmpty()) {
            return Optional.empty();
        }
        try (InputStream inputStream = pgnInputStream.get()) {
            return Optional.of(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            return Optional.empty();
        }
    }
}
