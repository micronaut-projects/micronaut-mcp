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
package io.micronaut.mcp.server.stateless.sync;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.mcp.server.conf.McpServerInfoConfiguration;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpStatelessServerTransport;
import jakarta.inject.Singleton;

import java.util.List;

@Factory
@Internal
class McpStatelessSyncServerFactory {
    @SuppressWarnings({"java:S107"})
    @Prototype
    McpServer.StatelessSyncSpecification createMcpServerSyncSpecification(McpStatelessServerTransport mcpStatelessServerTransport,
                                                                          @Nullable McpServerInfoConfiguration mcpServerInfoConfiguration,
                                                                          McpSchema.ServerCapabilities mcpServerCapabilities,
                                                                          List<McpStatelessServerFeatures.SyncToolSpecification> tools,
                                                                          List<McpStatelessServerFeatures.SyncCompletionSpecification> completions,
                                                                          List<McpStatelessServerFeatures.SyncPromptSpecification> prompts,
                                                                          List<McpSchema.ResourceTemplate> resourceTemplates,
                                                                          List<McpStatelessServerFeatures.SyncResourceSpecification> resources) {
        McpServer.StatelessSyncSpecification spec = McpServer.sync(mcpStatelessServerTransport)
            .capabilities(mcpServerCapabilities);
        if (mcpServerInfoConfiguration != null) {
            spec.serverInfo(mcpServerInfoConfiguration.getName(), mcpServerInfoConfiguration.getVersion());
        }
        if (CollectionUtils.isNotEmpty(tools)) {
            spec.tools(tools);
        }
        if (CollectionUtils.isNotEmpty(completions)) {
            spec.completions(completions);
        }
        if (CollectionUtils.isNotEmpty(prompts)) {
            spec.prompts(prompts);
        }
        if (CollectionUtils.isNotEmpty(resourceTemplates)) {
            spec.resourceTemplates(resourceTemplates);
        }
        if (CollectionUtils.isNotEmpty(resources)) {
            spec.resources(resources);
        }
        return spec;
    }

    @Singleton
    McpStatelessSyncServer createMcpStatelessSyncServer(McpServer.StatelessSyncSpecification specification) {
        return specification.build();
    }
}
