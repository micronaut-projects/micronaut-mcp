package example.micronaut;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PresentationTools {

    private List<Presentation> presentations = new ArrayList<>();

    public PresentationTools() {
        var keynoteOne = new Presentation("Java 24 Launch - Live from JavaOne 2025", "https://www.youtube.com/watch?v=mk_2MIWxLI0", 2025);
        var keynoteTwo = new Presentation("Java Turns 30 - Live from JavaOne 2025", "https://www.youtube.com/watch?v=GwR7Gvi80Xo", 2025);
        var concerto = new Presentation("Concerto for Java & AI - Building Production-Ready LLM Applications","https://www.youtube.com/watch?v=MhILqEAscss", 2025);
        var gatherers = new Presentation("Stream Gatherers - Deep Dive with the Expert","https://www.youtube.com/watch?v=v_5SKpfkI2U&t=6s", 2025);
        var ai202 = new Presentation("AI 202: Next-Level AI Mastery for Java Developers","https://www.youtube.com/watch?v=DIyj_V8h8X0",2025);
        var sequenced = new Presentation("Sequenced Collections - Deep Dive with the Expert","https://www.youtube.com/watch?v=6yuDqkkYTGU",2025);

        this.presentations.addAll(List.of(keynoteOne,keynoteTwo,concerto,gatherers,ai202,sequenced));
    }

    public List<Presentation> getPresentations() {
        return presentations;
    }

    public List<Presentation> getPresentationsByYear(int year) {
        return presentations.stream().filter(p -> p.year() == year).toList();
    }

    public List<Map<String, Object>> getPresentationsAsMapList() {
        return presentations.stream()
                .map(p -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("title", p.title());
                    map.put("url", p.url());
                    map.put("year", p.year());
                    return map;
                })
                .collect(Collectors.toList());
    }

}
