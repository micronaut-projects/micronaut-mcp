package example.micronaut.mcp;

import io.micronaut.jsonschema.utils.JsonSchemaClassPathResourceLoader;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(startApplication = false)
class GetAlertInputTest {

    @Test
    void jsonSchemaForGetAlertInput(JsonSchemaClassPathResourceLoader resourceLoader) {
        assertTrue(resourceLoader.jsonSchemaStringForClass(GetAlertInput.class).isPresent());
    }
}