import json
from typing import Annotated

from jakarta.inject import Inject
from micronaut.context.annotation import Property
from micronaut.http import HttpRequest
from micronaut.http.client import HttpClient
from micronaut.http.client.annotation import Client
from micronaut.mcp.docs.JsonRpcMessages import EXPECTED_TOOLS_CALL, EXPECTED_TOOLS_LIST, TOOLS_CALL, TOOLS_LIST
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test


@Property(name="micronaut.mcp.server.transport", value="HTTP")
@Property(name="spec.name", value="ToolsFactoryTest")
@MicronautTest
class ToolsFactoryTest:
    client: Annotated[HttpClient, Inject, Client("/")]

    @Test
    def tools_list(self) -> None:
        result = str(self.client.toBlocking().retrieve(HttpRequest.POST("/mcp", TOOLS_LIST)))
        assert json.loads(result) == json.loads(EXPECTED_TOOLS_LIST)

    @Test
    def tools_call(self) -> None:
        result = str(self.client.toBlocking().retrieve(HttpRequest.POST("/mcp", TOOLS_CALL)))
        assert json.loads(result) == json.loads(EXPECTED_TOOLS_CALL)
