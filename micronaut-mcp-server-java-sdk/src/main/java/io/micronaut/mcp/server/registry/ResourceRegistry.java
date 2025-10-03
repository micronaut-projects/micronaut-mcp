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
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
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

    private final ArgumentBinderRegistry<McpSchema.ReadResourceRequest> argumentBinderRegistry;

    public ResourceRegistry(List<McpErrorExceptionMapper<? extends Throwable>> exceptionMappers,
                            ArgumentBinderRegistry<McpSchema.ReadResourceRequest> argumentBinderRegistry,
                            BeanContext beanContext) {
        super(exceptionMappers, beanContext);
        this.argumentBinderRegistry = argumentBinderRegistry;
    }

    @Override
    public List<McpServerFeatures.SyncResourceSpecification> getSyncSpecs() {
        return drainMethods()
            .map(m -> new McpServerFeatures.SyncResourceSpecification(
                toResource(m.method()),
                syncHandler(m.beanDefinition(), m.method())
            ))
            .toList();
    }

    @Override
    public List<McpServerFeatures.AsyncResourceSpecification> getAsyncSpecs() {
        return drainMethods()
            .map(m -> new McpServerFeatures.AsyncResourceSpecification(
                toResource(m.method()),
                asyncHandler(m.beanDefinition(), m.method())
            ))
            .toList();
    }

    @Override
    public List<McpStatelessServerFeatures.SyncResourceSpecification> getStatelessSyncSpecs() {
        return drainMethods()
            .map(m -> new McpStatelessServerFeatures.SyncResourceSpecification(
                toResource(m.method()),
                statelessSyncHandler(m.beanDefinition(), m.method())
            ))
            .toList();
    }

    @Override
    public List<McpStatelessServerFeatures.AsyncResourceSpecification> getStatelessAsyncSpecs() {
        return drainMethods()
            .map(m -> new McpStatelessServerFeatures.AsyncResourceSpecification(
                toResource(m.method()),
                statelessAsyncHandler(m.beanDefinition(), m.method())
            ))
            .toList();
    }

    private <B> BiFunction<McpSyncServerExchange, McpSchema.ReadResourceRequest, McpSchema.ReadResourceResult> syncHandler(
        BeanDefinition<B> beanDefinition,
        ExecutableMethod<B, Object> method
    ) {
        return (exchange, request) -> invokeAndMap(beanDefinition, method, exchange, request);
    }

    private <B> BiFunction<McpAsyncServerExchange, McpSchema.ReadResourceRequest, Mono<McpSchema.ReadResourceResult>> asyncHandler(
        BeanDefinition<B> beanDefinition,
        ExecutableMethod<B, Object> method
    ) {
        return (exchange, request) -> Mono.just(invokeAndMap(beanDefinition, method, exchange, request));
    }

    private <B> BiFunction<McpTransportContext, McpSchema.ReadResourceRequest, McpSchema.ReadResourceResult> statelessSyncHandler(
        BeanDefinition<B> beanDefinition,
        ExecutableMethod<B, Object> method
    ) {
        return (ctx, request) -> invokeAndMap(beanDefinition, method, ctx, request);
    }

    private <B> BiFunction<McpTransportContext, McpSchema.ReadResourceRequest, Mono<McpSchema.ReadResourceResult>> statelessAsyncHandler(
        BeanDefinition<B> beanDefinition,
        ExecutableMethod<B, Object> method
    ) {
        return (ctx, request) -> Mono.just(invokeAndMap(beanDefinition, method, ctx, request));
    }

    private <B> McpSchema.ReadResourceResult invokeAndMap(BeanDefinition<B> beanDefinition,
                                                          ExecutableMethod<B, Object> method,
                                                          Object mcpTransportContext,
                                                          McpSchema.ReadResourceRequest request) {
        B bean = beanContext.getBean(beanDefinition);

        ExecutableBinder<McpSchema.ReadResourceRequest> executableBinder = new DefaultExecutableBinder<>(
            prepareBoundVariables(method, List.of(resolveMcpTransportContext(mcpTransportContext), request)));
        BoundExecutable executable = executableBinder.bind(method, argumentBinderRegistry, request);
        Object result = executable.invoke(bean);
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

    private static <B> McpSchema.Resource toResource(ExecutableMethod<B, Object> method) {
        String uri = method.stringValue(Resource.class, URI_PROPERTY).orElseThrow();
        String name = method.stringValue(Resource.class, NAME_PROPERTY).orElse(Resource.ELEMENT_NAME);
        if (Resource.ELEMENT_NAME.equals(name)) {
            name = method.getName();
        }
        String title = method.stringValue(Resource.class, TITLE_PROPERTY).orElse(null);
        String description = method.stringValue(Resource.class, DESCRIPTION_PROPERTY).orElse(null);
        String mimeType = method.stringValue(Resource.class, MIME_TYPE_PROPERTY).orElse(Resource.DEFAULT_MIME_TYPE);
        // size, attributes, and other optional fields are left null for declarative resources
        return new McpSchema.Resource(uri, name, title, description, mimeType, null, null, null);
    }

    @Override
    public boolean isNotEmpty() {
        return !getSyncSpecs().isEmpty()
            || !getAsyncSpecs().isEmpty()
            || !getStatelessSyncSpecs().isEmpty()
            || !getStatelessAsyncSpecs().isEmpty();
    }
}
