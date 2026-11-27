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

import io.micrometer.core.instrument.Meter.MeterProvider;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Singleton;

/**
 * Records the duration of MCP server operations with Micrometer, based on the OpenTelemetry semantic conventions for
 * MCP: a timer named {@value #METER_NAME}, tagged with the JSON-RPC method, the primitive name and the error type.
 * Unlike the conventions, every timer has the same tag keys, as the Prometheus registry requires: one primitive name
 * tag instead of a tool, prompt or resource attribute, and an error type of {@code none} on success.
 *
 * @see <a href="https://github.com/open-telemetry/semantic-conventions-genai/blob/main/docs/gen-ai/mcp.md">Semantic conventions for MCP</a>
 */
@Singleton
@Internal
@Requires(classes = MeterRegistry.class)
@Requires(beans = MeterRegistry.class)
@Requires(property = MetricsConfigurationProperties.PROPERTY_ENABLED, defaultValue = StringUtils.TRUE, pattern = MetricsConfigurationProperties.ENABLED_PATTERN)
final class MicrometerMcpServerObserver implements McpServerObserver {
    static final String METER_NAME = "mcp.server.operation.duration";
    static final String TAG_METHOD = "mcp.method.name";
    static final String TAG_NAME = "mcp.primitive.name";
    static final String TAG_ERROR_TYPE = "error.type";
    private static final String NO_ERROR = "none";

    private final MeterRegistry meterRegistry;
    private final MeterProvider<Timer> timers;

    MicrometerMcpServerObserver(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.timers = Timer.builder(METER_NAME)
            .description("The duration of MCP server operations")
            .withRegistry(meterRegistry);
    }

    @Override
    public Observation start(String method, String name) {
        Timer.Sample sample = Timer.start(meterRegistry);
        return errorType -> sample.stop(timers.withTags(
            TAG_METHOD, method, TAG_NAME, name, TAG_ERROR_TYPE, errorType != null ? errorType : NO_ERROR));
    }
}
