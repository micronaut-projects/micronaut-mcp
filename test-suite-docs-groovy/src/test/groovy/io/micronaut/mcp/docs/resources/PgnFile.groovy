package io.micronaut.mcp.docs.resources

import io.micronaut.context.annotation.EachProperty
import io.micronaut.context.annotation.Parameter
import io.micronaut.context.annotation.Requires
import io.micronaut.core.naming.Named

/**
 * Configuration of a PGN file: {@code pgn.<name>.path} and {@code pgn.<name>.round}.
 */
@Requires(property = "spec.name", value = "ResourcesFactorySpec")
@EachProperty("pgn")
class PgnFile implements Named {
    private final String name
    String path
    Integer round

    PgnFile(@Parameter String name) {
        this.name = name
    }

    @Override
    String getName() {
        name
    }
}
