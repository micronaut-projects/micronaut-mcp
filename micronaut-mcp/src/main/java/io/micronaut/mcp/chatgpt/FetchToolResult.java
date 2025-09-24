package io.micronaut.mcp.chatgpt;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.jsonschema.JsonSchema;

import java.util.Map;

/**
 * @param id a unique ID for the document or search result item
 * @param title a string title for the search result item
 * @param text The full text of the document or item
 * @param url a URL to the document or search result item. Useful for citing specific resources in research.
 * @param metadata an optional key/value pairing of data about the result
 */
@Introspected
@JsonSchema
public record FetchToolResult(
    @NonNull String id,
    @NonNull String title,
    @NonNull String text,
    @NonNull String url,
    @Nullable Map<String, Object> metadata
) {
}
