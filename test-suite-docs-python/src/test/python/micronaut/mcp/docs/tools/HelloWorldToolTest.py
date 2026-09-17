from typing import Annotated

from jakarta.inject import Inject
from micronaut.context.annotation import Property
from micronaut.http import HttpRequest
from micronaut.http.client import HttpClient
from micronaut.http.client.annotation import Client
from micronaut.mcp.docs.JsonRpcMessages import EXPECTED_HELLO_WORLD_TOOL_ANNOTATIONS, TOOLS_LIST
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test


@Property(name="micronaut.mcp.server.transport", value="HTTP")
@Property(name="spec.name", value="HelloWorldToolTest")
@MicronautTest
class HelloWorldToolTest:
    client: Annotated[HttpClient, Inject, Client("/")]

    @Test
    def tool_annotations(self) -> None:
        json = str(self.client.toBlocking().retrieve(HttpRequest.POST("/mcp", TOOLS_LIST)))
        assert EXPECTED_HELLO_WORLD_TOOL_ANNOTATIONS in json, json
