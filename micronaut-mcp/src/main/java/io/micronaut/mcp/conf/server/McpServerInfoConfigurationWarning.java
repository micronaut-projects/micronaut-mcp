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

import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Warns at startup that properties under {@code micronaut.mcp.server.info} are ignored because the name or the version
 * of the server is missing, so that title, icons or instructions are not dropped silently.
 *
 * @since 2.2.0
 */
@Internal
@Context
@Requires(property = McpServerInfoConfiguration.PREFIX)
@Requires(missingBeans = McpServerInfoConfiguration.class)
final class McpServerInfoConfigurationWarning {
    private static final Logger LOG = LoggerFactory.getLogger(McpServerInfoConfigurationWarning.class);

    McpServerInfoConfigurationWarning() {
        LOG.warn("Properties under {} are set but ignored: set both {} and {} to send the server information, including its title, icons and instructions, to clients",
            McpServerInfoConfiguration.PREFIX, McpServerInfoConfiguration.PROPERTY_NAME, McpServerInfoConfiguration.PROPERTY_VERSION);
    }
}
