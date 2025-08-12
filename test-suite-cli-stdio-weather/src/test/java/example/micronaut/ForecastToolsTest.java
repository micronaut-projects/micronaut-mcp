package example.micronaut;

import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.core.annotation.NonNull;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(
        named = "LANGCHAIN4J_OPEN_AI_API_KEY",
        matches = ".+"
)
@EnabledIf(value = "example.micronaut.ForecastToolsTest#mcpServerJarExists", disabledReason = "JAR does not exist in build/libs/test-suite-cli-stdio-weather-0.1-all.jar")
@Property(name = "spec.name", value = "ForecastToolsTest")
class ForecastToolsTest {
    public static final String MODULE = "test-suite-cli-stdio";
    public static final String JAR_PATH = "build/libs/test-suite-cli-stdio-weather-0.1-all.jar";

    static boolean mcpServerJarExists() {
        return new java.io.File(MODULE + JAR_PATH).exists() ||
            new java.io.File(JAR_PATH).exists();
    }

    interface Assistant {
        String chat(String message);
    }

    @Requires(property = "spec.name", value = "ForecastToolsTest")
    @Singleton
    static class McpClientBeanCreatedEventListener implements BeanCreatedEventListener<OpenAiChatModel.OpenAiChatModelBuilder> {

        @Override
        public OpenAiChatModel.OpenAiChatModelBuilder onCreated(@NonNull BeanCreatedEvent<OpenAiChatModel.OpenAiChatModelBuilder> event) {
            var builder = event.getBean();
            builder.logRequests(true);
            builder.logResponses(true);
            return builder;
        }
    }


    @Test
    void toolsExposedViaAnMcpServer() throws Exception {
        String path = null;
        if (new java.io.File(MODULE + JAR_PATH).exists()) {
            path = MODULE + JAR_PATH;
        } else if (new java.io.File(JAR_PATH).exists()) {
            path = JAR_PATH;
        }
        ApplicationContext applicationContext = ApplicationContext.run(
            Map.of(
                "spec.name",  "ForecastToolsTest",
                    "micronaut.server.port", "-1",
                   "micronaut.mcp.server.type", "SYNC",
                    "micronaut.mcp.server.info.name", "weather-mcp-server",
                    "micronaut.mcp.server.info.version", "0.0.1",
                    "langchain4j.open-ai.chat-model.model-name", "gpt-4.1",
                    "langchain4j.open-ai.api-key", System.getenv("LANGCHAIN4J_OPEN_AI_API_KEY"),
                    "langchain4j.mcp.client.transport.stdio.commands[0]", "java",
                    "langchain4j.mcp.client.transport.stdio.commands[1]", "-jar",
                    "langchain4j.mcp.client.transport.stdio.commands[2]", path
            )
        );
        ChatModel chatModel = applicationContext.getBean(ChatModel.class);
        assertNotNull(chatModel);
        McpClient mcpClient = applicationContext.getBean(McpClient.class);
        ToolProvider toolProvider = applicationContext.getBean(ToolProvider.class);

        var assistant = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .toolProvider(toolProvider)
                .build();

        assertDoesNotThrow(() -> {
            List<dev.langchain4j.agent.tool.ToolSpecification> tools = mcpClient.listTools();
            assertNotNull(tools, "Tool list should not be null");
            assertFalse(tools.isEmpty(), "Tool list should not be empty");
            // Add more specific assertions if needed, e.g., check tool names
            assertTrue(tools.stream().anyMatch(t -> "getAlerts".equals(t.name())),
                    "Should find the 'getAlerts' tool");

            assertTrue(tools.stream().anyMatch(t -> "getForecast".equals(t.name())),
                "Get weather forecast for a location.");
        }, "Listing tools should not throw an exception");

        String question = "How is the weather in NY?";
        String response = assertDoesNotThrow(() -> assistant.chat(question),
                "getAlerts request should not throw an exception");
        assertNotNull(response, "Response should not be null");
        assertFalse(response.isBlank(), "Response should not be blank");
        assertTrue(response.toLowerCase().contains("new york"),
                "Response should contain the weather information (New York)");
        String forecastQuestion = "How is the weather forecast in New York City?";
        response = assertDoesNotThrow(() -> assistant.chat(forecastQuestion),
            "getForecast request should not throw an exception");
        assertNotNull(response, "Response should not be null");
        assertFalse(response.isBlank(), "Response should not be blank");

        applicationContext.close();
    }
}
