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

import io.modelcontextprotocol.spec.McpSchema;

/**
 * Answers the elicitation requests of MCP servers, which ask the user for information through the client. Declare a bean of this type to advertise the elicitation capability.
 *
 * @since 2.2.0
 */
@FunctionalInterface
public interface McpElicitationHandler {

    /**
     * @param client The name of the connection to the server
     * @param request The elicitation request
     * @return the user's answer
     */
    McpSchema.ElicitResult elicit(String client, McpSchema.ElicitFormRequest request);
}
