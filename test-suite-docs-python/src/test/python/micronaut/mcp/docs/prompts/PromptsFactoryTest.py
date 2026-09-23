import json
from typing import Annotated

from jakarta.inject import Inject
from micronaut.context.annotation import Property
from micronaut.http import HttpRequest
from micronaut.http.client import HttpClient
from micronaut.http.client.annotation import Client
from micronaut.mcp.docs.JsonRpcMessages import EXPECTED_PROMPTS_GET_FACTORY, EXPECTED_PROMPTS_LIST, PROMPTS_GET, PROMPTS_LIST
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test


@Property(name="micronaut.mcp.server.transport", value="HTTP")
@Property(name="spec.name", value="PromptsFactoryTest")
@MicronautTest
class PromptsFactoryTest:
    client: Annotated[HttpClient, Inject, Client("/")]

    @Test
    def prompts_list(self) -> None:
        result = str(self.client.toBlocking().retrieve(HttpRequest.POST("/mcp", PROMPTS_LIST)))
        assert json.loads(result) == json.loads(EXPECTED_PROMPTS_LIST)

    @Test
    def prompts_get(self) -> None:
        result = str(self.client.toBlocking().retrieve(HttpRequest.POST("/mcp", PROMPTS_GET)))
        assert json.loads(result) == json.loads(EXPECTED_PROMPTS_GET_FACTORY)
