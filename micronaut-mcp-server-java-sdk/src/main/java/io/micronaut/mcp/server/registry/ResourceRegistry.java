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
import io.micronaut.core.bind.BoundExecutable;
import io.micronaut.core.bind.DefaultExecutableBinder;
import io.micronaut.core.bind.ExecutableBinder;
import io.micronaut.inject.ExecutableMethod;
import org.jspecify.annotations.Nullable;
import io.micronaut.mcp.annotations.Resource;
import io.micronaut.mcp.conf.server.McpServerConfiguration;
import io.micronaut.mcp.server.exceptions.McpErrorExceptionMapper;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.BiFunction;

/**
 * The registry of {@link Resource}-annotated methods.
 * Produces MCP Resource specifications with handlers that invoke the annotated methods.
 */
@Requires(beans = McpServerConfiguration.class)
@Singleton
@Internal
public final class ResourceRegistry extends AbstractMcpMethodRegistry<
    McpServerFeatures.SyncResourceSpecification,
    McpServerFeatures.AsyncResourceSpecification,
    McpStatelessServerFeatures.SyncResourceSpecification,
    McpStatelessServerFeatures.AsyncResourceSpecification> {

    private static final Class<?>[] BOUND_PARAMETER_TYPES = {McpTransportContext.class, McpSchema.ReadResourceRequest.class};
    private final ArgumentBinderRegistry<McpSchema.ReadResourceRequest> argumentBinderRegistry;

    public ResourceRegistry(List<McpErrorExceptionMapper<? extends Throwable>> exceptionMappers,
                            ArgumentBinderRegistry<McpSchema.ReadResourceRequest> argumentBinderRegistry,
                            BeanContext beanContext) {
        super(exceptionMappers, beanContext);
        this.argumentBinderRegistry = argumentBinderRegistry;
    }

    @Override
    protected Class<?>[] boundParameterTypes() {
        return BOUND_PARAMETER_TYPES;
    }

    @Override
    public List<McpServerFeatures.SyncResourceSpecification> getSyncSpecs() {
        return drainMethods()
            .map(m -> new McpServerFeatures.SyncResourceSpecification(
                toResource(m.method()),
                syncHandler(m)
            ))
            .toList();
    }

    @Override
    public List<McpServerFeatures.AsyncResourceSpecification> getAsyncSpecs() {
        return drainMethods()
            .map(m -> new McpServerFeatures.AsyncResourceSpecification(
                toResource(m.method()),
                asyncHandler(m)
            ))
            .toList();
    }

    @Override
    public List<McpStatelessServerFeatures.SyncResourceSpecification> getStatelessSyncSpecs() {
        return drainMethods()
            .map(m -> new McpStatelessServerFeatures.SyncResourceSpecification(
                toResource(m.method()),
                statelessSyncHandler(m)
            ))
            .toList();
    }

    @Override
    public List<McpStatelessServerFeatures.AsyncResourceSpecification> getStatelessAsyncSpecs() {
        return drainMethods()
            .map(m -> new McpStatelessServerFeatures.AsyncResourceSpecification(
                toResource(m.method()),
                statelessAsyncHandler(m)
            ))
            .toList();
    }

    private <B> BiFunction<McpSyncServerExchange, McpSchema.ReadResourceRequest, McpSchema.ReadResourceResult> syncHandler(
        Method<B> m
    ) {
        return (exchange, request) -> invokeAndMap(m, exchange, request);
    }

    private <B> BiFunction<McpAsyncServerExchange, McpSchema.ReadResourceRequest, Mono<McpSchema.ReadResourceResult>> asyncHandler(
        Method<B> m
    ) {
        return (exchange, request) -> invokeAndMapAsync(m, exchange, request);
    }

    private <B> BiFunction<McpTransportContext, McpSchema.ReadResourceRequest, McpSchema.ReadResourceResult> statelessSyncHandler(
        Method<B> m
    ) {
        return (ctx, request) -> invokeAndMap(m, ctx, request);
    }

    private <B> BiFunction<McpTransportContext, McpSchema.ReadResourceRequest, Mono<McpSchema.ReadResourceResult>> statelessAsyncHandler(
        Method<B> m
    ) {
        return (ctx, request) -> invokeAndMapAsync(m, ctx, request);
    }

    private <B> McpSchema.ReadResourceResult invokeAndMap(Method<B> m,
                                                          Object mcpTransportContext,
                                                          McpSchema.ReadResourceRequest request) {
        return observed(McpSchema.METHOD_RESOURCES_READ, request.uri(), () -> map(m, request, m.await(invoke(m, mcpTransportContext, request))));
    }

    private <B> Mono<McpSchema.ReadResourceResult> invokeAndMapAsync(Method<B> m,
                                                                     Object mcpTransportContext,
                                                                     McpSchema.ReadResourceRequest request) {
        return observedAsync(McpSchema.METHOD_RESOURCES_READ, request.uri(), () -> m.invokeAsync(() -> invoke(m, mcpTransportContext, request))
            .map(result -> map(m, request, result))
            .switchIfEmpty(Mono.fromSupplier(() -> map(m, request, null))));
    }

    private <B> @Nullable Object invoke(Method<B> m,
                                        Object mcpTransportContext,
                                        McpSchema.ReadResourceRequest request) {
        ExecutableMethod<B, Object> method = m.method();
        B bean = m.bean();

        ExecutableBinder<McpSchema.ReadResourceRequest> executableBinder = new DefaultExecutableBinder<>(
            m.preBound(mcpTransportContext, request));
        BoundExecutable executable = executableBinder.bind(method, argumentBinderRegistry, request);
        return executable.invoke(bean);
    }

    private <B> McpSchema.ReadResourceResult map(Method<B> m, McpSchema.ReadResourceRequest request, @Nullable Object result) {
        ExecutableMethod<B, Object> method = m.method();
        if (result instanceof McpSchema.ReadResourceResult r) {
            return r;
        }
        if (result instanceof String s) {
            String mimeType = method.getAnnotation(Resource.class)
                .stringValue(MIME_TYPE_PROPERTY)
                .orElse(Resource.DEFAULT_MIME_TYPE);
            McpSchema.TextResourceContents contents = new McpSchema.TextResourceContents(request.uri(), mimeType, s);
            return new McpSchema.ReadResourceResult(List.of(contents));
        }
        // Unsupported return type: return empty contents
        return new McpSchema.ReadResourceResult(List.of());
    }

    @Override
    protected String primitiveName(ExecutableMethod<?, ?> method) {
        return method.stringValue(Resource.class, URI_PROPERTY).orElse(null);
    }

    private static <B> McpSchema.Resource toResource(ExecutableMethod<B, Object> method) {
        String uri = method.stringValue(Resource.class, URI_PROPERTY).orElseThrow();
        String name = method.stringValue(Resource.class, NAME_PROPERTY).orElse(Resource.ELEMENT_NAME);
        if (Resource.ELEMENT_NAME.equals(name)) {
            name = method.getName();
        }
        String title = method.stringValue(Resource.class, TITLE_PROPERTY).orElse(null);
        String description = method.stringValue(Resource.class, DESCRIPTION_PROPERTY).orElse(null);
        String mimeType = method.stringValue(Resource.class, MIME_TYPE_PROPERTY).orElse(Resource.DEFAULT_MIME_TYPE);
        long size = method.longValue(Resource.class, "size").orElse(-1);
        McpSchema.Annotations annotations = resourceAnnotations(method.getAnnotation(Resource.class));
        return new McpSchema.Resource(uri, name, title, description, mimeType, size < 0 ? null : size, annotations, meta(method), icons(method));
    }
}
