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
package io.micronaut.mcp.server.context;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.spec.McpSchema;
import org.jspecify.annotations.Nullable;

/**
 * The request a tool, prompt, resource or completion method is invoked for. Declare a parameter of this type to report
 * progress, send log messages to the client, or check whether the request was cancelled.
 *
 * <pre>
 * &#64;Tool
 * String importCatalog(String url, McpRequestContext context) {
 *     for (int i = 0; i &lt; pages; i++) {
 *         importPage(url, i);
 *         context.progress(i + 1, pages, "Imported page " + (i + 1));
 *     }
 *     return "done";
 * }
 * </pre>
 *
 * <p>Over the HTTP transport a request whose client accepts {@code text/event-stream} is answered with a stream as soon
 * as the method sends a notification, and the notifications and the result are sent as events. Without notifications,
 * or when the client only accepts JSON, the result is sent as JSON and notifications are dropped.</p>
 *
 * @since 2.2.0
 */
public interface McpRequestContext {

    /**
     * @return The transport context of the request
     */
    McpTransportContext transportContext();

    /**
     * @return The progress token the client sent with the request, or {@code null} if it did not ask for progress
     */
    @Nullable
    Object progressToken();

    /**
     * Reports progress, if the client asked for it with a progress token.
     *
     * @param progress The progress so far, which must increase with each call
     * @param total The total progress, if known
     * @param message A human-readable description of the progress
     * @see <a href="https://modelcontextprotocol.io/specification/2025-11-25/basic/utilities/progress">Progress</a>
     */
    void progress(double progress, @Nullable Double total, @Nullable String message);

    /**
     * Reports progress, if the client asked for it with a progress token.
     *
     * @param progress The progress so far, which must increase with each call
     */
    default void progress(double progress) {
        progress(progress, null, null);
    }

    /**
     * Sends a log message to the client.
     *
     * @param level The level
     * @param logger The name of the logger, or {@code null}
     * @param message The message
     * @see <a href="https://modelcontextprotocol.io/specification/2025-11-25/server/utilities/logging">Logging</a>
     */
    void log(McpSchema.LoggingLevel level, @Nullable String logger, String message);

    /**
     * Sends a log message to the client.
     *
     * @param level The level
     * @param message The message
     */
    default void log(McpSchema.LoggingLevel level, String message) {
        log(level, null, message);
    }

    /**
     * @return Whether the client stopped waiting for the result, for example by closing the stream of an HTTP request
     */
    boolean isCancelled();

    /**
     * Asks the user for information through the client. Only the STDIO transport can send requests to the client.
     *
     * @param request The elicitation request
     * @return The user's answer
     * @throws UnsupportedOperationException over the stateless HTTP transport
     * @see <a href="https://modelcontextprotocol.io/specification/2025-11-25/client/elicitation">Elicitation</a>
     */
    McpSchema.ElicitResult elicit(McpSchema.ElicitRequest request);

    /**
     * Asks the client to sample a language model. Only the STDIO transport can send requests to the client.
     *
     * @param request The sampling request
     * @return The model's answer
     * @throws UnsupportedOperationException over the stateless HTTP transport
     * @see <a href="https://modelcontextprotocol.io/specification/2025-11-25/client/sampling">Sampling</a>
     */
    McpSchema.CreateMessageResult sample(McpSchema.CreateMessageRequest request);
}
