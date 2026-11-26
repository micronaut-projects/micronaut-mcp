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
import io.micronaut.http.uri.UriMatchTemplate;
import io.micronaut.inject.ExecutableMethod;
import org.jspecify.annotations.Nullable;
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

    private static final Class<?>[] BOUND_PARAMETER_TYPES = {McpTransportContext.class, McpSchema.ReadResourceRequest.class};
    private final ArgumentBinderRegistry<UriTemplateReadResourceRequest> argumentBinderRegistry;

    ResourceTemplateRegistry(List<McpErrorExceptionMapper<? extends Throwable>> exceptionMappers,
                             BeanContext beanContext,
                             ArgumentBinderRegistry<UriTemplateReadResourceRequest> argumentBinderRegistry) {
        super(exceptionMappers, beanContext);
        this.argumentBinderRegistry = argumentBinderRegistry;
    }

    @Override
    protected Class<?>[] boundParameterTypes() {
        return BOUND_PARAMETER_TYPES;
    }

    @Override
    public List<McpServerFeatures.SyncResourceTemplateSpecification> getSyncSpecs() {
        return streamMethods()
            .map(m -> {
                McpSchema.ResourceTemplate resourceTemplate = toResourceTemplate(m.method());
                return new McpServerFeatures.SyncResourceTemplateSpecification(resourceTemplate,
                    syncHandler(m, UriMatchTemplate.of(resourceTemplate.uriTemplate())));
            })
            .toList();
    }

    @Override
    public List<McpServerFeatures.AsyncResourceTemplateSpecification> getAsyncSpecs() {
        return streamMethods()
            .map(m -> {
                McpSchema.ResourceTemplate resourceTemplate = toResourceTemplate(m.method());
                return new McpServerFeatures.AsyncResourceTemplateSpecification(resourceTemplate,
                    asyncHandler(m, UriMatchTemplate.of(resourceTemplate.uriTemplate())));
            })
            .toList();
    }

    @Override
    public List<McpStatelessServerFeatures.SyncResourceTemplateSpecification> getStatelessSyncSpecs() {
        return streamMethods()
            .map(m -> {
                McpSchema.ResourceTemplate resourceTemplate = toResourceTemplate(m.method());
                return new McpStatelessServerFeatures.SyncResourceTemplateSpecification(resourceTemplate,
                    statelessSyncHandler(m, UriMatchTemplate.of(resourceTemplate.uriTemplate())));
            })
            .toList();
    }

    @Override
    public List<McpStatelessServerFeatures.AsyncResourceTemplateSpecification> getStatelessAsyncSpecs() {
        return streamMethods()
            .map(m -> {
                McpSchema.ResourceTemplate resourceTemplate = toResourceTemplate(m.method());
                return new McpStatelessServerFeatures.AsyncResourceTemplateSpecification(resourceTemplate,
                    statelessAsyncHandler(m, UriMatchTemplate.of(resourceTemplate.uriTemplate())));
            })
            .toList();
    }

    private static <B> McpSchema.ResourceTemplate toResourceTemplate(ExecutableMethod<B, Object> method) {
        String uri = method.stringValue(ResourceTemplate.class, URI_TEMPLATE_PROPERTY)
            .orElseThrow(() -> new ConfigurationException("Missing required member uriTemplate in @ResourceTemplate annotation"));
        String name = method.stringValue(ResourceTemplate.class, NAME_PROPERTY).orElse(ResourceTemplate.ELEMENT_NAME);
        if (ResourceTemplate.ELEMENT_NAME.equals(name)) {
            name = method.getName();
        }
        String title = method.stringValue(ResourceTemplate.class, TITLE_PROPERTY).orElse(null);
        String description = method.stringValue(ResourceTemplate.class, DESCRIPTION_PROPERTY).orElse(null);
        String mimeType = method.stringValue(ResourceTemplate.class, MIME_TYPE_PROPERTY).orElse(ResourceTemplate.DEFAULT_MIME_TYPE);
        McpSchema.Annotations annotations = resourceAnnotations(method, ResourceTemplate.class);
        return new McpSchema.ResourceTemplate(uri, name, title, description, mimeType, annotations, meta(method), icons(method));
    }

    private <B> BiFunction<McpSyncServerExchange, McpSchema.ReadResourceRequest, McpSchema.ReadResourceResult> syncHandler(
        Method<B> m,
        UriMatchTemplate uriTemplate
    ) {
        return (exchange, request) -> invokeAndMap(m, uriTemplate, exchange, request);
    }

    private <B> BiFunction<McpAsyncServerExchange, McpSchema.ReadResourceRequest, Mono<McpSchema.ReadResourceResult>> asyncHandler(
        Method<B> m,
        UriMatchTemplate uriTemplate
    ) {
        return (exchange, request) -> invokeAndMapAsync(m, uriTemplate, exchange, request);
    }

    private <B> BiFunction<McpTransportContext, McpSchema.ReadResourceRequest, McpSchema.ReadResourceResult> statelessSyncHandler(
        Method<B> m,
        UriMatchTemplate uriTemplate
    ) {
        return (ctx, request) -> invokeAndMap(m, uriTemplate, ctx, request);
    }

    private <B> BiFunction<McpTransportContext, McpSchema.ReadResourceRequest, Mono<McpSchema.ReadResourceResult>> statelessAsyncHandler(
        Method<B> m,
        UriMatchTemplate uriTemplate
    ) {
        return (ctx, request) -> invokeAndMapAsync(m, uriTemplate, ctx, request);
    }

    private <B> McpSchema.ReadResourceResult invokeAndMap(Method<B> m,
                                                          UriMatchTemplate uriTemplate,
                                                          Object mcpTransportContext,
                                                          McpSchema.ReadResourceRequest request) {
        return m.call(() -> m.invoke(argumentBinderRegistry, UriTemplateReadResourceRequest.of(uriTemplate, request), mcpTransportContext, request),
            mcpTransportContext, result -> map(m, request, result), e -> failWithMcpError(m, e));
    }

    private <B> Mono<McpSchema.ReadResourceResult> invokeAndMapAsync(Method<B> m,
                                                                     UriMatchTemplate uriTemplate,
                                                                     Object mcpTransportContext,
                                                                     McpSchema.ReadResourceRequest request) {
        return m.callAsync(() -> m.invoke(argumentBinderRegistry, UriTemplateReadResourceRequest.of(uriTemplate, request), mcpTransportContext, request),
            result -> map(m, request, result), e -> failWithMcpError(m, e));
    }

    private <B> McpSchema.ReadResourceResult map(Method<B> m, McpSchema.ReadResourceRequest request, @Nullable Object result) {
        ExecutableMethod<B, Object> method = m.method();
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
