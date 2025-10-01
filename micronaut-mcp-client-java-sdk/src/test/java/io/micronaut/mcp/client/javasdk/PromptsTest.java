package io.micronaut.mcp.client.javasdk;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
class PromptsTest {

    @Test
    void introspectionTesting(McpSyncClient client) {
        McpSchema.ListPromptsResult listPromptsResult = assertDoesNotThrow(() -> client.listPrompts());
        Optional<McpSchema.Prompt> promptOptional = listPromptsResult.prompts().stream().filter(prompt -> prompt.name().equals("introspection-testing")).findFirst();
        assertTrue(promptOptional.isPresent());
        McpSchema.Prompt prompt = promptOptional.get();
        assertEquals("introspection-testing", prompt.name());
        assertEquals("Introspection-Testing", prompt.title());
        assertEquals("Test whether a class is introspected in a Micronaut application", prompt.description());
        assertFalse(prompt.arguments().isEmpty());
        assertEquals(1, prompt.arguments().size());
        assertEquals("className", prompt.arguments().get(0).name());
        assertNull(prompt.arguments().get(0).title());
        assertEquals("The class for which you want to test introspection", prompt.arguments().get(0).description());
        McpSchema.GetPromptResult promptResult = client.getPrompt(new McpSchema.GetPromptRequest("introspection-testing", Map.of("className", "SearchResult")));
        assertFalse(promptResult.messages().isEmpty());
        McpSchema.PromptMessage promptMessage = promptResult.messages().get(0);
        //assertEquals(McpSchema.Role.USER, promptMessage.role());
        assertInstanceOf(McpSchema.TextContent.class, promptMessage.content());
        assertEquals("""
Please, write a test to verify introspection for SearchResult

The following tests shows how to test if a class is introspected. The following test verifies if the `CreateGame` class is annotated with `@Introspected`.

```java
@Test
void isAnnotatedWithIntrospected() {
    assertDoesNotThrow(() -> BeanIntrospection.getIntrospection(CreateGame.class));
}
```
""", ((McpSchema.TextContent) promptMessage.content()).text());
    }
}
