package io.micronaut.mcp.client.javasdk;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.Collections;
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

        promptOptional = listPromptsResult.prompts().stream().filter(p -> p.name().equals("dev-default-environment")).findFirst();
        assertTrue(promptOptional.isPresent());
        prompt = promptOptional.get();
        assertEquals("dev-default-environment", prompt.name());
        assertEquals("Development-Default-Environment", prompt.title());
        assertEquals("Modify a Micronaut application to set dev as the default environment", prompt.description());

        McpSchema.GetPromptResult promptResult = client.getPrompt(new McpSchema.GetPromptRequest("introspection-testing", Map.of("className", "SearchResult")));
        assertFalse(promptResult.messages().isEmpty());
        McpSchema.PromptMessage promptMessage = promptResult.messages().get(0);
        assertEquals(McpSchema.Role.USER, promptMessage.role());
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

        promptResult = client.getPrompt(new McpSchema.GetPromptRequest("dev-default-environment", Collections.emptyMap()));
        assertFalse(promptResult.messages().isEmpty());
        promptMessage = promptResult.messages().get(0);
        assertEquals(McpSchema.Role.USER, promptMessage.role());
        assertInstanceOf(McpSchema.TextContent.class, promptMessage.content());
        assertEquals("""
To configure a Micronaut application to define the `dev` environment as the default environment when running
the application locally you need to modify the `main`.

You may need to do replace such a main class:

```
package example.micronaut;

import io.micronaut.runtime.Micronaut;

public class Application {

    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }
}
```

with:

```java
package io.micronaut.documentation.search;

import io.micronaut.context.ApplicationContextBuilder;
import io.micronaut.context.ApplicationContextConfigurer;
import io.micronaut.context.annotation.ContextConfigurer;
import io.micronaut.context.env.Environment;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.runtime.Micronaut;

public class Application {
    @ContextConfigurer
    public static class DefaultEnvironmentConfigurer implements ApplicationContextConfigurer {
        @Override
        public void configure(@NonNull ApplicationContextBuilder builder) {
            builder.defaultEnvironments(Environment.DEVELOPMENT);
        }
    }

    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }
}
```

Modify the current `main` class to configure the dev environment as the default when running locally.
""", ((McpSchema.TextContent) promptMessage.content()).text());
    }
}
