package io.micronaut.mcp.server.utils;

public final class JsonRpcMessages {
    private JsonRpcMessages() {
    }

    public static final String RESOURCE_COMPLETION_REQUEST = """
        {"jsonrpc":"2.0","id": 1,"method":"completion/complete","params":{"ref":{"type":"ref/resource","uri":"file:///home/user/documents/{fileName}"},"argument":{"name": "fileName","value": "rep"}}}
        """;

    public static final String RESOURCE_COMPLETION_RESPONSE = """
        {
          "jsonrpc": "2.0",
          "id": 1,
          "result": {
            "completion": {
              "values": [
                "report.pdf"
              ],
              "total": 1,
              "hasMore": false
            }
          }
        }
    """;
    public static final String PROMPT_COMPLETION_REQUEST = """
        {"jsonrpc":"2.0","id": 1,"method":"completion/complete","params":{"ref":{"type": "ref/prompt","name": "code_review"},"argument":{"name": "language","value": "py"}}}""";

    public static final String PROMPT_COMPLETION_RESPONSE = """
        {
          "jsonrpc": "2.0",
          "id": 1,
          "result": {
            "completion": {
              "values": ["python", "pytorch", "pyside"],
              "total": 3,
              "hasMore": false
            }
          }
        }""";

    public static final String PROMPTS_GET = """
        {"jsonrpc":"2.0","id":0,"method": "prompts/get","params":{"name":"chess-statistics","arguments":{"name": "sergio"}}}""";

    public static final String PROMPTS_LIST = """
             {"jsonrpc":"2.0","id":0,"method":"prompts/list","params":{}}""";

    public static final String EXPECTED_PROMPTS = """
        {"jsonrpc":"2.0","id":0,"result":{"prompts":[{"name":"chess-statistics","description":"Displays statistics for chess games","arguments":[{"name":"name","description":"Player Name","required":true}]}]}}""";

    public static final String INITIALIZE = """
             {"jsonrpc":"2.0","id":0,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{"sampling":{},"elicitation":{},"roots":{"listChanged":true}},"clientInfo":{"name":"mcp-inspector","version":"0.16.3"}}}""";

    public static final String INITIALIZED = """
        {"jsonrpc": "2.0", "method": "notifications/initialized"}""";

    public static final String EXPECTED_INITIALIZATION = """
            {
              "jsonrpc":"2.0",
              "id":0,
              "result": {
                "protocolVersion":"2025-06-18",
                 "capabilities": {},
                 "serverInfo": {
                   "name": "mcp-server",
                   "version": "0.0.1"
                 }
               }
            }""";


    public static final String EXPECTED_RESOURCES_LIST_TEMPLATES = """
        {
          "jsonrpc": "2.0",
          "id": 6,
          "result": {
            "resourceTemplates": [
              {
                "uriTemplate": "pgn://round/{round}",
                "name": "2024ChessChampionshipRoundPgn",
                "title": "PGN of a round World Chess Championship 2024",
                "description": "Given a round, it returns a PGN of the World Chess Championship 2024 between Ding Liren and Gukesh Dommaraju",
                "mimeType": "application/x-chess-pgn"
              }
            ]
          }
        }
        """;

    public static final String RESOURCES_LIST = """
        {"jsonrpc":"2.0","id":2,"method":"resources/list","params":{"_meta":{"progressToken":2}}}""";

    public static final String RESOURCES_TEMPLATES_LIST = """
        {"jsonrpc":"2.0","id":6,"method":"resources/templates/list","params":{"_meta":{"progressToken":6}}}""";

    public static final String RESOURCES_READ = """
        {"jsonrpc":"2.0","id":9,"method":"resources/read","params":{"_meta":{"progressToken":9},"uri":"pgn://round/2"}}""";

    // Constants for @Resource annotation tests
    public static final String RESOURCES_READ_HELLO = """
        {"jsonrpc":"2.0","id":9,"method":"resources/read","params":{"_meta":{"progressToken":9},"uri":"example://hello"}}""";

