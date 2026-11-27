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
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
     * @throws McpError An invalid params error when the URI does not match the template, when a variable value is not a
     * valid percent-encoded UTF-8 value or contains an unpaired surrogate, or when the value of a simple variable
     * contains a {@code /} once decoded
     */
    static UriTemplateReadResourceRequest of(Template uriTemplate, McpSchema.ReadResourceRequest request) {
        return new UriTemplateReadResourceRequest(arguments(uriTemplate, request.uri()), request);
    }

    /**
     * variable values extracted by matching the `uriTemplate` against the supplied `uri`.
     * @param uriTemplate URI template
     * @param uri Uri
     * @return the variable values
     * @throws McpError An invalid params error, see {@link #of(Template, McpSchema.ReadResourceRequest)}
     */
    static Map<String, Object> arguments(String uriTemplate, String uri) {
        return arguments(Template.compile(uriTemplate), uri);
    }

    private static Map<String, Object> arguments(Template uriTemplate, String uri) {
        Optional<UriMatchInfo> matchOptional = uriTemplate.rejectsSimplePath(uri)
            ? Optional.empty()
            : uriTemplate.matchTemplate().match(uri);
        if (matchOptional.isEmpty()) {
            // Binding nothing would report the variables as missing, or bind null
            throw new InvalidUriTemplateException("The URI does not match the URI template " + uriTemplate.matchTemplate()
                + ": percent-encode reserved characters such as ?, +, ;, &, !, ( and ) in variable values, for example + as %2B");
        }
        Map<String, Object> variables = matchOptional.get().getVariableValues();
        // Copied only when a value contains an escape, or a surrogate that must be validated
        Map<String, Object> decoded = null;
        for (Map.Entry<String, Object> variable : variables.entrySet()) {
            if (variable.getValue() instanceof String value && (value.indexOf('%') >= 0 || hasSurrogate(value))) {
                if (decoded == null) {
                    decoded = new LinkedHashMap<>(variables);
                }
                decoded.put(variable.getKey(), decode(uriTemplate, variable.getKey(), value));
            }
        }
        return decoded == null ? variables : decoded;
    }

    private static boolean hasSurrogate(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isSurrogate(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Percent-decodes a URI component. Unlike form decoding, a {@code +} stays a {@code +}. Matching happens before
     * decoding, so a simple variable, which matches a single segment, must not contain a {@code /} once decoded: it
     * would let {@code %2F} smuggle path segments, such as {@code ..%2F..%2Fetc}, into the value. The values of reserved
     * ({@code {+var}}) variables may contain {@code /} and are decoded too.
     */
    private static String decode(Template uriTemplate, String name, String value) {
        String decoded = percentDecode(name, value);
        if (decoded.indexOf('/') >= 0 && !uriTemplate.reservedVariables().contains(name)) {
            throw new InvalidUriTemplateException("The value of URI template variable '" + name + "' must not contain an encoded / (%2F)");
        }
        return decoded;
    }

    /**
     * Decodes the escaped bytes of a value as UTF-8. Unlike {@link java.net.URLDecoder}, which replaces malformed UTF-8
     * with U+FFFD, it rejects them, so that two different URIs cannot bind the same value.
     */
    private static String percentDecode(String name, String value) {
        // A char takes at most 3 UTF-8 bytes, and an escape of 3 chars decodes to a single byte
        ByteBuffer bytes = ByteBuffer.allocate(value.length() * 3);
        int i = 0;
        while (i < value.length()) {
            char c = value.charAt(i);
            if (c == '%') {
                i = putEscape(bytes, name, value, i);
            } else if (c < 0x80) {
                bytes.put((byte) c);
                i++;
            } else {
                i = putNonAscii(bytes, name, value, i);
            }
        }
        return decodeUtf8(bytes.flip(), name);
    }

    /**
     * Puts the byte of the escape at {@code i}.
     *
     * @return the index after the escape
     */
    private static int putEscape(ByteBuffer bytes, String name, String value, int i) {
        int high = i + 2 < value.length() ? hexDigit(value.charAt(i + 1)) : -1;
        int low = high >= 0 ? hexDigit(value.charAt(i + 2)) : -1;
        if (low < 0) {
            throw malformed(name);
        }
        bytes.put((byte) ((high << 4) | low));
        return i + 3;
    }

    /**
     * Puts the UTF-8 bytes of the run of non-ASCII chars at {@code i}.
     *
     * @return the index after the run
     */
    private static int putNonAscii(ByteBuffer bytes, String name, String value, int i) {
        int end = i + 1;
        while (end < value.length() && value.charAt(end) >= 0x80) {
            end++;
        }
        CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            bytes.put(encoder.encode(CharBuffer.wrap(value, i, end)));
        } catch (CharacterCodingException _) {
            throw new InvalidUriTemplateException("The value of URI template variable '" + name + "' contains an unpaired surrogate");
        }
        return end;
    }

    private static String decodeUtf8(ByteBuffer bytes, String name) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(bytes)
                .toString();
        } catch (CharacterCodingException _) {
            throw malformed(name);
        }
    }

    /**
     * @return the value of an ASCII hexadecimal digit, or -1. Unlike {@link Character#digit(char, int)}, it does not
     * accept non-ASCII digits.
     */
    private static int hexDigit(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        return -1;
    }

    private static InvalidUriTemplateException malformed(String name) {
        return new InvalidUriTemplateException("Invalid percent-encoding in the value of URI template variable '" + name
            + "': escapes must be %XX hexadecimal pairs encoding UTF-8");
    }

    /**
     * Identifies validation failures raised before the application handler is invoked.
     */
    static final class InvalidUriTemplateException extends McpError {
        private InvalidUriTemplateException(String message) {
            super(new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INVALID_PARAMS, message));
        }
    }

    /**
     * A compiled URI template.
     *
     * @param matchTemplate The template the URIs are matched against
     * @param reservedVariables The names of the variables expanded with the reserved ({@code +}) operator
     * @param simplePathPattern A path pre-check for templates with only simple variables and no reserved literals
     */
    record Template(UriMatchTemplate matchTemplate, Set<String> reservedVariables, @Nullable Pattern simplePathPattern) {
        private static final String UNMATCHABLE_CHARS = "#(!{)&;+";
        private static final Pattern SIMPLE_EXPRESSION = Pattern.compile("\\{[A-Za-z0-9_]+(?:,[A-Za-z0-9_]+)*}");
        private static final Pattern EXPRESSION = Pattern.compile("\\{[^}]*}");

        // An expression ends at its first }, so a regular expression with braces, such as {+p:[a-z]{1,3}}, is not
        // parsed correctly. The names it adds by mistake match no variable, so they do no harm.
        private static final Pattern RESERVED_EXPRESSION = Pattern.compile("\\{\\+([^}]*)}");

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
            UriMatchTemplate matchTemplate = UriMatchTemplate.of(uriTemplate);
            Pattern simplePath = simpleVariablesOnly(uriTemplate) ? compileSimplePath(withoutTrailingSlashes(matchTemplate.toPathString())) : null;
            return new Template(matchTemplate, Set.copyOf(reserved), simplePath);
        }

        /**
         * The SDK routes with the query attached. Check the path alone before handing it to the core matcher, whose
         * lazy capture groups would otherwise backtrack on every failed split of a non-matching path.
         */
        boolean rejectsSimplePath(String uri) {
            if (simplePathPattern == null) {
                return false;
            }
            int queryStart = uri.indexOf('?');
            int pathEnd = queryStart < 0 ? uri.length() : queryStart;
            for (int i = 0; i < pathEnd; i++) {
                if (UNMATCHABLE_CHARS.indexOf(uri.charAt(i)) >= 0) {
                    return true;
                }
            }
            if (queryStart < 0) {
                return false;
            }
            // UriMatchTemplate ignores every trailing /, in the path and in the template
            while (pathEnd > 0 && uri.charAt(pathEnd - 1) == '/') {
                pathEnd--;
            }
            return !simplePathPattern.matcher(uri).region(0, pathEnd).matches();
        }

        private static String withoutTrailingSlashes(String path) {
            int end = path.length();
            while (end > 0 && path.charAt(end - 1) == '/') {
                end--;
            }
            return path.substring(0, end);
        }

        private static Pattern compileSimplePath(String path) {
            Matcher expressions = SIMPLE_EXPRESSION.matcher(path);
            StringBuilder pattern = new StringBuilder();
            int cursor = 0;
            int variables = 0;
            while (expressions.find()) {
                if (cursor < expressions.start()) {
                    String literal = Pattern.quote(path.substring(cursor, expressions.start()));
                    if (variables == 0) {
                        pattern.append(literal);
                    } else {
                        // Taking the first match leaves maximum room in the segment for the next variable.
                        // The atomic group prevents backtracking into later occurrences of the same literal.
                        pattern.append("(?>[^/]{").append(variables).append(",}?").append(literal).append(')');
                        variables = 0;
                    }
                }
                variables += 1 + (int) expressions.group().chars().filter(c -> c == ',').count();
                cursor = expressions.end();
            }
            if (variables > 0) {
                pattern.append("[^/]{").append(variables).append(",}");
            }
            return Pattern.compile(pattern.append(Pattern.quote(path.substring(cursor))).toString());
        }

        private static boolean simpleVariablesOnly(String uriTemplate) {
            Matcher expressions = EXPRESSION.matcher(uriTemplate);
            while (expressions.find()) {
                if (!SIMPLE_EXPRESSION.matcher(expressions.group()).matches()) {
                    return false;
                }
            }
            String literal = expressions.replaceAll("");
            for (int i = 0; i < literal.length(); i++) {
                char c = literal.charAt(i);
                if (c == '?' || UNMATCHABLE_CHARS.indexOf(c) >= 0) {
                    return false;
                }
            }
            return true;
        }
    }
}
