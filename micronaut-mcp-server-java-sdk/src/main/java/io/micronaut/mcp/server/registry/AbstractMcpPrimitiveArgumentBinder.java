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
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.bind.ArgumentBinder;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.ConversionError;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.convert.exceptions.ConversionErrorException;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static io.micronaut.core.bind.ArgumentBinder.BindingResult.*;

/**
 * Abstract base class for binding s from a map supplier using the conversion service.
 * @param <T> The source
 */
@Internal
abstract class AbstractMcpPrimitiveArgumentBinder<T> {
    private final ConversionService conversionService;

    AbstractMcpPrimitiveArgumentBinder(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @NonNull
    public ArgumentBinder.BindingResult<T> bind(@NonNull ArgumentConversionContext<T> context,
                                                @NonNull Supplier<Map<String, Object>> mapSupplier,
                                                @Nullable String argAnn,
                                                @Nullable String member,
                                                @Nullable String defaultMemberValue) {
        Map<String, Object> m = mapSupplier.get();
        String name = argumentName(context, argAnn, member, defaultMemberValue);
        if (!m.containsKey(name)) {
            return EMPTY;
        }
        Object obj = m.get(name);
        if (obj == null) {
            return EMPTY;
        }
        try {
            T result = conversionService.convertRequired(obj, context.getArgument().getType());
            return () -> Optional.of(result);
        } catch (ConversionErrorException e) {
             return new ArgumentBinder.BindingResult<>() {
                @Override
                public Optional<T> getValue() {
                    return Optional.empty();
                }

                @Override
                public List<ConversionError> getConversionErrors() {
                    return List.of(new ConversionError() {
                        @Override
                        public Exception getCause() {
                            return e;
                        }

                        @Override
                        public Optional<Object> getOriginalValue() {
                            return Optional.ofNullable(m);
                        }
                    });
                }
            };
        }
    }

    @NonNull
    private String argumentName(@NonNull ArgumentConversionContext<T> context,
                                @Nullable String argAnn,
                                @Nullable String member,
                                @Nullable String defaultMemberValue) {
        return argumentName(context.getArgument(), argAnn, member, defaultMemberValue);
    }

    @NonNull
    private String argumentName(@NonNull Argument<T> argument,
                                @Nullable String argAnn,
                                @Nullable String member,
                                @Nullable String defaultMemberValue) {
        if (argAnn != null && member != null && defaultMemberValue != null) {
            Optional<String> nameOptional = argument.findAnnotation(argAnn)
                .flatMap(ann -> ann.stringValue(member));
            if (nameOptional.isPresent()) {
                String name = nameOptional.get();
                if (StringUtils.isNotEmpty(name) && !name.equals(defaultMemberValue)) {
                    return name;
                }
            }
        }
        return argument.getName();
    }
}
