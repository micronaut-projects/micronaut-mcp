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
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link McpRequestContext} of a request received over STDIO, through the SDK server exchange, or over HTTP, through
 * the {@link McpNotificationEmitter} of the request.
 */
@Internal
public final class DefaultMcpRequestContext implements McpRequestContext {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultMcpRequestContext.class);
    private static final String UNSUPPORTED = "The stateless HTTP transport cannot send requests to the client, use the STDIO transport";

    private final McpTransportContext transportContext;
    private final @Nullable McpSyncServerExchange syncExchange;
    private final @Nullable McpAsyncServerExchange asyncExchange;
    private final @Nullable McpNotificationEmitter emitter;
    private final @Nullable Object progressToken;

    private DefaultMcpRequestContext(McpTransportContext transportContext,
                                     @Nullable McpSyncServerExchange syncExchange,
                                     @Nullable McpAsyncServerExchange asyncExchange,
                                     @Nullable McpNotificationEmitter emitter,
                                     @Nullable Object progressToken) {
        this.transportContext = transportContext;
        this.syncExchange = syncExchange;
        this.asyncExchange = asyncExchange;
        this.emitter = emitter;
        this.progressToken = progressToken;
    }

    /**
     * @param context The transport context, or the server exchange, the SDK invokes the method with
     * @param request The request
     * @return The request context
     */
    public static McpRequestContext of(@Nullable Object context, @Nullable Object request) {
        Object progressToken = request instanceof McpSchema.Request r ? r.progressToken() : null;
        if (context instanceof McpSyncServerExchange exchange) {
            return new DefaultMcpRequestContext(exchange.transportContext(), exchange, null, null, progressToken);
        }
        if (context instanceof McpAsyncServerExchange exchange) {
            return new DefaultMcpRequestContext(exchange.transportContext(), null, exchange, null, progressToken);
        }
        McpTransportContext transportContext = context instanceof McpTransportContext c ? c : McpTransportContext.EMPTY;
        McpNotificationEmitter emitter = transportContext instanceof HttpRequestMcpTransportContext http ? http.emitter() : null;
        return new DefaultMcpRequestContext(transportContext, null, null, emitter, progressToken);
    }

    @Override
    public McpTransportContext transportContext() {
        return transportContext;
    }

    @Override
    public @Nullable Object progressToken() {
        return progressToken;
    }

    @Override
    public void progress(double progress, @Nullable Double total, @Nullable String message) {
        if (progressToken == null) {
            return;
        }
        McpSchema.ProgressNotification notification = new McpSchema.ProgressNotification(progressToken, progress, total, message, null);
        if (syncExchange != null) {
            syncExchange.progressNotification(notification);
        } else if (asyncExchange != null) {
            asyncExchange.progressNotification(notification).subscribe(null, DefaultMcpRequestContext::notificationFailed);
        } else if (emitter != null) {
            emitter.emit(new McpSchema.JSONRPCNotification(McpSchema.JSONRPC_VERSION, McpSchema.METHOD_NOTIFICATION_PROGRESS, notification));
        }
    }

    @Override
    public void log(McpSchema.LoggingLevel level, @Nullable String logger, String message) {
        McpSchema.LoggingMessageNotification notification = new McpSchema.LoggingMessageNotification(level, logger, message, null);
        if (syncExchange != null) {
            syncExchange.loggingNotification(notification);
        } else if (asyncExchange != null) {
            asyncExchange.loggingNotification(notification).subscribe(null, DefaultMcpRequestContext::notificationFailed);
        } else if (emitter != null) {
            emitter.emit(new McpSchema.JSONRPCNotification(McpSchema.JSONRPC_VERSION, McpSchema.METHOD_NOTIFICATION_MESSAGE, notification));
        }
    }

    private static void notificationFailed(Throwable error) {
        if (LOG.isWarnEnabled()) {
            LOG.warn("Failed to send a notification to the client: {}", error.getMessage(), error);
        }
    }

    @Override
    public boolean isCancelled() {
        return emitter != null && emitter.isCancelled();
    }

    @Override
    public McpSchema.ElicitResult elicit(McpSchema.ElicitRequest request) {
        if (syncExchange != null) {
            return syncExchange.createElicitation(request);
        }
        if (asyncExchange != null) {
            return asyncExchange.createElicitation(request).block();
        }
        throw new UnsupportedOperationException(UNSUPPORTED);
    }

    @Override
    public McpSchema.CreateMessageResult sample(McpSchema.CreateMessageRequest request) {
        if (syncExchange != null) {
            return syncExchange.createMessage(request);
        }
        if (asyncExchange != null) {
            return asyncExchange.createMessage(request).block();
        }
        throw new UnsupportedOperationException(UNSUPPORTED);
    }
}
