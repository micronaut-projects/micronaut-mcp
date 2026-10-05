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
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.util.StringUtils;
import io.micronaut.mcp.annotations.Audience;
import io.micronaut.mcp.annotations.Icon;
import io.micronaut.mcp.annotations.Meta;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.ReturnType;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.scheduling.annotation.ExecuteOn;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.mcp.server.context.DefaultMcpRequestContext;
import io.micronaut.mcp.server.context.McpRequestContext;
import io.micronaut.mcp.server.exceptions.McpErrorExceptionMapper;
import io.micronaut.mcp.server.observability.McpServerObserver;
import jakarta.inject.Inject;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;

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
    private List<McpServerObserver> observers = List.of();
    private final Map<Class<? extends Throwable>, Optional<McpErrorExceptionMapper<? extends Throwable>>> classToExceptionMapper = new ConcurrentHashMap<>();

    AbstractMcpMethodRegistry(List<McpErrorExceptionMapper<? extends Throwable>> exceptionMappers,
                              BeanContext beanContext) {
        this.exceptionMappers = exceptionMappers;
        this.beanContext = beanContext;
    }

    /**
     * @param observers The observers of the operations of the server
     */
    @Inject
    protected void setObservers(List<McpServerObserver> observers) {
        this.observers = List.copyOf(observers);
    }

    /**
     * Starts observing an operation.
     *
     * @param method The JSON-RPC method
     * @param name The name of the primitive
     * @return The observation, which does nothing when there are no observers
     */
    protected final McpServerObserver.Observation observe(String method, String name) {
        List<McpServerObserver> current = observers;
        if (current.isEmpty()) {
            return McpServerObserver.Observation.NOOP;
        }
        if (current.size() == 1) {
            return current.getFirst().start(method, name);
        }
        List<McpServerObserver.Observation> observations = current.stream().map(o -> o.start(method, name)).toList();
        return errorType -> observations.forEach(o -> o.stop(errorType));
    }

    /**
     * Observes a synchronous operation.
     *
     * @param method The JSON-RPC method
     * @param name The name of the primitive
     * @param operation The operation
     * @param <R> The result type
     * @return The result
     */
    protected final <R> R observed(String method, String name, Supplier<R> operation) {
        return observed(method, name, operation, result -> null);
    }

    /**
     * Observes a synchronous operation whose result can be an error.
     *
     * @param method The JSON-RPC method
     * @param name The name of the primitive
     * @param operation The operation
     * @param errorType The error type of a result, or {@code null} when it is not an error
     * @param <R> The result type
     * @return The result
     */
    protected final <R> R observed(String method, String name, Supplier<R> operation, Function<? super R, String> errorType) {
        if (observers.isEmpty()) {
            return operation.get();
        }
        McpServerObserver.Observation observation = observe(method, name);
        try {
            R result = operation.get();
            observation.stop(errorType.apply(result));
            return result;
        } catch (RuntimeException e) {
            observation.stop(e.getClass().getName());
            throw e;
        }
    }

    /**
     * Observes an asynchronous operation, from subscription to completion.
     *
     * @param method The JSON-RPC method
     * @param name The name of the primitive
     * @param operation The operation
     * @param <R> The result type
     * @return The result
     */
    protected final <R> Mono<R> observedAsync(String method, String name, Supplier<Mono<R>> operation) {
        return observedAsync(method, name, operation, result -> null);
    }

    /**
     * Observes an asynchronous operation whose result can be an error, from subscription to completion.
     *
     * @param method The JSON-RPC method
     * @param name The name of the primitive
     * @param operation The operation
     * @param errorType The error type of a result, or {@code null} when it is not an error
     * @param <R> The result type
     * @return The result
     */
    protected final <R> Mono<R> observedAsync(String method, String name, Supplier<Mono<R>> operation, Function<? super R, String> errorType) {
        if (observers.isEmpty()) {
            return operation.get();
        }
        return Mono.defer(() -> {
            McpServerObserver.Observation observation = observe(method, name);
            return operation.get()
                .doOnSuccess(result -> observation.stop(result != null ? errorType.apply(result) : null))
                .doOnError(e -> observation.stop(e.getClass().getName()))
                .doOnCancel(() -> observation.stop("cancelled"));
        });
    }

    /**
     * The types of the values that can be bound to a method parameter without an argument binder, in the order they are matched.
     * The first is always the {@link McpTransportContext}, the second the request.
     *
     * @return the bound parameter types
     */
    protected abstract Class<?>[] boundParameterTypes();

    /**
     * Adds a new method to the registry by associating it with a given bean definition.
     *
     * @param beanDefinition the bean definition that declares or provides the method
     * @param method         the executable method to be added to the registry
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public final void addMethod(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {
        methods.add(new Method(beanContext, beanDefinition, method, boundParameterTypes()));
    }

    /**
     * Returns a stream of the methods currently stored in the registry.
     *
     * @return a stream of methods from the registry
     */
    protected final Stream<Method<Object>> drainMethods() {
        return methods.stream();
    }

    @Override
    public final boolean isNotEmpty() {
        return !methods.isEmpty();
    }

    protected McpError mcpError(Exception ex) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(ex.getMessage(), ex);
        }
        McpErrorExceptionMapper<? extends Throwable> exceptionMapper = getExceptionMapper(ex.getClass());
        if (exceptionMapper != null) {
            return mapException(exceptionMapper, ex);
        }
        // The SDK requires a message
        String message = ex.getMessage() != null && !ex.getMessage().isBlank() ? ex.getMessage() : ex.getClass().getSimpleName();
        return McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR).message(message).build();
    }

    @Nullable
    private McpErrorExceptionMapper<? extends Throwable> getExceptionMapper(@NonNull Class<? extends Throwable> exceptionClass) {
        // Misses are cached too, so exceptions without a mapper do not scan the mappers again
        return classToExceptionMapper.computeIfAbsent(exceptionClass, aClass -> {
            for (McpErrorExceptionMapper<?> exceptionMapper : exceptionMappers) {
                if (exceptionMapper.canMap(aClass)) {
                    return Optional.of(exceptionMapper);
                }
            }
            return Optional.empty();
        }).orElse(null);
    }

    @Nullable
    private static McpTransportContext resolveMcpTransportContext(@Nullable Object ctx) {
        if (ctx instanceof McpTransportContext mcpCtx) {
            return mcpCtx;
        }
        if (ctx instanceof McpSyncServerExchange ex) {
            return ex.transportContext();
        }
        if (ctx instanceof McpAsyncServerExchange ex) {
            return ex.transportContext();
        }
        return null;
    }

    /**
     * The type of the value a method produces: the method return type, or its first type argument when the method returns
     * a reactive type or a {@link CompletionStage}.
     *
     * @param method The method
     * @return The result argument
     */
    protected static Argument<?> resultArgument(ExecutableMethod<?, ?> method) {
        ReturnType<?> returnType = method.getReturnType();
        if (returnType.isAsyncOrReactive()) {
            return returnType.getFirstTypeVariable().orElse(Argument.OBJECT_ARGUMENT);
        }
        return returnType.asArgument();
    }

    /**
     * @param method The method
     * @return The icons declared with {@link Icon}, or {@code null} when there are none
     */
    protected static @Nullable List<McpSchema.Icon> icons(ExecutableMethod<?, ?> method) {
        List<AnnotationValue<Icon>> values = method.getAnnotationValuesByType(Icon.class);
        if (values.isEmpty()) {
            return null;
        }
        List<McpSchema.Icon> icons = new ArrayList<>(values.size());
        for (AnnotationValue<Icon> value : values) {
            String[] sizes = value.stringValues("sizes");
            icons.add(new McpSchema.Icon(
                value.stringValue("src").orElseThrow(),
                value.stringValue("mimeType").filter(StringUtils::isNotEmpty).orElse(null),
                sizes.length == 0 ? null : List.of(sizes),
                value.stringValue("theme").filter(StringUtils::isNotEmpty).orElse(null)));
        }
        return icons;
    }

    /**
     * @param method The method
     * @return The {@code _meta} entries declared with {@link Meta}, or {@code null} when there are none
     */
    protected static @Nullable Map<String, Object> meta(ExecutableMethod<?, ?> method) {
        List<AnnotationValue<Meta>> values = method.getAnnotationValuesByType(Meta.class);
        if (values.isEmpty()) {
            return null;
        }
        Map<String, Object> meta = new LinkedHashMap<>(values.size());
        for (AnnotationValue<Meta> value : values) {
            meta.put(value.stringValue("key").orElseThrow(), value.stringValue().orElse(""));
        }
        return meta;
    }

    /**
     * @param annotation A resource or resource template annotation
     * @return The resource annotations it declares, or {@code null} when it declares none
     */
    protected static McpSchema.@Nullable Annotations resourceAnnotations(AnnotationValue<?> annotation) {
        Audience[] audience = annotation.enumValues("audience", Audience.class);
        double priority = annotation.doubleValue("priority").orElse(-1);
        if (audience.length == 0 && priority < 0) {
            return null;
        }
        List<McpSchema.Role> roles = audience.length == 0 ? null : Arrays.stream(audience)
            .map(a -> a == Audience.USER ? McpSchema.Role.USER : McpSchema.Role.ASSISTANT)
            .toList();
        return new McpSchema.Annotations(roles, priority < 0 ? null : priority);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Exception> McpError mapException(McpErrorExceptionMapper<? extends Throwable> mapper, T ex) {
        return ((McpErrorExceptionMapper<T>) mapper).map(ex);
    }

    /**
     * A method associated with a bean definition. What every invocation needs, other than the request, is resolved once:
     * the method parameters that receive the transport context or the request, and the bean when it is a singleton.
     *
     * @param <B> The type of the bean.
     */
    protected static final class Method<B> {
        private static final int REQUEST_CONTEXT = -1;
        private final BeanContext beanContext;
        private final BeanDefinition<B> beanDefinition;
        private final ExecutableMethod<B, Object> method;
        private final Argument<?>[] boundArguments;
        private final int[] boundIndexes;
        private final Argument<?> resultArgument;
        private final @Nullable String executorName;
        private volatile @Nullable B singleton;
        private volatile @Nullable Scheduler scheduler;

        Method(BeanContext beanContext,
               BeanDefinition<B> beanDefinition,
               ExecutableMethod<B, Object> method,
               Class<?>[] boundParameterTypes) {
            this.beanContext = beanContext;
            this.beanDefinition = beanDefinition;
            this.method = method;
            this.resultArgument = AbstractMcpMethodRegistry.resultArgument(method);
            this.executorName = method.stringValue(ExecuteOn.class).orElse(null);
            List<Argument<?>> arguments = new ArrayList<>();
            List<Integer> indexes = new ArrayList<>();
            for (Argument<?> argument : method.getArguments()) {
                Class<?> type = argument.getType();
                if (type == McpRequestContext.class) {
                    arguments.add(argument);
                    indexes.add(REQUEST_CONTEXT);
                    continue;
                }
                for (int i = 0; i < boundParameterTypes.length; i++) {
                    Class<?> boundType = boundParameterTypes[i];
                    // A parameter declared as a subtype of a bound type is bound when the value is an instance of it
                    if (type.isAssignableFrom(boundType) || boundType.isAssignableFrom(type)) {
                        arguments.add(argument);
                        indexes.add(i);
                        break;
                    }
                }
            }
            this.boundArguments = arguments.toArray(Argument[]::new);
            this.boundIndexes = indexes.stream().mapToInt(Integer::intValue).toArray();
        }

        /**
         * @return The bean definition that declares or provides the method.
         */
        public BeanDefinition<B> beanDefinition() {
            return beanDefinition;
        }

        /**
         * @return The executable method.
         */
        public ExecutableMethod<B, Object> method() {
            return method;
        }

        /**
         * @return The bean to invoke the method on. A singleton is looked up once.
         */
        public B bean() {
            if (!beanDefinition.isSingleton()) {
                return beanContext.getBean(beanDefinition);
            }
            B bean = singleton;
            if (bean == null) {
                bean = beanContext.getBean(beanDefinition);
                singleton = bean;
            }
            return bean;
        }

        /**
         * @return The type of the value the method produces, unwrapped from a reactive type or a {@link CompletionStage}.
         */
        public Argument<?> resultArgument() {
            return resultArgument;
        }

        /**
         * Waits for the value of a reactive type or a {@link CompletionStage} returned by the method. Used by the synchronous
         * servers, which invoke methods on a thread that may block.
         *
         * @param result The value the method returned
         * @return The value it produced
         */
        public @Nullable Object await(@Nullable Object result) {
            if (result instanceof Publisher<?> publisher) {
                return Mono.from(publisher).block();
            }
            if (result instanceof CompletionStage<?> stage) {
                try {
                    return stage.toCompletableFuture().join();
                } catch (CompletionException e) {
                    if (e.getCause() instanceof RuntimeException cause) {
                        throw cause;
                    }
                    throw e;
                }
            }
            return result;
        }

        /**
         * Invokes the method when the returned publisher is subscribed to, on the executor named by {@link ExecuteOn} if
         * the method declares one, and adapts a reactive type or a {@link CompletionStage} it returns.
         *
         * @param invocation The invocation of the method
         * @return A publisher of the value the method produced, empty when it produced none
         */
        public Mono<Object> invokeAsync(Supplier<@Nullable Object> invocation) {
            Mono<Object> result = Mono.defer(() -> toMono(invocation.get()));
            Scheduler executor = scheduler();
            return executor != null ? result.subscribeOn(executor) : result;
        }

        @SuppressWarnings("unchecked")
        private static Mono<Object> toMono(@Nullable Object result) {
            if (result instanceof Publisher<?> publisher) {
                return Mono.from((Publisher<Object>) publisher);
            }
            if (result instanceof CompletionStage<?> stage) {
                return Mono.fromCompletionStage((CompletionStage<Object>) stage);
            }
            return Mono.justOrEmpty(result);
        }

        private @Nullable Scheduler scheduler() {
            if (executorName == null) {
                return null;
            }
            Scheduler executor = scheduler;
            if (executor == null) {
                executor = Schedulers.fromExecutorService(beanContext.getBean(ExecutorService.class, Qualifiers.byName(executorName)));
                scheduler = executor;
            }
            return executor;
        }

        /**
         * The values bound to method parameters without an argument binder.
         *
         * @param context The transport context or the server exchange
         * @param values The request and any other values, in the order of {@link #boundParameterTypes()} after the context
         * @return The pre-bound values by argument
         */
        public Map<Argument<?>, Object> preBound(@Nullable Object context, Object... values) {
            if (boundArguments.length == 0) {
                return Map.of();
            }
            Map<Argument<?>, Object> preBound = CollectionUtils.newHashMap(boundArguments.length);
            for (int i = 0; i < boundArguments.length; i++) {
                int index = boundIndexes[i];
                Object value;
                if (index == REQUEST_CONTEXT) {
                    value = DefaultMcpRequestContext.of(context, values[0]);
                } else {
                    value = index == 0 ? resolveMcpTransportContext(context) : values[index - 1];
                }
                Argument<?> argument = boundArguments[i];
                if (argument.getType().isInstance(value)) {
                    preBound.put(argument, value);
                }
            }
            return preBound;
        }
    }
}