    public static final String RESOURCES_READ_ZIP = """
        {"jsonrpc":"2.0","id":9,"method":"resources/read","params":{"_meta":{"progressToken":9},"uri":"example://zip"}}""";

    public static final String EXPECTED_RESOURCES_READ_HELLO = """
        {
          "jsonrpc": "2.0",
          "id": 9,
          "result": {
            "contents": [
              {
                "uri": "example://hello",
                "mimeType": "text/plain",
                "text":"Hello World"
              }
            ]
          }
        }
        """;

    public static final String EXPECTED_RESOURCES_LIST_ANNOTATIONS = """
        {
          "jsonrpc": "2.0",
          "id": 2,
          "result": {
            "resources": [
              {
                "uri": "example://hello",
                "name": "hello",
                "title": "Hello",
                "description": "Hello text",
                "mimeType": "text/plain"
              }
            ]
          }
        }""";


    public static final String PING = """
        {"jsonrpc":"2.0","method":"ping","id":"123"}""";

    public static final String PONG = """
        {"jsonrpc":"2.0","result":{},"id":"123"}""";

    public static final String EXPECTED_INITIALIZATION_2024_WITH_LOGGING = """
            {
              "jsonrpc":"2.0",
              "id":0,
              "result": {
                "protocolVersion":"2024-11-05",
                 "capabilities": {"logging":{}},
                 "serverInfo": {
                   "name": "mcp-server",
                   "version": "0.0.1"
                 }
               }
            }""";
    public static final String EXPECTED_INITIALIZATION_2024 = """
            {
              "jsonrpc":"2.0",
              "id":0,
              "result": {
                "protocolVersion":"2024-11-05",
                 "capabilities": {},
                 "serverInfo": {
                   "name": "mcp-server",
                   "version": "0.0.1"
                 }
               }
            }""";

    public static final String EXPECTED_TOOLS_CALL = """
        {
          "jsonrpc": "2.0",
          "id": 4,
          "result": {
            "content": [
              {
                "type": "text",
                "text": "+0.12"
              }
            ],
            "isError": false
          }
        }""";

    public static final String EXPECTED_TOOLS_CALL_OUT_SCHEMA = """
        {
          "jsonrpc": "2.0",
          "id": 4,
          "result": {
            "content": [
              {
                "type": "text",
                "text": "{\\"fen\\":\\"r1bqk2r/ppp2ppp/2n5/1BbpP3/3Nn3/8/PPP2PPP/RNBQK2R w KQkq - 1 8\\",\\"evaluation\\":\\"+0.12\\"}"
              }
            ],
            "isError": false,
            "structuredContent": {
              "evaluation": "+0.12",
              "fen": "r1bqk2r/ppp2ppp/2n5/1BbpP3/3Nn3/8/PPP2PPP/RNBQK2R w KQkq - 1 8"
            }
          }
        }""";

    public static final String TOOLS_CALL = """
        {"jsonrpc":"2.0","id":4,"method": "tools/call","params": {"name": "fenEvaluation","arguments": {"fen": "r1bqk2r/ppp2ppp/2n5/1BbpP3/3Nn3/8/PPP2PPP/RNBQK2R w KQkq - 1 8"},"_meta": {"progressToken": 0}}}""";

    public static final String EXPECTED_TOOLS_CALL_OBJECT_RETURN = "{\"jsonrpc\":\"2.0\",\"id\":4,\"result\":{\"content\":[{\"type\":\"text\",\"text\":\"{\\\"fen\\\":\\\"r1bqk2r/ppp2ppp/2n5/1BbpP3/3Nn3/8/PPP2PPP/RNBQK2R w KQkq - 1 8\\\",\\\"evaluation\\\":\\\"+0.12\\\"}\"}],\"isError\":false,\"structuredContent\":{\"evaluation\":\"+0.12\",\"fen\":\"r1bqk2r/ppp2ppp/2n5/1BbpP3/3Nn3/8/PPP2PPP/RNBQK2R w KQkq - 1 8\"}}}";

