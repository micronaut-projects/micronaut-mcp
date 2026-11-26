package io.micronaut.mcp.conf.server;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.StringUtils;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Property(name = "micronaut.mcp.server.transport-security.enabled", value = StringUtils.FALSE)
@Property(name = "micronaut.mcp.server.transport-security.allow-loopback-origins", value = StringUtils.FALSE)
@Property(name = "micronaut.mcp.server.transport-security.allowed-origins", value = "https://app.example.com,http://localhost:6274")
@MicronautTest(startApplication = false)
class TransportSecurityConfigurationPropertiesTest {

    @Test
    void transportSecurityConfiguration(TransportSecurityConfiguration configuration) {
        assertFalse(configuration.isAllowLoopbackOrigins());
        assertEquals(List.of("https://app.example.com", "http://localhost:6274"), configuration.getAllowedOrigins());
        assertFalse(((TransportSecurityConfigurationProperties) configuration).isEnabled());
    }

    @Test
    void nullAllowedOriginsAreNone() {
        TransportSecurityConfigurationProperties properties = new TransportSecurityConfigurationProperties();
        properties.setAllowedOrigins(null);
        assertEquals(List.of(), properties.getAllowedOrigins());
    }
}
