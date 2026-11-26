package io.micronaut.mcp.server.stateless;

import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.annotations.ToolArg;
import io.micronaut.mcp.server.exceptions.McpErrorExceptionMapper;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;
import jakarta.validation.constraints.Max;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Tools shared by the tool execution error tests of both execution models.
 */
@Requires(property = "tool-errors.tools")
@Singleton
class ToolErrorTools {
    @Tool
    String failing() {
        throw new IllegalStateException("the backend is down");
    }

    @Tool
    Mono<String> failingPublisher() {
        return Mono.error(new IllegalStateException("the backend is down"));
    }

    @Tool
    String failingWithoutMessage() {
        throw new IllegalStateException();
    }

    @Tool
    String protocolError() {
        throw McpError.builder(McpSchema.ErrorCodes.INVALID_PARAMS).message("unknown city").build();
    }

    @Tool
    String limited(@ToolArg @Max(10) int count) {
        return String.valueOf(count);
    }

    @Tool
    Mono<String> checkedPublisher() {
        return Mono.error(new IOException("boom"));
    }

    @Tool
    CompletableFuture<String> checkedFuture() {
        return CompletableFuture.failedFuture(new IOException("boom"));
    }

    @Tool
    String mapped() throws CityNotFoundException {
        throw new CityNotFoundException();
    }

    @Tool
    Mono<String> mappedPublisher() {
        return Mono.error(new CityNotFoundException());
    }

    @Tool
    Object unserializable() {
        return new Unserializable();
    }

    static final class Unserializable {
    }

    static final class CityNotFoundException extends Exception {
        CityNotFoundException() {
            super("no such city");
        }
    }

    @Requires(property = "tool-errors.tools")
    @Singleton
    static final class CityNotFoundMapper implements McpErrorExceptionMapper<CityNotFoundException> {
        @Override
        public boolean canMap(Class<? extends Throwable> clazz) {
            return CityNotFoundException.class.isAssignableFrom(clazz);
        }

        @Override
        public McpError map(CityNotFoundException exception) {
            return McpError.builder(McpSchema.ErrorCodes.INVALID_PARAMS).message(exception.getMessage()).build();
        }
    }
}
