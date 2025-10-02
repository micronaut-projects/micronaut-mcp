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
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.exceptions.IntrospectionException;
import io.micronaut.core.bind.ArgumentBinderRegistry;
import io.micronaut.core.bind.BoundExecutable;
import io.micronaut.core.bind.DefaultExecutableBinder;
import io.micronaut.core.bind.ExecutableBinder;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.json.JsonMapper;
import io.micronaut.jsonschema.JsonSchema;
import io.micronaut.jsonschema.utils.JsonSchemaClassPathResourceLoader;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.annotations.ToolArg;
import io.micronaut.mcp.server.exceptions.McpErrorExceptionMapper;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static io.micronaut.mcp.server.registry.JsonSchemaUtils.TYPE_OBJECT;
import static io.micronaut.mcp.server.registry.JsonSchemaUtils.TYPE_STRING;

/**
 * The registry of {@link Tool}s.
 */
@Singleton
@Internal
public final class ToolRegistry extends AbstractMcpMethodRegistry<McpServerFeatures.SyncToolSpecification, McpServerFeatures.AsyncToolSpecification, McpStatelessServerFeatures.SyncToolSpecification, McpStatelessServerFeatures.AsyncToolSpecification> {
    private static final Logger LOG = LoggerFactory.getLogger(ToolRegistry.class);
    private static final List<Class<?>> BINDABLE_PARAMETER_TYPES = List.of(McpTransportContext.class,
        McpSchema.CallToolRequest.class);
    private final JsonSchemaClassPathResourceLoader jsonSchemaClassPathResourceLoader;
    private final McpJsonMapper mcpJsonMapper;
    private final JsonMapper jsonMapper;
    private final ArgumentBinderRegistry<McpSchema.CallToolRequest> argumentBinderRegistry;
    private final BeanContext beanContext;

    ToolRegistry(JsonSchemaClassPathResourceLoader jsonSchemaClassPathResourceLoader,
                 List<McpErrorExceptionMapper<? extends Throwable>> exceptionMappers,
                 McpJsonMapper mcpJsonMapper,
                 JsonMapper jsonMapper,
                 ArgumentBinderRegistry<McpSchema.CallToolRequest> argumentBinderRegistry,
                 BeanContext beanContext) {
        super(exceptionMappers);
        this.jsonSchemaClassPathResourceLoader = jsonSchemaClassPathResourceLoader;
        this.mcpJsonMapper = mcpJsonMapper;
        this.jsonMapper = jsonMapper;
        this.argumentBinderRegistry = argumentBinderRegistry;
        this.beanContext = beanContext;
    }

