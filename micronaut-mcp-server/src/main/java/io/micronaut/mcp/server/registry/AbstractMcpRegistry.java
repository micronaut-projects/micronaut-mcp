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
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * The abstract registry.
 */
@Singleton
@Internal
abstract class AbstractMcpRegistry {

    protected final List<Method<Object>> methods = new ArrayList<>();

    public final boolean addMethod(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {
        return methods.add(new Method(beanDefinition, method));
    }

    protected final Stream<Method<Object>> drainMethods() {
        return methods.stream().onClose(methods::clear);
    }

    protected record Method<B>(BeanDefinition<B> beanDefinition,
                               ExecutableMethod<B, Object> method) {
    }
}
