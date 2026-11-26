package io.micronaut.mcp.server.stateless;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.StringUtils;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "micronaut.mcp.server.reactive", value = StringUtils.TRUE)
@Property(name = AbstractRequestContextStreamingTest.PROPERTY, value = StringUtils.TRUE)
@MicronautTest
class ReactiveRequestContextStreamingTest extends AbstractRequestContextStreamingTest {
}
