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
import io.micronaut.serde.exceptions.SerdeException;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * {@link McpErrorExceptionMapper} for {@link SerdeException}.
 */
@Requires(classes = SerdeException.class)
@Singleton
@Internal
final class SerdeExceptionMcpErrorMapper implements InputErrorMapper<SerdeException> {
    @Override
    public boolean canMap(Class<? extends Throwable> clazz) {
        return SerdeException.class.isAssignableFrom(clazz);
    }

    @Override
    public @Nullable String message(SerdeException exception) {
        return conciseMessage(exception);
    }

    /**
     * The message of the cause, which says what was wrong with the value, or the message of the exception when the
     * cause has none, because the SDK requires a message.
     */
    private static String conciseMessage(SerdeException e) {
        Throwable cause = e.getCause();
        String causeMessage = cause != null ? cause.getMessage() : null;
        return causeMessage != null && !causeMessage.isBlank() ? causeMessage : e.getMessage();
    }
}
