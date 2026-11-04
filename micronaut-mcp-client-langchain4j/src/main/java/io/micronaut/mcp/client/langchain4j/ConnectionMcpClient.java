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

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An {@link McpClient} that resolves the client bean of a connection when it is first used, so that a server that cannot
 * be reached fails the calls to its client, which the tool provider skips, instead of the creation of the tool provider.
 * The creation of the client is attempted again on the next call until it succeeds.
 */
@Internal
final class ConnectionMcpClient implements McpClient {
    private final String name;
    private final BeanContext beanContext;
    private final AtomicReference<McpClient> client = new AtomicReference<>();

    ConnectionMcpClient(String name, BeanContext beanContext) {
        this.name = name;
        this.beanContext = beanContext;
    }

    private McpClient client() {
        McpClient current = client.get();
        if (current == null) {
            current = beanContext.getBean(McpClient.class, Qualifiers.byName(name));
            client.compareAndSet(null, current);
        }
        return current;
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

    @Override
    public void close() {
        // The client bean is closed with the bean context, only forget it here
        client.set(null);
    }
}
