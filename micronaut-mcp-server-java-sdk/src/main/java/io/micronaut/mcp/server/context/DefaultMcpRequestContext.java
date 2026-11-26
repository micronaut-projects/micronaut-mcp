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
import io.micronaut.http.context.ServerRequestContext;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * The {@link McpRequestContext} of a request received over STDIO, through the SDK server exchange, or over HTTP, through
 * the {@link McpNotificationEmitter} of the request.
 */
@Internal
public final class DefaultMcpRequestContext implements McpRequestContext {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultMcpRequestContext.class);
    /**
     * The longest progress token a request may send.
     */
    static final int MAX_PROGRESS_TOKEN_LENGTH = 256;
    private static final String ELICITATION = "Elicitation";
    private static final String SAMPLING = "Sampling";
    /**
     * Whether the current thread is invoking a method on the thread that received its request, so a request context
     * must not wait there for an answer of the client.
     */
    private static final ThreadLocal<Boolean> ON_RECEIVING_THREAD = new ThreadLocal<>();

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
        Object progressToken = progressToken(request);
        if (context instanceof McpSyncServerExchange exchange) {
            return new DefaultMcpRequestContext(exchange.transportContext(), exchange, null, null, progressToken);
        }
        if (context instanceof McpAsyncServerExchange exchange) {
            return new DefaultMcpRequestContext(exchange.transportContext(), null, exchange, null, progressToken);
        }
        McpTransportContext transportContext = context instanceof McpTransportContext c ? c : McpTransportContext.EMPTY;
        McpNotificationEmitter emitter = transportContext instanceof HttpRequestMcpTransportContext http ? http.emitter() : currentEmitter();
        return new DefaultMcpRequestContext(transportContext, null, null, emitter, progressToken);
    }

    /**
     * Each progress notification repeats the progress token, so a token the protocol does not allow, or a long one, is
     * ignored, as if the client had not asked for progress.
     *
     * @param request The request
     * @return The progress token of the request: a string of at most {@value #MAX_PROGRESS_TOKEN_LENGTH} characters, or an
     * integer
     */
    static @Nullable Object progressToken(@Nullable Object request) {
        Object token = request instanceof McpSchema.Request r ? r.progressToken() : null;
        if (token instanceof String text) {
            return text.length() <= MAX_PROGRESS_TOKEN_LENGTH ? text : null;
        }
        return token instanceof Integer || token instanceof Long ? token : null;
    }

    /**
     * @return The emitter of the HTTP request in scope, for a transport context that does not carry the request, such as
     * one from a replaced {@link io.modelcontextprotocol.server.McpTransportContextExtractor}
     */
    private static @Nullable McpNotificationEmitter currentEmitter() {
        return ServerRequestContext.currentRequest()
            .flatMap(request -> request.getAttribute(McpNotificationEmitter.ATTRIBUTE, McpNotificationEmitter.class))
            .orElse(null);
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
            if (LOG.isTraceEnabled()) {
                LOG.trace("Dropped a progress notification: the client sent no progress token");
            }
            return;
        }
        McpSchema.ProgressNotification notification = new McpSchema.ProgressNotification(progressToken, progress, total, message, null);
        if (syncExchange != null) {
            syncExchange.progressNotification(notification);
        } else if (asyncExchange != null) {
            asyncExchange.progressNotification(notification).subscribe(null, DefaultMcpRequestContext::notificationFailed);
        } else {
            emit(McpSchema.METHOD_NOTIFICATION_PROGRESS, notification);
        }
    }

    @Override
    public void log(McpSchema.LoggingLevel level, @Nullable String logger, String message) {
        McpSchema.LoggingMessageNotification notification = new McpSchema.LoggingMessageNotification(level, logger, message, null);
        if (syncExchange != null) {
            syncExchange.loggingNotification(notification);
        } else if (asyncExchange != null) {
            asyncExchange.loggingNotification(notification).subscribe(null, DefaultMcpRequestContext::notificationFailed);
        } else {
            emit(McpSchema.METHOD_NOTIFICATION_MESSAGE, notification);
        }
    }

    private void emit(String method, Object notification) {
        if (emitter != null) {
            emitter.emit(new McpSchema.JSONRPCNotification(McpSchema.JSONRPC_VERSION, method, notification));
        } else if (LOG.isTraceEnabled()) {
            LOG.trace("Dropped a {} notification: the response is sent as JSON, because the client does not accept text/event-stream", method);
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
            checkMayWait(ELICITATION);
            return asyncExchange.createElicitation(request).block();
        }
        throw unsupported(ELICITATION);
    }

    @Override
    public McpSchema.CreateMessageResult sample(McpSchema.CreateMessageRequest request) {
        if (syncExchange != null) {
            return syncExchange.createMessage(request);
        }
        if (asyncExchange != null) {
            checkMayWait(SAMPLING);
            return asyncExchange.createMessage(request).block();
        }
        throw unsupported(SAMPLING);
    }

    /**
     * Invokes a method on the thread that received its request: a reactive server without an executor for the method.
     * Its request context then fails rather than waits for an answer of the client, which this thread would read.
     *
     * @param invocation The invocation of the method
     * @param <T> The type of the result
     * @return The result of the invocation
     */
    public static <T> T onReceivingThread(Supplier<T> invocation) {
        Boolean previous = ON_RECEIVING_THREAD.get();
        ON_RECEIVING_THREAD.set(Boolean.TRUE);
        try {
            return invocation.get();
        } finally {
            if (previous == null) {
                ON_RECEIVING_THREAD.remove();
            } else {
                ON_RECEIVING_THREAD.set(previous);
            }
        }
    }

    private static void checkMayWait(String feature) {
        if (Boolean.TRUE.equals(ON_RECEIVING_THREAD.get())) {
            throw new IllegalStateException(feature + " waits for the answer of the client, which the thread that runs the method"
                + " reads on a reactive server: annotate the method with @ExecuteOn(TaskExecutors.BLOCKING)");
        }
    }

    private static UnsupportedOperationException unsupported(String feature) {
        return new UnsupportedOperationException(feature + " is not supported over the stateless HTTP transport");
    }
}
