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

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

/**
 * Represents a JSON-RPC Error object as specified by the JSON-RPC specification.
 *
 * @param code    A Number that indicates the error type that occurred. This MUST be an integer.
 * @param message A String providing a short description of the error. SHOULD be concise and a single sentence.
 * @param data    A Primitive or Structured value that contains additional information about the error. This may be omitted.
 * @param <T>     The type of additional error information
 */
@Serdeable
public record Error<T> (
    int code,
    @NonNull String message,
    @Nullable T data
) {
    public Error(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMessage(), null);
    }
}
