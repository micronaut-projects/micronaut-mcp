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
import io.micronaut.context.processor.ExecutableMethodProcessor;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.Executable;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.mcp.annotations.Prompt;
import io.micronaut.mcp.annotations.PromptArg;
import io.micronaut.mcp.server.conf.McpServerConfiguration;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.server.McpTransportContext;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

@Requires(beans = McpServerConfiguration.class)
@Internal
@Singleton
class PromptExecutableMethodProcessor implements ExecutableMethodProcessor<Prompt> {
    public static final String MEMBER_NAME = "name";
    public static final String MEMBER_DESCRIPTION = "description";
    private final BeanContext beanContext;
    private final McpServerConfiguration  mcpServerConfiguration;

    PromptExecutableMethodProcessor(BeanContext beanContext,
                                    McpServerConfiguration mcpServerConfiguration) {
        this.beanContext = beanContext;
        this.mcpServerConfiguration = mcpServerConfiguration;
    }

    @Override
    public void process(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {
        if (method.hasDeclaredAnnotation(Prompt.class)) {
            Object bean = switch (mcpServerConfiguration.getType()) {
                case SYNC -> syncPromptSpecification(beanDefinition, method);
                case ASYNC -> asyncPromptSpecification(beanDefinition, method);
                case STATELESS_ASYNC -> statelessAsyncPromptSpecification(beanDefinition, method);
                case STATELESS_SYNC -> statelessSyncPromptSpecification(beanDefinition, method);
            };
            beanContext.registerSingleton(bean);
        }
    }

    private McpStatelessServerFeatures.SyncPromptSpecification statelessSyncPromptSpecification(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {
        return new McpStatelessServerFeatures.SyncPromptSpecification(prompt(beanDefinition, method), promptHandler(beanDefinition, method));
    }

    private McpStatelessServerFeatures.AsyncPromptSpecification statelessAsyncPromptSpecification(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {
        return new McpStatelessServerFeatures.AsyncPromptSpecification(prompt(beanDefinition, method), reactivePromptHandler(beanDefinition, method));
    }

    private McpServerFeatures.SyncPromptSpecification syncPromptSpecification(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {
        return new McpServerFeatures.SyncPromptSpecification(prompt(beanDefinition, method), syncPromptHandler(beanDefinition, method));
    }

    private McpServerFeatures.AsyncPromptSpecification asyncPromptSpecification(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {
        return new McpServerFeatures.AsyncPromptSpecification(prompt(beanDefinition, method),
            asyncPromptHandler(beanDefinition, method));
    }

    private BiFunction<McpTransportContext, McpSchema.GetPromptRequest, Mono<McpSchema.GetPromptResult>> reactivePromptHandler(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {
        return (mcpTransportContext, promptRequest)
            -> Mono.just(promptResult(beanDefinition, method, mcpTransportContext, promptRequest));
    }

    private BiFunction<McpTransportContext, McpSchema.GetPromptRequest, McpSchema.GetPromptResult> promptHandler(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {
        return (mcpTransportContext, promptRequest)
            -> promptResult(beanDefinition, method, mcpTransportContext, promptRequest);
    }

    private BiFunction<McpSyncServerExchange, McpSchema.GetPromptRequest, McpSchema.GetPromptResult> syncPromptHandler(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {
        return (mcpTransportContext, promptRequest)
            -> promptResult(beanDefinition, method, mcpTransportContext, promptRequest);
    }

    private BiFunction<McpAsyncServerExchange, McpSchema.GetPromptRequest, Mono<McpSchema.GetPromptResult>> asyncPromptHandler(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {
        return (mcpTransportContext, promptRequest)
            -> Mono.just(promptResult(beanDefinition, method, mcpTransportContext, promptRequest));
    }

    private McpSchema.GetPromptResult promptResult(BeanDefinition<?> beanDefinition,
                                                   ExecutableMethod<?, ?> method,
                                                   Object mcpTransportContext,
                                                   McpSchema.GetPromptRequest promptRequest) {
        Class<Object> returnClass = (Class<Object>) method.getReturnType().getType();
        Object bean = beanContext.getBean(beanDefinition);
        List<String> names = promptArgumentsNames(method);
        Object[] args = new Object[names.size()];
        for (int i = 0; i < names.size(); i++) {
            args[i] = promptRequest.arguments().get(names.get(i));
        }
        Object result = ((Executable<Object, ?>) method).invoke(bean, args);

        if (returnClass.isAssignableFrom(String.class)) {
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
        List<McpSchema.PromptArgument> result = new ArrayList<>();
        for (Argument argument : method.getArguments()) {
            result.add(promptArgument(argument));
        }
        return result;
    }

    private McpSchema.PromptArgument promptArgument(Argument argument) {
        return new McpSchema.PromptArgument(promptArgumentName(argument), promptArgumentDescription(argument).orElse(null), isPromptArgumentRequired(argument));
    }

    private static List<String> promptArgumentsNames(ExecutableMethod<?, ?> method) {
        List<String> names = new ArrayList<>();
        for (Argument argument : method.getArguments()) {
            names.add(promptArgumentName(argument));
        }
        return names;
    }

    private static String promptArgumentName(Argument argument) {
        String name = argument.findAnnotation(PromptArg.class)
            .flatMap(ann -> ann.stringValue(MEMBER_NAME))
            .orElse(argument.getName());
        if (name.equals(PromptArg.ELEMENT_NAME)) {
            return argument.getName();
        }
        return name;
    }

    private boolean isPromptArgumentRequired(Argument argument) {
        return !argument.isNullable();
    }

    private Optional<String> promptArgumentDescription(Argument argument) {
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
}
