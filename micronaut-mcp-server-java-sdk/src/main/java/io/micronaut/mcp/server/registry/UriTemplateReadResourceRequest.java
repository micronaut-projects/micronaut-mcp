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
        // The SDK routes the request with a laxer matcher, which accepts characters such as + that a simple variable
        // does not match, and a query that UriMatchTemplate drops before it matches. UriMatchTemplate backtracks over
        // every split point before it fails, which takes seconds for a long URI and a template with several variables in
        // a segment, such as log://{year}-{month}-{day}.log, so such a URI is rejected without matching it. Without these
        // characters, UriMatchTemplate matches whatever the SDK routed.
        Optional<UriMatchInfo> matchOptional = uriTemplate.simpleVariablesOnly() && hasUnmatchableChar(uri)
            ? Optional.empty()
            : uriTemplate.matchTemplate().match(uri);
        if (matchOptional.isEmpty()) {
            // Binding nothing would report the variables as missing, or bind null
            throw invalidParams("The URI does not match the URI template " + uriTemplate.matchTemplate()
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

    /**
     * @return Whether the URI contains a character that neither a simple variable nor the literal text of a
     * {@link Template#simpleVariablesOnly()} template matches, or a {@code ?}, which starts a query that such a template
     * does not declare
     */
    private static boolean hasUnmatchableChar(String uri) {
        for (int i = 0; i < uri.length(); i++) {
            if (Template.UNMATCHABLE_CHARS.indexOf(uri.charAt(i)) >= 0) {
                return true;
            }
        }
        return false;
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
            throw invalidParams("The value of URI template variable '" + name + "' must not contain an encoded / (%2F)");
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
            throw invalidParams("The value of URI template variable '" + name + "' contains an unpaired surrogate");
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

    private static McpError malformed(String name) {
        return invalidParams("Invalid percent-encoding in the value of URI template variable '" + name
            + "': escapes must be %XX hexadecimal pairs encoding UTF-8");
    }

    private static McpError invalidParams(String message) {
        return McpError.builder(McpSchema.ErrorCodes.INVALID_PARAMS).message(message).build();
    }

    /**
     * A compiled URI template.
     *
     * @param matchTemplate The template the URIs are matched against
     * @param reservedVariables The names of the variables expanded with the reserved ({@code +}) operator
     * @param simpleVariablesOnly Whether every expression is a list of simple variables without a modifier, and the
     * literal text contains none of the {@link #UNMATCHABLE_CHARS}, so that a URI that contains one cannot match
     */
    record Template(UriMatchTemplate matchTemplate, Set<String> reservedVariables, boolean simpleVariablesOnly) {
        /**
         * The characters that the pattern of a simple variable in {@link UriMatchTemplate} does not match, besides
         * {@code /}, and {@code ?}, which starts a query.
         */
        static final String UNMATCHABLE_CHARS = "?#(!{)&;+";
        private static final Pattern SIMPLE_EXPRESSION = Pattern.compile("\\{[A-Za-z0-9_,]+}");
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
            return new Template(UriMatchTemplate.of(uriTemplate), Set.copyOf(reserved), simpleVariablesOnly(uriTemplate));
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
                if (UNMATCHABLE_CHARS.indexOf(literal.charAt(i)) >= 0) {
                    return false;
                }
            }
            return true;
        }
    }
}
