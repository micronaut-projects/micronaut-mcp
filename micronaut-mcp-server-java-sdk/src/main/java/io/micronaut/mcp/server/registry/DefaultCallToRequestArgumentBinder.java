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
import io.micronaut.mcp.annotations.ToolArg;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;

import static io.micronaut.mcp.annotations.ToolArg.ELEMENT_NAME;

/**
 * Default implementation of {@link CallToolRequestArgumentBinder}.
 * @param <T> the argument type
 */
@Internal
@Singleton
final class DefaultCallToRequestArgumentBinder<T> extends AbstractMcpPrimitiveArgumentBinder<T> implements CallToolRequestArgumentBinder<T> {
    private static final String MEMBER_NAME = "name";

    DefaultCallToRequestArgumentBinder(ConversionService conversionService) {
        super(conversionService);
    }

    @Override
    public BindingResult<T> bind(ArgumentConversionContext<T> context, McpSchema.CallToolRequest source) {
        return bind(context, source::arguments, ToolArg.class.getName(), MEMBER_NAME, ELEMENT_NAME);
    }
}
