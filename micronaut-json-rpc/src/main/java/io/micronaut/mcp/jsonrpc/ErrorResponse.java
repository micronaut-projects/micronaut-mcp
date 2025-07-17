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
package io.micronaut.mcp.jsonrpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

/**
 * JSON-RPC Successful Response.
 * @see <a href="https://www.jsonrpc.org/specification">JSON-RPC</a>
 *
 * @param jsonrpc Version of the JSON-RPC protocol.
 * @param error Error representation
 * @param id This member is REQUIRED. It MUST be the same as the value of the id member in the Request Object. If there was an error in detecting the id in the Request object, it MUST be Null.
 * @param <I> The type of the id field (String, Number, or null).
 * @param <T> The type of additional error information
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
@Serdeable
public record ErrorResponse<I, T>(
    @NonNull String jsonrpc,
    @NonNull Error<T> error,
    @Nullable I id) {

    public ErrorResponse(Error<T> error, I id) {
        this(Request.VERSION_2_0, error, id);
    }

    public ErrorResponse(Error<T> error) {
        this(Request.VERSION_2_0, error, null);
    }
}
