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
 * A tool sends the protocol error returned by an application mapper to the client, which may pass its
 * code, message and data to the model. Use client-safe details in mapped errors.
 * Built-in argument mappers report validation and binding errors as tool results with {@code isError} set to true.
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