    @Override
    public List<McpServerFeatures.SyncToolSpecification> getSyncSpecs() {
        return drainMethods()
            .map(toolMethod -> McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool(toolMethod.method()))
                .callHandler(provideSyncCallHandler(toolMethod.beanDefinition(), toolMethod.method()))
                .build())
            .toList();
    }

    @Override
    public List<McpServerFeatures.AsyncToolSpecification> getAsyncSpecs() {
        return drainMethods()
            .map(toolMethod -> McpServerFeatures.AsyncToolSpecification.builder()
                .tool(tool(toolMethod.method()))
                .callHandler(provideReactiveCallHandler(toolMethod.beanDefinition(), toolMethod.method()))
                .build())
            .toList();
    }

    @Override
    public List<McpStatelessServerFeatures.SyncToolSpecification> getStatelessSyncSpecs() {
        return drainMethods()
            .map(toolMethod -> McpStatelessServerFeatures.SyncToolSpecification.builder()
                .tool(tool(toolMethod.method()))
                .callHandler(provideSyncCallHandler(toolMethod.beanDefinition(), toolMethod.method()))
                .build())
            .toList();
    }

    @Override
    public List<McpStatelessServerFeatures.AsyncToolSpecification> getStatelessAsyncSpecs() {
        return drainMethods()
            .map(toolMethod -> McpStatelessServerFeatures.AsyncToolSpecification.builder()
                .tool(tool(toolMethod.method()))
                .callHandler(provideReactiveCallHandler(toolMethod.beanDefinition(), toolMethod.method()))
                .build())
            .toList();
    }

    @Override
    public boolean isNotEmpty() {
        return CollectionUtils.isNotEmpty(getAsyncSpecs()) ||
            CollectionUtils.isNotEmpty(getSyncSpecs()) ||
            CollectionUtils.isNotEmpty(getStatelessAsyncSpecs()) ||
            CollectionUtils.isNotEmpty(getStatelessSyncSpecs());
    }

    private <B, C> BiFunction<C, McpSchema.CallToolRequest, McpSchema.CallToolResult> provideSyncCallHandler(BeanDefinition<B> beanDefinition, ExecutableMethod<B, Object> method) {
        return (mcpTransportContext, callToolRequest)
            -> callToolToResult(beanDefinition, method, mcpTransportContext, callToolRequest);
    }

    private <B, C> BiFunction<C, McpSchema.CallToolRequest, Mono<McpSchema.CallToolResult>> provideReactiveCallHandler(BeanDefinition<B> beanDefinition, ExecutableMethod<B, Object> method) {
        return (mcpTransportContext, callToolRequest)
            -> reactiveCallToolToResult(beanDefinition, method, mcpTransportContext, callToolRequest);
    }

    private <B> Mono<McpSchema.CallToolResult> reactiveCallToolToResult(BeanDefinition<B> beanDefinition,
                                                                        ExecutableMethod<B, Object> method,
                                                                        Object mcpTransportContext,
                                                                        McpSchema.CallToolRequest callToolRequest) {
        McpSchema.CallToolResult result = callToolToResult(beanDefinition, method, mcpTransportContext, callToolRequest);
        if (result == null) {
            return Mono.empty();
        }
        return Mono.just(result);
    }

    private <B> McpSchema.CallToolResult callToolToResult(BeanDefinition<B> beanDefinition,
                                                          ExecutableMethod<B, Object> method,
                                                          Object mcpTransportContext,
                                                          McpSchema.CallToolRequest callToolRequest) {
        Argument<?> returnClass = method.getReturnType().asArgument();
        B bean = beanContext.getBean(beanDefinition);
        ExecutableBinder<McpSchema.CallToolRequest> executableBinder = new DefaultExecutableBinder<>(
            prepareBoundVariables(method, List.of(resolveMcpTransportContext(mcpTransportContext), callToolRequest)));
        try {
            BoundExecutable executable = executableBinder.bind(method, argumentBinderRegistry, callToolRequest);
            Object result = executable.invoke(bean);
            String text = "";
            if (returnClass.isAssignableFrom(McpSchema.CallToolResult.class)) {
                return (McpSchema.CallToolResult) result;
            } else if (returnClass.isAssignableFrom(String.class)) {
                text = result.toString();
            } else if (Enum.class.isAssignableFrom(result.getClass())) {
                text = result.toString();
            } else {
                try {
                    text = jsonMapper.writeValueAsString(result);
                } catch (IOException e) {
                    if (LOG.isErrorEnabled()) {
                        LOG.error(e.getMessage(), e);
                    }
                    return new McpSchema.CallToolResult(text, true);
                }
            }
            if (toolOutputSchema(method).isPresent()) {
                Map<String, Object> structuredContent = jsonMapper.readValue(text, Argument.mapOf(String.class, Object.class));
                return McpSchema.CallToolResult.builder()
                    .structuredContent(structuredContent)
                    .isError(false)
                    .build();
            }
            return new McpSchema.CallToolResult(text, false);
        } catch (Exception ex) {
            throw mcpError(ex);
        }
    }

    private <B> McpSchema.Tool tool(ExecutableMethod<B, Object> method) {
        McpSchema.Tool.Builder toolBuilder = McpSchema.Tool.builder()
            .name(toolName(method));
        toolTitle(method).ifPresent(toolBuilder::title);
        toolDescription(method).ifPresent(toolBuilder::description);
        Optional<Argument<?>> jsonSchemaArgumentOptional = jsonSchemaArgument(method);
        String jsonSchema = jsonSchemaArgumentOptional.flatMap(this::jsonSchema).orElse(null);
        if (jsonSchema != null) {
            toolBuilder.inputSchema(mcpJsonMapper, jsonSchema);
            jsonSchemaArgumentOptional.ifPresent(argument ->
                argumentBinderRegistry.addArgumentBinder(new JsonMapperTypedCallToRequestArgumentBinder(argument.getType(), jsonMapper)));
        } else {
            toolBuilder.inputSchema(inputSchema(method, ToolRegistry::toolArgumentName, ToolRegistry::toolArgDescription));
        }
        toolOutputSchema(method).ifPresent(schema -> toolBuilder.outputSchema(mcpJsonMapper, schema));
        return toolBuilder.build();
    }

    private Optional<String> toolOutputSchema(ExecutableMethod<?, ?> method) {
        Class<?> returnClass = method.getReturnType().getType();
        return jsonSchemaClassPathResourceLoader.jsonSchemaStringForClass(returnClass);
    }

    private static String toolArgumentName(Argument<?> argument) {
        return argument.findAnnotation(ToolArg.class)
            .flatMap(annValue -> annValue.stringValue("name"))
            .filter(name -> !name.equals(ToolArg.ELEMENT_NAME))
            .orElseGet(argument::getName);
    }

    private static Optional<String> toolTitle(ExecutableMethod<?, ?> method) {
        return method.stringValue(Tool.class, MEMBER_TITLE);
    }

    private static Optional<String> toolDescription(ExecutableMethod<?, ?> method) {
        return method.stringValue(Tool.class, MEMBER_DESCRIPTION);
    }

    @Nullable
    private static String toolArgDescription(Argument<?> argument) {
        return argument.getAnnotationMetadata().stringValue(ToolArg.class, MEMBER_DESCRIPTION)
            .filter(desc -> !desc.isEmpty())
            .orElse(null);
    }

    private static String toolName(ExecutableMethod<?, ?> method) {
        String name = method.stringValue(Tool.class, "name").orElse(Tool.ELEMENT_NAME);
        if (name.equals(Tool.ELEMENT_NAME)) {
            return method.getName();
        }
        return name;
    }

    @NonNull
    private Optional<Argument<?>> jsonSchemaArgument(@NonNull ExecutableMethod<?, ?> method) {
        Argument<?>[] arguments = method.getArguments();
        for (Argument<?> arg : arguments) {
            try {
                BeanIntrospection<?> introspection = BeanIntrospection.getIntrospection(arg.getType());
                if (introspection.findAnnotation(JsonSchema.class).isPresent()) {
                    return Optional.of(arg);
                }
            } catch (IntrospectionException ex) {

            }
            if (arg.findAnnotation(JsonSchema.class).isPresent()) {
                return Optional.of(arg);
            }
        }
        return Optional.empty();
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

    @NonNull
    private Optional<String> jsonSchema(Argument<?> argument) {
        Class<?> argumentClass = argument.getType();
        return jsonSchemaClassPathResourceLoader.jsonSchemaStringForClass(argumentClass);
    }

    private <B> McpSchema.JsonSchema inputSchema(ExecutableMethod<B, Object> method,
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

    private static String argumentType(Argument<?> argument) {
        if (argument.isAssignableFrom(String.class)) {
            return TYPE_STRING;
        }
        return TYPE_OBJECT;
    }

    private static boolean isArgumentRequired(Argument<?> argument) {
        return !argument.isNullable();
    }
}
