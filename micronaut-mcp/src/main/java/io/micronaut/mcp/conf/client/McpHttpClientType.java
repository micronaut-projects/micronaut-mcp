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

/**
 * The HTTP client an MCP client sends its requests with.
 *
 * @since 2.2.0
 */
public enum McpHttpClientType {
    /**
     * The JDK HTTP client, used by the MCP client libraries.
     */
    JDK,
    /**
     * The Micronaut HTTP client, which applies the configuration of the {@code micronaut.http.services} entry named by
     * {@link McpClientHttpConfiguration#getServiceId()}, and the client filters of the application. Requires the
     * {@code micronaut-http-client} dependency.
     */
    MICRONAUT
}
