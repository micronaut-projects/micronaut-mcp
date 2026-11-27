package io.micronaut.mcp.conf.server;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest(startApplication = false)
class TransportSecurityConfigurationTest {

    @Test
    void transportSecurityConfiguration(TransportSecurityConfiguration configuration) {
        assertTrue(configuration.isAllowLoopbackOrigins());
        assertEquals(List.of(), configuration.getAllowedOrigins());
        assertTrue(((TransportSecurityConfigurationProperties) configuration).isEnabled());
    }

    @Test
    void interfaceDefaults() {
        TransportSecurityConfiguration configuration = new TransportSecurityConfiguration() { };
        assertTrue(configuration.isAllowLoopbackOrigins());
        assertEquals(List.of(), configuration.getAllowedOrigins());
    }
}
