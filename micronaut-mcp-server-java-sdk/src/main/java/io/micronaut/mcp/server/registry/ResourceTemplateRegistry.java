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
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.bind.ArgumentBinderRegistry;
import io.micronaut.core.bind.BoundExecutable;
import io.micronaut.core.bind.DefaultExecutableBinder;
import io.micronaut.core.bind.ExecutableBinder;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.mcp.conf.server.McpServerConfiguration;
import io.micronaut.mcp.server.exceptions.McpErrorExceptionMapper;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;
import io.micronaut.mcp.annotations.ResourceTemplate;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.BiFunction;

/**
 * The registry of {@link ResourceTemplate}-annotated methods.
 * Produces MCP Resource Template specifications with handlers that invoke the annotated methods.
 */
@Requires(beans = McpServerConfiguration.class)
@Singleton
@Internal
public final class ResourceTemplateRegistry extends AbstractMcpMethodRegistry<
    McpServerFeatures.SyncResourceTemplateSpecification,
    McpServerFeatures.AsyncResourceTemplateSpecification,
    McpStatelessServerFeatures.SyncResourceTemplateSpecification,
    McpStatelessServerFeatures.AsyncResourceTemplateSpecification> {

    private final ArgumentBinderRegistry<UriTemplateReadResourceRequest> argumentBinderRegistry;

    ResourceTemplateRegistry(List<McpErrorExceptionMapper<? extends Throwable>> exceptionMappers,
                             BeanContext beanContext,
                             ArgumentBinderRegistry<UriTemplateReadResourceRequest> argumentBinderRegistry) {
        super(exceptionMappers, beanContext);
        this.argumentBinderRegistry = argumentBinderRegistry;
    }

    @Override
    public List<McpServerFeatures.SyncResourceTemplateSpecification> getSyncSpecs() {
        return drainMethods()
            .map(m -> new McpServerFeatures.SyncResourceTemplateSpecification(
                toResourceTemplate(m.method()),
                syncHandler(m.beanDefinition(), m.method())
            ))
            .toList();
    }

    @Override
    public List<McpServerFeatures.AsyncResourceTemplateSpecification> getAsyncSpecs() {
        return drainMethods()
            .map(m -> new McpServerFeatures.AsyncResourceTemplateSpecification(
                toResourceTemplate(m.method()),
                asyncHandler(m.beanDefinition(), m.method())
            ))
            .toList();
    }

    @Override
    public List<McpStatelessServerFeatures.SyncResourceTemplateSpecification> getStatelessSyncSpecs() {
        return drainMethods()
            .map(m -> new McpStatelessServerFeatures.SyncResourceTemplateSpecification(
                toResourceTemplate(m.method()),
                statelessSyncHandler(m.beanDefinition(), m.method())
            ))
            .toList();
    }

    @Override
    public List<McpStatelessServerFeatures.AsyncResourceTemplateSpecification> getStatelessAsyncSpecs() {
        return drainMethods()
            .map(m -> new McpStatelessServerFeatures.AsyncResourceTemplateSpecification(
                toResourceTemplate(m.method()),
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

    private static <B> McpSchema.ResourceTemplate toResourceTemplate(ExecutableMethod<B, Object> method) {
        String uri = method.stringValue(ResourceTemplate.class, URI_TEMPLATE_PROPERTY).orElseThrow();
        String name = method.stringValue(ResourceTemplate.class, NAME_PROPERTY).orElse(ResourceTemplate.ELEMENT_NAME);
        if (ResourceTemplate.ELEMENT_NAME.equals(name)) {
            name = method.getName();
        }
        String title = method.stringValue(ResourceTemplate.class, TITLE_PROPERTY).orElse(null);
        String description = method.stringValue(ResourceTemplate.class, DESCRIPTION_PROPERTY).orElse(null);
        String mimeType = method.stringValue(ResourceTemplate.class, MIME_TYPE_PROPERTY).orElse(ResourceTemplate.DEFAULT_MIME_TYPE);
        return new McpSchema.ResourceTemplate(uri, name, title, description, mimeType, null, null);
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

        ExecutableBinder<UriTemplateReadResourceRequest> executableBinder = new DefaultExecutableBinder<>(
            prepareBoundVariables(method, List.of(resolveMcpTransportContext(mcpTransportContext), request)));

        String uriTemplate = method.getAnnotation(ResourceTemplate.class)
            .stringValue(URI_TEMPLATE_PROPERTY)
            .orElseThrow(() -> new ConfigurationException("Missing required member uriTemplate in @ResourceTemplate annotation"));
        BoundExecutable executable = executableBinder.bind(method, argumentBinderRegistry, new UriTemplateReadResourceRequest(uriTemplate, request));
        Object result = executable.invoke(bean);
        if (result instanceof McpSchema.ReadResourceResult r) {
            return r;
        }
        if (result instanceof String s) {
            String mimeType = method.stringValue(ResourceTemplate.class, MIME_TYPE_PROPERTY)
                .orElse(ResourceTemplate.DEFAULT_MIME_TYPE);
            McpSchema.TextResourceContents contents = new McpSchema.TextResourceContents(request.uri(), mimeType, s);
            return new McpSchema.ReadResourceResult(List.of(contents));
        }
        // Unsupported return type: return empty contents
        return new McpSchema.ReadResourceResult(List.of());
    }
}
