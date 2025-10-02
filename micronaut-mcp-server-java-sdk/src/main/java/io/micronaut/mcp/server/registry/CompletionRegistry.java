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
import io.micronaut.core.bind.ArgumentBinderRegistry;
import io.micronaut.core.bind.BoundExecutable;
import io.micronaut.core.bind.DefaultExecutableBinder;
import io.micronaut.core.bind.ExecutableBinder;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.mcp.annotations.PromptCompletion;
import io.micronaut.mcp.annotations.ResourceCompletion;
import io.micronaut.mcp.server.exceptions.McpErrorExceptionMapper;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

/**
 * The registry of {@link PromptCompletion} and {@link ResourceCompletion}.
 */
@Internal
@Singleton
public final class CompletionRegistry extends AbstractMcpMethodRegistry<
    McpServerFeatures.SyncCompletionSpecification,
    McpServerFeatures.AsyncCompletionSpecification,
    McpStatelessServerFeatures.SyncCompletionSpecification,
    McpStatelessServerFeatures.AsyncCompletionSpecification> {
    private static final Logger LOG = LoggerFactory.getLogger(CompletionRegistry.class);
    private final ArgumentBinderRegistry<McpSchema.CompleteRequest> argumentBinderRegistry;

    CompletionRegistry(List<McpErrorExceptionMapper<? extends Throwable>> exceptionMappers,
                       BeanContext beanContext,
                       ArgumentBinderRegistry<McpSchema.CompleteRequest> argumentBinderRegistry) {
        super(exceptionMappers, beanContext);
        this.argumentBinderRegistry = argumentBinderRegistry;
    }

    @Override
    public List<McpServerFeatures.SyncCompletionSpecification> getSyncSpecs() {
        return drainMethods()
            .map(m -> new McpServerFeatures.SyncCompletionSpecification(
                toCompletion(m.method()),
                syncHandler(m.beanDefinition(), m.method())
            ))
            .toList();
    }

    @Override
    public List<McpServerFeatures.AsyncCompletionSpecification> getAsyncSpecs() {
        return drainMethods()
            .map(m -> new McpServerFeatures.AsyncCompletionSpecification(
                toCompletion(m.method()),
                asyncHandler(m.beanDefinition(), m.method())
            ))
            .toList();
    }

    @Override
    public List<McpStatelessServerFeatures.SyncCompletionSpecification> getStatelessSyncSpecs() {
        return drainMethods()
            .map(m -> new McpStatelessServerFeatures.SyncCompletionSpecification(
                toCompletion(m.method()),
                statelessSyncHandler(m.beanDefinition(), m.method())
            ))
            .toList();
    }

    @Override
    public List<McpStatelessServerFeatures.AsyncCompletionSpecification> getStatelessAsyncSpecs() {
        return drainMethods()
            .map(m -> new McpStatelessServerFeatures.AsyncCompletionSpecification(
                toCompletion(m.method()),
                statelessAsyncHandler(m.beanDefinition(), m.method())
            ))
            .toList();
    }

    @Override
    public boolean isNotEmpty() {
        return !getSyncSpecs().isEmpty()
            || !getAsyncSpecs().isEmpty()
            || !getStatelessSyncSpecs().isEmpty()
            || !getStatelessAsyncSpecs().isEmpty();
    }

    private <B> BiFunction<McpSyncServerExchange, McpSchema.CompleteRequest, McpSchema.CompleteResult> syncHandler(
        BeanDefinition<B> beanDefinition,
        ExecutableMethod<B, Object> method
    ) {
        return (exchange, request) -> invokeAndMap(beanDefinition, method, exchange, request);
    }

    private <B> BiFunction<McpAsyncServerExchange, McpSchema.CompleteRequest, Mono<McpSchema.CompleteResult>> asyncHandler(
        BeanDefinition<B> beanDefinition,
        ExecutableMethod<B, Object> method
    ) {
        return (exchange, request) -> Mono.just(invokeAndMap(beanDefinition, method, exchange, request));
    }

    private <B> BiFunction<McpTransportContext, McpSchema.CompleteRequest, McpSchema.CompleteResult> statelessSyncHandler(
        BeanDefinition<B> beanDefinition,
        ExecutableMethod<B, Object> method
    ) {
        return (ctx, request) -> invokeAndMap(beanDefinition, method, ctx, request);
    }

    private <B> BiFunction<McpTransportContext, McpSchema.CompleteRequest, Mono<McpSchema.CompleteResult>> statelessAsyncHandler(
        BeanDefinition<B> beanDefinition,
        ExecutableMethod<B, Object> method
    ) {
        return (ctx, request) -> Mono.just(invokeAndMap(beanDefinition, method, ctx, request));
    }

    private <B> McpSchema.CompleteResult invokeAndMap(BeanDefinition<B> beanDefinition,
                                                      ExecutableMethod<B, Object> method,
                                                      Object mcpTransportContext,
                                                      McpSchema.CompleteRequest request) {
        B bean = beanContext.getBean(beanDefinition);

        ExecutableBinder<McpSchema.CompleteRequest> executableBinder = new DefaultExecutableBinder<>(
            prepareBoundVariables(method, List.of(resolveMcpTransportContext(mcpTransportContext), request, request.argument())));
        BoundExecutable executable = executableBinder.bind(method, argumentBinderRegistry, request);
        Object result = executable.invoke(bean);
        if (result instanceof McpSchema.CompleteResult r) {
            return r;
        }
        try {
            List<String> values = asStringList(result);
            return completeResult(values);
        } catch (IllegalArgumentException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("return type is not {} or List<String>", McpSchema.CompleteResult.class.getSimpleName());
            }
        }
        return completeResult(Collections.emptyList());
    }

    private static McpSchema.CompleteResult completeResult(List<String> values) {
        return new McpSchema.CompleteResult(new McpSchema.CompleteResult.CompleteCompletion(values, values.size(), false));
    }

    public static List<String> asStringList(Object result) {
        if (result instanceof List<?> list && list.stream().allMatch(String.class::isInstance)) {
            return list.stream().map(String.class::cast).toList();
        }
        throw new IllegalArgumentException("Not a List<String>");
    }

    private static <B> McpSchema.CompleteReference toCompletion(ExecutableMethod<B, Object> method) {
        if (method.hasAnnotation(PromptCompletion.class)) {
            return toPromptReference(method);
        } else if (method.hasAnnotation(ResourceCompletion.class)) {
            return toResourceReference(method);
        }
        throw new McpError(new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INTERNAL_ERROR, "completion method should be annotated either with ResourceCompletion or PromptCompletion", null));
    }

    private static <B> McpSchema.ResourceReference toResourceReference(ExecutableMethod<B, Object> method) {
        String uri = method.stringValue(ResourceCompletion.class, URI_PROPERTY).orElseThrow(() -> new IllegalStateException("ResourceCompletion must defined a uri"));
        return new McpSchema.ResourceReference(uri);
    }

    private static <B> McpSchema.PromptReference toPromptReference(ExecutableMethod<B, Object> method) {
        String name = method.stringValue(PromptCompletion.class, NAME_PROPERTY).orElse(PromptCompletion.ELEMENT_NAME);
        if (PromptCompletion.ELEMENT_NAME.equals(name)) {
            name = method.getName();
        }
        return new McpSchema.PromptReference(name);
    }
}
