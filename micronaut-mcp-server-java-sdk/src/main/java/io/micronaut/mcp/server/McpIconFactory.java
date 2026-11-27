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
package io.micronaut.mcp.server;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.util.StringUtils;
import io.modelcontextprotocol.spec.McpSchema;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Builds the MCP icons declared with {@code @Icon} or configured under {@code micronaut.mcp.server.info.icons}, so both
 * follow the same rules: a {@code src} that is an https or http URL without a user name or password, or a {@code data:}
 * URI of an image, is required, the theme is {@code light} or {@code dark}, and empty values are left out. Error messages
 * never repeat the rejected {@code src}, which could hold a credential or a large payload.
 *
 * @since 2.2.0
 */
@Internal
public final class McpIconFactory {

    private static final Set<String> THEMES = Set.of("light", "dark");
    private static final String MIME_TOKEN = "[A-Za-z0-9!#$%&'*+.^_`|~-]++";
    private static final Pattern IMAGE_DATA_HEADER = Pattern.compile("image/" + MIME_TOKEN + "(?:;" + MIME_TOKEN + "=" + MIME_TOKEN + ")*+(?:;base64)?", Pattern.CASE_INSENSITIVE);
    private static final int MAX_PORT = 65535;

    private McpIconFactory() {
    }

    /**
     * @param owner What declares the icon, for the error messages, for example {@code The @Icon of method Weather#forecast}
     * @param src The URI of the icon
     * @param mimeType The MIME type of the icon
     * @param sizes The sizes of the icon
     * @param theme The theme of the icon
     * @return The icon
     * @throws IllegalStateException If the {@code src} is blank, is neither an https or http URL nor a {@code data:} URI of
     * an image, or has a user name or password, or if the theme is neither {@code light} nor {@code dark}
     */
    public static McpSchema.Icon icon(String owner, @Nullable String src, @Nullable String mimeType, List<String> sizes, @Nullable String theme) {
        if (src == null || src.isBlank()) {
            throw new IllegalStateException(owner + " requires a src");
        }
        checkUrl(owner, "a src", src, true);
        String nonEmptyTheme = StringUtils.isNotEmpty(theme) ? theme : null;
        if (nonEmptyTheme != null && !THEMES.contains(nonEmptyTheme)) {
            throw new IllegalStateException(owner + " has the theme " + nonEmptyTheme + ", but it must be light or dark");
        }
        return new McpSchema.Icon(src,
            StringUtils.isNotEmpty(mimeType) ? mimeType : null,
            sizes.isEmpty() ? null : sizes,
            nonEmptyTheme);
    }

    /**
     * Checks that a value is an https or http URL with a host and without a user name or password, or, when allowed, a
     * {@code data:} URI of an image. The messages do not repeat the value.
     *
     * @param owner What declares the value, for the error messages
     * @param noun How the messages call the value, for example {@code a src}
     * @param url The value
     * @param imageDataAllowed Whether a {@code data:} URI of an image is accepted
     * @throws IllegalStateException If the value is not accepted
     */
    static void checkUrl(String owner, String noun, String url, boolean imageDataAllowed) {
        try {
            URI uri = new URI(url);
            if (imageDataAllowed && "data".equalsIgnoreCase(uri.getScheme()) && isImageData(uri)) {
                return;
            }
            if ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())) {
                String authority = uri.getRawAuthority();
                if (authority != null && authority.contains("@")) {
                    throw new IllegalStateException(owner + " has " + noun + " with a user name or password, which every client would receive");
                }
                if (authority != null && hasHostAndValidPort(authority)) {
                    return;
                }
            }
        } catch (URISyntaxException _) {
            // URI syntax errors include the input. Report only the configuration property or method below.
        }
        throw new IllegalStateException(owner + " has " + noun + " that is not "
            + (imageDataAllowed ? "an https or http URL, or a data: URI of an image" : "an https or http URL"));
    }

    private static boolean isImageData(URI uri) {
        String data = uri.getRawSchemeSpecificPart();
        int comma = data.indexOf(',');
        return comma > 0 && comma < data.length() - 1 && IMAGE_DATA_HEADER.matcher(data).region(0, comma).matches();
    }

    private static boolean hasHostAndValidPort(String authority) {
        // URI validates bracketed IPv6 literals. Use the raw authority to also accept hostnames containing underscores.
        int colon = authority.indexOf(':', authority.lastIndexOf(']') + 1);
        if (colon < 0) {
            return !authority.isEmpty();
        }
        if (colon == 0) {
            return false;
        }
        int port = 0;
        for (int i = colon + 1; i < authority.length(); i++) {
            char digit = authority.charAt(i);
            if (digit < '0' || digit > '9') {
                return false;
            }
            port = port * 10 + digit - '0';
            if (port > MAX_PORT) {
                return false;
            }
        }
        return true;
    }
}
