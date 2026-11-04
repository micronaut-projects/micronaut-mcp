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
package io.micronaut.mcp.conf.client;

import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;

/**
 * The configuration of a connection to an MCP server started as a process, which the client talks to over its standard
 * input and output.
 *
 * @since 2.2.0
 */
public interface McpClientStdioConfiguration extends McpClientConnectionConfiguration {
    /**
     * The prefix of the STDIO connections.
     */
    String PREFIX = McpClientConfiguration.PREFIX + ".stdio";

    /**
     * @return The command that starts the server, and its arguments
     */
    @NonNull
    List<String> getCommand();

    /**
     * @return The environment variables of the server process, in addition to those of this process
     */
    @NonNull
    Map<String, String> getEnvironment();

    /**
     * @return Whether to log the messages exchanged with the server
     */
    boolean isLogEvents();
}
