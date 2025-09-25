package io.micronaut.mcp.server.tools.fetch;

import io.micronaut.json.JsonMapper;
import io.micronaut.jsonschema.utils.JsonSchemaClassPathResourceLoader;
import io.micronaut.mcp.server.tools.search.SearchRequest;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest(startApplication = false)
class FetchResponseTest {
    @Test
    void jsonSchemaForFetchRequest(JsonSchemaClassPathResourceLoader resourceLoader) {
        assertTrue(resourceLoader.jsonSchemaStringForClass(FetchResponse.class).isPresent());
    }


    @Test
    void fetchResponseSerialization(JsonMapper jsonMapper) throws IOException {
        FetchResponse response = FetchResponse.builder()
            .id("micronaut-security")
            .title("Micronaut Security")
            .url("https://micronaut-projects.github.io/micronaut-security/latest/guide")
            .text("Built-in security features. Authentication providers and strategies, Token Propagation.")
            .build();
        String json = jsonMapper.writeValueAsString(response);
        String expected = "{\"id\":\"micronaut-security\",\"title\":\"Micronaut Security\",\"text\":\"Built-in security features. Authentication providers and strategies, Token Propagation.\",\"url\":\"https://micronaut-projects.github.io/micronaut-security/latest/guide\"}";
        assertEquals(expected, json);
    }
}
