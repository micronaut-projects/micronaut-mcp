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
 * @param <S> Sync Specification
 * @param <A> Async Specification
 * @param <SS> Stateless Sync Specification
 * @param <SA> Stateless Async Specification
 */
@Singleton
@Internal
abstract sealed class AbstractMcpMethodRegistry<S, A, SS, SA> implements McpPrimitiveRegistry<S, A, SS, SA>
    permits PromptRegistry, ToolRegistry, ResourceRegistry {
    protected final List<Method<Object>> methods = new ArrayList<>();

    /**
     * Adds a new method to the registry by associating it with a given bean definition.
     *
     * @param beanDefinition the bean definition that declares or provides the method
     * @param method         the executable method to be added to the registry
     */
    public final void addMethod(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {
        methods.add(new Method(beanDefinition, method));
    }

    /**
     * Returns a stream of the methods currently stored in the registry.
     * After the stream is consumed or closed, the underlying list of methods will be cleared.
     *
     * @return a stream of methods from the registry
     */
    protected final Stream<Method<Object>> drainMethods() {
        return methods.stream().onClose(methods::clear);
    }

    /**
     * A record representing a method associated with a bean definition.
     * This is used within the method registry to encapsulate the information of
     * a specific method and its corresponding bean definition.
     *
     * @param beanDefinition The bean definition that declares or provides the method.
     * @param method         The executable method being encapsulated.
     * @param <B>            The type of the bean.
     */
    protected record Method<B>(BeanDefinition<B> beanDefinition,
                               ExecutableMethod<B, Object> method) {
    }
}
