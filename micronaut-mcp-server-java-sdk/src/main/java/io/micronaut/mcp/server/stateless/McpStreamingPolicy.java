/*
 * Copyright 2017-2026 original authors
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
package io.micronaut.mcp.server.stateless;

import io.micronaut.core.annotation.Internal;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.json.tree.JsonNode;
import io.micronaut.mcp.server.registry.CompletionRegistry;
import io.micronaut.mcp.server.registry.PromptRegistry;
import io.micronaut.mcp.server.registry.ResourceRegistry;
import io.micronaut.mcp.server.registry.ResourceTemplateRegistry;
import io.micronaut.mcp.server.registry.ToolRegistry;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * Decides whether the response to a request may become an event stream: the client accepts one, and the request
 * designates a primitive that declares an {@link io.micronaut.mcp.server.context.McpRequestContext}, so it may send
 * notifications while it runs. Other requests are answered with JSON without building a streaming response.
 */
@Singleton
@Internal
final class McpStreamingPolicy {
    private static final String KEY_METHOD = "method";
    private static final String KEY_PARAMS = "params";
    private static final String KEY_NAME = "name";
    private static final String KEY_URI = "uri";
    private static final String KEY_REF = "ref";

    private final ToolRegistry toolRegistry;
    private final PromptRegistry promptRegistry;
    private final ResourceRegistry resourceRegistry;
    private final ResourceTemplateRegistry resourceTemplateRegistry;
    private final CompletionRegistry completionRegistry;

    McpStreamingPolicy(ToolRegistry toolRegistry,
                       PromptRegistry promptRegistry,
                       ResourceRegistry resourceRegistry,
                       ResourceTemplateRegistry resourceTemplateRegistry,
                       CompletionRegistry completionRegistry) {
        this.toolRegistry = toolRegistry;
        this.promptRegistry = promptRegistry;
        this.resourceRegistry = resourceRegistry;
        this.resourceTemplateRegistry = resourceTemplateRegistry;
        this.completionRegistry = completionRegistry;
    }

    /**
     * @param request The request
     * @param body The body of the request
     * @return Whether the response may become an event stream
     */
    boolean mayStream(HttpRequest<?> request, @Nullable JsonNode body) {
        if (body == null || !body.isObject()) {
            return false;
        }
        JsonNode method = body.get(KEY_METHOD);
        JsonNode params = body.get(KEY_PARAMS);
        if (method == null || !method.isString() || params == null || !params.isObject()) {
            return false;
        }
        boolean mayNotify = switch (method.getStringValue()) {
            case McpSchema.METHOD_TOOLS_CALL -> name(params, KEY_NAME) instanceof String name && toolRegistry.mayNotify(name);
            case McpSchema.METHOD_PROMPT_GET -> name(params, KEY_NAME) instanceof String name && promptRegistry.mayNotify(name);
            case McpSchema.METHOD_RESOURCES_READ -> name(params, KEY_URI) instanceof String uri
                && (resourceRegistry.mayNotify(uri) || resourceTemplateRegistry.mayNotifyUri(uri));
            case McpSchema.METHOD_COMPLETION_COMPLETE -> completionMayNotify(params.get(KEY_REF));
            default -> false;
        };
        return mayNotify && acceptsEventStream(request);
    }

    private boolean completionMayNotify(@Nullable JsonNode ref) {
        if (ref == null || !ref.isObject()) {
            return false;
        }
        String name = name(ref, KEY_NAME);
        return completionRegistry.mayNotify(name != null ? name : String.valueOf(name(ref, KEY_URI)));
    }

    private static @Nullable String name(JsonNode node, String key) {
        JsonNode value = node.get(key);
        return value != null && value.isString() ? value.getStringValue() : null;
    }

    /**
     * @param request The request
     * @return Whether the {@code Accept} header of the request accepts an event stream, with a quality other than 0
     */
    static boolean acceptsEventStream(HttpRequest<?> request) {
        for (String accept : request.getHeaders().getAll(HttpHeaders.ACCEPT)) {
            for (String range : accept.split(",")) {
                if (isEventStream(range)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isEventStream(String range) {
        String[] parts = range.split(";");
        if (!parts[0].trim().equalsIgnoreCase(MediaType.TEXT_EVENT_STREAM)) {
            return false;
        }
        for (int i = 1; i < parts.length; i++) {
            String parameter = parts[i].trim().toLowerCase(Locale.ROOT);
            if (parameter.startsWith("q=")) {
                try {
                    return Double.parseDouble(parameter.substring(2)) > 0;
                } catch (NumberFormatException _) {
                    return false;
                }
            }
        }
        return true;
    }
}
