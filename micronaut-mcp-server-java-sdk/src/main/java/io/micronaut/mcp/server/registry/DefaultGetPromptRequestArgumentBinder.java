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
import io.micronaut.mcp.annotations.PromptArg;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;

import static io.micronaut.mcp.annotations.PromptArg.ELEMENT_NAME;

/**
 * Default implementation of {@link GetPromptRequestArgumentBinder}.
 * @param <T> the argument type
 */
@Internal
@Singleton
non-sealed class DefaultGetPromptRequestArgumentBinder<T> extends AbstractMcpPrimitiveArgumentBinder<T> implements GetPromptRequestArgumentBinder<T> {
    private static final String MEMBER_NAME = "name";

    DefaultGetPromptRequestArgumentBinder(ConversionService conversionService) {
        super(conversionService);
    }

    @Override
    public BindingResult<T> bind(ArgumentConversionContext<T> context, McpSchema.GetPromptRequest source) {
        return bind(context, source::arguments, PromptArg.class.getName(), MEMBER_NAME, ELEMENT_NAME);
    }
}
