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
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.json.JsonMapper;
import io.micronaut.jsonschema.utils.JsonSchemaClassPathResourceLoader;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.annotations.ToolArg;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import static io.micronaut.mcp.server.registry.JsonSchemaUtils.TYPE_OBJECT;
import static io.micronaut.mcp.server.registry.JsonSchemaUtils.TYPE_STRING;

/**
 * The registry of {@link Tool}s.
 */
@Singleton
@Internal
public final class ToolRegistry extends AbstractMcpMethodRegistry<McpServerFeatures.SyncToolSpecification, McpServerFeatures.AsyncToolSpecification, McpStatelessServerFeatures.SyncToolSpecification, McpStatelessServerFeatures.AsyncToolSpecification> {
    private static final Logger LOG = LoggerFactory.getLogger(ToolRegistry.class);
    /**
     * @see <a href="https://json-schema.org/understanding-json-schema/reference/type">JSON Schema Type</a>
     */
    private static final String MEMBER_DESCRIPTION = "description";

    private final JsonSchemaClassPathResourceLoader jsonSchemaClassPathResourceLoader;
    private final JsonMapper jsonMapper;
    private final BeanContext beanContext;

    ToolRegistry(JsonSchemaClassPathResourceLoader jsonSchemaClassPathResourceLoader,
                 JsonMapper jsonMapper,
                 BeanContext beanContext) {
        this.jsonSchemaClassPathResourceLoader = jsonSchemaClassPathResourceLoader;
        this.jsonMapper = jsonMapper;
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

    private <B> Object[] methodArgs(ExecutableMethod<B, Object> method,
                                    McpSchema.CallToolRequest callToolRequest) {


        if (method.getArguments().length == 1 && jsonSchema(method).isPresent()) {
            Argument<?> argument = method.getArguments()[0];
            Class<?> classInputSchema = argument.getType();
            BeanIntrospection<?> introspection = BeanIntrospection.getIntrospection(classInputSchema);
            Object[] args = new Object[1];
            Object[] arguments = new Object[callToolRequest.arguments().size()];
            int count = 0;
            for (String name : introspection.getPropertyNames()) {
                if (callToolRequest.arguments().containsKey(name)) {
                    arguments[count] = callToolRequest.arguments().get(name);
                    count++;
                }
            }
            args[0] = introspection.instantiate(arguments);
            return args;
        }

        List<String> names = toolArgumentsNames(method);
        Object[] args = new Object[names.size()];
        for (int i = 0; i < names.size(); i++) {
            args[i] = callToolRequest.arguments().get(names.get(i));
        }
        return args;
    }

    private <B> McpSchema.CallToolResult callToolToResult(BeanDefinition<B> beanDefinition,
                                                          ExecutableMethod<B, Object> method,
                                                          Object mcpTransportContext,
                                                          McpSchema.CallToolRequest callToolRequest) {
        Argument<?> returnClass = method.getReturnType().asArgument();
        B bean = beanContext.getBean(beanDefinition);

        Object[] args = methodArgs(method, callToolRequest);
        Object result = method.invoke(bean, args);
        String text = "";
        if (returnClass.isAssignableFrom(String.class)) {
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
            return McpSchema.CallToolResult.builder()
                .structuredContent(text)
                .isError(false)
                .build();
        }
        return new McpSchema.CallToolResult(text, false);
    }

    private <B> McpSchema.Tool tool(ExecutableMethod<B, Object> method) {
        McpSchema.Tool.Builder toolBuilder = McpSchema.Tool.builder()
            .name(toolName(method));
        toolDescription(method).ifPresent(toolBuilder::description);
        Optional<String> jsonSchemaOptional = jsonSchema(method);
        if (jsonSchemaOptional.isPresent()) {
            toolBuilder.inputSchema(jsonSchemaOptional.get());
        } else {
            toolBuilder.inputSchema(inputSchema(method));
        }
        toolOutputSchema(method).ifPresent(toolBuilder::outputSchema);
        return toolBuilder.build();
    }

    private Optional<String> jsonSchema(ExecutableMethod<?, ?> method) {
        Argument<?>[] arguments = method.getArguments();
        if (arguments.length != 1) {
            return Optional.empty();
        }
        Argument<?> argument = arguments[0];
        Class<?> argumentClass = argument.getType();
        return jsonSchemaClassPathResourceLoader.jsonSchemaStringForClass(argumentClass);
    }

    private <B> McpSchema.JsonSchema inputSchema(ExecutableMethod<B, Object> method) {
        Argument<?>[] arguments = method.getArguments();
        Map<String, Object> properties = CollectionUtils.newHashMap(arguments.length);
        List<String> requiredProperties = new ArrayList<>(arguments.length);
        for (Argument<?> argument : arguments) {
            String propertyName = toolArgumentName(argument);
            properties.put(propertyName, toolArgumentJsonSchema(argument));
            if (isToolArgumentRequired(argument)) {
                requiredProperties.add(propertyName);
            }
        }
        return new McpSchema.JsonSchema(TYPE_OBJECT, properties, requiredProperties, null, null, null);
    }

    private Optional<String> toolOutputSchema(ExecutableMethod<?, ?> method) {
        Class<?> returnClass = method.getReturnType().getType();
        return jsonSchemaClassPathResourceLoader.jsonSchemaStringForClass(returnClass);
    }

    private static McpSchema.JsonSchema toolArgumentJsonSchema(Argument<?> argument) {
        return new McpSchema.JsonSchema(toolArgumentType(argument), null, null, null, null, null);
    }

    private static boolean isToolArgumentRequired(Argument<?> argument) {
        return !argument.isNullable();
    }

    private static String toolArgumentName(Argument<?> argument) {
        return argument.findAnnotation(ToolArg.class)
            .flatMap(annValue -> annValue.stringValue("name"))
            .filter(name -> !name.equals(ToolArg.ELEMENT_NAME))
            .orElseGet(argument::getName);
    }

    private static List<String> toolArgumentsNames(ExecutableMethod<?, ?> method) {
        List<String> names = new ArrayList<>();
        for (Argument<?> argument : method.getArguments()) {
            names.add(toolArgumentName(argument));
        }
        return names;
    }

    private static Optional<String> toolDescription(ExecutableMethod<?, ?> method) {
        return method.stringValue(Tool.class, MEMBER_DESCRIPTION);
    }

    private static Optional<String> toolArgumentDescription(Argument<?> argument) {
        return argument.findAnnotation(ToolArg.class)
            .flatMap(annValue -> annValue.stringValue(ToolRegistry.MEMBER_DESCRIPTION))
            .filter(StringUtils::isNotEmpty);
    }

    private static String toolArgumentType(Argument<?> argument) {
        if (argument.isAssignableFrom(String.class)) {
            return TYPE_STRING;
        }
        return TYPE_OBJECT;
    }

    private static String toolName(ExecutableMethod<?, ?> method) {
        String name = method.stringValue(Tool.class, "name").orElse(Tool.ELEMENT_NAME);
        if (name.equals(Tool.ELEMENT_NAME)) {
            return method.getName();
        }
        return name;
    }

    @Override
    public boolean isNotEmpty() {
        return CollectionUtils.isNotEmpty(getAsyncSpecs()) ||
            CollectionUtils.isNotEmpty(getSyncSpecs()) ||
            CollectionUtils.isNotEmpty(getStatelessAsyncSpecs()) ||
            CollectionUtils.isNotEmpty(getStatelessSyncSpecs());
    }
}
