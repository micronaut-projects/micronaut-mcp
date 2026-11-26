package io.micronaut.mcp.conf.client;

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Property(name = "micronaut.mcp.client.http.minimal.url", value = "https://weather.example.com/mcp")
@Property(name = "micronaut.mcp.client.http.headers.url", value = "https://weather.example.com/mcp")
@Property(name = "micronaut.mcp.client.http.headers.headers.X-Api-Key", value = "secret")
@Property(name = "micronaut.mcp.client.http.headers.headers.x_lower_case", value = "lower")
@Property(name = "micronaut.mcp.client.stdio.minimal.command", value = "server")
@MicronautTest(startApplication = false)
class McpClientConnectionDefaultsTest {

    @Inject
    BeanContext beanContext;

    @Test
    void anHttpConnectionWithOnlyItsUrlHasTheDefaults() {
        McpClientHttpConfiguration http = beanContext.getBean(McpClientHttpConfiguration.class, Qualifiers.byName("minimal"));
        assertEquals(Map.of(), http.getHeaders());
        assertNull(http.getTimeout());
        assertNull(http.getInitializationTimeout());
        assertNull(http.getRequestTimeout());
        assertTrue(http.isAutoHealthCheck());
        assertFalse(http.isLogRequests());
        assertFalse(http.isLogResponses());
    }

    @Test
    void aStdioConnectionWithOnlyItsCommandHasTheDefaults() {
        McpClientStdioConfiguration stdio = beanContext.getBean(McpClientStdioConfiguration.class, Qualifiers.byName("minimal"));
        assertEquals(List.of("server"), stdio.getCommand());
        assertEquals(Map.of(), stdio.getEnvironment());
        assertFalse(stdio.isLogEvents());
        assertNull(stdio.getInitializationTimeout());
        assertNull(stdio.getRequestTimeout());
        assertTrue(stdio.isAutoHealthCheck());
    }

    @Test
    void headerNamesKeepTheirCase() {
        McpClientHttpConfiguration http = beanContext.getBean(McpClientHttpConfiguration.class, Qualifiers.byName("headers"));
        assertEquals(Map.of("X-Api-Key", "secret", "x_lower_case", "lower"), http.getHeaders());
    }
}
