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

/**
 * JSON RPC pre-defined error codes.
 * @see <a href="https://www.jsonrpc.org/specification">JSON RPC Specification Error Object</a>
 */
public enum ErrorCode {
    PARSE_ERROR(-32700, "Parse error", "Invalid JSON was received by the server. An error occurred on the server while parsing the JSON text."),
    INVALID_REQUEST(-32600, "Invalid Request", "The JSON sent is not a valid Request object."),
    METHOD_NOT_FOUND(-32601, "Method not found", "The method does not exist / is not available."),
    INVALID_PARAMS(-32602, "Invalid params", "Invalid method parameter(s)."),
    INTERNAL_ERROR(-32603, "Internal error", "Internal JSON-RPC error."),
    SERVER_ERROR(-32000, "Server error", "Reserved for implementation-defined server-errors."); // Use for -32000 to -32099

    private final int code;
    @NonNull
    private final String message;

    @NonNull
    private final String meaning;

    ErrorCode(int code, String message, String meaning) {
        this.code = code;
        this.message = message;
        this.meaning = meaning;
    }

    public int getCode() {
        return code;
    }

    @NonNull
    public String getMessage() {
        return message;
    }

    @NonNull
    public String getMeaning() {
        return meaning;
    }

    @Nullable
    public static ErrorCode of(int code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.code == code) {
                return errorCode;
            }
        }
        return null;
    }
}
