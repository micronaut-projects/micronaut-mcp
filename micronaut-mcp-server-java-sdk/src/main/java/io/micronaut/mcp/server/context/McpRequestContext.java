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
 * progress, send log messages to the client, or check whether the client disconnected.
 *
 * <pre>
 * &#64;Tool
 * String importCatalog(String url, int pages, McpRequestContext context) {
 *     for (int i = 0; i &lt; pages; i++) {
 *         importPage(url, i);
 *         context.progress(i + 1, pages, "Imported page " + (i + 1));
 *     }
 *     return "done";
 * }
 * </pre>
 *
 * <p>Over the HTTP transport a request whose client accepts both {@code application/json} and {@code text/event-stream}
 * is answered with a stream as soon as the method sends a notification, and the notifications and the result are sent
 * as events. Without notifications, or when the client does not accept both, the result is sent as JSON and
 * notifications are dropped.</p>
 *
 * @since 2.2.0
 */
public interface McpRequestContext {

    /**
     * @return The transport context of the request
     */
    McpTransportContext transportContext();

    /**
     * The progress token the client sent with the request: a string of at most 256 characters, or an integer, as the
     * protocol allows. A token of another type, or a longer one, is ignored.
     *
     * @return The progress token, or {@code null} if the client did not ask for progress
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
     * @param total The total progress
     * @param message A human-readable description of the progress
     */
    default void progress(double progress, double total, @Nullable String message) {
        progress(progress, Double.valueOf(total), message);
    }

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
     * @return Whether the server observed the client disconnect from this HTTP response stream.
     * A disconnect stops event delivery and the invocation continues. This returns {@code false} for
     * STDIO, plain JSON responses and before a disconnect is observed.
     */
    boolean isClientDisconnected();

    /**
     * Asks the user for information through the client. Only the STDIO transport can send requests to the client.
     * The method waits for the answer, so on a reactive server, which invokes a method without an executor on the thread
     * that reads the messages of the client, it must run on a blocking executor, for example with
     * {@code @ExecuteOn(TaskExecutors.BLOCKING)}; otherwise it fails with an {@link IllegalStateException}.
     *
     * <pre>
     * &#64;Tool
     * &#64;ExecuteOn(TaskExecutors.BLOCKING)
     * String greet(McpRequestContext context) {
     *     McpSchema.ElicitResult answer = context.elicit(McpSchema.ElicitRequest.builder()
     *         .message("What is your name?")
     *         .requestedSchema(Map.of("type", "object", "properties", Map.of("name", Map.of("type", "string"))))
     *         .build());
     *     return answer.action() == McpSchema.ElicitResult.Action.ACCEPT ? "Hello " + answer.content().get("name") : "Hello";
     * }
     * </pre>
     *
     * @param request The elicitation request
     * @return The user's answer
     * @throws UnsupportedOperationException over the stateless HTTP transport
     * @throws IllegalStateException on a reactive server, in a method without an executor
     * @see <a href="https://modelcontextprotocol.io/specification/2025-11-25/client/elicitation">Elicitation</a>
     */
    McpSchema.ElicitResult elicit(McpSchema.ElicitRequest request);

    /**
     * Asks the client to sample a language model. Only the STDIO transport can send requests to the client.
     * The method waits for the answer, so on a reactive server, which invokes a method without an executor on the thread
     * that reads the messages of the client, it must run on a blocking executor, for example with
     * {@code @ExecuteOn(TaskExecutors.BLOCKING)}; otherwise it fails with an {@link IllegalStateException}.
     *
     * @param request The sampling request
     * @return The model's answer
     * @throws UnsupportedOperationException over the stateless HTTP transport
     * @throws IllegalStateException on a reactive server, in a method without an executor
     * @see <a href="https://modelcontextprotocol.io/specification/2025-11-25/client/sampling">Sampling</a>
     */
    McpSchema.CreateMessageResult sample(McpSchema.CreateMessageRequest request);
}
