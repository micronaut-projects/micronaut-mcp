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
package io.micronaut.mcp.server.exceptions;

import io.modelcontextprotocol.spec.McpError;

/**
 * API to define mappers from exceptions to {@link McpError}, the JSON-RPC error sent to the client.
 * <p>
 * For a tool, an exception that a mapper maps is answered with the protocol error the mapper returns. The model does not
 * see a protocol error, whereas it sees the message of an exception that no mapper maps, which is answered with a tool
 * execution error, a result whose {@code isError} is {@code true}. The built-in mappers of arguments that cannot be bound
 * or are not valid do not follow this rule: the message of their error is sent as a tool execution error.
 *
 * @param <T> Exception to Map
 */
public interface McpErrorExceptionMapper<T extends Throwable> {

    /**
     *
     * @param clazz Exception class
     * @return Whether the mapper can map the exception
     */
    boolean canMap(Class<? extends Throwable> clazz);

    /**
     * Map the given exception to a {@link McpError}.
     *
     * @param exception The exception to map
     * @return an Mcp Error
     */
    McpError map(T exception);
}
