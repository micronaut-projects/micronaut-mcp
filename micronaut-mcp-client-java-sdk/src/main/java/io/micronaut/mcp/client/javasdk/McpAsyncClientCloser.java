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
package io.micronaut.mcp.client.javasdk;

import io.micronaut.context.event.BeanPreDestroyEvent;
import io.micronaut.context.event.BeanPreDestroyEventListener;
import io.micronaut.core.annotation.Internal;
import io.modelcontextprotocol.client.McpAsyncClient;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

/**
 * Closes the asynchronous MCP clients gracefully when the application stops, waiting a bounded time for the transport,
 * for example the server process over STDIO, to close.
 */
@Singleton
@Internal
final class McpAsyncClientCloser implements BeanPreDestroyEventListener<McpAsyncClient> {
    private static final Logger LOG = LoggerFactory.getLogger(McpAsyncClientCloser.class);
    /**
     * As the synchronous client.
     */
    private static final Duration CLOSE_TIMEOUT = Duration.ofSeconds(10);

    @Override
    public @NonNull McpAsyncClient onPreDestroy(@NonNull BeanPreDestroyEvent<McpAsyncClient> event) {
        McpAsyncClient client = event.getBean();
        close(client);
        return client;
    }

    /**
     * Closes a client gracefully, or immediately when it does not close in time.
     *
     * @param client The client
     */
    static void close(McpAsyncClient client) {
        if (Schedulers.isInNonBlockingThread()) {
            client.close();
            return;
        }
        try {
            client.closeGracefully().block(CLOSE_TIMEOUT);
        } catch (RuntimeException e) {
            LOG.debug("The MCP client did not close gracefully, closing it", e);
            client.close();
        }
    }
}
