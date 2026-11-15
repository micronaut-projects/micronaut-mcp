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
package io.micronaut.mcp.server.registry;

import io.micronaut.core.annotation.Internal;
import io.micronaut.http.uri.UriMatchInfo;
import io.micronaut.http.uri.UriMatchTemplate;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Java Record to encapsulate the variables matched by a URI template and the Read Resource Request.
 * @param arguments the variable values extracted by matching the URI template against the {@link McpSchema.ReadResourceRequest#uri()}
 * @param request Read Resource Request
 */
@Internal
record UriTemplateReadResourceRequest(
    Map<String, Object> arguments,
    McpSchema.ReadResourceRequest request
) {
    /**
     * Matches the request URI once against an already compiled URI template.
     * @param uriTemplate URI template
     * @param request Read Resource Request
     * @return the request with the matched variables
     */
    static UriTemplateReadResourceRequest of(UriMatchTemplate uriTemplate, McpSchema.ReadResourceRequest request) {
        return new UriTemplateReadResourceRequest(arguments(uriTemplate, request.uri()), request);
    }

    /**
     * variable values extracted by matching the `uriTemplate` against the supplied `uri`.
     * @param uriTemplate URI template
     * @param uri Uri
     * @return the variable values
     */
    static Map<String, Object> arguments(String uriTemplate, String uri) {
        return arguments(UriMatchTemplate.of(uriTemplate), uri);
    }

    private static Map<String, Object> arguments(UriMatchTemplate uriMatchTemplate, String uri) {
        Optional<UriMatchInfo> matchOptional = uriMatchTemplate.match(uri);
        if (matchOptional.isEmpty()) {
            return Collections.emptyMap();
        }
        UriMatchInfo match = matchOptional.get();
        return match.getVariableValues();
    }
}
