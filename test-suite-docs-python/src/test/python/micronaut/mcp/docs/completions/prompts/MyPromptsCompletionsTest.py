import json
from typing import Annotated

from jakarta.inject import Inject
from micronaut.context.annotation import Property
from micronaut.http import HttpRequest
from micronaut.http.client import HttpClient
from micronaut.http.client.annotation import Client
from micronaut.mcp.docs.JsonRpcMessages import EXPECTED_PROMPT_COMPLETION, PROMPT_COMPLETION_REQUEST
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test


@Property(name="micronaut.mcp.server.transport", value="HTTP")
@Property(name="spec.name", value="MyPromptsCompletionsTest")
@MicronautTest
class MyPromptsCompletionsTest:
    client: Annotated[HttpClient, Inject, Client("/")]

    @Test
    def prompt_completion(self) -> None:
        result = str(self.client.toBlocking().retrieve(HttpRequest.POST("/mcp", PROMPT_COMPLETION_REQUEST)))
        assert json.loads(result) == json.loads(EXPECTED_PROMPT_COMPLETION)
