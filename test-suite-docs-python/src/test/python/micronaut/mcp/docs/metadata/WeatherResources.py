from micronaut.context.annotation import Requires
# tag::imports[]
from jakarta.inject import Singleton
from micronaut.mcp.annotations import Audience, Resource
# end::imports[]


@Requires(property="spec.name", value="MetadataTest")
# tag::clazz[]
@Singleton
class WeatherResources:

    @Resource(uri="weather://stations", size=4, audience=[Audience.ASSISTANT], priority=0.5)
    def stations(self) -> str:
        return "KSEA"
# end::clazz[]
