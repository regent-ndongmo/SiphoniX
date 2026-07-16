package tools.spirals.cerberus237.siphonix.api.plugin.management;

import java.io.IOException;
import java.util.Map;

public interface ScenarioSource {

    String getType();

    String getReference();

    Map<String, Object> load() throws IOException;
}
