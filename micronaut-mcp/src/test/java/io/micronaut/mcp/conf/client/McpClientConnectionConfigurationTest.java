package io.micronaut.mcp.conf.client;

import io.micronaut.context.annotation.Property;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.context.BeanContext;
import jakarta.inject.Inject;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Property(name = "micronaut.mcp.client.http.weather.url", value = "https://weather.example.com/mcp")
@Property(name = "micronaut.mcp.client.http.weather.headers.Authorization", value = "Bearer token")
@Property(name = "micronaut.mcp.client.http.weather.log-requests", value = "true")
@Property(name = "micronaut.mcp.client.http.weather.log-responses", value = "true")
@Property(name = "micronaut.mcp.client.http.weather.initialization-timeout", value = "5s")
@Property(name = "micronaut.mcp.client.http.weather.request-timeout", value = "30s")
@Property(name = "micronaut.mcp.client.http.weather.auto-health-check", value = "false")
@Property(name = "micronaut.mcp.client.http.weather.http-client", value = "MICRONAUT")
@Property(name = "micronaut.mcp.client.http.weather.service-id", value = "weather-service")
@Property(name = "micronaut.mcp.client.http.weather.max-message-size", value = "1MB")
@Property(name = "micronaut.mcp.client.stdio.files.command[0]", value = "npx")
@Property(name = "micronaut.mcp.client.stdio.files.command[1]", value = "server")
@Property(name = "micronaut.mcp.client.stdio.files.environment.DEBUG", value = "false")
@Property(name = "micronaut.mcp.client.stdio.files.log-events", value = "true")
@Property(name = "micronaut.mcp.client.stdio.files.initialization-timeout", value = "10s")
@Property(name = "micronaut.mcp.client.stdio.files.request-timeout", value = "20s")
@Property(name = "micronaut.mcp.client.stdio.files.auto-health-check", value = "false")
@MicronautTest(startApplication = false)
class McpClientConnectionConfigurationTest {

    @Inject
    BeanContext beanContext;

    @Test
    void httpConnectionsAreConfigured() {
        McpClientHttpConfiguration http = beanContext.getBean(McpClientHttpConfiguration.class, Qualifiers.byName("weather"));
        assertEquals("weather", http.getName());
        assertEquals(URI.create("https://weather.example.com/mcp"), http.getUrl());
        assertEquals(Map.of("Authorization", "Bearer token"), http.getHeaders());
        assertTrue(http.isLogRequests());
        assertTrue(http.isLogResponses());
        assertEquals(Duration.ofSeconds(5), http.getInitializationTimeout());
        assertEquals(Duration.ofSeconds(30), http.getRequestTimeout());
        assertFalse(http.isAutoHealthCheck());
        assertEquals(McpHttpClientType.MICRONAUT, http.getHttpClient());
        assertEquals("weather-service", http.getServiceId());
        assertEquals(1024L * 1024, http.getMaxMessageSize());
    }

    @Test
    void stdioConnectionsAreConfigured() {
        McpClientStdioConfiguration stdio = beanContext.getBean(McpClientStdioConfiguration.class, Qualifiers.byName("files"));
        assertEquals("files", stdio.getName());
        assertEquals(List.of("npx", "server"), stdio.getCommand());
        assertEquals(Map.of("DEBUG", "false"), stdio.getEnvironment());
        assertTrue(stdio.isLogEvents());
        assertEquals(Duration.ofSeconds(10), stdio.getInitializationTimeout());
        assertEquals(Duration.ofSeconds(20), stdio.getRequestTimeout());
        assertFalse(stdio.isAutoHealthCheck());
    }

    @Test
    void connectionsHaveDefaults() {
        McpClientHttpConfiguration http = new McpClientHttpConfiguration() {
            @Override
            public @NonNull URI getUrl() {
                return URI.create("http://localhost/mcp");
            }

            @Override
            public Duration getTimeout() {
                return null;
            }

            @Override
            public boolean isLogRequests() {
                return false;
            }

            @Override
            public boolean isLogResponses() {
                return false;
            }

            @Override
            public @NonNull String getName() {
                return "defaults";
            }
        };
        assertNull(http.getInitializationTimeout());
        assertNull(http.getRequestTimeout());
        assertTrue(http.isAutoHealthCheck());
        assertEquals(Map.of(), http.getHeaders());
        assertEquals(McpHttpClientType.JDK, http.getHttpClient());
        assertNull(http.getServiceId());
        assertEquals(16L * 1024 * 1024, http.getMaxMessageSize());
    }
}
