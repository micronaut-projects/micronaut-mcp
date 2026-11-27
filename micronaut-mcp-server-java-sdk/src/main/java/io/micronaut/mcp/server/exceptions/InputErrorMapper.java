/*
 * Copyright 2017-2025 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.mcp.server.exceptions;

import io.micronaut.core.annotation.Internal;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import org.jspecify.annotations.Nullable;

/**
 * Marks the built-in mappers of failures to bind or validate the arguments of a primitive. A tool reports the errors
 * they map as tool execution errors, which the model can see and correct, while the errors of any other mapper are
 * sent as protocol errors.
 *
 * @param <T> Exception to Map
 * @since 2.2.0
 */
@Internal
public interface InputErrorMapper<T extends Throwable> extends McpErrorExceptionMapper<T> {

    /**
     * The message of the error, which a tool sends as a tool execution error without building an {@link McpError}.
     *
     * @param exception The exception to map
     * @return The message of the error, or {@code null} when the exception has none
     */
    @Nullable String message(T exception);

    /**
     * @param exception The exception to map
     * @return A client-facing input error message, including when the exception has no message
     */
    default String messageOrDefault(T exception) {
        String message = message(exception);
        return message == null || message.isBlank() ? "Invalid arguments" : message;
    }

    @Override
    default McpError map(T exception) {
        return McpError.builder(McpSchema.ErrorCodes.INVALID_PARAMS)
            .message(messageOrDefault(exception))
            .build();
    }
}
