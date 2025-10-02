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
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.ConversionService;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;

import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of {@link CompleteRequestArgumentBinder}.
 * @param <T> the argument type
 */
@Singleton
@Internal
final class DefaultCompleteRequestArgumentBinder<T> extends AbstractMcpPrimitiveArgumentBinder<T> implements CompleteRequestArgumentBinder<T> {
    DefaultCompleteRequestArgumentBinder(ConversionService conversionService) {
        super(conversionService);
    }

    @Override
    public BindingResult<T> bind(ArgumentConversionContext<T> context, McpSchema.CompleteRequest source) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put(source.argument().name(), source.argument().value());
        if (source.meta() != null) {
            arguments.putAll(source.meta());
        }
        return bind(context, () -> arguments, null, null, null);
    }
}
