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
package io.micronaut.mcp.client.langchain4j;

import io.micronaut.context.BeanContext;
import io.micronaut.context.env.Environment;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.annotation.Internal;
import io.micronaut.mcp.client.langchain4j.stdio.LegacyMcpClientStdioConfiguration;
import io.micronaut.mcp.conf.client.McpClientConnectionConfiguration;
import io.micronaut.mcp.conf.client.McpClientHttpConfiguration;
import io.micronaut.mcp.conf.client.McpClientStdioConfiguration;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Checks the configuration of the MCP connections when the application starts, so that a mistake fails the startup
 * instead of the first use of a client, which the tool providers skip like a server that cannot be reached. Warns
 * about the deprecated properties.
 */
@Singleton
@Internal
final class McpClientConnectionsValidator implements ApplicationEventListener<StartupEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(McpClientConnectionsValidator.class);
    private static final Pattern CREDENTIAL_HEADER = Pattern.compile("auth|cookie|token|key|secret|password", Pattern.CASE_INSENSITIVE);
    private static final Pattern LOOPBACK_IPV4 = Pattern.compile("127(\\.\\d{1,3}){3}");

    private final BeanContext beanContext;
    private final Environment environment;

    McpClientConnectionsValidator(BeanContext beanContext, Environment environment) {
        this.beanContext = beanContext;
        this.environment = environment;
    }

    @Override
    public void onApplicationEvent(StartupEvent event) {
        warnAboutDeprecatedProperties();
        List<McpClientConnectionConfiguration> connections = new ArrayList<>();
        for (McpClientHttpConfiguration http : beanContext.getBeansOfType(McpClientHttpConfiguration.class)) {
            requireUrl(http);
            warnAboutCleartextCredentials(http);
            connections.add(http);
        }
        for (McpClientStdioConfiguration stdio : beanContext.getBeansOfType(McpClientStdioConfiguration.class)) {
            requireCommand(stdio);
            connections.add(stdio);
        }
        McpClientConnections.requireUniqueNames(connections);
    }

    private void warnAboutDeprecatedProperties() {
        String transport = LegacyMcpClientStdioConfiguration.TRANSPORT;
        if (environment.containsProperty(transport)) {
            LOG.warn("{} is deprecated: the connections of {} and {} are all used, whatever their transport, and it only enables {}",
                transport, McpClientHttpConfiguration.PREFIX, McpClientStdioConfiguration.PREFIX, LegacyMcpClientStdioConfiguration.PROPERTY);
        }
        if (environment.containsProperty(LegacyMcpClientStdioConfiguration.PROPERTY)) {
            boolean enabled = environment.getProperty(transport, String.class)
                .filter(LegacyMcpClientStdioConfiguration.TRANSPORT_STDIO::equals)
                .isPresent();
            LOG.warn("{} is deprecated{}: declare a STDIO connection with {}.<name>.command instead",
                LegacyMcpClientStdioConfiguration.PROPERTY,
                enabled ? " and declares a STDIO connection named stdio" : " and ignored, as " + transport + " is not STDIO",
                McpClientStdioConfiguration.PREFIX);
        }
    }

    private static void requireUrl(McpClientHttpConfiguration http) {
        // The interface declares the URL non-null, but a connection declared without it binds none
        if (http.getUrl() == null) {
            throw new ConfigurationException("The MCP HTTP connection " + http.getName() + " has no url, set "
                + McpClientHttpConfiguration.PREFIX + "." + http.getName() + ".url");
        }
    }

    private static void warnAboutCleartextCredentials(McpClientHttpConfiguration http) {
        URI url = http.getUrl();
        boolean credentials = url.getRawUserInfo() != null
            || http.getHeaders().keySet().stream().anyMatch(header -> CREDENTIAL_HEADER.matcher(header).find());
        if (credentials && "http".equalsIgnoreCase(url.getScheme()) && !isLoopback(url.getHost())) {
            // Only the name, never the value of a header or of the user information
            LOG.warn("The MCP HTTP connection {} sends credentials over http, use https", http.getName());
        }
    }

    private static boolean isLoopback(String host) {
        return host != null && (host.equalsIgnoreCase("localhost") || LOOPBACK_IPV4.matcher(host).matches() || host.equals("[::1]"));
    }

    private void requireCommand(McpClientStdioConfiguration stdio) {
        if (stdio.getCommand().isEmpty() || stdio.getCommand().getFirst().isBlank()) {
            String property = McpClientStdioConfiguration.PREFIX + "." + stdio.getName() + ".command";
            String hint = environment.containsProperties(property + "s") ? " (the property is command, not commands)" : "";
            throw new ConfigurationException("The MCP STDIO connection " + stdio.getName() + " has no command, set " + property + hint);
        }
    }
}
