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
package io.micronaut.mcp.server.registry;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.processor.ExecutableMethodProcessor;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.mcp.annotations.McpPrimitive;
import io.micronaut.mcp.annotations.Prompt;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.server.conf.McpServerConfiguration;
import jakarta.inject.Singleton;

@Requires(beans = McpServerConfiguration.class)
@Internal
@Singleton
final class McpExecutableMethodProcessor implements ExecutableMethodProcessor<McpPrimitive> {

    private final ToolRegistry toolRegistry;
    private final PromptRegistry promptRegistry;

    McpExecutableMethodProcessor(ToolRegistry toolRegistry, PromptRegistry promptRegistry) {
        this.toolRegistry = toolRegistry;
        this.promptRegistry = promptRegistry;
    }

    @Override
    public void process(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {
        if (method.hasStereotype(Prompt.class)) {
            promptRegistry.addMethod(beanDefinition, method);
        } else if (method.hasStereotype(Tool.class)) {
            toolRegistry.addMethod(beanDefinition, method);
        } else {
            throw new IllegalStateException("Unknown method: " + method);
        }
    }

}
