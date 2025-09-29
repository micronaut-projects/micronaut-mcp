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

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.bind.exceptions.UnsatisfiedArgumentException;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;

/**
 * {@link McpErrorExceptionMapper} for {@link io.micronaut.core.bind.exceptions.UnsatisfiedArgumentException}.
 */
@Requires(classes = UnsatisfiedArgumentException.class)
@Singleton
@Internal
class UnsatisfiedArgumentExceptionMcpErrorMapper implements McpErrorExceptionMapper<UnsatisfiedArgumentException> {
    @Override
    public boolean canMap(Class<? extends Throwable> clazz) {
        return UnsatisfiedArgumentException.class.isAssignableFrom(clazz);
    }

    @Override
    public McpError map(UnsatisfiedArgumentException exception) {
        String message = exception.getMessage();
        return McpError.builder(McpSchema.ErrorCodes.INVALID_PARAMS)
            .message(message)
            .build();
    }
}
