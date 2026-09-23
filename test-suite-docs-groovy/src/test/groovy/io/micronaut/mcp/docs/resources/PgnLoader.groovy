package io.micronaut.mcp.docs.resources

import groovy.util.logging.Slf4j
import io.micronaut.core.io.ResourceLoader
import jakarta.inject.Singleton

import java.nio.charset.StandardCharsets

/**
 * Loads the PGN of a round of the World Chess Championship 2024 from the classpath.
 */
@Slf4j
@Singleton
class PgnLoader {
    private final ResourceLoader resourceLoader

    PgnLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader
    }

    Optional<String> loadPgn(Integer round) {
        Optional<InputStream> pgnInputStream = resourceLoader.getResourceAsStream("classpath:fidewwc2024/round_${round}.pgn")
        if (pgnInputStream.isEmpty()) {
            return Optional.empty()
        }
        try {
            pgnInputStream.get().withCloseable { InputStream inputStream ->
                Optional.of(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8))
            }
        } catch (IOException e) {
            log.error(e.message, e)
            Optional.empty()
        }
    }
}
