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

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;

import java.util.stream.Collectors;

/**
 * {@link McpErrorExceptionMapper} for {@link InvalidFormatException}.
 */
@Requires(classes = InvalidFormatException.class)
@Singleton
@Internal
class InvalidFormatExceptionMcpErrorMapper implements McpErrorExceptionMapper<InvalidFormatException> {
    @Override
    public boolean canMap(Class<? extends Throwable> clazz) {
        return InvalidFormatException.class.isAssignableFrom(clazz);
    }

    @Override
    public McpError map(InvalidFormatException exception) {
        String message = conciseMessage(exception);
        return McpError.builder(McpSchema.ErrorCodes.INVALID_PARAMS)
            .message(message)
            .build();
    }

    private static String conciseMessage(InvalidFormatException e) {
        String path = e.getPath() == null || e.getPath().isEmpty() ? "" :
            e.getPath().stream()
                .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : ("[" + ref.getIndex() + "]"))
                .collect(Collectors.joining("."));
        String value = String.valueOf(e.getValue());
        if (!path.isEmpty()) {
            return "Invalid value for '" + path + "': '" + value + "'";
        }
        String original = e.getOriginalMessage();
        return original != null ? original : "Invalid value '" + value + "'";
    }
}
