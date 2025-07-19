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
package io.micronaut.mcp;

import io.micronaut.context.annotation.Primary;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.mcp.jsonrpc.ErrorCode;
import io.micronaut.mcp.jsonrpc.ErrorResponse;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.Map;

/**
 * A composite {@link McpRequestHandler} that delegates to other handlers based on the method name.
 * If no handler is found for a method, it returns an error response.
 */
@Internal
@Named(CompositeMcpRequestHandler.NAME)
@Primary
@Singleton
class CompositeMcpRequestHandler implements McpRequestHandler {
    public static final String NAME = "composite";
    private final Map<String, McpRequestHandler> handlers;

    public CompositeMcpRequestHandler(Map<String, McpRequestHandler> handlers) {
        this.handlers = handlers;
    }

    @Override
    @NonNull
    public McpResponse handle(@NonNull McpRequest request) {
        McpRequestHandler handler = handlers.get(request.message().method());
        if (handler != null) {
            return handler.handle(request);
        }
        return new ErrorMcpResponse(new ErrorResponse<>(ErrorCode.METHOD_NOT_FOUND));
    }

    @Override
    public @NonNull String getName() {
        return NAME;
    }
}
