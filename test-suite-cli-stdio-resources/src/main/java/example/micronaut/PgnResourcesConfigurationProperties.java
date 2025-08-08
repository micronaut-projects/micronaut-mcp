package example.micronaut;

import io.micronaut.context.annotation.ConfigurationProperties;

@ConfigurationProperties("pgn.resources")
public class PgnResourcesConfigurationProperties implements PgnResourcesConfiguration{
    private String folder;

    @Override
    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }
}
