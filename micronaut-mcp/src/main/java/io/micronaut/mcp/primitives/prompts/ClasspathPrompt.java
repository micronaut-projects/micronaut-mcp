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
package io.micronaut.mcp.primitives.prompts;

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link EachProperty} to drive the creation of Prompts via configuration.
 */
@EachProperty("micronaut.mcp.classpath-prompts")
@Internal
public class ClasspathPrompt {

    private String nameQualifier;
    private String name;
    private String title;
    private String description;
    private String path;
    private List<PromptArgument> arguments;

    /**
     *
     * @param name Name Qualifier
     */
    public ClasspathPrompt(@Parameter String name) {
        this.nameQualifier = name;
    }

    /**
     *
     * @return Prompt name
     */
    public String getName() {
        return name;
    }

    /**
     *
     * @param name Prompt Name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     *
      * @return Name Qualifier
     */
    public String getNameQualifier() {
        return nameQualifier;
    }

    /**
     *
     * @param nameQualifier Name Qualifier
     */
    public void setNameQualifier(String nameQualifier) {
        this.nameQualifier = nameQualifier;
    }

    /**
     *
     * @return Prompt Title
     */
    public String getTitle() {
        return title;
    }

    /**
     *
     * @param title Prompt title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     *
     * @return Prompt Description
     */
    public String getDescription() {
        return description;
    }

    /**
     *
     * @param description Prompt description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     *
     * @return Prompt arguments
     */
    public List<PromptArgument> getArguments() {
        return arguments == null ? Collections.emptyList() : arguments;
    }

    /**
     *
     * @param arguments Prompt arguments
     */
    public void setArguments(List<PromptArgument> arguments) {
        this.arguments = arguments;
    }

    /**
     *
     * @return path to the prompt resource. Don't include the `classpath:` prefix
     */
    public String getPath() {
        return path;
    }

    /**
     *
     * @param path path to the prompt resource. Don't include the `classpath:` prefix
     */
    public void setPath(String path) {
        this.path = path;
    }
}
