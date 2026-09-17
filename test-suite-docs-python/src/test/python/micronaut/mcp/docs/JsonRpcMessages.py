"""JSON-RPC messages exchanged with the MCP server by the documentation tests."""

FEN = """r1bqk2r/ppp2ppp/2n5/1BbpP3/3Nn3/8/PPP2PPP/RNBQK2R w KQkq - 1 8"""

TOOLS_LIST = """{"jsonrpc":"2.0","id":3,"method":"tools/list","params":{"_meta":{"progressToken":3}}}"""

TOOLS_CALL = """{"jsonrpc":"2.0","id":4,"method": "tools/call","params": {"name": "fenEvaluation","arguments": {"fen": "r1bqk2r/ppp2ppp/2n5/1BbpP3/3Nn3/8/PPP2PPP/RNBQK2R w KQkq - 1 8"},"_meta": {"progressToken": 0}}}"""

TOOLS_CALL_SNAKE_CASE = """{"jsonrpc":"2.0","id":4,"method": "tools/call","params": {"name": "fen_evaluation","arguments": {"fen": "r1bqk2r/ppp2ppp/2n5/1BbpP3/3Nn3/8/PPP2PPP/RNBQK2R w KQkq - 1 8"},"_meta": {"progressToken": 0}}}"""

# The tool name defaults to the method name, which is snake_case in Python: `fen_evaluation`
EXPECTED_TOOLS_LIST_SNAKE_CASE = """{"jsonrpc":"2.0","id":3,"result":{"tools":[{"name":"fen_evaluation","description":"Evaluate a chess position using a FEN string.","inputSchema":{"type":"object","properties":{"fen":{"type":"string"}},"required":["fen"]}}]}}"""

EXPECTED_TOOLS_LIST = """{"jsonrpc":"2.0","id":3,"result":{"tools":[{"name":"fenEvaluation","description":"Evaluate a chess position using a FEN string.","inputSchema":{"type":"object","properties":{"fen":{"type":"string"}},"required":["fen"]}}]}}"""

EXPECTED_TOOLS_CALL = """{"jsonrpc":"2.0","id":4,"result":{"content":[{"type":"text","text":"+0.12"}],"isError":false}}"""

EXPECTED_TOOLS_LIST_WITH_INPUT_SCHEMA = """{"jsonrpc":"2.0","id":3,"result":{"tools":[{"name":"fen_evaluation","description":"Evaluate a chess position using a FEN string.","inputSchema":{"$schema":"https://json-schema.org/draft/2020-12/schema","title":"FenEvaluationRequest","type":"object","properties":{"fen":{"description":"A Chess position in Forsyth–Edwards Notation","type":"string"}},"$id":"http://localhost:8080/schemas/fen-evaluation-request.schema.json"}}]}}"""

EXPECTED_TOOLS_LIST_WITH_INPUT_AND_OUTPUT_SCHEMA = """{"jsonrpc":"2.0","id":3,"result":{"tools":[{"name":"fen_evaluation","description":"Evaluate a chess position using a FEN string.","inputSchema":{"$schema":"https://json-schema.org/draft/2020-12/schema","title":"FenEvaluationRequest","type":"object","properties":{"fen":{"description":"A Chess position in Forsyth–Edwards Notation","type":"string"}},"$id":"http://localhost:8080/schemas/fen-evaluation-request.schema.json"},"outputSchema":{"$schema":"https://json-schema.org/draft/2020-12/schema","title":"FenEvaluationResponse","type":"object","properties":{"evaluation":{"type":"string","minLength":1},"fen":{"type":"string","minLength":1}},"required":["fen","evaluation"],"$id":"http://localhost:8080/schemas/fen-evaluation-response.schema.json"}}]}}"""

