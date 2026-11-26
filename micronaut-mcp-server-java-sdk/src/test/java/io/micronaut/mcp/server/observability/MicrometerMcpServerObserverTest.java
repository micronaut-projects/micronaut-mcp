package io.micronaut.mcp.server.observability;

import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MicrometerMcpServerObserverTest {

    private final MockClock clock = new MockClock();
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry(SimpleConfig.DEFAULT, clock);
    private final MicrometerMcpServerObserver observer = new MicrometerMcpServerObserver(meterRegistry);

    @Test
    void anOperationIsTimedFromStartToStop() {
        McpServerObserver.Observation observation = observer.start("tools/call", "add");
        clock.add(Duration.ofMillis(5));
        observation.stop(null);

        Timer timer = timer("tools/call", "add", "none");
        assertEquals(1, timer.count());
        assertEquals(5, timer.totalTime(TimeUnit.MILLISECONDS));
    }

    @Test
    void operationsWithTheSameTagsShareATimerAndAnErrorTypeHasItsOwn() {
        observer.start("tools/call", "add").stop(null);
        McpServerObserver.Observation failing = observer.start("tools/call", "add");
        clock.add(Duration.ofMillis(7));
        failing.stop(McpServerObserver.TOOL_ERROR);
        observer.start("tools/call", "add").stop(null);

        assertEquals(2, timer("tools/call", "add", "none").count());
        Timer toolError = timer("tools/call", "add", McpServerObserver.TOOL_ERROR);
        assertEquals(1, toolError.count());
        assertEquals(7, toolError.totalTime(TimeUnit.MILLISECONDS));
        assertEquals(2, meterRegistry.find(MicrometerMcpServerObserver.METER_NAME).timers().size());
    }

    private Timer timer(String method, String name, String errorType) {
        Timer timer = meterRegistry.find(MicrometerMcpServerObserver.METER_NAME)
            .tag(MicrometerMcpServerObserver.TAG_METHOD, method)
            .tag(MicrometerMcpServerObserver.TAG_NAME, name)
            .tag(MicrometerMcpServerObserver.TAG_ERROR_TYPE, errorType)
            .timer();
        assertNotNull(timer);
        return timer;
    }
}
