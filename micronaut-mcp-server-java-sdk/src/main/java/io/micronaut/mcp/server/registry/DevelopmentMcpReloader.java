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
package io.micronaut.mcp.server.registry;

import io.micronaut.context.BeanContext;
import io.micronaut.context.BeanRegistration;
import io.micronaut.context.WatchableBeanContext;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Executable;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.DevelopmentActive;
import io.micronaut.context.watch.BeanExecutableMethod;
import io.micronaut.context.watch.ExecutableMethodChange;
import io.micronaut.context.watch.ReloadingConfigurationWatcher;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.mcp.annotations.McpPrimitive;
import io.micronaut.mcp.annotations.Prompt;
import io.micronaut.mcp.annotations.PromptCompletion;
import io.micronaut.mcp.annotations.Resource;
import io.micronaut.mcp.annotations.ResourceCompletion;
import io.micronaut.mcp.annotations.ResourceTemplate;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.conf.server.McpServerConfiguration;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpStatelessAsyncServer;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.server.McpSyncServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Keeps the tools, prompts, resources and resource templates of a running MCP server in step with the annotated
 * methods of the context in development mode. It exists only in development mode, so nothing of it is on the path of
 * a request, and the server and registries are built at startup exactly as they always are.
 *
 * <ul>
 *     <li>A method annotated with {@link Tool}, {@link Prompt}, {@link Resource} or {@link ResourceTemplate} that a
 *     definition registered at runtime, or a development reload, adds, removes or redefines is added to, removed
 *     from or replaced in each running server, through the SDK's own {@code addTool}, {@code removeTool} and the
 *     like. A reload applied in place replaces the definitions of the reloaded classes, so their methods come back
 *     here as replacements: the server then calls the new generation.</li>
 *     <li>The SDK notifies connected clients that the list changed when the server was configured to, with
 *     {@code micronaut.mcp.server.tools.list-changed}, {@code micronaut.mcp.server.prompts.list-changed} and
 *     {@code micronaut.mcp.server.resources.list-changed}: the sessions stay connected.</li>
 *     <li>A change of the configuration under {@code micronaut.mcp.server} asks for a restart: the transport, the
 *     server information and the capabilities are fixed when the server is built.</li>
 * </ul>
 *
 * <p>The server is changed rather than recreated: a server over STDIO owns the session of its one client, which a new
 * server would drop. A primitive the server cannot take in place, such as the first tool of a server built without
 * the tools capability, or a completion, which the SDK cannot add or remove, is logged and applied by a restart.</p>
 *
 * <p>The specifications are built by registries created for the change and dropped after it, from the methods the
 * change carries, as the registries of the context built them at startup. It holds the context only: what was
 * registered is read from the change, so nothing of a generation is kept here. The registries of the context, which
 * the server was built from, are emptied at each change, so that they keep no method of a retired generation.</p>
 *
 * @author graemerocher
 * @since 2.2.0
 */
@Internal
@Context
@DevelopmentActive
@Requires(beans = McpServerConfiguration.class)
final class DevelopmentMcpReloader {

    private static final Logger LOG = LoggerFactory.getLogger(DevelopmentMcpReloader.class);

    private final BeanContext beanContext;

    /**
     * @param beanContext The context, watched when it can be
     */
    DevelopmentMcpReloader(BeanContext beanContext) {
        this.beanContext = beanContext;
        if (beanContext instanceof WatchableBeanContext watchable) {
            watchable.methods(McpPrimitive.class).watch(this::onMethodChange);
            watchable.configuration(McpServerConfiguration.PREFIX).watchReloading(change -> onConfigurationChange());
        }
    }

    private ReloadingConfigurationWatcher.Outcome onConfigurationChange() {
        if (servers().isEmpty()) {
            return ReloadingConfigurationWatcher.Outcome.IGNORED;
        }
        LOG.debug("The MCP server configuration changed: a restart applies it");
        return ReloadingConfigurationWatcher.Outcome.REQUIRES_RESTART;
    }

    private void onMethodChange(ExecutableMethodChange<McpPrimitive> change) {
        // the first batch is what the server was built from at startup: only what changes after it matters
        if (change.initial() || (change.added().isEmpty() && change.removed().isEmpty())) {
            return;
        }
        List<Object> servers = servers();
        if (servers.isEmpty()) {
            // not built yet: it is built from the registries of the context, which the context feeds what is added
            return;
        }
        if (hasCompletion(change.removed()) || hasCompletion(change.added())) {
            LOG.info("An MCP completion changed: the server cannot add or remove completions, a restart applies it");
        }
        for (Object server : servers) {
            LOG.debug("Updating the primitives of the MCP server {}", server);
            // registries of their own, for each server and each side of the change, so that each builds the
            // specifications of the methods of that side only
            Registries removed = registries(change.removed());
            Registries added = registries(change.added());
            if (server instanceof McpSyncServer sync) {
                apply(sync, removed, added);
            } else if (server instanceof McpAsyncServer async) {
                apply(async, removed, added);
            } else if (server instanceof McpStatelessSyncServer statelessSync) {
                apply(statelessSync, removed, added);
            } else if (server instanceof McpStatelessAsyncServer statelessAsync) {
                apply(statelessAsync, removed, added);
            }
        }
        forgetMethods();
    }

