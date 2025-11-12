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
import io.micronaut.mcp.annotations.PromptCompletion;
import io.micronaut.mcp.annotations.ResourceCompletion;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.annotations.Resource;
import io.micronaut.mcp.annotations.ResourceTemplate;
import io.micronaut.mcp.conf.server.McpServerConfiguration;
import jakarta.inject.Singleton;

@Requires(beans = McpServerConfiguration.class)
@Internal
@Singleton
final class McpExecutableMethodProcessor implements ExecutableMethodProcessor<McpPrimitive> {

    private final ToolRegistry toolRegistry;
    private final PromptRegistry promptRegistry;
    private final ResourceRegistry resourceRegistry;
    private final ResourceTemplateRegistry resourceTemplateRegistry;
    private final CompletionRegistry completionRegistry;

    McpExecutableMethodProcessor(ToolRegistry toolRegistry,
                                 PromptRegistry promptRegistry,
                                 ResourceRegistry resourceRegistry,
                                 ResourceTemplateRegistry resourceTemplateRegistry,
                                 CompletionRegistry completionRegistry) {
        this.toolRegistry = toolRegistry;
        this.promptRegistry = promptRegistry;
        this.resourceRegistry = resourceRegistry;
        this.resourceTemplateRegistry = resourceTemplateRegistry;
        this.completionRegistry = completionRegistry;
    }

    @Override
    public void process(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {
        if (method.hasStereotype(Prompt.class)) {
            promptRegistry.addMethod(beanDefinition, method);
        } else if (method.hasStereotype(Tool.class)) {
            toolRegistry.addMethod(beanDefinition, method);
        } else if (method.hasStereotype(Resource.class)) {
            resourceRegistry.addMethod(beanDefinition, method);
        } else if (method.hasStereotype(ResourceTemplate.class)) {
            resourceTemplateRegistry.addMethod(beanDefinition, method);
        } else if (method.hasStereotype(PromptCompletion.class)) {
            completionRegistry.addMethod(beanDefinition, method);
        } else if (method.hasStereotype(ResourceCompletion.class)) {
            completionRegistry.addMethod(beanDefinition, method);
        } else {
            throw new IllegalStateException("Unknown method: " + method);
        }
    }
}
