package io.micronaut.mcp.server.registry;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micronaut.context.annotation.Property;
import io.micronaut.mcp.server.observability.McpServerObserver;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sets the observers of a registry directly, so each test controls how many there are and how they fail.
 */
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "micronaut.mcp.server.metrics.enabled", value = "false")
@MicronautTest(startApplication = false)
class ObserverBehaviourTest {
    private static final String INVALID = String.valueOf(McpSchema.ErrorCodes.INVALID_PARAMS);

    @Inject
    PromptRegistry registry;

    private final RecordingObserver recording = new RecordingObserver();
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private final Logger logger = (Logger) LoggerFactory.getLogger(AbstractMcpMethodRegistry.class);
    private Level level;

    @BeforeEach
    void attachAppender() {
        level = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
        logger.setLevel(level);
        registry.setObservers(List.of());
    }

    @Test
    void withoutObserversTheOperationIsReturnedUntouched() {
        registry.setObservers(List.of());
        Mono<String> operation = Mono.just("value");

        assertSame(operation, registry.observedAsync("prompts/get", "p", () -> operation));
        assertEquals("value", registry.observed("prompts/get", "p", () -> "value"));
    }

    @Test
    void aSingleObserverIsNotifiedOnce() {
        registry.setObservers(List.of(recording));

        assertEquals("value", registry.observed("prompts/get", "p", () -> "value"));

        assertEquals(List.of("prompts/get p null"), recording.stops);
    }

    @Test
    void anAsynchronousOperationIsTimedFromSubscriptionToCompletion() {
        TimingObserver timing = new TimingObserver();
        registry.setObservers(List.of(timing));

        Mono<Void> operation = registry.observedAsync("prompts/get", "slow", () -> Mono.delay(Duration.ofMillis(50)).then());
        assertEquals(0, timing.starts.get());

        operation.block();

        assertEquals(1, timing.starts.get());
        assertTrue(timing.elapsed.size() == 1 && timing.elapsed.getFirst() >= Duration.ofMillis(50).toNanos(), String.valueOf(timing.elapsed));
    }

    @Test
    void aCancellationAfterTheValueIsNotRecorded() {
        registry.setObservers(List.of(recording));

        cancelAfterEachSignal(registry.observedAsync("prompts/get", "value", () -> Mono.just("value")));
        cancelAfterEachSignal(registry.observedAsync("prompts/get", "empty", Mono::empty));
        cancelAfterEachSignal(registry.observedAsync("prompts/get", "error", () -> Mono.error(invalidParams())));

        assertEquals(List.of("prompts/get value null", "prompts/get empty null", "prompts/get error " + INVALID), recording.stops);
    }

    @ParameterizedTest
    @EnumSource(Failure.class)
    void aFailingObserverFailsNoOperationAndLeavesTheOthersRecording(Failure failure) {
        registry.setObservers(List.of(failure.observer(), recording));
        McpError error = invalidParams();

        assertEquals("value", registry.observed("prompts/get", "sync", () -> "value"));
        assertSame(error, assertThrows(McpError.class, () -> registry.observed("prompts/get", "sync", () -> {
            throw error;
        })));
        assertEquals("value", registry.observedAsync("prompts/get", "async", () -> Mono.just("value")).block());
        assertSame(error, assertThrows(McpError.class,
            () -> registry.observedAsync("prompts/get", "async", () -> Mono.error(error)).block()));

        assertEquals(List.of("prompts/get sync null", "prompts/get sync " + INVALID, "prompts/get async null", "prompts/get async " + INVALID),
            recording.stops);
    }

    @Test
    void aFailingObserverIsLoggedInFullOnceAndAtDebugLevelAfterwards() {
        McpServerObserver failing = Failure.START_THROWS.observer();
        registry.setObservers(List.of(failing));

        registry.observed("prompts/get", "first", () -> "value");
        registry.observed("prompts/get", "second", () -> "value");

        List<ILoggingEvent> events = observerEvents();
        assertEquals(List.of(Level.WARN, Level.DEBUG), events.stream().map(ILoggingEvent::getLevel).toList());
        ILoggingEvent warning = events.getFirst();
        assertTrue(warning.getFormattedMessage().contains(failing.getClass().getName() + " failed to start the observation of prompts/get first"),
            warning.getFormattedMessage());
        assertNotNull(warning.getThrowableProxy());
    }

    @Test
    void eachFailingObserverIsLoggedInFull() {
        registry.setObservers(List.of(Failure.STOP_THROWS.observer(), Failure.STOP_THROWS.observer()));

        registry.observed("prompts/get", "p", () -> "value");

        assertEquals(List.of(Level.WARN, Level.WARN), observerEvents().stream().map(ILoggingEvent::getLevel).toList());
    }

    private List<ILoggingEvent> observerEvents() {
        return appender.list.stream().filter(e -> e.getFormattedMessage().startsWith("MCP server observer")).toList();
    }

    private static McpError invalidParams() {
        return new McpError(new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INVALID_PARAMS, "invalid", null));
    }

    /**
     * Subscribes and cancels the subscription after every signal, as the server does once it has the result.
     */
    private static <T> void cancelAfterEachSignal(Mono<T> mono) {
        mono.subscribe(new Subscriber<T>() {
            private Subscription subscription;

            @Override
            public void onSubscribe(Subscription s) {
                subscription = s;
                s.request(1);
            }

            @Override
            public void onNext(T value) {
                subscription.cancel();
            }

            @Override
            public void onError(Throwable error) {
                subscription.cancel();
            }

            @Override
            public void onComplete() {
                subscription.cancel();
            }
        });
    }

    enum Failure {
        START_THROWS, STOP_THROWS, START_FAILS_WITH_AN_ERROR, STOP_FAILS_WITH_AN_ERROR, START_RETURNS_NULL;

        McpServerObserver observer() {
            return switch (this) {
                case START_THROWS -> new McpServerObserver() {
                    @Override
                    public Observation start(String method, String name) {
                        throw new IllegalStateException("start is broken");
                    }
                };
                case STOP_THROWS -> new McpServerObserver() {
                    @Override
                    public Observation start(String method, String name) {
                        return errorType -> {
                            throw new IllegalStateException("stop is broken");
                        };
                    }
                };
                case START_FAILS_WITH_AN_ERROR -> new McpServerObserver() {
                    @Override
                    public Observation start(String method, String name) {
                        throw new AssertionError("start is broken");
                    }
                };
                case STOP_FAILS_WITH_AN_ERROR -> new McpServerObserver() {
                    @Override
                    public Observation start(String method, String name) {
                        return errorType -> {
                            throw new AssertionError("stop is broken");
                        };
                    }
                };
                case START_RETURNS_NULL -> new McpServerObserver() {
                    @Override
                    public Observation start(String method, String name) {
                        return null;
                    }
                };
            };
        }
    }

    static final class RecordingObserver implements McpServerObserver {
        final List<String> stops = Collections.synchronizedList(new ArrayList<>());

        @Override
        public Observation start(String method, String name) {
            return errorType -> stops.add(method + " " + name + " " + errorType);
        }
    }

    static final class TimingObserver implements McpServerObserver {
        final AtomicInteger starts = new AtomicInteger();
        final List<Long> elapsed = Collections.synchronizedList(new ArrayList<>());

        @Override
        public Observation start(String method, String name) {
            starts.incrementAndGet();
            long start = System.nanoTime();
            return errorType -> elapsed.add(System.nanoTime() - start);
        }
    }
}
