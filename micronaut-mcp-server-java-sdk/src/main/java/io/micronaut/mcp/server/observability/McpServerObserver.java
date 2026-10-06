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

import org.jspecify.annotations.Nullable;

/**
 * Observes the operations of the MCP server: tool calls, prompt gets, resource reads and completions. Every bean of this
 * type is notified when an operation starts and when it ends.
 *
 * @since 2.2.0
 */
public interface McpServerObserver {

    /**
     * The error type of a tool call whose result is a tool execution error.
     */
    String TOOL_ERROR = "tool_error";

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
    interface Observation {

        /**
         * An observation that records nothing.
         */
        Observation NOOP = errorType -> { };

        /**
         * Called when the operation ends.
         *
         * @param errorType The type of the error that ended the operation, such as an exception class name or
         * {@link #TOOL_ERROR}, or {@code null} when it succeeded
         */
        void stop(@Nullable String errorType);
    }
}
