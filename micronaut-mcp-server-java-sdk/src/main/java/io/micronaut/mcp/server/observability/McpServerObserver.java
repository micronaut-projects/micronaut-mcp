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
package io.micronaut.mcp.server.observability;

import io.micronaut.core.annotation.Experimental;
import org.jspecify.annotations.Nullable;

/**
 * Observes the operations of the MCP server: calls of annotated tools, gets of annotated prompts, reads of annotated
 * resources and resource templates, and annotated completions. Every bean of this type is notified when an operation
 * starts and when it ends. Primitives added at runtime through the MCP Java SDK server, and requests the SDK rejects
 * before dispatching them, are not observed.
 *
 * <p>This interface is experimental: it may change, for example to give observers the context of the request.</p>
 *
 * @since 2.2.0
 */
@Experimental
public interface McpServerObserver {

    /**
     * The error type of a tool call whose result is a tool execution error.
     */
    String TOOL_ERROR = "tool_error";

    /**
     * The error type of an operation on the reactive server whose result was cancelled before it produced a value, for
     * example because the client went away.
     */
    String CANCELLED = "cancelled";

    /**
     * Called when an operation starts.
     *
     * @param method The JSON-RPC method, such as {@code tools/call}
     * @param name The name of the tool or prompt, or the URI of the resource
     * @return The observation of the operation, stopped when it ends
     */
    Observation start(String method, String name);

    /**
     * The observation of an operation.
     */
    @FunctionalInterface
    interface Observation {

        /**
         * An observation that records nothing.
         */
        Observation NOOP = errorType -> { };

        /**
         * Called when the operation ends.
         *
         * @param errorType The type of the error that ended the operation, or {@code null} when it succeeded: the
         * JSON-RPC error code of a protocol error (for example {@code -32603}), {@link #TOOL_ERROR},
         * {@link #CANCELLED}, or the class name of any other exception
         */
        void stop(@Nullable String errorType);
    }
}
