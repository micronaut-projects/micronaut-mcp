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
package io.micronaut.mcp.server.tools;

import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.exceptions.ConfigurationException;
import org.jspecify.annotations.NonNull;
import io.micronaut.core.io.ResourceLoader;
import io.micronaut.core.util.StringUtils;
import io.micronaut.mcp.conf.server.McpServerConfiguration;
import io.micronaut.mcp.primitives.prompts.ClasspathPrompt;
import io.micronaut.mcp.primitives.prompts.PromptArgument;
import io.micronaut.mcp.primitives.utils.StringInterpolator;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Factory
final class ClasspathPromptFactory {
    private static final String CLASSPATH_PREFIX = "classpath:";
    private static final McpSchema.CompleteResult EMPTY_COMPLETION = new McpSchema.CompleteResult(
        new McpSchema.CompleteResult.CompleteCompletion(Collections.emptyList(), 0, false));
    private final Map<String, String> nameToPrompt = new ConcurrentHashMap<>();

    ClasspathPromptFactory(ResourceLoader resourceLoader,
                           List<ClasspathPrompt> prompts) {
        for (ClasspathPrompt prompt : prompts) {
            String path = CLASSPATH_PREFIX + prompt.getPath();
            Optional<InputStream> resourceAsStream = resourceLoader.getResourceAsStream(path);
            if (resourceAsStream.isEmpty()) {
               throw new ConfigurationException("classpath resource for prompt " + prompt.getName() + " at path: " + path);
            }
            String text = null;
            try (InputStream inputStream = resourceAsStream.get()) {
                text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new ConfigurationException("error reading prompt at path: " + path, e);
            }
            nameToPrompt.put(prompt.getName(), text);
        }
    }

    @Requires(property = McpServerConfiguration.PROPERTY_REACTIVE, value = StringUtils.FALSE, defaultValue = StringUtils.FALSE)
    @Requires(property = McpServerConfiguration.PROPERTY_TRANSPORT, value = McpServerConfiguration.TRANSPORT_HTTP)
    @EachBean(ClasspathPrompt.class)
    @Singleton
    McpStatelessServerFeatures.SyncPromptSpecification stalessSyncPromptSpecification(ClasspathPrompt classpathPrompt) {
        return new McpStatelessServerFeatures.SyncPromptSpecification(prompt(classpathPrompt), this::result);
    }

    @Requires(property = McpServerConfiguration.PROPERTY_REACTIVE, value = StringUtils.TRUE)
    @Requires(property = McpServerConfiguration.PROPERTY_TRANSPORT, value = McpServerConfiguration.TRANSPORT_HTTP)
    @EachBean(ClasspathPrompt.class)
    @Singleton
    McpStatelessServerFeatures.AsyncPromptSpecification stalessAsyncPromptSpecification(ClasspathPrompt classpathPrompt) {
        return new McpStatelessServerFeatures.AsyncPromptSpecification(prompt(classpathPrompt),
                (mcpTransportContext, getPromptRequest) -> Mono.just(result(mcpTransportContext, getPromptRequest)));
    }

    @Requires(property = McpServerConfiguration.PROPERTY_REACTIVE, value = StringUtils.FALSE, defaultValue = StringUtils.FALSE)
    @Requires(property = McpServerConfiguration.PROPERTY_TRANSPORT, value = McpServerConfiguration.TRANSPORT_STDIO)
    @EachBean(ClasspathPrompt.class)
    @Singleton
    McpServerFeatures.SyncPromptSpecification syncPromptSpecification(ClasspathPrompt classpathPrompt) {
        return new McpServerFeatures.SyncPromptSpecification(prompt(classpathPrompt),
                (mcpSyncServerExchange, getPromptRequest) -> result(mcpSyncServerExchange.transportContext(), getPromptRequest));
    }

    @Requires(property = McpServerConfiguration.PROPERTY_REACTIVE, value = StringUtils.TRUE)
    @Requires(property = McpServerConfiguration.PROPERTY_TRANSPORT, value = McpServerConfiguration.TRANSPORT_STDIO)
    @EachBean(ClasspathPrompt.class)
    @Singleton
    McpServerFeatures.AsyncPromptSpecification asyncPromptSpecification(ClasspathPrompt classpathPrompt) {
        return new McpServerFeatures.AsyncPromptSpecification(prompt(classpathPrompt),
                (mcpSyncServerExchange, getPromptRequest) -> Mono.just(result(mcpSyncServerExchange.transportContext(), getPromptRequest)));
    }

