package io.micronaut.mcp.server.tools.search;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

import static org.junit.jupiter.api.Assertions.*;
import io.micronaut.jsonschema.utils.JsonSchemaClassPathResourceLoader;
import org.junit.jupiter.api.Test;

@MicronautTest
class SearchRequestTest {

    @Test
    void jsonSchemaForSearchRequest(JsonSchemaClassPathResourceLoader resourceLoader) {
        assertTrue(resourceLoader.jsonSchemaStringForClass(SearchRequest.class).isPresent());
    }
}