EXPECTED_TOOLS_CALL_WITH_OUTPUT_SCHEMA = """{"jsonrpc":"2.0","id":4,"result":{"content":[{"type":"text","text":"{\\"fen\\":\\"r1bqk2r/ppp2ppp/2n5/1BbpP3/3Nn3/8/PPP2PPP/RNBQK2R w KQkq - 1 8\\",\\"evaluation\\":\\"+0.12\\"}"}],"isError":false,"structuredContent":{"evaluation":"+0.12","fen":"r1bqk2r/ppp2ppp/2n5/1BbpP3/3Nn3/8/PPP2PPP/RNBQK2R w KQkq - 1 8"}}}"""

EXPECTED_HELLO_WORLD_TOOL_ANNOTATIONS = ""","annotations":{"title":"Hello World","readOnlyHint":true,"destructiveHint":false,"idempotentHint":true,"openWorldHint":false,"returnDirect":true}}]}}"""

SEARCH_TOOL_CALL = """{"jsonrpc":"2.0","id":20,"method":"tools/call","params":{"name":"search","arguments":{"query":"security"}}}"""

EXPECTED_SEARCH_TOOL_CALL = """{"jsonrpc":"2.0","id":20,"result":{"content":[{"type":"text","text":"{\\"results\\":[{\\"id\\":\\"micronaut-security\\",\\"title\\":\\"Micronaut Security\\",\\"url\\":\\"https://micronaut-projects.github.io/micronaut-security/latest/guide\\"}]}"}],"isError":false,"structuredContent":{"results":[{"id":"micronaut-security","title":"Micronaut Security","url":"https://micronaut-projects.github.io/micronaut-security/latest/guide"}]}}}"""

FETCH_TOOL_CALL = """{"jsonrpc":"2.0","id":20,"method":"tools/call","params":{"name":"fetch","arguments":{"id":"micronaut-security"}}}"""

EXPECTED_FETCH_TOOL_CALL = """{"jsonrpc":"2.0","id":20,"result":{"content":[{"type":"text","text":"{\\"id\\":\\"micronaut-security\\",\\"title\\":\\"Micronaut Security\\",\\"text\\":\\"Built-in security features. Authentication providers and strategies, Token Propagation.\\",\\"url\\":\\"https://micronaut-projects.github.io/micronaut-security/latest/guide\\"}"}],"isError":false,"structuredContent":{"id":"micronaut-security","title":"Micronaut Security","text":"Built-in security features. Authentication providers and strategies, Token Propagation.","url":"https://micronaut-projects.github.io/micronaut-security/latest/guide"}}}"""

PROMPTS_LIST = """{"jsonrpc":"2.0","id":0,"method":"prompts/list","params":{}}"""

PROMPTS_GET = """{"jsonrpc":"2.0","id":0,"method":"prompts/get","params":{"name":"chess-statistics","arguments":{"name":"sergio"}}}"""

EXPECTED_PROMPTS_LIST = """{"jsonrpc":"2.0","id":0,"result":{"prompts":[{"name":"chess-statistics","description":"Displays statistics for chess games","arguments":[{"name":"name","description":"Player Name","required":true}]}]}}"""

EXPECTED_PROMPTS_GET = """{"jsonrpc":"2.0","id":0,"result":{"messages":[{"role":"assistant","content":{"type":"text","text":"You generate chess statistics for sergio ...."}}]}}"""

EXPECTED_PROMPTS_GET_FACTORY = """{"jsonrpc":"2.0","id":0,"result":{"description":"Chess statistics","messages":[{"role":"assistant","content":{"type":"text","text":"You generate chess statistics for sergio ...."}}]}}"""

RESOURCES_LIST = """{"jsonrpc":"2.0","id":2,"method":"resources/list","params":{"_meta":{"progressToken":2}}}"""

RESOURCES_TEMPLATES_LIST = """{"jsonrpc":"2.0","id":6,"method":"resources/templates/list","params":{"_meta":{"progressToken":6}}}"""

