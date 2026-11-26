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

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Builds the MCP icons declared with {@code @Icon} or configured under {@code micronaut.mcp.server.info.icons}, so both
 * follow the same rules: a {@code src} with the scheme {@code https}, {@code http} or {@code data} is required, the theme
 * is {@code light} or {@code dark}, and empty values are left out.
 *
 * @since 2.2.0
 */
@Internal
public final class McpIconFactory {

    private static final Set<String> THEMES = Set.of("light", "dark");
    private static final List<String> SRC_SCHEMES = List.of("https:", "http:", "data:");

    private McpIconFactory() {
    }

    /**
     * @param owner What declares the icon, for the error messages, for example {@code The @Icon of method Weather#forecast}
     * @param src The URI of the icon
     * @param mimeType The MIME type of the icon
     * @param sizes The sizes of the icon
     * @param theme The theme of the icon
     * @return The icon
     * @throws IllegalStateException If the {@code src} is blank or has another scheme than {@code https}, {@code http} or
     * {@code data}, or if the theme is neither {@code light} nor {@code dark}
     */
    public static McpSchema.Icon icon(String owner, @Nullable String src, @Nullable String mimeType, List<String> sizes, @Nullable String theme) {
        if (src == null || src.isBlank()) {
            throw new IllegalStateException(owner + " requires a src");
        }
        if (!hasScheme(src, SRC_SCHEMES)) {
            throw new IllegalStateException(owner + " has the src " + src + ", but it must be an https or http URL, or a data: URI");
        }
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
     * @param uri A URI
     * @param schemes The accepted schemes, in lowercase and followed by a colon
     * @return Whether the URI starts with one of the schemes, ignoring case
     */
    static boolean hasScheme(String uri, List<String> schemes) {
        String lowerCase = uri.toLowerCase(Locale.ROOT);
        return schemes.stream().anyMatch(lowerCase::startsWith);
    }
}
