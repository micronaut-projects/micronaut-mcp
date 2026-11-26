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
package io.micronaut.mcp.server.observability;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.util.Toggleable;

/**
 * The configuration of the metrics of the MCP server. {@link MicrometerMcpServerObserver} requires its property rather
 * than this bean, so that disabling the metrics leaves no observer; this class makes the property known to the
 * configuration reference and to IDEs.
 */
@ConfigurationProperties(MetricsConfigurationProperties.PREFIX)
@Internal
final class MetricsConfigurationProperties implements Toggleable {
    static final String PREFIX = "micronaut.mcp.server.metrics";
    static final String PROPERTY_ENABLED = PREFIX + ".enabled";
    private static final boolean DEFAULT_ENABLED = true;

    private boolean enabled = DEFAULT_ENABLED;

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Whether the server times its operations with Micrometer when a MeterRegistry bean exists. Custom McpServerObserver beans run either way. Default: true.
     *
     * @param enabled Whether the server times its operations
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
