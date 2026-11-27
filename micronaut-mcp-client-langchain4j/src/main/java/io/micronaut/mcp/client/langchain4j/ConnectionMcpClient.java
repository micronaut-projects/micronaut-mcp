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
package io.micronaut.mcp.client.langchain4j;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.McpGetPromptResult;
import dev.langchain4j.mcp.client.McpPrompt;
import dev.langchain4j.mcp.client.McpReadResourceResult;
import dev.langchain4j.mcp.client.McpResource;
import dev.langchain4j.mcp.client.McpResourceTemplate;
import dev.langchain4j.mcp.client.McpRoot;
import dev.langchain4j.service.tool.ToolExecutionResult;
import io.micronaut.context.BeanContext;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.qualifiers.Qualifiers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An {@link McpClient} that resolves the client bean of a connection when it is first used, so that a server that cannot
 * be reached fails the calls to its client, which the tool providers skip, instead of the creation of the tool providers.
 * There is one per connection, shared by the tool providers and the health indicator.
 * <p>
 * Only one attempt to create the client runs at a time: the calls made while it runs fail at once with a
 * {@link McpClientConnectingException} instead of waiting for it, and a failed attempt is made again by the next call.
 * A failure is logged as a warning when the connection becomes unavailable, and its recovery when it becomes available
 * again.
 *
 * @since 2.2.0
 */
@Internal
public final class ConnectionMcpClient implements McpClient {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectionMcpClient.class);
    private final String name;
    private final BeanContext beanContext;
    private final AtomicReference<McpClient> client = new AtomicReference<>();
    private final AtomicBoolean connecting = new AtomicBoolean();
    private volatile boolean unavailable;

    ConnectionMcpClient(String name, BeanContext beanContext) {
        this.name = name;
        this.beanContext = beanContext;
    }

    // The failure is logged here, which names the connection, and rethrown for the tool provider, which skips the server
    @SuppressWarnings("java:S2139")
    private McpClient client() {
        McpClient current = client.get();
        if (current != null) {
            return current;
        }
        if (!connecting.compareAndSet(false, true)) {
            throw new McpClientConnectingException(name);
        }
        try {
            current = beanContext.getBean(McpClient.class, Qualifiers.byName(name));
            client.set(current);
            if (unavailable) {
                unavailable = false;
                LOG.info("The client of the MCP connection {} is created", name);
            }
            return current;
        } catch (RuntimeException e) {
            // Names the connection, which the errors of the tool provider do not
            if (unavailable) {
                LOG.debug("The client of the MCP connection {} could not be created again: {}", name, McpClientConnections.reason(e));
            } else {
                unavailable = true;
                LOG.warn("The client of the MCP connection {} could not be created, it is created again on the next call: {}",
                    name, McpClientConnections.reason(e));
            }
            // The full error only at debug level, as the remote server writes part of it
            LOG.debug("The error of the MCP connection {}", name, e);
            throw e;
        } finally {
            connecting.set(false);
        }
    }

    @Override
    public String key() {
        return name;
    }

    @Override
    public String instructions() {
        return client().instructions();
    }

    @Override
    public List<ToolSpecification> listTools() {
        return client().listTools();
    }

    @Override
    public List<ToolSpecification> listTools(InvocationContext invocationContext) {
        return client().listTools(invocationContext);
    }

    @Override
    public ToolExecutionResult executeTool(ToolExecutionRequest executionRequest) {
        return client().executeTool(executionRequest);
    }

    @Override
    public ToolExecutionResult executeTool(ToolExecutionRequest executionRequest, InvocationContext invocationContext) {
        return client().executeTool(executionRequest, invocationContext);
    }

    @Override
    public CompletableFuture<ToolExecutionResult> executeToolAsync(ToolExecutionRequest executionRequest, InvocationContext invocationContext) {
        return client().executeToolAsync(executionRequest, invocationContext);
    }

    @Override
    public List<McpResource> listResources() {
        return client().listResources();
    }

    @Override
    public List<McpResource> listResources(InvocationContext invocationContext) {
        return client().listResources(invocationContext);
    }

    @Override
    public List<McpResourceTemplate> listResourceTemplates() {
        return client().listResourceTemplates();
    }

    @Override
    public List<McpResourceTemplate> listResourceTemplates(InvocationContext invocationContext) {
        return client().listResourceTemplates(invocationContext);
    }

    @Override
    public McpReadResourceResult readResource(String uri) {
        return client().readResource(uri);
    }

    @Override
    public McpReadResourceResult readResource(String uri, InvocationContext invocationContext) {
        return client().readResource(uri, invocationContext);
    }

    @Override
    public void subscribeToResource(String uri) {
        client().subscribeToResource(uri);
    }

    @Override
    public void unsubscribeFromResource(String uri) {
        client().unsubscribeFromResource(uri);
    }

    @Override
    public long subscribeToResources(List<String> uris) {
        return client().subscribeToResources(uris);
    }

    @Override
    public void unsubscribeFromResources(long subscriptionId) {
        client().unsubscribeFromResources(subscriptionId);
    }

    @Override
    public List<McpPrompt> listPrompts() {
        return client().listPrompts();
    }

    @Override
    public McpGetPromptResult getPrompt(String name, Map<String, Object> arguments) {
        return client().getPrompt(name, arguments);
    }

    @Override
    public void checkHealth() {
        client().checkHealth();
    }

    @Override
    public void setRoots(List<McpRoot> roots) {
        client().setRoots(roots);
    }

    /**
     * Forgets the client of the connection without closing it, as the client bean is closed with the bean context. The
     * next call resolves the client bean again.
     */
    @Override
    public void close() {
        client.set(null);
    }
}
