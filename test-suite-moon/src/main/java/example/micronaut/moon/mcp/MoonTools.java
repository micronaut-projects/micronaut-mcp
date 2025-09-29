package example.micronaut.moon.mcp;

import example.micronaut.moon.MoonPhaseEmoji;
import example.micronaut.moon.MoonPhaseRequest;
import example.micronaut.moon.MoonPhasesService;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.mcp.annotations.Tool;
import jakarta.inject.Singleton;
import jakarta.validation.Valid;

@Singleton
class MoonTools {
    private final MoonPhasesService moonPhasesService;

    MoonTools(MoonPhasesService moonPhasesService) {
        this.moonPhasesService = moonPhasesService;
    }

    @Tool(name = "current-moon-phase",
        description = "Provides the current moon phase")
    @NonNull
    MoonPhaseEmoji currentMoonPhase() {
        return moonPhasesService.currentMoonPhase();
    }

    @Tool(name = "moon-phase-at-date", description = "Provides the moon phase at a certain date (with a format of yyyy-MM-dd)")
    @NonNull
    MoonPhaseEmoji moonPhaseAtDate(@Valid MoonPhaseRequest moonPhaseRequest) {
        return moonPhasesService.moonPhaseAtDate(moonPhaseRequest.date());
    }
}
