package io.micronaut.mcp.server.stdio;

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@Property(name = "micronaut.mcp.server.transport", value = "STDIO")
@MicronautTest(startApplication = false)
class StdioServerTransportProviderFactoryTest {

    @Inject
    BeanContext beanContext;

    /**
     * Obtaining the bean only constructs the SDK provider, which stores {@link System#in} and {@link System#out}; it starts
     * reading them only once a session factory is set, which this test never does.
     */
    @Test
    void stdioTransportProviderIsDecoratedWithGracefulListing() {
        McpServerTransportProvider provider = beanContext.getBean(McpServerTransportProvider.class);
        assertInstanceOf(GracefulListingServerTransportProvider.class, provider);
    }
}
