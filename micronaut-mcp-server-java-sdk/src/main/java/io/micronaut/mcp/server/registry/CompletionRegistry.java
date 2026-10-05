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
import io.micronaut.inject.ExecutableMethod;
import org.jspecify.annotations.Nullable;
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
    private static final Class<?>[] BOUND_PARAMETER_TYPES = {McpTransportContext.class, McpSchema.CompleteRequest.class, McpSchema.CompleteRequest.CompleteArgument.class};
    private final ArgumentBinderRegistry<McpSchema.CompleteRequest> argumentBinderRegistry;

    CompletionRegistry(List<McpErrorExceptionMapper<? extends Throwable>> exceptionMappers,
                       BeanContext beanContext,
                       ArgumentBinderRegistry<McpSchema.CompleteRequest> argumentBinderRegistry) {
        super(exceptionMappers, beanContext);
        this.argumentBinderRegistry = argumentBinderRegistry;
    }

    @Override
    protected Class<?>[] boundParameterTypes() {
        return BOUND_PARAMETER_TYPES;
    }

    @Override
    public List<McpServerFeatures.SyncCompletionSpecification> getSyncSpecs() {
        return drainMethods()
            .map(m -> new McpServerFeatures.SyncCompletionSpecification(
                toCompletion(m.method()),
                syncHandler(m)
            ))
            .toList();
    }

    @Override
    public List<McpServerFeatures.AsyncCompletionSpecification> getAsyncSpecs() {
        return drainMethods()
            .map(m -> new McpServerFeatures.AsyncCompletionSpecification(
                toCompletion(m.method()),
                asyncHandler(m)
            ))
            .toList();
    }

    @Override
    public List<McpStatelessServerFeatures.SyncCompletionSpecification> getStatelessSyncSpecs() {
        return drainMethods()
            .map(m -> new McpStatelessServerFeatures.SyncCompletionSpecification(
                toCompletion(m.method()),
                statelessSyncHandler(m)
            ))
            .toList();
    }

    @Override
    public List<McpStatelessServerFeatures.AsyncCompletionSpecification> getStatelessAsyncSpecs() {
        return drainMethods()
            .map(m -> new McpStatelessServerFeatures.AsyncCompletionSpecification(
                toCompletion(m.method()),
                statelessAsyncHandler(m)
            ))
            .toList();
    }

    private <B> BiFunction<McpSyncServerExchange, McpSchema.CompleteRequest, McpSchema.CompleteResult> syncHandler(
        Method<B> m
    ) {
        return (exchange, request) -> invokeAndMap(m, exchange, request);
    }

    private <B> BiFunction<McpAsyncServerExchange, McpSchema.CompleteRequest, Mono<McpSchema.CompleteResult>> asyncHandler(
        Method<B> m
    ) {
        return (exchange, request) -> invokeAndMapAsync(m, exchange, request);
    }

    private <B> BiFunction<McpTransportContext, McpSchema.CompleteRequest, McpSchema.CompleteResult> statelessSyncHandler(
        Method<B> m
    ) {
        return (ctx, request) -> invokeAndMap(m, ctx, request);
    }

    private <B> BiFunction<McpTransportContext, McpSchema.CompleteRequest, Mono<McpSchema.CompleteResult>> statelessAsyncHandler(
        Method<B> m
    ) {
        return (ctx, request) -> invokeAndMapAsync(m, ctx, request);
    }

    private <B> McpSchema.CompleteResult invokeAndMap(Method<B> m,
                                                      Object mcpTransportContext,
                                                      McpSchema.CompleteRequest request) {
        return observed(McpSchema.METHOD_COMPLETION_COMPLETE, completionName(request), () -> map(m, request, m.await(invoke(m, mcpTransportContext, request))));
    }

    private <B> Mono<McpSchema.CompleteResult> invokeAndMapAsync(Method<B> m,
                                                                 Object mcpTransportContext,
                                                                 McpSchema.CompleteRequest request) {
        return observedAsync(McpSchema.METHOD_COMPLETION_COMPLETE, completionName(request), () -> m.invokeAsync(() -> invoke(m, mcpTransportContext, request))
            .map(result -> map(m, request, result))
            .switchIfEmpty(Mono.fromSupplier(() -> map(m, request, null))));
    }

    private <B> @Nullable Object invoke(Method<B> m,
                                        Object mcpTransportContext,
                                        McpSchema.CompleteRequest request) {
        ExecutableMethod<B, Object> method = m.method();
        B bean = m.bean();

        ExecutableBinder<McpSchema.CompleteRequest> executableBinder = new DefaultExecutableBinder<>(
            m.preBound(mcpTransportContext, request, request.argument()));
        BoundExecutable executable = executableBinder.bind(method, argumentBinderRegistry, request);
        return executable.invoke(bean);
    }

    private <B> McpSchema.CompleteResult map(Method<B> m, McpSchema.CompleteRequest request, @Nullable Object result) {
        ExecutableMethod<B, Object> method = m.method();
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

    private static String completionName(McpSchema.CompleteRequest request) {
        if (request.ref() instanceof McpSchema.PromptReference prompt) {
            return prompt.name();
        }
        if (request.ref() instanceof McpSchema.ResourceReference resource) {
            return resource.uri();
        }
        return String.valueOf(request.ref());
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
