package io.micronaut.mcp.client.langchain4j.stdio;

import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import io.micronaut.context.BeanContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(startApplication = false)
class StdioMcpTransportConfigurationTest {
    @Inject
    BeanContext beanContext;

    @Test
    void unlessYouSpecifyCommandsNoStdioTransport() {
        assertFalse(beanContext.containsBean(StdioMcpTransportConfiguration.class));
        assertFalse(beanContext.containsBean(StdioMcpTransport.class));
    }
}
