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

import io.micronaut.core.annotation.Internal;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * Sends the notifications of an HTTP request to its client, on the stream the response is upgraded to.
 */
@Internal
public interface McpNotificationEmitter {

    /**
     * The request attribute the emitter of a request is stored in.
     */
    String ATTRIBUTE = McpNotificationEmitter.class.getName();

    /**
     * @param notification The notification
     */
    void emit(McpSchema.JSONRPCNotification notification);

    /**
     * @return Whether the client stopped waiting for the result
     */
    boolean isCancelled();
}
