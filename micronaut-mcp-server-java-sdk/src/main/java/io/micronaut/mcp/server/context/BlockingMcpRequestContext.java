/*
 * Copyright 2017-2025 original authors
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
package io.micronaut.mcp.server.context;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.propagation.PropagatedContext;
import io.micronaut.core.propagation.PropagatedContextElement;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import reactor.util.context.ContextView;

/**
 * The thread and subscriber context of the blocking HTTP route while it handles a request.
 *
 * @param thread The thread subscribing on the blocking executor
 * @param context The HTTP subscriber context
 */
@Internal
@NullMarked
public record BlockingMcpRequestContext(Thread thread, ContextView context) implements PropagatedContextElement {
    /**
     * @return The active blocking MCP request, if any
     */
    public static @Nullable BlockingMcpRequestContext current() {
        return PropagatedContext.getOrEmpty().find(BlockingMcpRequestContext.class)
            .orElse(null);
    }
}
