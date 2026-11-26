package io.micronaut.mcp.server.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

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

    @Test
    void metricsCanBeDisabled() {
        try (ApplicationContext context = ApplicationContext.run(Map.of(MetricsConfigurationProperties.PROPERTY_ENABLED, false))) {
            assertTrue(context.containsBean(MeterRegistry.class));
            assertFalse(context.containsBean(MicrometerMcpServerObserver.class));
            assertFalse(context.getBean(MetricsConfigurationProperties.class).isEnabled());
        }
    }
}
