from micronaut.context.annotation import Requires
# tag::imports[]
from jakarta.inject import Singleton
from micronaut.mcp.annotations import Icon, Meta, Tool
# end::imports[]


@Requires(property="spec.name", value="MetadataTest")
# tag::clazz[]
@Singleton
class WeatherTools:

    @Tool
    @Icon(src="https://example.com/sun.svg", mimeType="image/svg+xml", sizes=["any"])
    @Meta(key="com.example/category", value="weather")
    def forecast(self, city: str) -> str:
        return "sunny"
# end::clazz[]
