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
package io.micronaut.mcp.conf.server;

import io.micronaut.core.annotation.Introspected;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * An icon of the MCP server, configured for example with {@code micronaut.mcp.server.info.icons[0].src}.
 *
 * @see <a href="https://modelcontextprotocol.io/specification/2025-11-25/basic/index#icons">Icons</a>
 * @since 2.2.0
 */
@Introspected
public class IconConfiguration {
    private @Nullable String src;
    private @Nullable String mimeType;
    private List<String> sizes = List.of();
    private @Nullable String theme;

    /**
     * @return The URI of the icon: an HTTPS URL or a {@code data:} URI
     */
    public @Nullable String getSrc() {
        return src;
    }

    /**
     * @param src The URI of the icon: an HTTPS URL or a {@code data:} URI
     */
    public void setSrc(@Nullable String src) {
        this.src = src;
    }

    /**
     * @return The MIME type of the icon, for example {@code image/png}
     */
    public @Nullable String getMimeType() {
        return mimeType;
    }

    /**
     * @param mimeType The MIME type of the icon, for example {@code image/png}
     */
    public void setMimeType(@Nullable String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * @return The sizes the icon is available in, for example {@code 48x48} or {@code any}
     */
    public List<String> getSizes() {
        return sizes;
    }

    /**
     * @param sizes The sizes the icon is available in, for example {@code 48x48} or {@code any}
     */
    public void setSizes(@Nullable List<String> sizes) {
        this.sizes = sizes != null ? sizes : List.of();
    }

    /**
     * @return The theme the icon is designed for: {@code light} or {@code dark}
     */
    public @Nullable String getTheme() {
        return theme;
    }

    /**
     * @param theme The theme the icon is designed for: {@code light} or {@code dark}
     */
    public void setTheme(@Nullable String theme) {
        this.theme = theme;
    }
}