    /**
     * Empties the registries of the context once their server is built. They are read when the server is built, at
     * startup, and never again, but the context still feeds them the methods a change adds, as it feeds every
     * {@link io.micronaut.context.processor.ExecutableMethodProcessor} written before watches: emptied at each change,
     * they hold no method of a generation a later reload retires.
     */
    private void forgetMethods() {
        forgetMethods(ToolRegistry.class);
        forgetMethods(PromptRegistry.class);
        forgetMethods(ResourceRegistry.class);
        forgetMethods(ResourceTemplateRegistry.class);
        forgetMethods(CompletionRegistry.class);
    }

    private <R extends AbstractMcpMethodRegistry<?, ?, ?, ?>> void forgetMethods(Class<R> type) {
        for (BeanRegistration<R> registration : beanContext.getActiveBeanRegistrations(type)) {
            registration.bean().methods.clear();
        }
    }

    private static void apply(McpSyncServer server, Registries removed, Registries added) {
        apply("tool", removed.tools().getSyncSpecs(), added.tools().getSyncSpecs(),
            spec -> spec.tool().name(), server::removeTool, server::addTool);
        apply("prompt", removed.prompts().getSyncSpecs(), added.prompts().getSyncSpecs(),
            spec -> spec.prompt().name(), server::removePrompt, server::addPrompt);
        apply("resource", removed.resources().getSyncSpecs(), added.resources().getSyncSpecs(),
            spec -> spec.resource().uri(), server::removeResource, server::addResource);
        apply("resource template", removed.templates().getSyncSpecs(), added.templates().getSyncSpecs(),
            spec -> spec.resourceTemplate().uriTemplate(), server::removeResourceTemplate, server::addResourceTemplate);
    }

    private static void apply(McpAsyncServer server, Registries removed, Registries added) {
        apply("tool", removed.tools().getAsyncSpecs(), added.tools().getAsyncSpecs(),
            spec -> spec.tool().name(), name -> server.removeTool(name).block(), spec -> server.addTool(spec).block());
        apply("prompt", removed.prompts().getAsyncSpecs(), added.prompts().getAsyncSpecs(),
            spec -> spec.prompt().name(), name -> server.removePrompt(name).block(), spec -> server.addPrompt(spec).block());
        apply("resource", removed.resources().getAsyncSpecs(), added.resources().getAsyncSpecs(),
            spec -> spec.resource().uri(), uri -> server.removeResource(uri).block(), spec -> server.addResource(spec).block());
        apply("resource template", removed.templates().getAsyncSpecs(), added.templates().getAsyncSpecs(),
            spec -> spec.resourceTemplate().uriTemplate(), uri -> server.removeResourceTemplate(uri).block(),
            spec -> server.addResourceTemplate(spec).block());
    }

    private static void apply(McpStatelessSyncServer server, Registries removed, Registries added) {
        apply("tool", removed.tools().getStatelessSyncSpecs(), added.tools().getStatelessSyncSpecs(),
            spec -> spec.tool().name(), server::removeTool, server::addTool);
        apply("prompt", removed.prompts().getStatelessSyncSpecs(), added.prompts().getStatelessSyncSpecs(),
            spec -> spec.prompt().name(), server::removePrompt, server::addPrompt);
        apply("resource", removed.resources().getStatelessSyncSpecs(), added.resources().getStatelessSyncSpecs(),
            spec -> spec.resource().uri(), server::removeResource, server::addResource);
        apply("resource template", removed.templates().getStatelessSyncSpecs(), added.templates().getStatelessSyncSpecs(),
            spec -> spec.resourceTemplate().uriTemplate(), server::removeResourceTemplate, server::addResourceTemplate);
    }

    private static void apply(McpStatelessAsyncServer server, Registries removed, Registries added) {
        apply("tool", removed.tools().getStatelessAsyncSpecs(), added.tools().getStatelessAsyncSpecs(),
            spec -> spec.tool().name(), name -> server.removeTool(name).block(), spec -> server.addTool(spec).block());
        apply("prompt", removed.prompts().getStatelessAsyncSpecs(), added.prompts().getStatelessAsyncSpecs(),
            spec -> spec.prompt().name(), name -> server.removePrompt(name).block(), spec -> server.addPrompt(spec).block());
        apply("resource", removed.resources().getStatelessAsyncSpecs(), added.resources().getStatelessAsyncSpecs(),
            spec -> spec.resource().uri(), uri -> server.removeResource(uri).block(), spec -> server.addResource(spec).block());
        apply("resource template", removed.templates().getStatelessAsyncSpecs(), added.templates().getStatelessAsyncSpecs(),
            spec -> spec.resourceTemplate().uriTemplate(), uri -> server.removeResourceTemplate(uri).block(),
            spec -> server.addResourceTemplate(spec).block());
    }