    @Requires(property = McpServerConfiguration.PROPERTY_REACTIVE, value = StringUtils.FALSE, defaultValue = StringUtils.FALSE)
    @Requires(property = McpServerConfiguration.PROPERTY_TRANSPORT, value = McpServerConfiguration.TRANSPORT_HTTP)
    @EachBean(ClasspathPrompt.class)
    McpStatelessServerFeatures.SyncCompletionSpecification statelessSyncCompletionSpecification(ClasspathPrompt classpathPrompt) {
        return new McpStatelessServerFeatures.SyncCompletionSpecification(new McpSchema.PromptReference(classpathPrompt.getName()),
            (mcpTransportContext, completeRequest) -> EMPTY_COMPLETION);
    }

    @Requires(property = McpServerConfiguration.PROPERTY_REACTIVE, value = StringUtils.FALSE, defaultValue = StringUtils.FALSE)
    @Requires(property = McpServerConfiguration.PROPERTY_TRANSPORT, value = McpServerConfiguration.TRANSPORT_STDIO)
    @EachBean(ClasspathPrompt.class)
    McpServerFeatures.SyncCompletionSpecification syncCompletionSpecification(ClasspathPrompt classpathPrompt) {
        return new McpServerFeatures.SyncCompletionSpecification(new McpSchema.PromptReference(classpathPrompt.getName()),
            (mcpSyncServerExchange, completeRequest) -> EMPTY_COMPLETION);
    }

    @Requires(property = McpServerConfiguration.PROPERTY_REACTIVE, value = StringUtils.TRUE)
    @Requires(property = McpServerConfiguration.PROPERTY_TRANSPORT, value = McpServerConfiguration.TRANSPORT_HTTP)
    @EachBean(ClasspathPrompt.class)
    McpStatelessServerFeatures.AsyncCompletionSpecification statelessAsyncCompletionSpecification(ClasspathPrompt classpathPrompt) {
        return new McpStatelessServerFeatures.AsyncCompletionSpecification(new McpSchema.PromptReference(classpathPrompt.getName()),
            (mcpTransportContext, completeRequest) -> Mono.just(EMPTY_COMPLETION));
    }

    @Requires(property = McpServerConfiguration.PROPERTY_REACTIVE, value = StringUtils.TRUE)
    @Requires(property = McpServerConfiguration.PROPERTY_TRANSPORT, value = McpServerConfiguration.TRANSPORT_STDIO)
    @EachBean(ClasspathPrompt.class)
    McpServerFeatures.AsyncCompletionSpecification asyncCompletionSpecification(ClasspathPrompt classpathPrompt) {
        return new McpServerFeatures.AsyncCompletionSpecification(new McpSchema.PromptReference(classpathPrompt.getName()),
            (mcpAsyncServerExchange, completeRequest) -> Mono.just(EMPTY_COMPLETION));
    }

    private McpSchema.GetPromptResult result(McpTransportContext transportContext, McpSchema.GetPromptRequest getPromptRequest) {
        String text = nameToPrompt.get(getPromptRequest.name());
        return new McpSchema.GetPromptResult(null,
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER, new McpSchema.TextContent(StringInterpolator.interpolate(text, getPromptRequest.arguments())))));
    }

    private McpSchema.@NonNull Prompt prompt(@NonNull ClasspathPrompt classpathPrompt) {
        return new McpSchema.Prompt(classpathPrompt.getName(),
                classpathPrompt.getTitle(),
                classpathPrompt.getDescription(),
                classpathPrompt.getArguments()
                        .stream()
                        .map(this::promptArgument)
                        .toList());
    }

    private McpSchema.@NonNull PromptArgument promptArgument(@NonNull PromptArgument arg) {
        return new McpSchema.PromptArgument(arg.getName(), arg.getTitle(), arg.getDescription(), arg.isRequired());
    }
}
