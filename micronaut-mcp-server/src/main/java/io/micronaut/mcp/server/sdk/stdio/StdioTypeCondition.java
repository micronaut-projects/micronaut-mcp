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
package io.micronaut.mcp.server.sdk.stdio;

import io.micronaut.context.condition.Condition;
import io.micronaut.context.condition.ConditionContext;
import io.micronaut.core.annotation.Internal;
import io.micronaut.mcp.server.sdk.conf.McpServerConfiguration;
import io.micronaut.mcp.server.sdk.conf.ServerType;

@Internal
class StdioTypeCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context) {
        ServerType type = context.getProperty(McpServerConfiguration.PROPERTY_TYPE, ServerType.class, ServerType.STATELESS_SYNC);
        return type.equals(ServerType.ASYNC) || type.equals(ServerType.SYNC) ;
    }
}
