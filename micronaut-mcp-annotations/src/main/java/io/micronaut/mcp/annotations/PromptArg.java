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

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Optional;

/**
 * Annotates a parameter of a {@link Prompt} method.
 * Forked from: https://raw.githubusercontent.com/quarkiverse/quarkus-mcp-server/refs/heads/main/core/runtime/src/main/java/io/quarkiverse/mcp/server/PromptArg.java
 */
@Documented
@Retention(RUNTIME)
@Target(ElementType.PARAMETER)
public @interface PromptArg {

    /**
     * Constant value for {@link #name()} indicating that the annotated element's name should be used as-is.
     */
    String ELEMENT_NAME = "<<element name>>";

    String name() default ELEMENT_NAME;

    String description() default "";

    /**
     * An argument is required by default. However, if the annotated type is {@link Optional} and no annotation value is set
     * explicitly then the argument is not required.
     * @return Wether the argument is required
     */
    boolean required() default true;

    /**
     * @return The default value is used when an MCP client does not provide an argument value.
     */
    String defaultValue() default "";
}