    public static final String EXPECTED_TOOLS_LIST_WITH_TOOL_ARGS = """
        {"jsonrpc":"2.0","id":3,"result":{"tools":[{"name":"fenEvaluation","description":"Evaluate a chess position using a FEN string.","inputSchema":{"type":"object","properties":{"tetWrLong":{"type":"number"},"fenList":{"type":"array"},"testReq":{"type":"object"},"tetBol":{"type":"boolean"},"tetLong":{"type":"number"},"fen":{"type":"string"},"tetWrBol":{"type":"boolean"}},"required":["fen","fenList","tetBol","tetWrBol","tetLong","tetWrLong","testReq"]}}]}}""";

    public static final String EXPECTED_TOOLS_LIST = """
        {"jsonrpc":"2.0","id":3,"result":{"tools":[{"name":"fenEvaluation","description":"Evaluate a chess position using a FEN string.","inputSchema":{"type":"object","properties":{"fen":{"type":"string"}},"required":["fen"]}}]}}""";

    public static final String EXPECTED_TOOLS_LIST_WITH_DESCRIPTION = """
        {"jsonrpc":"2.0","id":3,"result":{"tools":[{"name":"fenEvaluation","description":"Evaluate a chess position using a FEN string.","inputSchema":{"type":"object","properties":{"fen":{"description":"A Chess position in Forsyth–Edwards Notation","type":"string"}}}}]}}""";

    public static final String EXPECTED_TOOLS_LIST_WITH_INPUT_AND_OUTPUT_SCHEMA = """
        {"jsonrpc":"2.0","id":3,"result":{"tools":[{"name":"fenEvaluation","description":"Evaluate a chess position using a FEN string.","inputSchema":{"type":"object","properties":{"fen":{"description":"A Chess position in Forsyth–Edwards Notation","type":"string"}}},"outputSchema":{"$schema":"https://json-schema.org/draft/2020-12/schema","title":"FenEvaluationResponse","type":"object","properties":{"evaluation":{"type":"string","minLength":1},"fen":{"type":"string","minLength":1}},"required":["fen","evaluation"],"$id":"http://localhost:8080/schemas/fen-evaluation-response.schema.json"}}]}}""";

    public static final String TOOLS_LIST = """
        {"jsonrpc":"2.0","id":3,"method":"tools/list","params":{"_meta":{"progressToken":3}}}""";

