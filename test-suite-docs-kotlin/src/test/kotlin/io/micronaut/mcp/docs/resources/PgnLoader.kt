package io.micronaut.mcp.docs.resources

import io.micronaut.core.io.ResourceLoader
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.Optional

/**
 * Loads the PGN of a round of the World Chess Championship 2024 from the classpath.
 */
@Singleton
class PgnLoader(private val resourceLoader: ResourceLoader) {

    fun loadPgn(round: Int): Optional<String> {
        val pgnInputStream = resourceLoader.getResourceAsStream("classpath:fidewwc2024/round_$round.pgn")
        if (pgnInputStream.isEmpty) {
            return Optional.empty()
        }
        try {
            pgnInputStream.get().use { inputStream ->
                return Optional.of(String(inputStream.readAllBytes(), StandardCharsets.UTF_8))
            }
        } catch (e: IOException) {
            LOG.error(e.message, e)
            return Optional.empty()
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(PgnLoader::class.java)
    }
}
