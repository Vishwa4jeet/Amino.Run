package amino.run.app;

import amino.run.common.Utils;
import amino.run.policy.scalability.LoadBalancedFrontendPolicy;
import amino.run.policy.scalability.ScaleUpFrontendPolicy;
import org.junit.Assert;
import org.junit.Test;

public class SapphireObjectSpecTest {
    @Test
    public void testToYamlFromYaml() {
        SapphireObjectSpec soSpec = createSpec();
        SapphireObjectSpec soSpecClone = SapphireObjectSpec.fromYaml(soSpec.toString());
        Assert.assertEquals(soSpec, soSpecClone);
    }

    @Test
    public void testSerializeEmptySpec() {
        SapphireObjectSpec spec = SapphireObjectSpec.newBuilder().create();
        SapphireObjectSpec clone = SapphireObjectSpec.fromYaml(spec.toString());
        Assert.assertEquals(spec, clone);
    }

    @Test
    public void testSerialization() throws Exception {
        SapphireObjectSpec spec = createSpec();
        SapphireObjectSpec clone = (SapphireObjectSpec) Utils.toObject(Utils.toBytes(spec));
        Assert.assertEquals(spec, clone);
    }

    private SapphireObjectSpec createSpec() {
        NodeSelectorSpec nodeSelectorSpec = new NodeSelectorSpec();
        nodeSelectorSpec.addAndLabel("and_label");
        nodeSelectorSpec.addOrLabel("or_label");

        ScaleUpFrontendPolicy.Config scaleUpConfig = new ScaleUpFrontendPolicy.Config();
        scaleUpConfig.setReplicationRateInMs(100);

        LoadBalancedFrontendPolicy.Config lbConfig = new LoadBalancedFrontendPolicy.Config();
        lbConfig.setMaxConcurrentReq(200);
        lbConfig.setReplicaCount(30);

        DMSpec dmSpec =
                DMSpec.newBuilder()
                        .setName(ScaleUpFrontendPolicy.class.getName())
                        .addConfig(scaleUpConfig)
                        .addConfig(lbConfig)
                        .create();

        return SapphireObjectSpec.newBuilder()
                .setLang(Language.js)
                .setName("com.org.College")
                .setSourceFileLocation("src/main/js/college.js")
                .setConstructorName("college")
                .addDMSpec(dmSpec)
                .setNodeSelectorSpec(nodeSelectorSpec)
                .create();
    }
}