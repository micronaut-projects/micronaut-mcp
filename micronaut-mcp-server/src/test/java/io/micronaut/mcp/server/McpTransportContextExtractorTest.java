package io.micronaut.mcp.server;

import io.micronaut.context.BeanContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest(startApplication = false)
class McpTransportContextExtractorTest {

    @Inject
    BeanContext beanContext;

    @Test
    void beanOfTypeMcpTransportContextExtractorExists() {
        assertTrue(beanContext.containsBean(McpTransportContextExtractor.class));
    }

}
