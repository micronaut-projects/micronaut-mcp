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

import java.util.Optional;

/**
 * Default implementation of {@link ArgumentBinderRegistry} for {@link McpSchema.ReadResourceRequest} that provides a default binder.
 */
@Internal
@Singleton
final class ReadResourceRequestArgumentBinderRegistry implements ArgumentBinderRegistry<McpSchema.ReadResourceRequest> {
    private final DefaultReadResourceRequestArgumentBinder defaultBinder;

    ReadResourceRequestArgumentBinderRegistry(DefaultReadResourceRequestArgumentBinder defaultBinder) {
        this.defaultBinder = defaultBinder;
    }

    @Override
    public <T> Optional<ArgumentBinder<T, McpSchema.ReadResourceRequest>> findArgumentBinder(Argument<T> argument) {
        return Optional.of(defaultBinder);
    }
}
