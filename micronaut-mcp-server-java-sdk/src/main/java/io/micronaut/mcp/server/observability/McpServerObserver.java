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
     * The error type of a tool call whose result is a tool execution error: the tool threw an exception that no
     * {@link io.micronaut.mcp.server.exceptions.McpErrorExceptionMapper} maps to a protocol error, or the arguments of the
     * call could not be bound or validated.
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
     * @param name The name of the tool or prompt; the URI of the resource; the URI template, as declared, of the resource
     * template (not the URI that was read); or, for {@code completion/complete}, the name of the prompt or the URI of the
     * resource being completed
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
         * @param errorType The type of the error that ended the operation, or {@code null} when it succeeded:
         * {@link #TOOL_ERROR}; the JSON-RPC error code, as a string, of the protocol error the operation ended with, so
         * {@code -32603} for an exception thrown by a prompt, resource or completion method unless an
         * {@link io.micronaut.mcp.server.exceptions.McpErrorExceptionMapper} maps it to another code; {@link #CANCELLED};
         * or the fully qualified class name of an {@link Error} the method threw, for example
         * {@code java.lang.StackOverflowError}. An {@link Error} that reaches the server through a returned publisher or
         * future, or that a method declaring {@code @ExecuteOn} throws on the synchronous server, is handled like an
         * exception.
         */
        void stop(@Nullable String errorType);
    }
}
