package io.micronaut.mcp.server.later;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.StringUtils;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "micronaut.mcp.server.reactive", value = StringUtils.TRUE)
@Property(name = "spec.name", value = "LaterResultsTest")
@MicronautTest
class ReactiveServerLaterResultsTest extends AbstractLaterResultsTest {
}
