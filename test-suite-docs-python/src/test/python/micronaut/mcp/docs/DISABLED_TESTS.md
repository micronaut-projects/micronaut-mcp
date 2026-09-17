# Python Docs Disabled Test Inventory

This file tracks the Python documentation examples of Micronaut MCP under `test-suite-docs-python/src/test/python/micronaut/mcp/docs`
that are disabled, or that carry a workaround because the direct port of the Java example does not compile or does not behave
like the Java example yet (Python compiler gaps). It is the bug-fixing task list for the Python compiler
(`micronaut-inject-python` / `micronaut-context-python`); every row references a `TODO(python)` comment in the sources.

The Python examples are compiled by every build and their tests run with `./gradlew pythonCheck -Ppython-ci`
(the "Python CI" GitHub workflow).

## Reconciliation

- Last generated active `@Disabled` count: 0.
- Last generated command: `rg -n "@Disabled\(" test-suite-docs-python/src/test/python`.
- Last full-suite command: `./gradlew :test-suite-docs-python:test -Ppython-ci`.
- Last full-suite result: see the PR description.

## Migration Rules

- The snippet classes live in `io.micronaut.mcp.docs` in every language (`micronaut.mcp.docs` in Python): a Python source
  package cannot be an imported Java package itself (the compiler generates the `__init__.py` shims of the imported
  `micronaut.mcp.annotations` / `micronaut.mcp.server...` packages).
- The MCP Java SDK types live in the `io.modelcontextprotocol` package, which cannot be imported directly from Python yet
  (`io` is the standard library module): they are imported by name inside a `try:` block with an `except ImportError`
  fallback to the generated `modelcontextprotocol...` shim packages. The `try:` block sits outside the `imports` snippet tag.
- The MCP annotations (`@Tool`, `@Prompt`, `@Resource`, `@ResourceTemplate`, `@PromptCompletion`, `@ResourceCompletion`) are
  `@Executable(processOnStartup = true)` stereotypes and work on the methods of a Python `@Singleton` like on Java methods.
  The tool name defaults to the Python method name verbatim (`fen_evaluation`); the guide carries a `[.lang-python]` note.
- The JSON schema request/response records are `@JsonSchema @Serdeable @dataclass` classes; the attribute docstring becomes
  the property description and `Annotated[str, NonNull, NotBlank]` yields the same `required` / `minLength` schema as Java.
  `@Serdeable` is required (not a compiler gap): Micronaut Serde only deserializes plain `@Introspected` beans of the
  `io.micronaut` package by default, which the Python `micronaut.mcp.docs` classes are not (the original Java records of the
  module tests relied on that default). The four suites now use `@Serdeable`, like an application in its own package must.
- Every Python test is a `@MicronautTest` class calling the MCP server over HTTP (`POST /mcp`) with the JSON-RPC messages of
  `JsonRpcMessages.py`, exactly like the Java suite; the responses are compared with `json.loads(...)`.

## Active `@Disabled` Tests

None.

## Workarounds in the Sources

| Source | Reason |
| --- | --- |
| `resources/PgnFile.py` | The Java `PgnFile` implements `io.micronaut.core.naming.Named`. A Python class with a `name` attribute cannot implement it: the compiler generates both a property accessor `getName()` for the attribute and a bridge `getName()` for the interface method ("method getName() is already defined in class ... PgnFile"). The Python class keeps the `name` attribute and drops the interface (nothing in the example relies on it). |

| `tools/search/MicronautModulesSearchTest.py`, `tools/fetch/MicronautModulesFetchTest.py` | The Java tests inject the `SearchTool` / `FetchTool` bean and assert its default `getName()` / `getTitle()`. Injecting the Python bean into a Python test yields the Python object, which has no default interface methods (`AttributeError: 'MicronautModulesSearch' object has no attribute 'getName'`). The Python tests assert the same name/title/description through the `tools/list` response of the server instead, where the Java factory reads them from the bean's Java stub. |

## `java.type` usages

None.