    public static final String EXPECTED_RESOURCES_READ = """
        {
          "jsonrpc": "2.0",
          "id": 9,
          "result": {
            "contents": [
              {
                "uri": "pgn://round/2",
                "mimeType": "application/x-chess-pgn",
                "text":"[Event \\"FIDE World Championship Match 2024\\"]\\n[Site \\"Singapore SGP\\"]\\n[Date \\"2024.11.26\\"]\\n[Round \\"2\\"]\\n[White \\"Ding, Liren\\"]\\n[Black \\"Gukesh D\\"]\\n[Result \\"1/2-1/2\\"]\\n[WhiteElo \\"2728\\"]\\n[WhiteTitle \\"GM\\"]\\n[WhiteFideId \\"8603677\\"]\\n[BlackElo \\"2783\\"]\\n[BlackTitle \\"GM\\"]\\n[BlackFideId \\"46616543\\"]\\n[Annotator \\"https://lichess.org/@/RealDavidNavara\\"]\\n[Variant \\"Standard\\"]\\n[ECO \\"C50\\"]\\n[Opening \\"Italian Game: Giuoco Pianissimo, Italian Four Knights Variation\\"]\\n[StudyName \\"World Championship 2024: Annotated Games\\"]\\n[ChapterName \\"Ding, Liren - Gukesh D (Navara)\\"]\\n\\n1. e4 { Ding Liren plays 1.e4 rarely, but as mentioned yesterday, it cannot be a big surprise in such an important match. There was a lot of time to prepare. } 1... e5 2. Nf3 2... Nc6 3. Bc4 3... Bc5 4. d3 4... Nf6 5. Nc3 5... a6 { Black makes a useful move, creating an escape route for his bishop and waiting what comes next. } (5... d6 { White has numerous options, including } 6. Na4 (6. Bg5 h6 7. Bxf6 Qxf6 8. Nd5 Qd8 { is a completely different story, but not very terrifying for Black. }) (6. O-O { Black has many decent options: } 6... Na5 (6... h6) (6... O-O 7. Bg5 h6 { as well as some others. })) 6... Bb6 7. a3 O-O 8. O-O { with a slight strategic initiative with hardly any risk. In general, an exchange of a knight for the bishop on c5 (or c4) is mostly desirable in these structures. }) 6. a4 { White gains space on the queenside, at the same time creating an escape route for his bishop as well. } 6... d6 7. O-O (7. Bg5 h6 { is not dangerous for Black. The exchange on f6 makes less sense than in the 6.Bg5 line, while } 8. Bh4?! g5 9. Bg3 (9. Nxg5? { often makes sense with Black's king already on g8, but not here: } 9... hxg5 10. Bxg5 Rg8 11. h4 Bb4 (11... Bg4!?) 12. Qf3 Rg6 $17) 9... Bg4 $15 { favours Black, whose king is very safe. White's bishop is completely out of play on g3. }) 7... h6 (7... O-O 8. Bg5 h6 9. Bh4 { is a different story. } { Black should avoid } 9... g5? (9... Bg4! 10. h3 Bxf3 11. Qxf3 Nd4 12. Qd1 g5 13. Bg3 { might be marginally better for White, as Black's king is potentially weak. }) 10. Nxg5 hxg5 11. Bxg5 { , when the pin along the h4-d8 diagonal could have p(a)inful consequences for him. A crazy computer line } 11... Nb4 12. Kh1 (12. Ra3!? Be6 (12... Kh7 13. d4! Bxd4 14. Ne2 $16) 13. d4! { is a more creative way to bring a rook into the attack. White's attack should decide after } 13... Bxc4 14. dxc5 Bxf1 15. Nd5 Nbxd5 16. exd5 $18 { . The rook comes to g3 and the queen to f3 if needed. }) 12... Kg7 13. f4 Rh8! 14. fxe5 Ng4! (14... Rxh2+!? 15. Kxh2 Qh8+ 16. Kg3 Nh5+ 17. Kf3 Nc6! 18. e6!! { is another proof that an engine does not always help to create good annotations. White is close to winning, but the lines are very chaotic. } 18... Ne5+ (18... Nd4+ 19. Kf2! Nxe6+ 20. Be3 Bxe3+ 21. Kxe3 Qh6+ 22. Kf2 $18) 19. Ke2 Ng3+ 20. Kd2 Kg6 21. Rf3! Qh2 22. Be3 { and White should prevail. But who is reading this? }) 15. Bf6+! Nxf6 16. exf6+ Kf8 17. d4 $16 { favours White, who has enough pawns for the piece and keeps attacking. }) 8. Be3 8... Be6! (8... Bxe3 9. fxe3 { . White could also push d3-d4 to increase his central control. Then } 9... Be6 { resembles the game, but an early exchange on e3 gives White extra options including } 10. Nh4 O-O 11. Nf5 { , which makes no sense with the bishops still on e3 and c5. }) 9. a5!? { This move is not amongst the top three recommendations of a computer, yet it contains some venom. } (9. Bxe6 fxe6 10. Bxc5 dxc5 { leads to a double-edged position. White has a better pawn structure, but Black has opened some files for his major pieces and his doubled pawns control many important squares in the centre. White should probably play something like } 11. Nb1!? { , trying to transfer the knight to c4. Black has many options including } 11... g5 12. Na3 Qe7 13. Nc4 Nd7 14. c3 O-O-O { with double-edged play. }) (9. Nd5 { was an option even here, but } 9... Bxe3 10. fxe3 O-O 11. Nxf6+ Qxf6 12. Nd4 Qg6 { looks equal. } 13. Bxe6 (13. Nxe6 fxe6) 13... fxe6 14. Nxc6 bxc6 { White's slightly better pawn structure does not play a big role here. }) 9... Bxc4 { Gukesh spent 8 minutes on this move. } (9... Bb4?! { looks tempting but it is risky: } 10. Nd5! Bxa5 (10... O-O 11. Nxb4 Nxb4 12. Bxe6 fxe6 13. c3 Nc6 14. Nd2 { is better for White, who might follow up with d3-d4 and/or f2-f3. }) 11. d4! Nxe4 12. Qe2! { Black has many tactical weaknesses, especially the bishop on a5. The play might continue } 12... Ng5 13. Nxg5 hxg5 14. dxe5 dxe5 15. Rfd1 g4 16. g3! Qc8 17. c3 { with a huge compensation. After } 17... O-O! 18. Rxa5 b5! { black regains the piece, but White should maintain an edge with } 19. Raa1 bxc4 20. Qxc4 Bxd5 21. Rxd5 Qe6 22. Rc5 Qxc4 23. Rxc4 $14) 10. dxc4 10... O-O (10... Bb4!? 11. Nd5! Nxe4 12. Qd3! Nc5 13. Qf5 Ne6 14. Qg4!? { might require further tests. White has a nice compensation and can transfer his knight through h4 to f5, but Black's position is solid. That said, it would be very risky to enter such a line with Black against a prepared opponent. }) (10... Bxe3 11. fxe3 O-O { again leads to an interesting imbalanced position. Ding Liren played quickly until now, so he was undoubtedly prepared. } (11... Nb8?! { is ill-timed here, as } 12. c5! { prevents Nb8-d7. After } 12... O-O 13. cxd6 cxd6 $14 { Black's backward pawn d6 would be weaker than White's doubled pawns, which protect many important squares. })) 11. Bxc5 11... dxc5 { A largely symmetrical position has arisen. White has won some space on the queenside. His a5-pawn might be vulnerable, but the same applies to Black's c5-pawn, as b7-b6 is mostly undesirable. The exchange on b6 would leave Black with too many weak squares on the queenside. } 12. b3 $14 { White has a slight edge in a quiet position. It is not much, but he can push with no risk. } 12... Qxd1 13. Rfxd1 13... Rad8 (13... Rfd8 { . White might maintain some tension with } 14. Re1 (14. Rdc1 Rd6 15. Ne1 Rad8 16. f3 $36 { , when the knights come to d5 and d3. }) 14... Nd4 15. Nxd4 cxd4 16. Nd5 $36) 14. Rdc1 { Ding had spent around 4 minutes until now, and over 9 minutes here. White prepares a knight transfer to d3. } (14. Ne1!? { was also possible. After } 14... Rxd1 15. Rxd1 Nxa5 16. Nd3 { Black can return the pawn with } 16... Rd8! (16... Nd7 17. Nd5! { , but it is more difficult. A sample line } 17... c6 18. Ne7+ Kh7 19. Nb2! Nf6 20. Ra1 b6 21. Na4! Re8 22. Nf5 Nb7 23. Nxb6 Nxe4 { might suffice for equality, but is extremely unlikely to appear on the board, as moves like 19.Nb2! are very easy to miss. }) 17. Nxc5 (17. f3 b6 18. Nxe5 Rxd1+ 19. Nxd1 Ne8 20. Kf2 f6) 17... Rxd1+ 18. Nxd1 Kf8 19. Ne3 g6 { with equality. }) 14... Nd4 { Gukesh spent almost 28 minutes on this move. } (14... Rd6 { . }) 15. Ne1! { Ding spent 13 minutes here. The knight heads for d3, but as we will see soon, it is not so easy to get there. The position has come to a standstill to some extent. } 15... Rd6 16. Kf1 (16. f3!? g5 17. Kf2 g4 18. Rd1 { was a minor improvement over the game continuation, but it is not much, either. White wants to play 19.Rd2, 20.Rad1 and then move his knight from c3 to a4, d5 or e2, depending on the circumstances. }) (16. Nd3?? { loses a pawn to } 16... Nxb3! (16... Nxc2! 17. Rxc2 Rxd3 $17 { is equally strong }) 17. cxb3 Rxd3 $17 { . }) 16... g6 (16... g5!?) (16... h5!? { , with a good counterplay in either case. It resembles one line in Caro-Kann (1.e4 c6 2.Nf3 d5 3.d3 dxe4 4.dxe4 Qxd1+ 5.Kxd1 Nf6 6.Nfd2, where these two moves are good for the same reason. In both positions, White needs to play f2-f3 sooner or later to fortify the e4 pawn, and then moves g7-g5 and h7-h5 followed by g5-g4 or even h5-h4 might give Black counterplay and prevent White from increasing the pressure on the other side of the board. }) 17. Rd1 17... Rfd8 (17... h5!?) 18. f3 18... Kg7 19. Kf2 19... h5 20. Ne2?! (20. h4! $14 { would have contained Black's counterplay, maintaining a slight edge. White wants to prepare the c2-c3 advance or attack the c5-pawn through Nc3-a4, then double the rooks on the d-file and hope to improve his position. Computer suggests } 20... Nh7!? 21. Nd5 f5 22. exf5 gxf5 { , when } 23. Nxc7 (23. Nd3!? c6 (23... Nxc2? 24. Rac1 Nb4 25. Nxc5 c6 26. Ne3! Rxd1 27. Rxd1 Rxd1 28. Nxd1 $16) 24. Nxc5! cxd5 25. Nxb7 Nxc2 { leads to big complications. Black should maintain the balance. }) 23... Nf6 { allows Black to restore the material balance, e.g. } 24. Rab1 R8d7 25. Na8 Nxc2! 26. Rxd6 Rxd6 27. Nxc2 Rd2+ { . That said, it is not the most natural line, and not exactly forced, either. }) 20... Nc6 { It seems that both player were happy with a repetition. } (20... Nd7!? 21. Nxd4 (21. h4 f5 22. Ng3) 21... cxd4 22. b4 { leads to a balanced position, which is not outright drawish. In fact, } 22... c5!? { could easily become sharp. }) (20... Nxe2 21. Rxd6 cxd6!? 22. Kxe2 Nd7 { , either. Black might continue with Nd7-b8-c6 or with f7-f5 and Nf6, with sufficient counterplay in both cases. White's knight cannot get to the d6-pawn. }) 21. Nc3 21... Nd4 (21... Rxd1 22. Nxd1 h4 { was just equal with some slight chances for both sides. }) 22. Ne2 22... Nc6 23. Nc3 23... Nd4 { A threefold repetition has been reached. Gukesh has stabilized after the previous loss, whereas Ding Liren has maintained his lead. I assume that the next game will last longer, as Gukesh will try to make most of the White pieces. } 1/2-1/2\\n\\n\\n"
              }
            ]
          }
        }
        """;

