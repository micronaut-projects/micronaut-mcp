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
package io.micronaut.mcp.server.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Records the duration of MCP server operations with Micrometer, following the OpenTelemetry semantic conventions for
 * MCP: a timer named {@value #METER_NAME}, tagged with the JSON-RPC method, the primitive name and the error type.
 *
 * @see <a href="https://opentelemetry.io/docs/specs/semconv/gen-ai/mcp/">Semantic conventions for MCP</a>
 * @since 2.2.0
 */
@Singleton
@Internal
@Requires(classes = MeterRegistry.class)
@Requires(beans = MeterRegistry.class)
@Requires(property = MicrometerMcpServerObserver.PROPERTY_ENABLED, notEquals = StringUtils.FALSE)
final class MicrometerMcpServerObserver implements McpServerObserver {
    /**
     * The property that disables the metrics.
     */
    static final String PROPERTY_ENABLED = "micronaut.mcp.server.metrics.enabled";
    static final String METER_NAME = "mcp.server.operation.duration";
    static final String TAG_METHOD = "mcp.method.name";
    static final String TAG_NAME = "mcp.primitive.name";
    static final String TAG_ERROR_TYPE = "error.type";
    private static final String NO_ERROR = "none";

    private final MeterRegistry meterRegistry;
    /**
     * The timers by tags. The server only invokes registered primitives, so the names, and with them the timers, are bounded.
     */
    private final Map<TimerKey, Timer> timers = new ConcurrentHashMap<>();

    MicrometerMcpServerObserver(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Observation start(String method, String name) {
        Timer.Sample sample = Timer.start(meterRegistry);
        return errorType -> sample.stop(timers.computeIfAbsent(new TimerKey(method, name, errorType(errorType)), this::timer));
    }

    private Timer timer(TimerKey key) {
        return Timer.builder(METER_NAME)
            .description("The duration of MCP server operations")
            .tag(TAG_METHOD, key.method())
            .tag(TAG_NAME, key.name())
            .tag(TAG_ERROR_TYPE, key.errorType())
            .register(meterRegistry);
    }

    private static String errorType(@Nullable String errorType) {
        return errorType != null ? errorType : NO_ERROR;
    }

    /**
     * The tags of a timer.
     *
     * @param method The JSON-RPC method
     * @param name The name of the primitive
     * @param errorType The error type
     */
    private record TimerKey(String method, String name, String errorType) {
    }
}
