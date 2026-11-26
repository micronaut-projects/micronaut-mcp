package io.micronaut.mcp.conf.server;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@Property(name = "spec.name", value = "TransportSecurityConfigurationReplacementTest")
@MicronautTest(startApplication = false)
class TransportSecurityConfigurationReplacementTest {

    @Test
    void theConfigurationCanBeReplaced(TransportSecurityConfiguration configuration) {
        assertInstanceOf(CustomTransportSecurityConfiguration.class, configuration);
        assertEquals(List.of("https://app.example.com"), configuration.getAllowedOrigins());
    }

    @Requires(property = "spec.name", value = "TransportSecurityConfigurationReplacementTest")
    @Replaces(TransportSecurityConfiguration.class)
    @Singleton
    static class CustomTransportSecurityConfiguration implements TransportSecurityConfiguration {
        @Override
        public List<String> getAllowedOrigins() {
            return List.of("https://app.example.com");
        }
    }
}
