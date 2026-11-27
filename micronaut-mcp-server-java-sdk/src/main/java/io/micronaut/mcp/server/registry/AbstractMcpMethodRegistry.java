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
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.util.StringUtils;
import io.micronaut.mcp.annotations.Audience;
import io.micronaut.mcp.annotations.Icon;
import io.micronaut.mcp.annotations.Meta;
import io.micronaut.mcp.server.McpIconFactory;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.bind.ArgumentBinder;
import io.micronaut.core.bind.ArgumentBinderRegistry;
import io.micronaut.core.bind.exceptions.UnsatisfiedArgumentException;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.ConversionContext;
import io.micronaut.core.convert.ConversionError;
import io.micronaut.core.convert.exceptions.ConversionErrorException;
import io.micronaut.core.propagation.PropagatedContext;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.ReturnType;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.mcp.server.context.BlockingMcpRequestContext;
import io.micronaut.mcp.server.exceptions.McpErrorExceptionMapper;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The abstract registry.
 * @param <S> Sync Specification
 * @param <A> Async Specification
 * @param <T> Stateless Sync Specification
 * @param <U> Stateless Async Specification
 */
@Internal
abstract sealed class AbstractMcpMethodRegistry<S, A, T, U> implements McpPrimitiveRegistry<S, A, T, U>
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
    private static final String SRC_PROPERTY = "src";
    private static final String SIZES_PROPERTY = "sizes";
    private static final String THEME_PROPERTY = "theme";
    private static final String KEY_PROPERTY = "key";
    private static final String META_PREFIX_LABEL = "[A-Za-z](?:[A-Za-z0-9-]*[A-Za-z0-9])?";
    private static final String META_NAME = "[A-Za-z0-9](?:[A-Za-z0-9._-]*[A-Za-z0-9])?";
    // A prefix may stand alone with its trailing slash. The full key must still be non-empty.
    private static final Pattern META_KEY = Pattern.compile("(?:" + META_PREFIX_LABEL + "(?:\\." + META_PREFIX_LABEL + ")*+/)?(?:" + META_NAME + ")?");
    private static final String AUDIENCE_PROPERTY = "audience";
    private static final String PRIORITY_PROPERTY = "priority";
    private static final double UNSET_PRIORITY = -1;
    private static final Logger LOG = LoggerFactory.getLogger(AbstractMcpMethodRegistry.class);
    private static final String INTERNAL_ERROR_MESSAGE = "Internal error";
    protected final List<Method<Object>> methods = new ArrayList<>();
    protected final BeanContext beanContext;
    private final List<McpErrorExceptionMapper<?>> exceptionMappers;
    private final Map<Class<? extends Throwable>, Optional<McpErrorExceptionMapper<? extends Throwable>>> classToExceptionMapper = new ConcurrentHashMap<>();
    /**
     * The methods and unmapped exception types already logged with a stack trace.
     */
    private final Set<Map.Entry<ExecutableMethod<?, ?>, Class<? extends Throwable>>> loggedUnmappedExceptions = ConcurrentHashMap.newKeySet();

    AbstractMcpMethodRegistry(List<McpErrorExceptionMapper<? extends Throwable>> exceptionMappers,
                              BeanContext beanContext) {
        this.exceptionMappers = exceptionMappers;
        this.beanContext = beanContext;
    }

    /**
     * The types of the values that can be bound to a method parameter without an argument binder, in the order they are matched.
     * The first is always the {@link McpTransportContext}, the second the request.
     *
     * @return the bound parameter types
     */
    protected abstract Class<?>[] boundParameterTypes();

    /**
     * Whether a method returning a reactive type that may emit several values has its values collected into a {@link List}.
     * Registries that do not collect reject such methods when they are added.
     *
     * @param method The method
     * @return Whether the values are collected
     */
    protected boolean collectsValues(ExecutableMethod<?, ?> method) {
        return false;
    }

    /**
     * Adds a new method to the registry by associating it with a given bean definition.
     *
     * @param beanDefinition the bean definition that declares or provides the method
     * @param method         the executable method to be added to the registry
     * @throws IllegalStateException If the method returns a reactive type that may emit several values and the registry does not collect them
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public final void addMethod(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {
        methods.add(new Method(beanContext, beanDefinition, method, boundParameterTypes(), collectsValues(method)));
    }

    /**
     * Returns a stream of the methods currently stored in the registry.
     *
     * @return a stream of methods from the registry
     */
    protected final Stream<Method<Object>> streamMethods() {
        return methods.stream();
    }

    @Override
    public boolean isNotEmpty() {
        return !methods.isEmpty();
    }

    /**
     * Maps a failure of a method to the protocol error sent to the client: a failure that an {@link McpErrorExceptionMapper} maps is mapped by
     * it, another {@link McpError} is returned unchanged, and any other failure becomes an internal error.
     *
     * @param method The method that was invoked
     * @param throwable The failure
     * @return The protocol error
     */
    protected McpError mcpError(ExecutableMethod<?, ?> method, Throwable throwable) {
        McpErrorExceptionMapper<? extends Throwable> exceptionMapper = getExceptionMapper(throwable.getClass());
        if (exceptionMapper != null) {
            // A mapped exception is an expected client error
            if (LOG.isDebugEnabled()) {
                LOG.debug(throwable.getMessage(), throwable);
            }
            return mapException(exceptionMapper, throwable);
        }
        if (throwable instanceof McpError mcpError) {
            // An error the method throws keeps its code, unless a mapper maps it
            return mcpError;
        }
        // The SDK logs only exceptions that are not an McpError, so an unmapped exception is logged here
        if (LOG.isWarnEnabled()) {
            // The stack trace is logged once per method and exception type, so a method that keeps failing does not flood
            // the log, and a failure of one method does not hide the stack trace of another
            if (loggedUnmappedExceptions.add(Map.entry(method, throwable.getClass()))) {
                LOG.warn("MCP method {}#{} threw an exception that no McpErrorExceptionMapper maps, so it is returned as an internal error",
                    method.getDeclaringType().getName(), method.getMethodName(), throwable);
            } else {
                LOG.warn("MCP method {}#{} threw {} again, returned as an internal error", method.getDeclaringType().getName(),
                    method.getMethodName(), throwable.getClass().getName());
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Repeated exception of MCP method {}#{}", method.getDeclaringType().getName(), method.getMethodName(), throwable);
                }
            }
        }
        // The message of an unexpected exception may tell internal details, such as those of a database, so it stays in
        // the server log
        return McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR).message(INTERNAL_ERROR_MESSAGE).build();
    }

    /**
     * An error handler for {@link Method#call} and {@link Method#callAsync} that fails with the protocol error of the failure.
     *
     * @param m The method that was invoked
     * @param throwable The failure
     * @param <R> The result type
     * @return Never returns
     */
    protected final <R> R failWithMcpError(Method<?> m, Throwable throwable) {
        throw mcpError(m.method(), throwable);
    }

    /**
     * @param exceptionClass The class of a failure
     * @return The mapper of the failure, if any
     */
    // Each mapper handles its own type of exception, so the type of the mapper is only known as a wildcard
    @SuppressWarnings("java:S1452")
    @Nullable
    final McpErrorExceptionMapper<? extends Throwable> getExceptionMapper(@NonNull Class<? extends Throwable> exceptionClass) {
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

    /**
     * The type of the value a method produces: the method return type, or its first type argument when the method returns
     * a reactive type or a {@link CompletionStage}.
     *
     * @param method The method
     * @return The result argument
     */
    @SuppressWarnings("unchecked")
    protected static Argument<Object> resultArgument(ExecutableMethod<?, ?> method) {
        ReturnType<?> returnType = method.getReturnType();
        if (returnType.isAsyncOrReactive()) {
            return (Argument<Object>) returnType.getFirstTypeVariable().orElse(Argument.OBJECT_ARGUMENT);
        }
        return (Argument<Object>) returnType.asArgument();
    }

    /**
     * @param method The method
     * @return The icons declared with {@link Icon}, or {@code null} when there are none
     * @throws IllegalStateException If an icon has a blank {@code src} or a theme other than {@code light} or {@code dark}
     */
    protected static @Nullable List<McpSchema.Icon> icons(ExecutableMethod<?, ?> method) {
        List<AnnotationValue<Icon>> values = method.getAnnotationValuesByType(Icon.class);
        if (values.isEmpty()) {
            return null;
        }
        List<McpSchema.Icon> icons = new ArrayList<>(values.size());
        for (AnnotationValue<Icon> value : values) {
            icons.add(McpIconFactory.icon("The @Icon of method " + methodName(method),
                value.stringValue(SRC_PROPERTY).orElse(null),
                value.stringValue(MIME_TYPE_PROPERTY).orElse(null),
                List.of(value.stringValues(SIZES_PROPERTY)),
                value.stringValue(THEME_PROPERTY).orElse(null)));
        }
        return icons;
    }

    /**
     * @param method The method
     * @return The {@code _meta} entries declared with {@link Meta}, or {@code null} when there are none
     * @throws IllegalStateException If a key is empty, malformed or has conflicting values
     */
    protected static @Nullable Map<String, Object> meta(ExecutableMethod<?, ?> method) {
        List<AnnotationValue<Meta>> values = method.getAnnotationValuesByType(Meta.class);
        if (values.isEmpty()) {
            return null;
        }
        Map<String, Object> meta = LinkedHashMap.newLinkedHashMap(values.size());
        for (AnnotationValue<Meta> value : values) {
            String key = value.stringValue(KEY_PROPERTY).filter(StringUtils::isNotEmpty).orElseThrow(() ->
                new IllegalStateException("The @Meta of method " + methodName(method) + " requires a key"));
            if (!META_KEY.matcher(key).matches()) {
                throw new IllegalStateException("Invalid @Meta key on method " + methodName(method));
            }
            String entryValue = value.stringValue().orElse("");
            Object previous = meta.putIfAbsent(key, entryValue);
            if (previous != null && !previous.equals(entryValue)) {
                throw new IllegalStateException("The @Meta key " + key + " has conflicting values on method " + methodName(method));
            }
        }
        return meta;
    }

    /**
     * @param method The method
     * @param annotationType The resource or resource template annotation
     * @return The resource annotations it declares, or {@code null} when it declares none
     * @throws IllegalStateException If the priority is set and is not between 0 and 1
     */
    protected static McpSchema.@Nullable Annotations resourceAnnotations(ExecutableMethod<?, ?> method, Class<? extends Annotation> annotationType) {
        AnnotationValue<?> annotation = method.findAnnotation(annotationType).orElseThrow();
        Audience[] audience = annotation.enumValues(AUDIENCE_PROPERTY, Audience.class);
        double priority = annotation.doubleValue(PRIORITY_PROPERTY).orElse(UNSET_PRIORITY);
        boolean hasPriority = Double.compare(priority, UNSET_PRIORITY) != 0;
        // NaN fails the range check too
        if (hasPriority && !(priority >= 0 && priority <= 1)) {
            throw new IllegalStateException("The priority of @" + annotationType.getSimpleName() + " on method " + methodName(method)
                + " is " + priority + "; it must be between 0 and 1, or left out");
        }
        if (audience.length == 0 && !hasPriority) {
            return null;
        }
        List<McpSchema.Role> roles = null;
        if (audience.length > 0) {
            roles = Arrays.stream(audience).map(AbstractMcpMethodRegistry::role).toList();
        }
        return new McpSchema.Annotations(roles, hasPriority ? priority : null);
    }

    private static String methodName(ExecutableMethod<?, ?> method) {
        return method.getDeclaringType().getName() + "#" + method.getMethodName();
    }

    private static McpSchema.Role role(Audience audience) {
        return switch (audience) {
            case USER -> McpSchema.Role.USER;
            case ASSISTANT -> McpSchema.Role.ASSISTANT;
        };
    }

    /**
     * @param mapper The mapper
     * @param ex The failure to map
     * @param <T> The failure type
     * @return The error the mapper maps the failure to
     */
    @SuppressWarnings("unchecked")
    static <T extends Throwable> McpError mapException(McpErrorExceptionMapper<? extends Throwable> mapper, T ex) {
        return ((McpErrorExceptionMapper<T>) mapper).map(ex);
    }

    /**
     * A method associated with a bean definition. What every invocation needs, other than the request, is resolved once:
     * the method parameters that receive the transport context or the request, the argument binders of the other
     * parameters and the executor named by {@link ExecuteOn}.
     *
     * @param <B> The type of the bean.
     */
    protected static final class Method<B> {
        private static final int NOT_BOUND = -1;
        private final BeanContext beanContext;
        private final BeanDefinition<B> beanDefinition;
        private final ExecutableMethod<B, Object> executableMethod;
        private final Argument<?>[] arguments;
        /**
         * For each parameter, the index in {@link AbstractMcpMethodRegistry#boundParameterTypes()} of the type of the value
         * it can receive without an argument binder, or {@link #NOT_BOUND} when there is no declared match.
         */
        private final int[] boundIndexes;
        private final boolean collectsValues;
        private final boolean usesBlockingExecutor;
        /**
         * Whether the declared return type admits a reactive type or a {@link CompletionStage}, so that a plain value
         * of a declared class other than Object is mapped without looking for a reactive type that it could be converted to.
         */
        private final boolean mayBeDeferred;
        private final @Nullable Scheduler scheduler;
        /**
         * The argument binders by parameter, resolved on the first invocation, because binders for JSON schema types
         * are registered when the specifications are built. Concurrent first invocations resolve the same binders.
         * The binders are kept only once every parameter has one.
         */
        private final AtomicReference<ArgumentBinder<?, ?>[]> binders = new AtomicReference<>();

        Method(BeanContext beanContext,
               BeanDefinition<B> beanDefinition,
               ExecutableMethod<B, Object> method,
               Class<?>[] boundParameterTypes,
               boolean collectsValues) {
            if (boundParameterTypes.length == 0 || boundParameterTypes[0] != McpTransportContext.class) {
                throw new IllegalArgumentException("The first bound parameter type must be " + McpTransportContext.class.getName());
            }
            this.beanContext = beanContext;
            this.beanDefinition = beanDefinition;
            this.executableMethod = method;
            this.arguments = method.getArguments();
            this.boundIndexes = new int[arguments.length];
            for (int a = 0; a < arguments.length; a++) {
                boundIndexes[a] = boundIndex(arguments[a].getType(), boundParameterTypes);
            }
            this.collectsValues = collectsValues(method, collectsValues);
            // Resolved here, so that an unknown executor fails the startup rather than every invocation
            String executorName = method.stringValue(ExecuteOn.class).orElse(null);
            ExecutorService executor = executorName != null ? beanContext.getBean(ExecutorService.class, Qualifiers.byName(executorName)) : null;
            this.scheduler = executor != null ? Schedulers.fromExecutorService(executor) : null;
            this.usesBlockingExecutor = executor != null
                && executor == beanContext.findBean(ExecutorService.class, Qualifiers.byName(TaskExecutors.BLOCKING)).orElse(null);
            this.mayBeDeferred = mayBeDeferred(method.getReturnType());
        }

        private static int boundIndex(Class<?> type, Class<?>[] boundParameterTypes) {
            for (int i = 0; i < boundParameterTypes.length; i++) {
                Class<?> boundType = boundParameterTypes[i];
                // A parameter declared as a subtype of a bound type is bound when the value is an instance of it
                if (type.isAssignableFrom(boundType) || boundType.isAssignableFrom(type)) {
                    return i;
                }
            }
            return NOT_BOUND;
        }

        private static boolean mayBeDeferred(ReturnType<?> returnType) {
            Class<?> type = returnType.getType();
            return returnType.isAsyncOrReactive() || type == Object.class || type.isInterface();
        }

        private static boolean collectsValues(ExecutableMethod<?, ?> method, boolean collectsValues) {
            ReturnType<?> returnType = method.getReturnType();
            if (!returnType.isReactive() || returnType.isSingleResult()) {
                return false;
            }
            if (collectsValues) {
                return true;
            }
            // A plain Publisher is expected to emit at most one value, which is checked when it is subscribed to
            if (returnType.getType() != Publisher.class) {
                throw new IllegalStateException("MCP method " + method.getDeclaringType().getName() + "." + method.getMethodName()
                    + " returns " + returnType.getType().getSimpleName() + ", which may emit several values, but it must produce a single value."
                    + " Return a single value reactive type, such as Mono, or a CompletableFuture, or annotate the method with @SingleResult.");
            }
            return false;
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
            return executableMethod;
        }

        /**
         * @param argumentIndex The index of a method parameter
         * @return Whether the parameter receives the transport context, the request or another value bound without an argument binder
         */
        public boolean isBound(int argumentIndex) {
            return boundIndexes[argumentIndex] >= 0;
        }

        /**
         * Resolves the bean through the context on each call so replacements are visible.
         *
         * @return The bean to invoke the method on
         */
        public B bean() {
            return beanContext.getBean(beanDefinition);
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
         * Invokes the method for a synchronous server, which calls it on a thread that may block, and maps the value it
         * produced. The method runs on the executor named by {@link ExecuteOn} if it declares one, and a reactive type or
         * a {@link CompletionStage} it returns is waited for.
         *
         * @param invocation The invocation of the method
         * @param context The transport context or the server exchange
         * @param mapper Maps the value the method produced, {@code null} when it produced none
         * @param errorHandler Maps a failure to a result, or throws the error to fail with
         * @param <R> The result type
         * @return The result
         */
        public <R> R call(Supplier<@Nullable Object> invocation,
                          @Nullable Object context,
                          Function<@Nullable Object, R> mapper,
                          Function<Throwable, R> errorHandler) {
            try {
                Object value;
                if (scheduler != null && !alreadyOnBlockingExecutor()) {
                    // Subscribe on the named executor, but wait on the caller so asynchronous work can use that executor too.
                    Mono<Object> result = schedule(() -> {
                        Object returned = invocation.get();
                        return isDeferred(returned) ? toMono(returned) : Mono.justOrEmpty(returned);
                    }, scheduler);
                    value = block(result, context);
                } else {
                    value = await(invocation.get(), context);
                }
                return mapper.apply(value);
            } catch (Exception e) {
                return errorHandler.apply(unwrap(e));
            }
        }

        private boolean alreadyOnBlockingExecutor() {
            if (!usesBlockingExecutor) {
                return false;
            }
            BlockingMcpRequestContext request = BlockingMcpRequestContext.current();
            return request != null && request.thread() == Thread.currentThread();
        }

        /**
         * Invokes the method for a reactive server and maps the value it produced. A method that declares an executor with
         * {@link ExecuteOn} is invoked on it when the result is subscribed to; otherwise it is invoked right away and a plain
         * value is mapped without any reactive operator.
         *
         * @param invocation The invocation of the method
         * @param mapper Maps the value the method produced, {@code null} when it produced none
         * @param errorHandler Maps a failure to a result, or throws the error to fail with
         * @param <R> The result type
         * @return A publisher of the result
         */
        public <R> Mono<R> callAsync(Supplier<@Nullable Object> invocation,
                                     Function<@Nullable Object, R> mapper,
                                     Function<Throwable, R> errorHandler) {
            if (scheduler == null) {
                return invokeNow(invocation, mapper, errorHandler);
            }
            return schedule(() -> invokeNow(invocation, mapper, errorHandler), scheduler);
        }

        private static <R> Mono<R> schedule(Supplier<Mono<R>> task, Scheduler scheduler) {
            return Mono.defer(PropagatedContext.wrapCurrent(task)).subscribeOn(scheduler);
        }

        private <R> Mono<R> invokeNow(Supplier<@Nullable Object> invocation,
                                      Function<@Nullable Object, R> mapper,
                                      Function<Throwable, R> errorHandler) {
            Object result;
            try {
                result = invocation.get();
                if (!isDeferred(result)) {
                    return Mono.just(mapper.apply(result));
                }
            } catch (Exception e) {
                return handle(errorHandler, e);
            }
            return toMono(result)
                .map(mapper)
                .switchIfEmpty(Mono.fromSupplier(() -> mapper.apply(null)))
                .onErrorResume(e -> handle(errorHandler, e));
        }

        private static <R> Mono<R> handle(Function<Throwable, R> errorHandler, Throwable error) {
            try {
                return Mono.just(errorHandler.apply(unwrap(error)));
            } catch (RuntimeException e) {
                return Mono.error(e);
            }
        }

        private boolean isDeferred(@Nullable Object result) {
            return mayBeDeferred && result != null && (result instanceof Publisher<?> || result instanceof CompletionStage<?> || Publishers.isConvertibleToPublisher(result));
        }

        /**
         * Waits for the value of a reactive type or a {@link CompletionStage}. A reactive type is subscribed to with the
         * transport context in its Reactor context, as it is on a reactive server.
         */
        private @Nullable Object await(@Nullable Object result, @Nullable Object context) {
            if (result instanceof CompletionStage<?> stage) {
                return stage.toCompletableFuture().join();
            }
            if (!isDeferred(result)) {
                return result;
            }
            return block(toMono(result), context);
        }

        private static @Nullable Object block(Mono<Object> mono, @Nullable Object context) {
            McpTransportContext transportContext = resolveMcpTransportContext(context);
            if (transportContext != null) {
                mono = mono.contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext));
            }
            BlockingMcpRequestContext request = BlockingMcpRequestContext.current();
            if (request != null) {
                mono = mono.contextWrite(request.context());
            }
            return mono.block();
        }

        @SuppressWarnings("unchecked")
        private Mono<Object> toMono(Object result) {
            if (result instanceof CompletionStage<?> stage) {
                return Mono.fromCompletionStage((CompletionStage<Object>) stage);
            }
            Publisher<Object> publisher = result instanceof Publisher<?> p
                ? (Publisher<Object>) p
                : Publishers.convertToPublisher(beanContext.getConversionService(), result);
            if (collectsValues) {
                return CompletionRegistry.collectCompletions(publisher).map(Object.class::cast);
            }
            if (publisher instanceof Mono<Object> mono) {
                return mono;
            }
            // Fails rather than silently dropping values when a publisher emits more than one
            return Flux.from(publisher).singleOrEmpty();
        }

        private static Throwable unwrap(Throwable throwable) {
            Throwable error = Exceptions.unwrap(throwable);
            while ((error instanceof CompletionException || error instanceof ExecutionException) && error.getCause() != null) {
                error = Exceptions.unwrap(error.getCause());
            }
            return error;
        }

        /**
         * Binds the method parameters and invokes the method. The runtime transport context takes precedence over the
         * request values. Parameters with no matching value use the argument binder, with the same semantics as
         * {@link io.micronaut.core.bind.DefaultExecutableBinder}.
         *
         * @param registry The argument binder registry
         * @param source The source of the argument binders
         * @param context The transport context or the server exchange
         * @param values The request and any other values: one for each of {@link AbstractMcpMethodRegistry#boundParameterTypes()}
         *               after the transport context, in the same order
         * @param <S> The source type
         * @return The result of the method
         * @throws UnsatisfiedArgumentException If a required parameter cannot be bound
         * @throws ConversionErrorException If the value of a required parameter cannot be converted
         */
        public <S> @Nullable Object invoke(ArgumentBinderRegistry<S> registry, S source, @Nullable Object context, Object... values) {
            if (arguments.length == 0) {
                return executableMethod.invoke(bean());
            }
            ArgumentBinder<?, ?>[] argumentBinders = binders(registry);
            McpTransportContext transportContext = resolveMcpTransportContext(context);
            Object[] argumentValues = new Object[arguments.length];
            for (int i = 0; i < arguments.length; i++) {
                Argument<?> argument = arguments[i];
                Class<?> type = argument.getType();
                int index = boundIndexes[i];
                if (type.isInstance(transportContext)) {
                    argumentValues[i] = transportContext;
                } else if (index > 0 && type.isInstance(values[index - 1])) {
                    argumentValues[i] = values[index - 1];
                } else {
                    argumentValues[i] = bind(argument, argumentBinders[i], source);
                }
            }
            return executableMethod.invoke(bean(), argumentValues);
        }

        @SuppressWarnings("unchecked")
        private <S> ArgumentBinder<?, ?>[] binders(ArgumentBinderRegistry<S> registry) {
            ArgumentBinder<?, ?>[] resolved = binders.get();
            if (resolved == null) {
                resolved = new ArgumentBinder<?, ?>[arguments.length];
                boolean complete = true;
                for (int i = 0; i < arguments.length; i++) {
                    resolved[i] = registry.findArgumentBinder((Argument<Object>) arguments[i]).orElse(null);
                    complete &= resolved[i] != null;
                }
                // A missing binder is looked up again on the next invocation, in case it is registered later
                if (complete) {
                    binders.set(resolved);
                }
            }
            return resolved;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Nullable
        private static <S> Object bind(Argument<?> argument, @Nullable ArgumentBinder<?, ?> binder, S source) {
            if (binder == null) {
                throw new UnsatisfiedArgumentException(argument);
            }
            ArgumentConversionContext conversionContext = ConversionContext.of(argument);
            ArgumentBinder.BindingResult<?> bindingResult = ((ArgumentBinder) binder).bind(conversionContext, source);
            if (bindingResult.isPresentAndSatisfied()) {
                return bindingResult.get();
            }
            if (argument.isNullable()) {
                return null;
            }
            Optional<ConversionError> lastError = conversionContext.getLastError();
            if (lastError.isPresent()) {
                throw new ConversionErrorException(argument, lastError.get());
            }
            throw new UnsatisfiedArgumentException(argument);
        }
    }
}
