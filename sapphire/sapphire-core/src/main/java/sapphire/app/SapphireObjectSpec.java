package sapphire.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Sapphire Object Specification.
 *
 * <p>Most applications should use Yaml file to specify sapphire objects. Yaml files can be parsed
 * into SapphireObjectSpec with {@link #fromYaml(String)}.
 *
 * <p>Java application has the option to create {@link SapphireObjectSpec} programmatically with
 * {@link Builder} class. <code>
 *      LoadBalancedFrontendPolicy.Config config = new LoadBalancedFrontendPolicy.Config();
 *      config.setMaxConcurrentReq(200);
 *      config.setReplicaCount(30);
 *
 *      DMSpec dm = Utils.toDMSpec(config);
 *      SapphireObjectSpec spec = SapphireObjectSpec.newBuilder()
 *                                      .setName("soname")
 *                                      .setLang(Language.Java)
 *                                      .addDM(dm)
 *                                      .create();
 * </code>
 */
public class SapphireObjectSpec {
    /** Programming Language in which the Sapphire object is written */
    private Language lang;

    /** Name of Sapphire object */
    private String name;

    /** Java class name of Sapphire object. Only used when {@link #lang} is Java. */
    private String javaClassName;

    /** Location of Sapphire object source file. Used when {@link #lang} is not Java */
    private String sourceFileLocation;

    /** Name of Sapphire object constructor. Used when {@link #lang} is not Java */
    private String constructorName;

    /** List of Deployment Managers to be applied on Sapphire object */
    private List<DMSpec> dmList;

    public static Builder newBuilder() {
        return new Builder();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSourceFileLocation() {
        return sourceFileLocation;
    }

    public void setSourceFileLocation(String sourceFileLocation) {
        this.sourceFileLocation = sourceFileLocation;
    }

    public String getJavaClassName() {
        return javaClassName;
    }

    public void setJavaClassName(String javaClassName) {
        this.javaClassName = javaClassName;
    }

    public String getConstructorName() {
        return constructorName;
    }

    public void setConstructorName(String constructorName) {
        this.constructorName = constructorName;
    }

    public Language getLang() {
        return lang;
    }

    public void setLang(Language lang) {
        this.lang = lang;
    }

    public List<DMSpec> getDmList() {
        return dmList;
    }

    public void setDmList(List<DMSpec> dmList) {
        this.dmList = dmList;
    }

    public void addDM(DMSpec dmSpec) {
        if (dmList == null) {
            dmList = new ArrayList<DMSpec>();
        }
        dmList.add(dmSpec);
    }

    public static SapphireObjectSpec fromYaml(String yamlString) {
        Yaml yaml = new Yaml();
        return yaml.loadAs(yamlString, SapphireObjectSpec.class);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SapphireObjectSpec that = (SapphireObjectSpec) o;
        return lang == that.lang
                && Objects.equals(name, that.name)
                && Objects.equals(javaClassName, that.javaClassName)
                && Objects.equals(sourceFileLocation, that.sourceFileLocation)
                && Objects.equals(constructorName, that.constructorName)
                && Objects.equals(dmList, that.dmList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lang, name, javaClassName, sourceFileLocation, constructorName, dmList);
    }

    @Override
    public String toString() {
        DumperOptions options = new DumperOptions();
        options.setAllowReadOnlyProperties(true);
        Yaml yaml = new Yaml(options);
        return yaml.dump(this);
    }

    public static class Builder {
        private String name;
        private Language lang;
        private String javaClassName;
        private String sourceFileLocation;
        private String constructorName;
        private List<DMSpec> dmList;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setLang(Language lang) {
            this.lang = lang;
            return this;
        }

        public Builder setJavaClassName(String javaClassName) {
            this.javaClassName = javaClassName;
            return this;
        }

        public Builder setSourceFileLocation(String sourceFileLocation) {
            this.sourceFileLocation = sourceFileLocation;
            return this;
        }

        public Builder setConstructorName(String constructorName) {
            this.constructorName = constructorName;
            return this;
        }

        public Builder setDmList(List<DMSpec> dmList) {
            this.dmList = dmList;
            return this;
        }

        public Builder addDM(DMSpec dmSpec) {
            if (dmList == null) {
                dmList = new ArrayList<>();
            }
            dmList.add(dmSpec);

            return this;
        }

        public SapphireObjectSpec create() {
            SapphireObjectSpec spec = new SapphireObjectSpec();
            spec.setName(name);
            spec.setLang(lang);
            spec.setJavaClassName(javaClassName);
            spec.setSourceFileLocation(sourceFileLocation);
            spec.setConstructorName(constructorName);
            spec.setDmList(dmList);
            return spec;
        }
    }
}
