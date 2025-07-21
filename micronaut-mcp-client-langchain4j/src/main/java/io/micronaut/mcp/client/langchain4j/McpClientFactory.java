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
package io.micronaut.mcp.client.langchain4j;

import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.core.annotation.Internal;
import jakarta.inject.Singleton;

@Internal
@Factory
class McpClientFactory {
    @Prototype
    DefaultMcpClient.Builder crateMcpClientBuilder(McpTransport transport) {
        return new DefaultMcpClient.Builder()
            .transport(transport);
    }
    
    @Bean(preDestroy = "close")
    @Singleton
    McpClient createMcpClient(DefaultMcpClient.Builder builder) {
        return builder.build();
    }
}
