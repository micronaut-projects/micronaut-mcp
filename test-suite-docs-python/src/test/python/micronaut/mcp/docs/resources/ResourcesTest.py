import json
from typing import Annotated

from jakarta.inject import Inject
from micronaut.context.annotation import Property
from micronaut.http import HttpRequest
from micronaut.http.client import HttpClient
from micronaut.http.client.annotation import Client
from micronaut.mcp.docs.JsonRpcMessages import EXPECTED_RESOURCES_LIST_HELLO, EXPECTED_RESOURCES_READ_HELLO, RESOURCES_LIST, RESOURCES_READ_HELLO
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test


@Property(name="micronaut.mcp.server.transport", value="HTTP")
@Property(name="spec.name", value="ResourcesTest")
@MicronautTest
class ResourcesTest:
    client: Annotated[HttpClient, Inject, Client("/")]

    @Test
    def resources_list(self) -> None:
        result = str(self.client.toBlocking().retrieve(HttpRequest.POST("/mcp", RESOURCES_LIST)))
        assert json.loads(result) == json.loads(EXPECTED_RESOURCES_LIST_HELLO)

    @Test
    def resources_read(self) -> None:
        result = str(self.client.toBlocking().retrieve(HttpRequest.POST("/mcp", RESOURCES_READ_HELLO)))
        assert json.loads(result) == json.loads(EXPECTED_RESOURCES_READ_HELLO)