    public static final String EXPECTED_RESOURCES_LIST = """
        {
          "jsonrpc": "2.0",
          "id": 2,
          "result": {
            "resources": [
              {
                "uri": "pgn://round/4",
                "name": "round4PgnFideWCC2024",
                "title": "PGN of the Round 4 game of the World Chess Championship",
                "description": "PGN of the Round 4 game of the World Chess Championship between Ding Liren and Gukesh Dommaraju",
                "mimeType": "application/x-chess-pgn",
                "size": 5956
              },
              {
                "uri": "pgn://round/5",
                "name": "round5PgnFideWCC2024",
                "title": "PGN of the Round 5 game of the World Chess Championship",
                "description": "PGN of the Round 5 game of the World Chess Championship between Ding Liren and Gukesh Dommaraju",
                "mimeType": "application/x-chess-pgn",
                "size": 9915
              },
              {
                "uri": "pgn://round/6",
                "name": "round6PgnFideWCC2024",
                "title": "PGN of the Round 6 game of the World Chess Championship",
                "description": "PGN of the Round 6 game of the World Chess Championship between Ding Liren and Gukesh Dommaraju",
                "mimeType": "application/x-chess-pgn",
                "size": 10354
              },
              {
                "uri": "pgn://round/7",
                "name": "round7PgnFideWCC2024",
                "title": "PGN of the Round 7 game of the World Chess Championship",
                "description": "PGN of the Round 7 game of the World Chess Championship between Ding Liren and Gukesh Dommaraju",
                "mimeType": "application/x-chess-pgn",
                "size": 15861
              },
              {
                "uri": "pgn://round/8",
                "name": "round8PgnFideWCC2024",
                "title": "PGN of the Round 8 game of the World Chess Championship",
                "description": "PGN of the Round 8 game of the World Chess Championship between Ding Liren and Gukesh Dommaraju",
                "mimeType": "application/x-chess-pgn",
                "size": 11984
              },
              {
                "uri": "pgn://round/9",
                "name": "round9PgnFideWCC2024",
                "title": "PGN of the Round 9 game of the World Chess Championship",
                "description": "PGN of the Round 9 game of the World Chess Championship between Ding Liren and Gukesh Dommaraju",
                "mimeType": "application/x-chess-pgn",
                "size": 6597
              },
              {
                "uri": "pgn://round/13",
                "name": "round13PgnFideWCC2024",
                "title": "PGN of the Round 13 game of the World Chess Championship",
                "description": "PGN of the Round 13 game of the World Chess Championship between Ding Liren and Gukesh Dommaraju",
                "mimeType": "application/x-chess-pgn",
                "size": 11425
              },
              {
                "uri": "pgn://round/14",
                "name": "round14PgnFideWCC2024",
                "title": "PGN of the Round 14 game of the World Chess Championship",
                "description": "PGN of the Round 14 game of the World Chess Championship between Ding Liren and Gukesh Dommaraju",
                "mimeType": "application/x-chess-pgn",
                "size": 9280
              },
              {
                "uri": "pgn://round/11",
                "name": "round11PgnFideWCC2024",
                "title": "PGN of the Round 11 game of the World Chess Championship",
                "description": "PGN of the Round 11 game of the World Chess Championship between Ding Liren and Gukesh Dommaraju",
                "mimeType": "application/x-chess-pgn",
                "size": 12525
              },
              {
                "uri": "pgn://round/12",
                "name": "round12PgnFideWCC2024",
                "title": "PGN of the Round 12 game of the World Chess Championship",
                "description": "PGN of the Round 12 game of the World Chess Championship between Ding Liren and Gukesh Dommaraju",
                "mimeType": "application/x-chess-pgn",
                "size": 12790
              },
              {
                "uri": "pgn://round/10",
                "name": "round10PgnFideWCC2024",
                "title": "PGN of the Round 10 game of the World Chess Championship",
                "description": "PGN of the Round 10 game of the World Chess Championship between Ding Liren and Gukesh Dommaraju",
                "mimeType": "application/x-chess-pgn",
                "size": 7647
              },
              {
                "uri": "pgn://round/1",
                "name": "round1PgnFideWCC2024",
                "title": "PGN of the Round 1 game of the World Chess Championship",
                "description": "PGN of the Round 1 game of the World Chess Championship between Ding Liren and Gukesh Dommaraju",
                "mimeType": "application/x-chess-pgn",
                "size": 14533
              },
              {
                "uri": "pgn://round/2",
                "name": "round2PgnFideWCC2024",
                "title": "PGN of the Round 2 game of the World Chess Championship",
                "description": "PGN of the Round 2 game of the World Chess Championship between Ding Liren and Gukesh Dommaraju",
                "mimeType": "application/x-chess-pgn",
                "size": 9378
              },
              {
                "uri": "pgn://round/3",
                "name": "round3PgnFideWCC2024",
                "title": "PGN of the Round 3 game of the World Chess Championship",
                "description": "PGN of the Round 3 game of the World Chess Championship between Ding Liren and Gukesh Dommaraju",
                "mimeType": "application/x-chess-pgn",
                "size": 16296
              }
            ]
          }
        }""";
}
