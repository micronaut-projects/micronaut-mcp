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

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

@Requires(property = McpServerInfoConfiguration.PROPERTY_NAME)
@Requires(property = McpServerInfoConfiguration.PROPERTY_VERSION)
@ConfigurationProperties(McpServerInfoConfiguration.PREFIX)
@Internal
final class McpServerInfoConfigurationProperties implements McpServerInfoConfiguration {

    private String name;
    private String version;
    private @Nullable String title;
    private @Nullable String description;
    private @Nullable String websiteUrl;
    private List<IconConfiguration> icons = List.of();
    private @Nullable String instructions;

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public @Nullable String getTitle() {
        return title;
    }

    /**
     * @param title A human-readable title of the server, for display
     */
    public void setTitle(@Nullable String title) {
        this.title = title;
    }

    @Override
    public @Nullable String getDescription() {
        return description;
    }

    /**
     * @param description A description of the server
     */
    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    @Override
    public @Nullable String getWebsiteUrl() {
        return websiteUrl;
    }

    /**
     * @param websiteUrl The URL of the website of the server
     */
    public void setWebsiteUrl(@Nullable String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    @Override
    public @NonNull List<IconConfiguration> getIcons() {
        return icons;
    }

    /**
     * @param icons The icons of the server
     */
    public void setIcons(@Nullable List<IconConfiguration> icons) {
        this.icons = icons != null ? icons : List.of();
    }

    @Override
    public @Nullable String getInstructions() {
        return instructions;
    }

    /**
     * @param instructions Instructions describing how to use the server and its features, which clients may add to the model's prompt
     */
    public void setInstructions(@Nullable String instructions) {
        this.instructions = instructions;
    }
}
