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
package io.micronaut.mcp.server.processor;

import io.micronaut.context.BeanContext;
import io.micronaut.context.processor.ExecutableMethodProcessor;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.json.JsonMapper;
import io.micronaut.jsonschema.utils.JsonSchemaClassPathResourceLoader;
import io.micronaut.mcp.annotations.Tool;
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
import java.util.stream.Stream;

/**
 * The registry of {@link Tool}s.
 */
@Singleton
@Internal
public final class ToolRegistry implements ExecutableMethodProcessor<Tool> {
    private static final Logger LOG = LoggerFactory.getLogger(ToolRegistry.class);
    /**
     * @see <a href="https://json-schema.org/understanding-json-schema/reference/type">JSON Schema Type</a>
     */
    private static final String TYPE_STRING = "string";
    private static final String TYPE_NUMBER = "number";
    private static final String TYPE_OBJECT = "object";
    private static final String TYPE_ARRAY = "array";
    private static final String TYPE_BOOL = "bool";
    private static final String TYPE_NULL = "null";
    private static final String MEMBER_DESCRIPTION = "description";

    private final JsonSchemaClassPathResourceLoader jsonSchemaClassPathResourceLoader;
    private final JsonMapper jsonMapper;
    private final BeanContext beanContext;

    private final List<ToolMethod<Object>> toolMethods = new ArrayList<>();

    ToolRegistry(JsonSchemaClassPathResourceLoader jsonSchemaClassPathResourceLoader,
                 JsonMapper jsonMapper,
                 BeanContext beanContext) {
        this.jsonSchemaClassPathResourceLoader = jsonSchemaClassPathResourceLoader;
        this.jsonMapper = jsonMapper;
        this.beanContext = beanContext;
    }

    @Override
    public void process(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {
        if (method.hasDeclaredAnnotation(Tool.class)) {
            toolMethods.add(new ToolMethod(beanDefinition, method));
        }
    }

    private Stream<ToolMethod<Object>> drainToolMethods() {
        return toolMethods.stream().onClose(toolMethods::clear);
    }

    public List<McpServerFeatures.SyncToolSpecification> getSyncToolSpecs() {
        return drainToolMethods()
            .map(toolMethod -> McpServerFeatures.SyncToolSpecification.builder()
                .tool(toolArgument(toolMethod.method()))
                .callHandler(provideSyncCallHandler(toolMethod.beanDefinition(), toolMethod.method()))
                .build())
            .toList();
    }

    public List<McpServerFeatures.AsyncToolSpecification> getAsyncToolSpecs() {
        return drainToolMethods()
            .map(toolMethod -> McpServerFeatures.AsyncToolSpecification.builder()
                .tool(toolArgument(toolMethod.method()))
                .callHandler(provideReactiveCallHandler(toolMethod.beanDefinition(), toolMethod.method()))
                .build())
            .toList();
    }

    public List<McpStatelessServerFeatures.SyncToolSpecification> getStatelessSyncToolSpecs() {
        return drainToolMethods()
            .map(toolMethod -> McpStatelessServerFeatures.SyncToolSpecification.builder()
                .tool(toolArgument(toolMethod.method()))
                .callHandler(provideSyncCallHandler(toolMethod.beanDefinition(), toolMethod.method()))
                .build())
            .toList();
    }

    public List<McpStatelessServerFeatures.AsyncToolSpecification> getStatelessAsyncToolSpecs() {
        return drainToolMethods()
            .map(toolMethod -> McpStatelessServerFeatures.AsyncToolSpecification.builder()
                .tool(toolArgument(toolMethod.method()))
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

    private <B> McpSchema.CallToolResult callToolToResult(BeanDefinition<B> beanDefinition,
                                                          ExecutableMethod<B, Object> method,
                                                          Object mcpTransportContext,
                                                          McpSchema.CallToolRequest callToolRequest) {
        Class<?> returnClass = method.getReturnType().getType();
        B bean = beanContext.getBean(beanDefinition);
        List<String> names = toolArgumentsNames(method);
        Object[] args = new Object[names.size()];
        for (int i = 0; i < names.size(); i++) {
            args[i] = callToolRequest.arguments().get(names.get(i));
        }
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

    private <B> McpSchema.Tool toolArgument(ExecutableMethod<B, Object> method) {
        McpSchema.Tool.Builder toolBuilder = McpSchema.Tool.builder()
            .name(toolName(method));
        toolArgumentDescription(method).ifPresent(toolBuilder::description);
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
        McpSchema.JsonSchema inputSchema = new McpSchema.JsonSchema(TYPE_OBJECT, properties, requiredProperties, null, null, null);
        toolBuilder.inputSchema(inputSchema);
        toolOutputSchema(method).ifPresent(toolBuilder::outputSchema);
        return toolBuilder.build();
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
        return argument.getName();
    }

    private static List<String> toolArgumentsNames(ExecutableMethod<?, ?> method) {
        List<String> names = new ArrayList<>();
        for (Argument<?> argument : method.getArguments()) {
            names.add(toolArgumentName(argument));
        }
        return names;
    }

    private static Optional<String> toolArgumentDescription(ExecutableMethod<?, ?> method) {
        return method.stringValue(Tool.class, MEMBER_DESCRIPTION);
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

    private record ToolMethod<B>(BeanDefinition<B> beanDefinition,
                                 ExecutableMethod<B, Object> method) {
    }
}
