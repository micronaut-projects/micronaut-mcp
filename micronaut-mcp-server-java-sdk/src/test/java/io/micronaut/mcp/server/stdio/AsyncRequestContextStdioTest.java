package io.micronaut.mcp.server.stdio;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.StringUtils;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

@Property(name = AbstractRequestContextStdioTest.PROPERTY, value = StringUtils.TRUE)
@Property(name = "micronaut.mcp.server.transport", value = "STDIO")
@Property(name = "micronaut.mcp.server.reactive", value = StringUtils.TRUE)
@MicronautTest(startApplication = false)
class AsyncRequestContextStdioTest extends AbstractRequestContextStdioTest {
    @Override
    String tool() {
        return "greetOnBlockingExecutor";
    }
}
