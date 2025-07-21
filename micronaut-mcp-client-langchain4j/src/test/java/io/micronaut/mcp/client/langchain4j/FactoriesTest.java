package io.micronaut.mcp.client.langchain4j;

import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.service.tool.ToolProvider;
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest(startApplication = false)
@Property(name = "langchain4j.mcp.client.transport.stdio.commands[0]", value = "java")
@Property(name = "langchain4j.mcp.client.transport.stdio.commands[1]", value = "-jar")
@Property(name = "langchain4j.mcp.client.transport.stdio.commands[2]", value = "build/libs/mcp-server-0.1-all.jar")
class FactoriesTest {
    @Inject
    BeanContext beanContext;
    @Test
    void beansInstantiatedInFactories() {
        assertTrue(beanContext.containsBean(McpTransport.class));
        assertTrue(beanContext.containsBean(McpClient.class));
        assertTrue(beanContext.containsBean(ToolProvider.class));

        StdioMcpTransportConfiguration config = assertDoesNotThrow(()  -> beanContext.getBean(StdioMcpTransportConfiguration.class));
        assertEquals("java -jar build/libs/mcp-server-0.1-all.jar", String.join(" ", config.getCommands()));
    }
}
