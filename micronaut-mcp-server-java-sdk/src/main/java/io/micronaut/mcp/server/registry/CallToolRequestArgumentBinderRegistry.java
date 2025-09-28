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
package io.micronaut.mcp.server.registry;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.bind.ArgumentBinder;
import io.micronaut.core.bind.ArgumentBinderRegistry;
import io.micronaut.core.type.Argument;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Default implementation of {@link ArgumentBinderRegistry} for {@link McpSchema.CallToolRequest} that provides a default binder and the ability add {@link TypedCallToRequestArgumentBinder}.
 */
@Internal
@Singleton
final class CallToolRequestArgumentBinderRegistry implements ArgumentBinderRegistry<McpSchema.CallToolRequest> {
    private final Map<Integer, TypedCallToRequestArgumentBinder> byType = new LinkedHashMap<>();
    private final DefaultCallToRequestArgumentBinder defaultBinder;

    CallToolRequestArgumentBinderRegistry(List<CallToolRequestArgumentBinder<?>> binders,
                                          DefaultCallToRequestArgumentBinder defaultBinder) {
        this.defaultBinder = defaultBinder;
        for (CallToolRequestArgumentBinder binder : binders) {
            addBinder(binder);
        }
    }

    @Override
    public <T> void addArgumentBinder(ArgumentBinder<T, McpSchema.CallToolRequest> binder) {
        addBinder(binder);
    }

    @Override
    public <T> Optional<ArgumentBinder<T, McpSchema.CallToolRequest>> findArgumentBinder(Argument<T> argument) {
        TypedCallToRequestArgumentBinder<T> binder = byType.get(argument.typeHashCode());
        if (binder != null) {
            return Optional.ofNullable(byType.get(Argument.of(argument.getType()).typeHashCode()));
        }
        return Optional.ofNullable(defaultBinder);
    }

    private <T> void addBinder(ArgumentBinder<T, McpSchema.CallToolRequest> binder) {
        if (binder instanceof TypedCallToRequestArgumentBinder<?> typedRequestArgumentBinder) {
            byType.put(typedRequestArgumentBinder.argumentType().typeHashCode(), typedRequestArgumentBinder);
        }
    }
}
