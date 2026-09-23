from micronaut.context.annotation import Requires
# tag::imports[]
from jakarta.inject import Singleton
from micronaut.mcp.annotations import Resource
# end::imports[]


@Requires(property="spec.name", value="ResourcesTest")
# tag::clazz[]
@Singleton
class Resources:

    @Resource(
        uri="example://hello",
        name="hello",
        title="Hello",
        description="Hello text",
        mimeType="text/plain"
    )
    def hello(self) -> str:
        return "Hello World"
# end::clazz[]
