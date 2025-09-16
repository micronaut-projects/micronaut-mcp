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
import jakarta.inject.Singleton;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;

import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@link McpErrorExceptionMapper} for {@link ConstraintViolationException}.
 */
@Singleton
@Internal
class ConstraintViolationExceptionMcpErrorMapper implements McpErrorExceptionMapper<ConstraintViolationException> {
    @Override
    public boolean canMap(Class<? extends Throwable> clazz) {
        return ConstraintViolationException.class.isAssignableFrom(clazz);
    }

    @Override
    public McpError map(ConstraintViolationException exception) {
        String message = conciseMessage(exception);
        return McpError.builder(McpSchema.ErrorCodes.INVALID_PARAMS)
            .message(message)
            .build();
    }

    private static String conciseMessage(ConstraintViolationException e) {
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        if (violations == null || violations.isEmpty()) {
            return e.getMessage();
        }
        return violations.stream()
            .map(ConstraintViolationExceptionMcpErrorMapper::formatViolation)
            .collect(Collectors.joining("; "));
    }

    private static String formatViolation(ConstraintViolation<?> v) {
        String path = lastSegment(v.getPropertyPath());
        String msg = v.getMessage();
        if (path.isEmpty()) {
            return msg;
        }
        return path + ": " + msg;
    }

    private static String lastSegment(Path propertyPath) {
        if (propertyPath == null) {
            return "";
        }
        String lastName = "";
        Integer lastIndex = null;
        Iterator<Path.Node> it = propertyPath.iterator();
        while (it.hasNext()) {
            Path.Node node = it.next();
            String name = node.getName();
            if (name != null && !name.isEmpty()) {
                lastName = name;
                lastIndex = node.getIndex();
            }
        }
        if (lastName.isEmpty()) {
            return "";
        }
        return lastIndex != null ? lastName + '[' + lastIndex + ']' : lastName;
    }
}
