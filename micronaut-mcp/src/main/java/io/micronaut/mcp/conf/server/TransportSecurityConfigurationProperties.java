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
    private boolean allowLoopbackOrigins = DEFAULT_ALLOW_LOOPBACK_ORIGINS;

    /**
     * @return Whether the {@code Origin} and {@code MCP-Protocol-Version} request headers are validated
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Whether the Origin and MCP-Protocol-Version request headers of the HTTP endpoint are validated. Default value `true`.
     *
     * @param enabled Whether the headers are validated
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isAllowLoopbackOrigins() {
        return allowLoopbackOrigins;
    }

    /**
     * Whether loopback origins (localhost, 127.0.0.1 and [::1], with http or https and any port) are allowed. Default value `true`.
     *
     * @param allowLoopbackOrigins Whether loopback origins are allowed
     */
    public void setAllowLoopbackOrigins(boolean allowLoopbackOrigins) {
        this.allowLoopbackOrigins = allowLoopbackOrigins;
    }

    @Override
    public @NonNull List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    /**
     * The origins allowed besides loopback origins, such as https://app.example.com, each scheme://host[:port] without a path. * allows every origin. None by default.
     *
     * @param allowedOrigins The allowed origins
     */
    public void setAllowedOrigins(@Nullable List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins != null ? allowedOrigins : List.of();
    }
}
