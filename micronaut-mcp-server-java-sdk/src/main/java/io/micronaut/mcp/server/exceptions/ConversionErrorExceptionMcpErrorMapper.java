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
import io.micronaut.core.convert.exceptions.ConversionErrorException;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * {@link McpErrorExceptionMapper} for {@link ConversionErrorException}, thrown when the value of an argument cannot be
 * converted to the type of the parameter.
 */
@Singleton
@Internal
final class ConversionErrorExceptionMcpErrorMapper implements InputErrorMapper<ConversionErrorException> {
    @Override
    public boolean canMap(Class<? extends Throwable> clazz) {
        return ConversionErrorException.class.isAssignableFrom(clazz);
    }

    @Override
    public @Nullable String message(ConversionErrorException exception) {
        return exception.getMessage();
    }
}