RESOURCES_READ_HELLO = """{"jsonrpc":"2.0","id":9,"method":"resources/read","params":{"_meta":{"progressToken":9},"uri":"example://hello"}}"""

RESOURCES_READ_ROUND_2 = """{"jsonrpc":"2.0","id":9,"method":"resources/read","params":{"_meta":{"progressToken":9},"uri":"pgn://round/2"}}"""

RESOURCES_READ_ROUND_99 = """{"jsonrpc":"2.0","id":9,"method":"resources/read","params":{"_meta":{"progressToken":9},"uri":"pgn://round/99"}}"""

EXPECTED_RESOURCES_LIST_HELLO = """{"jsonrpc":"2.0","id":2,"result":{"resources":[{"uri":"example://hello","name":"hello","title":"Hello","description":"Hello text","mimeType":"text/plain"}]}}"""

EXPECTED_RESOURCES_READ_HELLO = """{"jsonrpc":"2.0","id":9,"result":{"contents":[{"uri":"example://hello","mimeType":"text/plain","text":"Hello World"}]}}"""

EXPECTED_RESOURCES_LIST_PGN = """{"jsonrpc":"2.0","id":2,"result":{"resources":[
          {"uri":"pgn://round/1","name":"round1PgnFideWCC2024","title":"PGN of the Round 1 game of the World Chess Championship","description":"PGN of the Round 1 game of the World Chess Championship between Ding Liren and Gukesh Dommaraju","mimeType":"application/x-chess-pgn","size":14533},
          {"uri":"pgn://round/2","name":"round2PgnFideWCC2024","title":"PGN of the Round 2 game of the World Chess Championship","description":"PGN of the Round 2 game of the World Chess Championship between Ding Liren and Gukesh Dommaraju","mimeType":"application/x-chess-pgn","size":9378},
          {"uri":"pgn://round/3","name":"round3PgnFideWCC2024","title":"PGN of the Round 3 game of the World Chess Championship","description":"PGN of the Round 3 game of the World Chess Championship between Ding Liren and Gukesh Dommaraju","mimeType":"application/x-chess-pgn","size":16296}
        ]}}"""

EXPECTED_RESOURCES_TEMPLATES_LIST = """{"jsonrpc":"2.0","id":6,"result":{"resourceTemplates":[{"uriTemplate":"pgn://round/{round}","name":"2024ChessChampionshipRoundPgn","title":"PGN of a round World Chess Championship 2024","description":"Given a round, it returns a PGN of the World Chess Championship 2024 between Ding Liren and Gukesh Dommaraju","mimeType":"application/x-chess-pgn"}]}}"""

PROMPT_COMPLETION_REQUEST = """{"jsonrpc":"2.0","id":1,"method":"completion/complete","params":{"ref":{"type":"ref/prompt","name":"code_review"},"argument":{"name":"language","value":"py"}}}"""

EXPECTED_PROMPT_COMPLETION = """{"jsonrpc":"2.0","id":1,"result":{"completion":{"values":["python","pytorch","pyside"],"total":3,"hasMore":false}}}"""

RESOURCE_COMPLETION_REQUEST = """{"jsonrpc":"2.0","id":1,"method":"completion/complete","params":{"ref":{"type":"ref/resource","uri":"file:///home/user/documents/{fileName}"},"argument":{"name":"fileName","value":"rep"}}}"""

EXPECTED_RESOURCE_COMPLETION = """{"jsonrpc":"2.0","id":1,"result":{"completion":{"values":["report.pdf"],"total":1,"hasMore":false}}}"""

RESOURCES_READ_REPORT = """{"jsonrpc":"2.0","id":9,"method":"resources/read","params":{"uri":"file:///home/user/documents/report.pdf"}}"""

EXPECTED_RESOURCES_READ_REPORT = """{"jsonrpc":"2.0","id":9,"result":{"contents":[{"uri":"file:///home/user/documents/report.pdf","mimeType":"text/plain","text":"Report PDF"}]}}"""
