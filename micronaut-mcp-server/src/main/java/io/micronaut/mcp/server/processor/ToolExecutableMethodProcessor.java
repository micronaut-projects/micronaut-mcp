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
import io.micronaut.core.type.Executable;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.json.JsonMapper;
import io.micronaut.jsonschema.utils.JsonSchemaClassPathResourceLoader;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.server.conf.McpServerConfiguration;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.micronaut.mcp.server.processor.JsonSchemaUtils.TYPE_OBJECT;
import static io.micronaut.mcp.server.processor.JsonSchemaUtils.TYPE_STRING;

@Singleton
@Internal
class ToolExecutableMethodProcessor implements ExecutableMethodProcessor<Tool> {
    private static final Logger LOG = LoggerFactory.getLogger(ToolExecutableMethodProcessor.class);
    private static final String MEMBER_DESCRIPTION = "description";
    private final JsonSchemaClassPathResourceLoader jsonSchemaClassPathResourceLoader;
    private final JsonMapper jsonMapper;
    private final BeanContext beanContext;
    private final McpServerConfiguration mcpServerConfiguration;

    ToolExecutableMethodProcessor(JsonSchemaClassPathResourceLoader jsonSchemaClassPathResourceLoader,
                 JsonMapper jsonMapper,
                 BeanContext beanContext,
                 McpServerConfiguration mcpServerConfiguration) {
        this.jsonSchemaClassPathResourceLoader = jsonSchemaClassPathResourceLoader;
        this.jsonMapper = jsonMapper;
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
        McpStatelessServerFeatures.AsyncToolSpecification.Builder builder = McpStatelessServerFeatures.AsyncToolSpecification.builder()
            .tool(tool);
        Class<Object> returnClass = (Class<Object>) method.getReturnType().getType();
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

    private McpSchema.Tool toolArgument(ExecutableMethod<?, ?> method) {
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
        toolOutputSchema(method).ifPresent(toolBuilder::outputSchema);
        return toolBuilder.build();
    }

    private Optional<String> toolOutputSchema(ExecutableMethod<?, ?> method) {
        Class<Object> returnClass = (Class<Object>) method.getReturnType().getType();
        return jsonSchemaClassPathResourceLoader.jsonSchemaStringForClass(returnClass);
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
