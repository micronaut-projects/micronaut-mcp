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
package io.micronaut.mcp.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Method annotation to define an MCP Tool.
 * <a href="https://modelcontextprotocol.io/specification/2025-06-18/server/tools">Tools</a>
 * Micronaut registers Tools singletons for beans with executable methods (e.g. methods in a class annotated with `@Singleton`) annotated with {@link Tool}.
 *
 * Forked from: https://github.com/quarkiverse/quarkus-mcp-server/blob/main/core/runtime/src/main/java/io/quarkiverse/mcp/server/Tool.java
 */
@Documented
@Retention(RUNTIME)
@Target(METHOD)
@McpPrimitive
public @interface Tool {
    /**
     * Constant value for {@link #name()} indicating that the annotated element's name should be used as-is.
     */
    String ELEMENT_NAME = "<<element name>>";

    /**
     * Each tool must have a unique name. By default, the name is derived from the name of the annotated method.
     * @return {@value #ELEMENT_NAME}
     */
    String name() default ELEMENT_NAME;

    /**
     * @return A human-readable title for the tool.
     */
    String title() default "";

    /**
     * @return A human-readable description of the tool. A hint to the model.
     */
    String description() default "";

    /**
     * Additional hints for clients.
     * <p>
     * Note that the default value of this annotation member is ignored. In other words, the annotations have to be declared
     * explicitly in order to be included in Tool metadata.
     * @return Additional hints for clients.
     */
    Annotations annotations() default @Annotations;

    /**
     * Additional hints for clients.
     * <a href="https://modelcontextprotocol.io/specification/2025-06-18/schema#toolannotations">Tool Annotations</a>
     */
    @Retention(RUNTIME)
    @Target(ElementType.ANNOTATION_TYPE)
    @interface Annotations {

        /**
         * @return A human-readable title for the tool.
         */
        String title() default "";

        /**
         * @return If true, the tool does not modify its environment.
         */
        boolean readOnlyHint() default false;

        /**
         * @return If true, the tool may perform destructive updates to its environment. If false, the tool performs only additive
         * updates.
         */
        boolean destructiveHint() default true;

        /**
         * @return If true, calling the tool repeatedly with the same arguments will have no additional effect on the its environment.
         */
        boolean idempotentHint() default false;

        /**
         * @return If true, this tool may interact with an "open world" of external entities. If false, the tool's domain of interaction
         * is closed.
         */
        boolean openWorldHint() default true;

        /**
         * @return It tells the client/agent whether the tool's result can be surfaced to the end user immediately (as the assistant reply) without another model turn.
         */
        boolean returnDirect() default false;
    }
}
