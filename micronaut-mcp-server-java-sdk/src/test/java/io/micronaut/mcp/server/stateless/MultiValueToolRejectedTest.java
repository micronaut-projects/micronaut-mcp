package io.micronaut.mcp.server.stateless;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.annotations.Tool;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiValueToolRejectedTest {

    @Test
    void aToolReturningAFluxFailsTheStartup() {
        Map<String, Object> properties = Map.of(
            "micronaut.mcp.server.info.name", "mcp-server",
            "micronaut.mcp.server.info.version", "0.0.1",
            "micronaut.mcp.server.transport", "HTTP",
            "spec.name", "MultiValueToolRejectedTest");
        Throwable error = assertThrows(RuntimeException.class, () -> ApplicationContext.run(properties));
        while (error.getCause() != null && !(error instanceof IllegalStateException)) {
            error = error.getCause();
        }
        String message = error.getMessage();
        assertTrue(message.contains("FluxTools.words") && message.contains("may emit several values"), message);
    }

    @Requires(property = "spec.name", value = "MultiValueToolRejectedTest")
    @Singleton
    static class FluxTools {
        @Tool
        Flux<String> words() {
            return Flux.just("a", "b");
        }
    }
}
