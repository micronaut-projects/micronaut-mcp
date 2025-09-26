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
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.json.JsonMapper;
import io.micronaut.jsonschema.utils.JsonSchemaClassPathResourceLoader;
import io.micronaut.mcp.server.exceptions.McpErrorExceptionMapper;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static io.micronaut.mcp.server.registry.JsonSchemaUtils.TYPE_OBJECT;
import static io.micronaut.mcp.server.registry.JsonSchemaUtils.TYPE_STRING;

import io.micronaut.inject.BeanDefinition;

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
    /**
     * @see <a href="https://json-schema.org/understanding-json-schema/reference/type">JSON Schema Type</a>
     */
    protected static final String MEMBER_DESCRIPTION = "description";
    protected static final String KEY_TYPE = "type";
    private static final Logger LOG = LoggerFactory.getLogger(AbstractMcpMethodRegistry.class);
    private static final List<Class<?>> BINDABLE_PARAMETER_TYPES = List.of(McpTransportContext.class,
        McpSchema.CallToolRequest.class,
        McpSchema.ReadResourceRequest.class,
        McpSchema.GetPromptRequest.class);
    protected final List<Method<Object>> methods = new ArrayList<>();
    private final JsonSchemaClassPathResourceLoader jsonSchemaClassPathResourceLoader;
    private final JsonMapper jsonMapper;
    private final List<McpErrorExceptionMapper<?>> exceptionMappers;
    private final Map<Class<? extends Throwable>, McpErrorExceptionMapper<? extends Throwable>> classToExceptionMapper = new ConcurrentHashMap<>();

    AbstractMcpMethodRegistry(JsonSchemaClassPathResourceLoader jsonSchemaClassPathResourceLoader,
                               JsonMapper jsonMapper,
                               List<McpErrorExceptionMapper<? extends Throwable>> exceptionMappers) {
        this.jsonSchemaClassPathResourceLoader = jsonSchemaClassPathResourceLoader;
        this.jsonMapper = jsonMapper;
        this.exceptionMappers = exceptionMappers;
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

    protected <B> McpSchema.JsonSchema inputSchema(ExecutableMethod<B, Object> method,
                                                 Function<Argument<?>, String> propertyNameFunction,
                                                 Function<Argument<?>, String> argumentDescription) {
        Collection<Integer> boundArgumentsPositions = boundArgumentsPositions(method).values();
        Argument<?>[] arguments = method.getArguments();
        Map<String, Object> properties = CollectionUtils.newHashMap(arguments.length);
        List<String> requiredProperties = new ArrayList<>(arguments.length);
        for (int i = 0; i < arguments.length; i++) {
            if (boundArgumentsPositions.contains(i)) {
                continue;
            }
            Argument<?> argument = arguments[i];
            String propertyName = propertyNameFunction.apply(argument);
            properties.put(propertyName, argumentJsonSchema(argument, argumentDescription));
            if (isArgumentRequired(argument)) {
                requiredProperties.add(propertyName);
            }
        }
        return new McpSchema.JsonSchema(TYPE_OBJECT, properties, requiredProperties, null, null, null);
    }

    protected McpError mcpError(Exception ex) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(ex.getMessage(), ex);
        }
        McpErrorExceptionMapper<? extends Throwable> exceptionMapper = getExceptionMapper(ex.getClass());
        if (exceptionMapper != null) {
            return mapException(exceptionMapper, ex);
        }
        return new McpError(McpSchema.ErrorCodes.INTERNAL_ERROR);
    }

    private static Object argumentJsonSchema(Argument<?> argument,
                                                 Function<Argument<?>, String> argumentDescription) {
        String description = argumentDescription.apply(argument);

        if (description != null) {
            Map<String, Object> schema = new LinkedHashMap<>();
            schema.put(KEY_TYPE, argumentType(argument));
            if (StringUtils.isNotEmpty(description)) {
                schema.put(MEMBER_DESCRIPTION, description);
            }
            return schema;
        }

        return new McpSchema.JsonSchema(argumentType(argument), null, null, null, null, null);
    }

    private static String argumentType(Argument<?> argument) {
        if (argument.isAssignableFrom(String.class)) {
            return TYPE_STRING;
        }
        return TYPE_OBJECT;
    }

    private static boolean isArgumentRequired(Argument<?> argument) {
        return !argument.isNullable();
    }

    private <B> Object instantiateArgumentViaJsonMapper(ExecutableMethod<B, Object> method,
                                                        int position,
                                                        Map<String, Object> arguments) {
        try {
            String payload = jsonMapper.writeValueAsString(arguments);
            Argument<?> argument = method.getArguments()[position];
            Class<?> classInputSchema = argument.getType();
            return jsonMapper.readValue(payload, classInputSchema);

        } catch (IOException ex) {
            throw mcpError(ex);
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

    protected <B> Object[] methodArgs(ExecutableMethod<B, Object> method,
                                      Object request,
                                      Object ctx) {
        return methodArgs(method, Collections.emptyMap(), request, ctx, Argument::getName);
    }

    protected <B> Object[] methodArgs(ExecutableMethod<B, Object> method,
                                    Map<String, Object> arguments,
                                      Object request,
                                    Object ctx,
                                   Function<Argument<?>, String> argumentName) {
        Object[] args = new Object[method.getArguments().length];
        LinkedHashMap<Class<?>, Integer> boundArgumentsPositions = boundArgumentsPositions(method);
        McpTransportContext mcpTransportContext = resolveMcpTransportContext(ctx);
        Integer position = boundArgumentsPositions.get(McpTransportContext.class);
        if (position != null) {
            args[position] = mcpTransportContext;
        }
        position = boundArgumentsPositions.get(request.getClass());
        if (position != null) {
            args[position] = request;
        }

        if (jsonSchema(method).isPresent()) {
            for (int i = 0; i < args.length; i++) {
                if (boundArgumentsPositions.values().contains(i)) {
                    continue;
                }
                args[i] = instantiateArgumentViaJsonMapper(method, i, arguments);
            }
            return args;
        }
        for (int i = 0; i < args.length; i++) {
            if (boundArgumentsPositions.values().contains(i)) {
                continue;
            }
            args[i] = arguments.get(argName(method, i, argumentName));
        }
        return args;
    }

    @NonNull
    protected Optional<String> jsonSchema(@NonNull ExecutableMethod<?, ?> method) {
        Argument<?>[] arguments = method.getArguments();
        int boundArguments = 0;
        for (int i = 0; i < arguments.length; i++) {
            Argument<?> argument = arguments[i];
            Class<?> type = argument.getType();
            if (BINDABLE_PARAMETER_TYPES.stream().anyMatch(bindableParameterType -> bindableParameterType.isAssignableFrom(type))) {
                boundArguments++;
            }
        }
        if ((method.getArguments().length - boundArguments) != 1) {
            return Optional.empty();
        }
        int index = -1;
        for (int i = 0; i < arguments.length; i++) {
            Argument<?> argument = arguments[i];
            Class<?> type = argument.getType();
            if (BINDABLE_PARAMETER_TYPES.stream().anyMatch(bindableParameterType -> bindableParameterType.isAssignableFrom(type))) {
                continue;
            }
            index = i;
            break;
        }
        if (index == -1) {
            return Optional.empty();
        }
        Argument<?> argument = arguments[index];
        Class<?> argumentClass = argument.getType();
        return jsonSchemaClassPathResourceLoader.jsonSchemaStringForClass(argumentClass);
    }

    private static String argName(ExecutableMethod<?, ?> method, int position, Function<Argument<?>, String> argumentName) {
        Argument<?> argument = method.getArguments()[position];
        return argumentName.apply(argument);
    }

    @NonNull
    private <B> LinkedHashMap<Class<?>, Integer> boundArgumentsPositions(@NonNull ExecutableMethod<B, Object> method) {
        LinkedHashMap<Class<?>, Integer> result = new LinkedHashMap<>();
        Argument<?>[] arguments = method.getArguments();
        for (int i = 0; i < arguments.length; i++) {
            Argument<?> argument = arguments[i];
            for (Class<?> bindinableParameterType : BINDABLE_PARAMETER_TYPES) {
                if (bindinableParameterType.isAssignableFrom(argument.getType())) {
                    result.put(bindinableParameterType, i);
                    break;
                }
            }
        }
        return result;
    }

    @Nullable
    private McpTransportContext resolveMcpTransportContext(Object ctx) {
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
