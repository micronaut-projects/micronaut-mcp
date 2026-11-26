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
import io.micronaut.core.bind.ArgumentBinderRegistry;
import io.micronaut.core.type.Argument;
import io.micronaut.inject.ExecutableMethod;
import org.jspecify.annotations.Nullable;
import io.micronaut.mcp.annotations.Prompt;
import io.micronaut.mcp.annotations.PromptArg;
import io.micronaut.mcp.conf.server.McpServerConfiguration;
import io.micronaut.mcp.server.exceptions.McpErrorExceptionMapper;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
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
    private static final Class<?>[] BOUND_PARAMETER_TYPES = {McpTransportContext.class, McpSchema.GetPromptRequest.class};
    private final ArgumentBinderRegistry<McpSchema.GetPromptRequest> argumentBinderRegistry;

    PromptRegistry(List<McpErrorExceptionMapper<? extends Throwable>> exceptionMappers,
                   BeanContext beanContext,
                   ArgumentBinderRegistry<McpSchema.GetPromptRequest> argumentBinderRegistry) {
        super(exceptionMappers, beanContext);
        this.argumentBinderRegistry = argumentBinderRegistry;
    }

    @Override
    protected Class<?>[] boundParameterTypes() {
        return BOUND_PARAMETER_TYPES;
    }

    @Override
    public List<McpServerFeatures.SyncPromptSpecification> getSyncSpecs() {
        return streamMethods()
            .map(method -> new McpServerFeatures.SyncPromptSpecification(
                prompt(method),
                syncPromptHandler(method)
            ))
            .toList();
    }

    @Override
    public List<McpServerFeatures.AsyncPromptSpecification> getAsyncSpecs() {
        return streamMethods()
            .map(method -> new McpServerFeatures.AsyncPromptSpecification(
                prompt(method),
                asyncPromptHandler(method)
            ))
            .toList();
    }

    @Override
    public List<McpStatelessServerFeatures.SyncPromptSpecification> getStatelessSyncSpecs() {
        return streamMethods()
            .map(method -> new McpStatelessServerFeatures.SyncPromptSpecification(
                prompt(method),
                promptHandler(method)
            ))
            .toList();
    }

    @Override
    public List<McpStatelessServerFeatures.AsyncPromptSpecification> getStatelessAsyncSpecs() {
        return streamMethods()
            .map(method -> new McpStatelessServerFeatures.AsyncPromptSpecification(
                prompt(method),
                reactivePromptHandler(method)
            ))
            .toList();
    }

    private <B> BiFunction<McpTransportContext, McpSchema.GetPromptRequest, Mono<McpSchema.GetPromptResult>> reactivePromptHandler(Method<B> m) {
        String name = observedName(m);
        return (mcpTransportContext, promptRequest)
            -> promptResultAsync(m, name, mcpTransportContext, promptRequest);
    }

    private <B> BiFunction<McpTransportContext, McpSchema.GetPromptRequest, McpSchema.GetPromptResult> promptHandler(Method<B> m) {
        String name = observedName(m);
        return (mcpTransportContext, promptRequest)
            -> promptResult(m, name, mcpTransportContext, promptRequest);
    }

    private <B> BiFunction<McpSyncServerExchange, McpSchema.GetPromptRequest, McpSchema.GetPromptResult> syncPromptHandler(Method<B> m) {
        String name = observedName(m);
        return (mcpTransportContext, promptRequest)
            -> promptResult(m, name, mcpTransportContext, promptRequest);
    }

    private <B> BiFunction<McpAsyncServerExchange, McpSchema.GetPromptRequest, Mono<McpSchema.GetPromptResult>> asyncPromptHandler(Method<B> m) {
        String name = observedName(m);
        return (mcpTransportContext, promptRequest)
            -> promptResultAsync(m, name, mcpTransportContext, promptRequest);
    }

    private <B> McpSchema.GetPromptResult promptResult(Method<B> m,
                                                       String name,
                                                       Object mcpTransportContext,
                                                       McpSchema.GetPromptRequest promptRequest) {
        return observed(McpSchema.METHOD_PROMPT_GET, name,
            () -> m.call(() -> m.invoke(argumentBinderRegistry, promptRequest, mcpTransportContext, promptRequest),
                mcpTransportContext, PromptRegistry::map, e -> failWithMcpError(m, e)));
    }

    private <B> Mono<McpSchema.GetPromptResult> promptResultAsync(Method<B> m,
                                                                  String name,
                                                                  Object mcpTransportContext,
                                                                  McpSchema.GetPromptRequest promptRequest) {
        return observedAsync(McpSchema.METHOD_PROMPT_GET, name,
            () -> m.callAsync(() -> m.invoke(argumentBinderRegistry, promptRequest, mcpTransportContext, promptRequest),
                PromptRegistry::map, e -> failWithMcpError(m, e)));
    }

    private static McpSchema.GetPromptResult map(@Nullable Object result) {
        if (result instanceof McpSchema.GetPromptResult promptResult) {
            return promptResult;
        }
        if (result instanceof CharSequence text) {
            McpSchema.TextContent assistantContent = new McpSchema.TextContent(text.toString());
            McpSchema.PromptMessage assistantMessage = new McpSchema.PromptMessage(McpSchema.Role.ASSISTANT, assistantContent);
            //TODO is it possible to get the description from the javadoc @return of the method
            String description = null;
            return new McpSchema.GetPromptResult(description, List.of(assistantMessage), null);
        }
        //TODO Manage other return types
        return new McpSchema.GetPromptResult("", Collections.emptyList(), null);
    }

    private McpSchema.Prompt prompt(Method<?> m) {
        ExecutableMethod<?, Object> method = m.method();
        return new McpSchema.Prompt(promptName(method), promptTitle(method).orElse(null), promptDescription(method).orElse(null),
            promptArguments(m), meta(method), icons(method));
    }

    private List<McpSchema.PromptArgument> promptArguments(Method<?> m) {
        Argument<?>[] arguments = m.method().getArguments();
        List<McpSchema.PromptArgument> result = new ArrayList<>(arguments.length);
        for (int i = 0; i < arguments.length; i++) {
            // Values bound from the request are not arguments the client fills in
            if (!m.isBound(i)) {
                result.add(promptArgument(arguments[i]));
            }
        }
        return result;
    }

    private McpSchema.PromptArgument promptArgument(Argument<?> argument) {
        return new McpSchema.PromptArgument(promptArgumentName(argument), promptArgumentDescription(argument).orElse(null), isPromptArgumentRequired(argument));
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

    private static Optional<String> promptTitle(ExecutableMethod<?, ?> method) {
        return method.stringValue(Prompt.class, MEMBER_TITLE);
    }

    private static Optional<String> promptDescription(ExecutableMethod<?, ?> method) {
        return method.stringValue(Prompt.class, MEMBER_DESCRIPTION);
    }

    @Override
    protected String primitiveName(ExecutableMethod<?, ?> method) {
        return promptName(method);
    }

    private static String promptName(ExecutableMethod<?, ?> method) {
        String name = method.stringValue(Prompt.class, MEMBER_NAME).orElse(Prompt.ELEMENT_NAME);
        if (name.equals(Prompt.ELEMENT_NAME)) {
            return method.getName();
        }
        return name;
    }
}
