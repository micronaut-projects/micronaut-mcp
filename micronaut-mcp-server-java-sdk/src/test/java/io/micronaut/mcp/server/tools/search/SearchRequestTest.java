package io.micronaut.mcp.server.tools.search;

import io.micronaut.json.JsonMapper;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

import static org.junit.jupiter.api.Assertions.*;
import io.micronaut.jsonschema.utils.JsonSchemaClassPathResourceLoader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

@MicronautTest
class SearchRequestTest {

    @Test
    void jsonSchemaForSearchRequest(JsonSchemaClassPathResourceLoader resourceLoader) {
        assertTrue(resourceLoader.jsonSchemaStringForClass(SearchRequest.class).isPresent());
    }

    @Test
    void searchRequestSerialization(JsonMapper jsonMapper) throws IOException {
        SearchRequest request = new SearchRequest("security");
        String json = jsonMapper.writeValueAsString(request);
        String expected = "{\"query\":\"security\"}";
        assertEquals(expected, json);
    }
}
