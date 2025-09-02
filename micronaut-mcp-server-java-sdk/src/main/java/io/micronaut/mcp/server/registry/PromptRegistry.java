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
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.mcp.annotations.Prompt;
import io.micronaut.mcp.annotations.PromptArg;
import io.micronaut.mcp.conf.McpServerConfiguration;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.server.McpTransportContext;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * The registry of {@link Prompt}s.
 */
@Requires(beans = McpServerConfiguration.class)
@Named
@Internal
@Singleton
public final class PromptRegistry
    extends AbstractMcpMethodRegistry<McpServerFeatures.SyncPromptSpecification, McpServerFeatures.AsyncPromptSpecification, McpStatelessServerFeatures.SyncPromptSpecification, McpStatelessServerFeatures.AsyncPromptSpecification> {
    public static final String MEMBER_NAME = "name";
    public static final String MEMBER_DESCRIPTION = "description";
    private final BeanContext beanContext;

    PromptRegistry(BeanContext beanContext) {
        this.beanContext = beanContext;
    }

    @Override
    public List<McpServerFeatures.SyncPromptSpecification> getSyncSpecs() {
        return drainMethods()
            .map(method -> new McpServerFeatures.SyncPromptSpecification(
                prompt(method.beanDefinition(), method.method()),
                syncPromptHandler(method.beanDefinition(), method.method())
            ))
            .toList();
    }

    @Override
    public List<McpServerFeatures.AsyncPromptSpecification> getAsyncSpecs() {
        return drainMethods()
            .map(method -> new McpServerFeatures.AsyncPromptSpecification(
                prompt(method.beanDefinition(), method.method()),
                asyncPromptHandler(method.beanDefinition(), method.method())
            ))
            .toList();
    }

    @Override
    public List<McpStatelessServerFeatures.SyncPromptSpecification> getStatelessSyncSpecs() {
        return drainMethods()
            .map(method -> new McpStatelessServerFeatures.SyncPromptSpecification(
                prompt(method.beanDefinition(), method.method()),
                promptHandler(method.beanDefinition(), method.method())
            ))
            .toList();
    }

    @Override
    public List<McpStatelessServerFeatures.AsyncPromptSpecification> getStatelessAsyncSpecs() {
        return drainMethods()
            .map(method -> new McpStatelessServerFeatures.AsyncPromptSpecification(
                prompt(method.beanDefinition(), method.method()),
                reactivePromptHandler(method.beanDefinition(), method.method())
            ))
            .toList();
    }

    private <B> BiFunction<McpTransportContext, McpSchema.GetPromptRequest, Mono<McpSchema.GetPromptResult>> reactivePromptHandler(BeanDefinition<B> beanDefinition,
                                                                                                                                   ExecutableMethod<B, Object> method) {
        return (mcpTransportContext, promptRequest)
            -> Mono.just(promptResult(beanDefinition, method, mcpTransportContext, promptRequest));
    }

    private <B> BiFunction<McpTransportContext, McpSchema.GetPromptRequest, McpSchema.GetPromptResult> promptHandler(BeanDefinition<B> beanDefinition,
                                                                                                                     ExecutableMethod<B, Object> method) {
        return (mcpTransportContext, promptRequest)
            -> promptResult(beanDefinition, method, mcpTransportContext, promptRequest);
    }

    private <B> BiFunction<McpSyncServerExchange, McpSchema.GetPromptRequest, McpSchema.GetPromptResult> syncPromptHandler(BeanDefinition<B> beanDefinition,
                                                                                                                           ExecutableMethod<B, Object> method) {
        return (mcpTransportContext, promptRequest)
            -> promptResult(beanDefinition, method, mcpTransportContext, promptRequest);
    }

    private <B> BiFunction<McpAsyncServerExchange, McpSchema.GetPromptRequest, Mono<McpSchema.GetPromptResult>> asyncPromptHandler(BeanDefinition<B> beanDefinition,
                                                                                                                                   ExecutableMethod<B, Object> method) {
        return (mcpTransportContext, promptRequest)
            -> Mono.just(promptResult(beanDefinition, method, mcpTransportContext, promptRequest));
    }

    private <B> McpSchema.GetPromptResult promptResult(BeanDefinition<B> beanDefinition,
                                                       ExecutableMethod<B, Object> method,
                                                       Object mcpTransportContext,
                                                       McpSchema.GetPromptRequest promptRequest) {
        B bean = beanContext.getBean(beanDefinition);
        List<String> names = promptArgumentsNames(method);
        Object[] args = new Object[names.size()];
        for (int i = 0; i < names.size(); i++) {
            args[i] = promptRequest.arguments().get(names.get(i));
        }
        Object result = method.invoke(bean, args);

        if (method.getReturnType().getType().isAssignableFrom(String.class)) {
            McpSchema.TextContent assistantContent = new McpSchema.TextContent(result.toString());
            McpSchema.PromptMessage assistantMessage = new McpSchema.PromptMessage(McpSchema.Role.ASSISTANT, assistantContent);
            //TODO is it possible to get the description from the javadoc @return of the method
            String description = null;
            return new McpSchema.GetPromptResult(description, List.of(assistantMessage), null);
        }
        //TODO Manage other return types
        return new McpSchema.GetPromptResult("", Collections.emptyList(), null);
    }

    private McpSchema.Prompt prompt(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {
        return new McpSchema.Prompt(promptName(method), promptDescription(method).orElse(null),
            promptArguments(beanDefinition, method));
    }

    private List<McpSchema.PromptArgument> promptArguments(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {
        List<McpSchema.PromptArgument> result = new ArrayList<>(method.getArguments().length);
        for (Argument<?> argument : method.getArguments()) {
            result.add(promptArgument(argument));
        }
        return result;
    }

    private McpSchema.PromptArgument promptArgument(Argument<?> argument) {
        return new McpSchema.PromptArgument(promptArgumentName(argument), promptArgumentDescription(argument).orElse(null), isPromptArgumentRequired(argument));
    }

    private static List<String> promptArgumentsNames(ExecutableMethod<?, ?> method) {
        List<String> names = new ArrayList<>(method.getArguments().length);
        for (Argument<?> argument : method.getArguments()) {
            names.add(promptArgumentName(argument));
        }
        return names;
    }

    private static String promptArgumentName(Argument<?> argument) {
        String name = argument.findAnnotation(PromptArg.class)
            .flatMap(ann -> ann.stringValue(MEMBER_NAME))
            .orElse(argument.getName());
        if (name.equals(PromptArg.ELEMENT_NAME)) {
            return argument.getName();
        }
        return name;
    }

    private boolean isPromptArgumentRequired(Argument<?> argument) {
        return !argument.isNullable();
    }

    private Optional<String> promptArgumentDescription(Argument<?> argument) {
        return argument.findAnnotation(PromptArg.class)
            .flatMap(ann -> ann.stringValue(MEMBER_DESCRIPTION));
    }

    private static Optional<String> promptDescription(ExecutableMethod<?, ?> method) {
        return method.stringValue(Prompt.class, MEMBER_DESCRIPTION);
    }

    private static String promptName(ExecutableMethod<?, ?> method) {
        String name = method.stringValue(Prompt.class, MEMBER_NAME).orElse(Prompt.ELEMENT_NAME);
        if (name.equals(Prompt.ELEMENT_NAME)) {
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
