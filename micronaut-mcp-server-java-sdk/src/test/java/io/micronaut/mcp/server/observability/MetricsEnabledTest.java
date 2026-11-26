package io.micronaut.mcp.server.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricsEnabledTest {

    @Test
    void metricsAreRecordedByDefault() {
        try (ApplicationContext context = ApplicationContext.run()) {
            assertTrue(context.containsBean(MeterRegistry.class));
            assertTrue(context.containsBean(MicrometerMcpServerObserver.class));
            assertTrue(context.getBean(MetricsConfigurationProperties.class).isEnabled());
        }
    }

    @Test
    void disablingMicrometerDisablesTheMetrics() {
        try (ApplicationContext context = ApplicationContext.run(Map.of("micronaut.metrics.enabled", false))) {
            assertFalse(context.containsBean(MeterRegistry.class));
            assertFalse(context.containsBean(MicrometerMcpServerObserver.class));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "TRUE", "on", "yes"})
    void metricsCanBeEnabledExplicitly(String value) {
        try (ApplicationContext context = ApplicationContext.run(Map.of(MetricsConfigurationProperties.PROPERTY_ENABLED, value))) {
            assertTrue(context.containsBean(MicrometerMcpServerObserver.class));
            assertTrue(context.getBean(MetricsConfigurationProperties.class).isEnabled());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"false", "FALSE", "off", "no", "0"})
    void metricsCanBeDisabled(String value) {
        // every value that the bound property converts to false, not only the lowercase string
        try (ApplicationContext context = ApplicationContext.run(Map.of(MetricsConfigurationProperties.PROPERTY_ENABLED, value))) {
            assertTrue(context.containsBean(MeterRegistry.class));
            assertFalse(context.containsBean(MicrometerMcpServerObserver.class));
            assertFalse(context.getBean(MetricsConfigurationProperties.class).isEnabled());
        }
    }
}
