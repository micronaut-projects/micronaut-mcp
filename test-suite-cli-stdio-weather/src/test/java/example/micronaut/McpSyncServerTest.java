package example.micronaut;

import io.micronaut.context.BeanContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@MicronautTest(startApplication = false)
class McpServerTest {
    @Inject
    BeanContext beanContext;

    @Test
    void testIsPossibleToInjectABeanOfTypeMcpServer() {
        assertDoesNotThrow(() -> beanContext.getBean(McpSyncServer.class));
    }
}
