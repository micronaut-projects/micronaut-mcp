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
import io.micronaut.context.processor.ExecutableMethodProcessor;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.Executable;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.server.conf.McpServerConfiguration;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Singleton
@Internal
class ToolRegistry implements ExecutableMethodProcessor<Tool> {
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

    private final BeanContext beanContext;
    private final McpServerConfiguration mcpServerConfiguration;

    ToolRegistry(BeanContext beanContext,
                 McpServerConfiguration mcpServerConfiguration) {
        this.beanContext = beanContext;
        this.mcpServerConfiguration = mcpServerConfiguration;
    }

    @Override
    public void process(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {
        if (method.hasDeclaredAnnotation(Tool.class)) {
            Object bean = switch (mcpServerConfiguration.getType()) {
                case SYNC -> syncToolSpecification(beanDefinition, method);
                case ASYNC -> asyncToolSpecification(beanDefinition, method);
                case STATELESS_ASYNC -> statelessAsyncToolSpecification(beanDefinition, method);
                case STATELESS_SYNC -> statelessSyncToolSpecification(beanDefinition, method);
            };
            beanContext.registerSingleton(bean);
        }
    }

    private McpServerFeatures.SyncToolSpecification syncToolSpecification(BeanDefinition beanDefinition, ExecutableMethod<?, ?> method) {
        McpSchema.Tool tool = toolArgument(method);
        Class<Object> returnClass = (Class<Object>) method.getReturnType().getType();
        McpServerFeatures.SyncToolSpecification.Builder builder = McpServerFeatures.SyncToolSpecification.builder()
            .tool(tool);
        if (returnClass.isAssignableFrom(String.class)) {
            builder.callHandler((mcpTransportContext, callToolRequest)
                -> callToolToResult(beanDefinition, method, mcpTransportContext, callToolRequest));
        }
        return builder.build();
    }

    private McpServerFeatures.AsyncToolSpecification asyncToolSpecification(BeanDefinition beanDefinition, ExecutableMethod<?, ?> method) {
        McpSchema.Tool tool = toolArgument(method);
        Class<Object> returnClass = (Class<Object>) method.getReturnType().getType();
        McpServerFeatures.AsyncToolSpecification.Builder builder = McpServerFeatures.AsyncToolSpecification.builder()
            .tool(tool);
        if (returnClass.isAssignableFrom(String.class)) {
            builder.callHandler((mcpTransportContext, callToolRequest) ->
                reactiveCallToolToResult(beanDefinition, method, mcpTransportContext, callToolRequest));
        }
        return builder.build();
    }

    private McpStatelessServerFeatures.AsyncToolSpecification statelessAsyncToolSpecification(BeanDefinition beanDefinition, ExecutableMethod<?, ?> method) {
        McpSchema.Tool tool = toolArgument(method);
        Class<Object> returnClass = (Class<Object>) method.getReturnType().getType();
        McpStatelessServerFeatures.AsyncToolSpecification.Builder builder = McpStatelessServerFeatures.AsyncToolSpecification.builder()
            .tool(tool);
        if (returnClass.isAssignableFrom(String.class)) {
            builder.callHandler((mcpTransportContext, callToolRequest)
                -> reactiveCallToolToResult(beanDefinition, method, mcpTransportContext, callToolRequest));
        }
        return builder.build();
    }

    private McpStatelessServerFeatures.SyncToolSpecification statelessSyncToolSpecification(BeanDefinition beanDefinition, ExecutableMethod<?, ?> method) {
        McpSchema.Tool tool = toolArgument(method);
        McpStatelessServerFeatures.SyncToolSpecification.Builder builder = McpStatelessServerFeatures.SyncToolSpecification.builder()
            .tool(tool);

        builder.callHandler((mcpTransportContext, callToolRequest)
                -> callToolToResult(beanDefinition, method, mcpTransportContext, callToolRequest));

        return builder.build();
    }

    private Mono<McpSchema.CallToolResult> reactiveCallToolToResult(BeanDefinition<?> beanDefinition,
                                                           ExecutableMethod<?, ?> method,
                                                           Object mcpTransportContext,
                                                           McpSchema.CallToolRequest callToolRequest) {
        McpSchema.CallToolResult result = callToolToResult(beanDefinition, method, mcpTransportContext, callToolRequest);
        if (result == null) {
            return Mono.empty();
        }
        return Mono.just(result);
    }

    private McpSchema.CallToolResult callToolToResult(BeanDefinition<?> beanDefinition,
                                                      ExecutableMethod<?, ?> method,
                                                      Object mcpTransportContext,
                                                      McpSchema.CallToolRequest callToolRequest) {
        Class<Object> returnClass = (Class<Object>) method.getReturnType().getType();
        Object bean = beanContext.getBean(beanDefinition);
        List<String> names = toolArgumentsNames(method);
        Object[] args = new Object[names.size()];
        for (int i = 0; i < names.size(); i++) {
            args[i] = callToolRequest.arguments().get(names.get(i));
        }
        Object result = ((Executable<Object, ?>) method).invoke(bean, args);
        if (returnClass.isAssignableFrom(String.class)) {
            return new McpSchema.CallToolResult(result.toString(), false);
        }
        return null; //TODO
    }

    private static McpSchema.Tool toolArgument(ExecutableMethod<?, ?> method) {
        McpSchema.Tool.Builder toolBuilder = McpSchema.Tool.builder()
            .name(toolName(method));
        toolArgumentDescription(method).ifPresent(toolBuilder::description);
        Map<String, Object> properties = new HashMap<>();
        List<String> requiredProperties = new ArrayList<>();
        for (Argument argument : method.getArguments()) {
            String propertyName = toolArgumentName(argument);
            properties.put(propertyName, toolArgumentJsonSchema(argument));
            if (isToolArgumentRequired(argument)) {
                requiredProperties.add(propertyName);
            }
        }
        McpSchema.JsonSchema inputSchema = new McpSchema.JsonSchema(TYPE_OBJECT, properties, requiredProperties, null, null, null);
        toolBuilder.inputSchema(inputSchema);
        return toolBuilder.build();
    }

    private static McpSchema.JsonSchema toolArgumentJsonSchema(Argument argument) {
        return new McpSchema.JsonSchema(toolArgumentType(argument), null, null, null, null, null);
    }

    private static boolean isToolArgumentRequired(Argument argument) {
        return !argument.isNullable();
    }

    private static String toolArgumentName(Argument argument) {
        return argument.getName();
    }

    private static List<String> toolArgumentsNames(ExecutableMethod<?, ?> method) {
        List<String> names = new ArrayList<>();
        for (Argument argument : method.getArguments()) {
            names.add(toolArgumentName(argument));
        }
        return names;
    }

    private static Optional<String> toolArgumentDescription(ExecutableMethod<?, ?> method) {
        return method.stringValue(Tool.class, MEMBER_DESCRIPTION);
    }

    private static String toolArgumentType(Argument argument) {
        if (argument.getType().isAssignableFrom(String.class)) {
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
}
