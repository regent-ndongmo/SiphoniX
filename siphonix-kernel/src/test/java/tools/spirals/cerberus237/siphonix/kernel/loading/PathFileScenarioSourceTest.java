package tools.spirals.cerberus237.siphonix.kernel.loading;

import java.nio.file.Path;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class PathFileScenarioSourceTest {

    @Test
    public void shouldExposeTypeReferenceAndEmptyPayload() {
        Path path = Path.of("/tmp/scenarios/sample.yaml");
        PathFileScenarioSource source = new PathFileScenarioSource(path);

        Assert.assertEquals(PathFileScenarioSource.TYPE, source.getType());
        Assert.assertEquals(path.toString(), source.getReference());
        Assert.assertEquals(Map.of(), source.load());
    }
}
