package io.micronaut.mcp.docs.resources

import io.micronaut.context.annotation.EachProperty
import io.micronaut.context.annotation.Parameter
import io.micronaut.context.annotation.Requires
import io.micronaut.core.naming.Named

/**
 * Configuration of a PGN file: `pgn.<name>.path` and `pgn.<name>.round`.
 */
@Requires(property = "spec.name", value = "ResourcesFactoryTest")
@EachProperty("pgn")
class PgnFile(@param:Parameter private val name: String) : Named {
    var path: String? = null
    var round: Int? = null

    override fun getName(): String = name
}
