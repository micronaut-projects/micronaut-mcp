package io.micronaut.mcp.server.tools.search;

import io.micronaut.json.JsonMapper;
import io.micronaut.jsonschema.utils.JsonSchemaClassPathResourceLoader;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest(startApplication = false)
class SearchResponseTest {
    @Test
    void jsonSchemaForSearchRequest(JsonSchemaClassPathResourceLoader resourceLoader) {
        assertTrue(resourceLoader.jsonSchemaStringForClass(SearchResponse.class).isPresent());
    }

    @Test
    void searchResponseTest(JsonMapper jsonMapper) throws IOException {
        SearchResponse results = new SearchResponse(List.of(new SearchResult("mndocs", "Micronaut Docs", "https://micronaut.io")));
        String json = jsonMapper.writeValueAsString(results);
        String expected = "{\"results\":[{\"id\":\"mndocs\",\"title\":\"Micronaut Docs\",\"url\":\"https://micronaut.io\"}]}";
        assertEquals(expected, json);

        results = new SearchResponse(List.of(SearchResult.builder()
            .id("mndocs")
            .title("Micronaut Docs")
            .url("https://micronaut.io")
            .build()));
        assertEquals(expected, jsonMapper.writeValueAsString(results));

        results = new SearchResponse(Collections.emptyList());
        assertEquals("{\"results\":[]}", jsonMapper.writeValueAsString(results));
    }
}
