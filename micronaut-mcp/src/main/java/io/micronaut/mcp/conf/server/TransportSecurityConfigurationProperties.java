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
package io.micronaut.mcp.conf.server;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.annotation.Internal;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * {@link ConfigurationProperties} implementation of {@link TransportSecurityConfiguration}.
 */
@ConfigurationProperties(TransportSecurityConfiguration.PREFIX)
@Internal
final class TransportSecurityConfigurationProperties implements TransportSecurityConfiguration {

    private boolean enabled = DEFAULT_ENABLED;
    private List<String> allowedOrigins = List.of();

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Whether the {@code Origin} and {@code MCP-Protocol-Version} request headers are validated. Default value {@value #DEFAULT_ENABLED}.
     *
     * @param enabled Whether the headers are validated
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public @NonNull List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    /**
     * The origins allowed besides loopback origins, such as {@code https://app.example.com}. {@code *} allows every origin.
     *
     * @param allowedOrigins The allowed origins
     */
    public void setAllowedOrigins(@Nullable List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins != null ? allowedOrigins : List.of();
    }
}
