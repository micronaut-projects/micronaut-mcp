package io.micronaut.mcp.server.stdio;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.StringUtils;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

@Property(name = AbstractRequestContextStdioTest.PROPERTY, value = StringUtils.TRUE)
@Property(name = "micronaut.mcp.server.transport", value = "STDIO")
@MicronautTest(startApplication = false)
class SyncRequestContextStdioTest extends AbstractRequestContextStdioTest {
    @Override
    String tool() {
        return "greet";
    }
}
