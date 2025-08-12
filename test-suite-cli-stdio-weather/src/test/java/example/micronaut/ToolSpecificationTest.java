package example.micronaut;

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Property(name = "micronaut.mcp.server.type", value = "SYNC")
@MicronautTest(startApplication = false)
class ToolSpecificationTest {

    @Inject
    BeanContext beanContext;

    @Test
    void beansOfTypeToolSpecification() {
        var tools = beanContext.getBeansOfType(McpServerFeatures.SyncToolSpecification.class);
        assertEquals(2, tools.size());
        McpSyncServer server = beanContext.getBean(McpSyncServer.class);
        server.close();
    }
}
