package example.micronaut;

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.StringUtils;
import io.micronaut.mcp.conf.McpServerConfiguration;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "micronaut.mcp.server.enabled", value = StringUtils.FALSE)
@MicronautTest(startApplication = false)
public class McpServerDisabledTest {

    @Inject
    BeanContext beanContext;

    @Test
    void mcpServerDisabled() {
        assertFalse(beanContext.containsBean(McpServerConfiguration.class));
    }
}
