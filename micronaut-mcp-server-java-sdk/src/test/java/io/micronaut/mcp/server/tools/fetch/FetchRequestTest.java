package io.micronaut.mcp.server.tools.fetch;

import io.micronaut.jsonschema.utils.JsonSchemaClassPathResourceLoader;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
class FetchRequestTest {

    @Test
    void jsonSchemaForFetchRequest(JsonSchemaClassPathResourceLoader resourceLoader) {
        assertTrue(resourceLoader.jsonSchemaStringForClass(FetchRequest.class).isPresent());
    }
}
