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

import io.micronaut.context.BeanContext;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.type.ReturnType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.mcp.server.exceptions.McpErrorExceptionMapper;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

import io.micronaut.inject.BeanDefinition;

import java.util.stream.Stream;

/**
 * The abstract registry.
 * @param <S> Sync Specification
 * @param <A> Async Specification
 * @param <SS> Stateless Sync Specification
 * @param <SA> Stateless Async Specification
 */
@Internal
abstract sealed class AbstractMcpMethodRegistry<S, A, SS, SA> implements McpPrimitiveRegistry<S, A, SS, SA>
    permits CompletionRegistry, PromptRegistry, ResourceRegistry, ResourceTemplateRegistry, ToolRegistry {
    protected static final String MEMBER_DESCRIPTION = "description";
    protected static final String KEY_TYPE = "type";
    protected static final String DESCRIPTION_PROPERTY = "description";
    protected static final String MIME_TYPE_PROPERTY = "mimeType";
    protected static final String NAME_PROPERTY = "name";
    protected static final String TITLE_PROPERTY = "title";
    protected static final String URI_PROPERTY = "uri";
    protected static final String URI_TEMPLATE_PROPERTY = "uriTemplate";
    protected static final String MEMBER_NAME = "name";
    protected static final String MEMBER_TITLE = "title";
    private static final Logger LOG = LoggerFactory.getLogger(AbstractMcpMethodRegistry.class);
    protected final List<Method<Object>> methods = new ArrayList<>();
    protected final BeanContext beanContext;
    private final List<McpErrorExceptionMapper<?>> exceptionMappers;
    private final Map<Class<? extends Throwable>, McpErrorExceptionMapper<? extends Throwable>> classToExceptionMapper = new ConcurrentHashMap<>();

    AbstractMcpMethodRegistry(List<McpErrorExceptionMapper<? extends Throwable>> exceptionMappers,
                              BeanContext beanContext) {
        this.exceptionMappers = exceptionMappers;
        this.beanContext = beanContext;
    }

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

    protected McpError mcpError(Exception ex) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(ex.getMessage(), ex);
        }
        McpErrorExceptionMapper<? extends Throwable> exceptionMapper = getExceptionMapper(ex.getClass());
        if (exceptionMapper != null) {
            return mapException(exceptionMapper, ex);
        }
        return McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR).build();
    }

    protected Map<Argument<?>, Object> prepareBoundVariables(ExecutableMethod<?, ?> executable, List<?> parameters) {
        Map<Argument<?>, Object> preBound = CollectionUtils.newHashMap(executable.getArguments().length);
        for (Argument<?> argument : executable.getArguments()) {
            Class<?> type = argument.getType();
            for (Object object : parameters) {
                if (type.isInstance(object)) {
                    preBound.put(argument, object);
                    break;
                }
            }
        }
        return preBound;
    }

    /**
     * The type of what a method answers with. For a method that returns a {@link CompletionStage} or a publisher of
     * one item it is the type of that item, and for a publisher of several items a list of them.
     *
     * @param method a method of a tool, a prompt, a resource or a completion
     * @return the type of its answer
     */
    protected static Argument<?> resultArgument(ExecutableMethod<?, ?> method) {
        ReturnType<?> returnType = method.getReturnType();
        Argument<?> declared = returnType.asArgument();
        if (!returnType.isAsync() && !returnType.isReactive()) {
            return declared;
        }
        Argument<?> item = declared.getFirstTypeVariable().orElse(Argument.OBJECT_ARGUMENT);
        return returnType.isReactive() && !returnType.isSingleResult() ? Argument.listOf(item) : item;
    }

    /**
     * What a method answers with, as a publisher, where it returned a {@link CompletionStage} or a publisher: the
     * item a stage or a publisher of one item settles with, and the items of a publisher of several as a list.
     *
     * @param method   the method
     * @param returned what it returned
     * @return a publisher of one value, which is empty where the method answered with nothing; or null for a method
     *     that returned its answer itself
     */
    @SuppressWarnings("unchecked")
    protected @Nullable Mono<Optional<Object>> later(ExecutableMethod<?, ?> method, @Nullable Object returned) {
        Mono<Object> answer;
        if (returned instanceof CompletionStage<?> stage) {
            answer = Mono.fromCompletionStage((CompletionStage<Object>) stage);
        } else if (returned != null && (returned instanceof Publisher<?> || method.getReturnType().isReactive())) {
            Publisher<Object> publisher = Publishers.convertToPublisher(beanContext.getConversionService(), returned);
            boolean single = method.getReturnType().isReactive() ? method.getReturnType().isSingleResult() : Publishers.isSingle(returned.getClass());
            answer = single ? Mono.from(publisher) : Flux.from(publisher).collectList().map(items -> items);
        } else {
            return null;
        }
        return answer.map(Optional::of).defaultIfEmpty(Optional.empty());
    }

    /**
     * What a method answers with, for a handler that answers as it returns: where the method returned a
     * {@link CompletionStage} or a publisher, the thread waits for it.
     *
     * @param method   the method
     * @param returned what it returned
     * @return its answer
     */
    protected @Nullable Object settled(ExecutableMethod<?, ?> method, @Nullable Object returned) {
        Mono<Optional<Object>> later = later(method, returned);
        if (later == null) {
            return returned;
        }
        try {
            Optional<Object> answer = later.block();
            return answer == null ? null : answer.orElse(null);
        } catch (RuntimeException e) {
            // what the stage or the publisher failed with, and not what the wait wrapped it in
            if (Exceptions.unwrap(e) instanceof RuntimeException failure) {
                throw failure;
            }
            throw e;
        }
    }

    @Nullable
    private McpErrorExceptionMapper<? extends Throwable> getExceptionMapper(@NonNull Class<? extends Throwable> exceptionClass) {
        return classToExceptionMapper.computeIfAbsent(exceptionClass, aClass -> {
            for (McpErrorExceptionMapper<?> exceptionMapper : exceptionMappers) {
                if (exceptionMapper.canMap(aClass)) {
                    return exceptionMapper;
                }
            }
            return null;
        });
    }

    @Nullable
    protected McpTransportContext resolveMcpTransportContext(Object ctx) {
        McpTransportContext mcpTransportContext = null;
        if (ctx instanceof McpTransportContext mcpCtx) {
            mcpTransportContext = mcpCtx;
        }
        if (mcpTransportContext == null && ctx instanceof McpSyncServerExchange ex) {
            mcpTransportContext = ex.transportContext();
        }
        if (mcpTransportContext == null && ctx instanceof McpAsyncServerExchange ex) {
            mcpTransportContext = ex.transportContext();
        }
        return mcpTransportContext;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Exception> McpError mapException(McpErrorExceptionMapper<? extends Throwable> mapper, T ex) {
        return ((McpErrorExceptionMapper<T>) mapper).map(ex);
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
