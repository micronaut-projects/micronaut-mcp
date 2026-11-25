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
package io.micronaut.mcp.server.stdio;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.core.annotation.Internal;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpServerTransportProvider;

import java.io.InputStream;
import java.io.OutputStream;

@Internal
@Factory
final class StdioServerTransportProviderFactory {
    @Prototype
    McpServerTransportProvider createStdioServerTransportProvider(McpJsonMapper mcpJsonMapper) {
        return stdioServerTransportProvider(mcpJsonMapper, System.in, System.out);
    }

    /**
     * Builds the stdio transport provider for the given streams. Tests use it with in-memory pipes so that they exercise the
     * same composition as the {@link System#in}/{@link System#out} bean.
     *
     * @param mcpJsonMapper JSON mapper
     * @param inputStream   the stream the server reads requests from
     * @param outputStream  the stream the server writes responses to
     * @return the stdio transport provider
     */
    static McpServerTransportProvider stdioServerTransportProvider(McpJsonMapper mcpJsonMapper,
                                                                   InputStream inputStream,
                                                                   OutputStream outputStream) {
        return new GracefulListingServerTransportProvider(
            new StdioServerTransportProvider(mcpJsonMapper, inputStream, outputStream));
    }
}
