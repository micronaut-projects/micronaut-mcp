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
import io.micronaut.core.convert.ConversionError;
import io.micronaut.core.type.Argument;
import io.micronaut.json.JsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * {@link TypedCallToRequestArgumentBinder} which uses {@link JsonMapper} to bind to a specific type.
 * @param <T> The argument type
 */
@Internal
class JsonMapperTypedCallToRequestArgumentBinder<T> implements TypedCallToRequestArgumentBinder<T> {
    private static final Logger LOG = LoggerFactory.getLogger(JsonMapperTypedCallToRequestArgumentBinder.class);
    private final Argument<T> argument;
    private final JsonMapper jsonMapper;

    JsonMapperTypedCallToRequestArgumentBinder(Class<T> clazz, JsonMapper jsonMapper) {
        this.argument = Argument.of(clazz);
        this.jsonMapper = jsonMapper;
    }

    @Override
    public Argument<T> argumentType() {
        return argument;
    }

    @Override
    public BindingResult<T> bind(ArgumentConversionContext<T> context, McpSchema.CallToolRequest source) {
        try {
            String payload = jsonMapper.writeValueAsString(source.arguments());
            Argument<?> argument = context.getArgument();
            Class<?> classInputSchema = argument.getType();
            T obj = (T) jsonMapper.readValue(payload, classInputSchema);
            return () -> Optional.ofNullable(obj);

        } catch (IOException ex) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Error binding argument [" + context.getArgument().getName() + "] of type: " + context.getArgument().getType(), ex);
            }
            return new BindingResult<>() {
                @Override
                public Optional<T> getValue() {
                    return Optional.empty();
                }

                @Override
                public List<ConversionError> getConversionErrors() {
                    return List.of(new ConversionError() {
                        @Override
                        public Exception getCause() {
                            return ex;
                        }

                        @Override
                        public Optional<Object> getOriginalValue() {
                            return Optional.ofNullable(source.arguments());
                        }
                    });
                }
            };
        }
    }
}
