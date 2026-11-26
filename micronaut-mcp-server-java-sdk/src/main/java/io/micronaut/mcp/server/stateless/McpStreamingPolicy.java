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
 * Decides whether the response to a request may become an event stream: the client accepts both JSON and an event
 * stream, as the Streamable HTTP transport requires, and the request designates a primitive that declares an
 * {@link io.micronaut.mcp.server.context.McpRequestContext}, so it may send notifications while it runs. Other requests
 * are answered with JSON without building a streaming response.
 */
@Singleton
@Internal
final class McpStreamingPolicy {
    private static final String KEY_METHOD = "method";
    private static final String KEY_PARAMS = "params";
    private static final String KEY_NAME = "name";
    private static final String KEY_URI = "uri";
    private static final String KEY_REF = "ref";
    private static final String KEY_TYPE = "type";

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
        return switch (method.getStringValue()) {
            case McpSchema.METHOD_TOOLS_CALL -> name(params, KEY_NAME) instanceof String name && toolRegistry.mayNotify(name)
                && acceptsJsonAndEventStream(request);
            case McpSchema.METHOD_PROMPT_GET -> name(params, KEY_NAME) instanceof String name && promptRegistry.mayNotify(name)
                && acceptsJsonAndEventStream(request);
            // Matching the URI templates costs more than parsing the Accept header, so it comes last
            case McpSchema.METHOD_RESOURCES_READ -> name(params, KEY_URI) instanceof String uri && acceptsJsonAndEventStream(request)
                && (resourceRegistry.mayNotify(uri) || resourceTemplateRegistry.mayNotify(uri));
            case McpSchema.METHOD_COMPLETION_COMPLETE -> completionMayNotify(params.get(KEY_REF)) && acceptsJsonAndEventStream(request);
            default -> false;
        };
    }

    private boolean completionMayNotify(@Nullable JsonNode ref) {
        if (ref == null || !ref.isObject()) {
            return false;
        }
        // A prompt reference designates the prompt by its name, a resource reference the resource by its URI
        String type = name(ref, KEY_TYPE);
        String name = null;
        if (McpSchema.PromptReference.TYPE.equals(type)) {
            name = name(ref, KEY_NAME);
        } else if (McpSchema.ResourceReference.TYPE.equals(type)) {
            name = name(ref, KEY_URI);
        }
        return name != null && completionRegistry.mayNotify(name);
    }

    private static @Nullable String name(JsonNode node, String key) {
        JsonNode value = node.get(key);
        return value != null && value.isString() ? value.getStringValue() : null;
    }

    /**
     * @param request The request
     * @return Whether the {@code Accept} header of the request lists both JSON and an event stream, with a quality other
     * than 0, as the Streamable HTTP transport requires of a POST request
     */
    static boolean acceptsJsonAndEventStream(HttpRequest<?> request) {
        return accepts(request, MediaType.APPLICATION_JSON) && accepts(request, MediaType.TEXT_EVENT_STREAM);
    }

    private static boolean accepts(HttpRequest<?> request, String mediaType) {
        for (String accept : request.getHeaders().getAll(HttpHeaders.ACCEPT)) {
            for (String range : accept.split(",")) {
                if (isAccepted(range, mediaType)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isAccepted(String range, String mediaType) {
        String[] parts = range.split(";");
        if (!parts[0].trim().equalsIgnoreCase(mediaType)) {
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