    /**
     * Removes from a server what went and is not back, then adds what came, which replaces what has the same key.
     *
     * @param kind The kind of primitive, for the log
     * @param removed The specifications of the methods that went
     * @param added The specifications of the methods that came
     * @param key The key the server knows a specification by: a name, a URI or a URI template
     * @param remove Removes from the server by key
     * @param add Adds to the server, replacing what it has under the same key
     * @param <S> The specification type
     */
    private static <S> void apply(String kind, List<S> removed, List<S> added, Function<S, String> key, Consumer<String> remove, Consumer<S> add) {
        Set<String> gone = new LinkedHashSet<>();
        for (S spec : removed) {
            gone.add(key.apply(spec));
        }
        for (S spec : added) {
            gone.remove(key.apply(spec));
        }
        for (String name : gone) {
            try {
                remove.accept(name);
            } catch (RuntimeException e) {
                LOG.warn("The MCP {} '{}' could not be removed from the running server, a restart applies it: {}", kind, name, e.getMessage());
            }
        }
        for (S spec : added) {
            try {
                add.accept(spec);
            } catch (RuntimeException e) {
                LOG.warn("The MCP {} '{}' could not be added to the running server, a restart applies it: {}", kind, key.apply(spec), e.getMessage());
            }
        }
    }

    /**
     * @return The MCP servers the context has built, of whichever kind
     */
    private List<Object> servers() {
        List<Object> servers = new ArrayList<>();
        addBeans(servers, beanContext.getActiveBeanRegistrations(McpSyncServer.class));
        addBeans(servers, beanContext.getActiveBeanRegistrations(McpAsyncServer.class));
        addBeans(servers, beanContext.getActiveBeanRegistrations(McpStatelessSyncServer.class));
        addBeans(servers, beanContext.getActiveBeanRegistrations(McpStatelessAsyncServer.class));
        return servers;
    }

    private static <T> void addBeans(List<Object> servers, Collection<BeanRegistration<T>> registrations) {
        for (BeanRegistration<T> registration : registrations) {
            servers.add(registration.bean());
        }
    }

    /**
     * Creates registries for the methods of one side of a change, filled as the startup pass fills the registries of
     * the context, through {@link McpExecutableMethodProcessor}'s choice of registry.
     *
     * @param entries The methods
     * @return The registries
     */
    private Registries registries(List<? extends BeanExecutableMethod<?>> entries) {
        Registries registries = new Registries(
            beanContext.createBean(ToolRegistry.class),
            beanContext.createBean(PromptRegistry.class),
            beanContext.createBean(ResourceRegistry.class),
            beanContext.createBean(ResourceTemplateRegistry.class)
        );
        for (BeanExecutableMethod<?> entry : entries) {
            if (!processed(entry)) {
                continue;
            }
            ExecutableMethod<?, ?> method = entry.method();
            if (method.hasStereotype(Prompt.class)) {
                registries.prompts().addMethod(entry.definition(), method);
            } else if (method.hasStereotype(Tool.class)) {
                registries.tools().addMethod(entry.definition(), method);
            } else if (method.hasStereotype(Resource.class)) {
                registries.resources().addMethod(entry.definition(), method);
            } else if (method.hasStereotype(ResourceTemplate.class)) {
                registries.templates().addMethod(entry.definition(), method);
            }
        }
        return registries;
    }

    /**
     * @param entry A method of a change
     * @return Whether the startup pass would have given it to the registries: a method marked for processing at startup
     */
    private static boolean processed(BeanExecutableMethod<?> entry) {
        return entry.definition().requiresMethodProcessing()
            && entry.method().booleanValue(Executable.class, Executable.MEMBER_PROCESS_ON_STARTUP).orElse(false);
    }

    private static boolean hasCompletion(List<? extends BeanExecutableMethod<?>> entries) {
        for (BeanExecutableMethod<?> entry : entries) {
            if (entry.method().hasStereotype(PromptCompletion.class) || entry.method().hasStereotype(ResourceCompletion.class)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The registries one side of a change is built with.
     *
     * @param tools The tools
     * @param prompts The prompts
     * @param resources The resources
     * @param templates The resource templates
     */
    private record Registries(ToolRegistry tools,
                              PromptRegistry prompts,
                              ResourceRegistry resources,
                              ResourceTemplateRegistry templates) {
    }
}
