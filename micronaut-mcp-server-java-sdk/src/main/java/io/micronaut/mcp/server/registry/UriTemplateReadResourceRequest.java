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
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.http.uri.UriMatchInfo;
import io.micronaut.http.uri.UriMatchTemplate;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
     * @throws McpError An invalid params error when a variable value is not a valid percent-encoded value, or when the
     * value of a simple variable contains a {@code /} once decoded
     */
    static UriTemplateReadResourceRequest of(Template uriTemplate, McpSchema.ReadResourceRequest request) {
        return new UriTemplateReadResourceRequest(arguments(uriTemplate, request.uri()), request);
    }

    /**
     * variable values extracted by matching the `uriTemplate` against the supplied `uri`.
     * @param uriTemplate URI template
     * @param uri Uri
     * @return the variable values
     */
    static Map<String, Object> arguments(String uriTemplate, String uri) {
        return arguments(Template.compile(uriTemplate), uri);
    }

    private static Map<String, Object> arguments(Template uriTemplate, String uri) {
        Optional<UriMatchInfo> matchOptional = uriTemplate.matchTemplate().match(uri);
        if (matchOptional.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> variables = matchOptional.get().getVariableValues();
        if (variables.values().stream().noneMatch(value -> value instanceof String s && s.indexOf('%') >= 0)) {
            return variables;
        }
        Map<String, Object> decoded = CollectionUtils.newLinkedHashMap(variables.size());
        variables.forEach((name, value) -> decoded.put(name, value instanceof String s ? decode(uriTemplate, name, s) : value));
        return decoded;
    }

    /**
     * Percent-decodes a URI component. Unlike form decoding, a {@code +} stays a {@code +}. Matching happens before
     * decoding, so a simple variable, which matches a single segment, must not contain a {@code /} once decoded: it
     * would let {@code %2F} smuggle path segments, such as {@code ..%2F..%2Fetc}, into the value. The values of reserved
     * ({@code {+var}}) and fragment ({@code {#var}}) variables may contain {@code /} and are decoded too.
     */
    private static String decode(Template uriTemplate, String name, String value) {
        if (value.indexOf('%') < 0) {
            return value;
        }
        String decoded;
        try {
            decoded = URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw invalidParams("Invalid percent-encoding in the value of URI template variable " + name);
        }
        if (decoded.indexOf('/') >= 0 && !uriTemplate.reservedVariables().contains(name)) {
            throw invalidParams("The value of URI template variable " + name + " must not contain an encoded /");
        }
        return decoded;
    }

    private static McpError invalidParams(String message) {
        return McpError.builder(McpSchema.ErrorCodes.INVALID_PARAMS).message(message).build();
    }

    /**
     * A compiled URI template.
     *
     * @param matchTemplate The template the URIs are matched against
     * @param reservedVariables The names of the variables expanded with the reserved ({@code +}) or fragment ({@code #}) operator
     */
    record Template(UriMatchTemplate matchTemplate, Set<String> reservedVariables) {
        private static final Pattern RESERVED_EXPRESSION = Pattern.compile("\\{[+#]([^}]*)}");

        /**
         * @param uriTemplate The URI template
         * @return The compiled template
         */
        static Template compile(String uriTemplate) {
            Set<String> reserved = new HashSet<>();
            Matcher matcher = RESERVED_EXPRESSION.matcher(uriTemplate);
            while (matcher.find()) {
                for (String variable : matcher.group(1).split(",")) {
                    // Drops a prefix length or an explode modifier
                    reserved.add(variable.replaceAll("[:*].*$", "").trim());
                }
            }
            return new Template(UriMatchTemplate.of(uriTemplate), Set.copyOf(reserved));
        }
    }
}
