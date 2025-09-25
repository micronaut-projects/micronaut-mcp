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
package io.micronaut.mcp.server.tools.search;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.serde.annotation.Serdeable;

import java.util.Objects;

/**
 * @param id a unique ID for the document or search result item
 * @param title human-readable title
 * @param url canonical URL for citation
 */
@Serdeable
public record SearchResult(
    @NonNull String id,
    @NonNull String title,
    @NonNull String url
) {

    /**
     * Create a new builder.
     * @return builder
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link SearchResult}.
     */
    public static final class Builder {
        private String id;
        private String title;
        private String url;

        private Builder() {
        }

        /**
         * Set the id.
         * @param id unique id
         * @return this builder
         */
        @NonNull
        public Builder id(@NonNull String id) {
            this.id = id;
            return this;
        }

        /**
         * Set the title.
         * @param title title
         * @return this builder
         */
        @NonNull
        public Builder title(@NonNull String title) {
            this.title = title;
            return this;
        }

        /**
         * Set the url.
         * @param url canonical url
         * @return this builder
         */
        @NonNull
        public Builder url(@NonNull String url) {
            this.url = url;
            return this;
        }

        /**
         * Build the {@link SearchResult} instance.
         * @return new SearchResult
         */
        @NonNull
        public SearchResult build() {
            return new SearchResult(
                Objects.requireNonNull(id),
                Objects.requireNonNull(title),
                Objects.requireNonNull(url)
            );
        }
    }
}
