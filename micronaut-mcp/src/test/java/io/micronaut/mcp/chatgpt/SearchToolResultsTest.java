package io.micronaut.mcp.chatgpt;

import io.micronaut.json.JsonMapper;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest(startApplication = false)
class SearchToolResultsTest {

    @Test
    void searchToolResultsTest(JsonMapper jsonMapper) throws IOException {
        SearchToolResults results = new SearchToolResults(List.of(new SearchToolResult("mndocs", "Micronaut Docs", "https://micronaut.io")));
        String json = jsonMapper.writeValueAsString(results);
        String expected = "{\"results\":[{\"id\":\"mndocs\",\"title\":\"Micronaut Docs\",\"url\":\"https://micronaut.io\"}]}";
        assertEquals(expected, json);
    }
}
