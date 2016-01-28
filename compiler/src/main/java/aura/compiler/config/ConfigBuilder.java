package aura.compiler.config;

import aura.compiler.config.tools.Tools;
import aura.compiler.log.Logger;
import aura.compiler.plugin.*;
import org.apache.commons.io.IOUtils;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.Registry;
import org.simpleframework.xml.convert.RegistryStrategy;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.filter.PlatformFilter;
import org.simpleframework.xml.stream.Format;

import java.io.*;
import java.util.*;

/**
 * Created by ash on 20/01/16.
 */
public class ConfigBuilder {
    Config config;

    ConfigBuilder(Config config) {
        this.config = config;
    }

    public ConfigBuilder() throws IOException {
        this.config = new Config();
    }

    public Config getConfig() {
        return config;
    }

    public ConfigBuilder os(OS os) {
        config.os = os;
        return this;
    }

    public ConfigBuilder arch(Arch arch) {
        return archs(arch);
    }

    public ConfigBuilder archs(Arch... archs) {
        return archs(Arrays.asList(archs));
    }

    public ConfigBuilder archs(List<Arch> archs) {
        if (config.archs == null) {
            config.archs = new ArrayList<>();
        }
        config.archs.clear();
        config.archs.addAll(archs);
        return this;
    }

    public ConfigBuilder clearClasspathEntries() {
        if (config.classpath != null) {
            config.classpath.clear();
        }
        return this;
    }

    public ConfigBuilder addClasspathEntry(File f) {
        if (config.classpath == null) {
            config.classpath = new ArrayList<File>();
        }
        config.classpath.add(f);
        return this;
    }

    public ConfigBuilder clearBootClasspathEntries() {
        if (config.bootclasspath != null) {
            config.bootclasspath.clear();
        }
        return this;
    }

    public ConfigBuilder addBootClasspathEntry(File f) {
        if (config.bootclasspath == null) {
            config.bootclasspath = new ArrayList<File>();
        }
        config.bootclasspath.add(f);
        return this;
    }

    public ConfigBuilder mainJar(File f) {
        config.mainJar = f;
        return this;
    }

    public ConfigBuilder installDir(File installDir) {
        config.installDir = installDir;
        return this;
    }

    public ConfigBuilder executableName(String executableName) {
        config.executableName = executableName;
        return this;
    }

    public ConfigBuilder imageName(String imageName) {
        config.imageName = imageName;
        return this;
    }

    public ConfigBuilder home(Config.Home home) {
        config.home = home;
        return this;
    }

    public ConfigBuilder cacheDir(File cacheDir) {
        config.cacheDir = cacheDir;
        return this;
    }

    public ConfigBuilder clean(boolean b) {
        config.clean = b;
        return this;
    }

    public ConfigBuilder ccBinPath(File ccBinPath) {
        config.ccBinPath = ccBinPath;
        return this;
    }

    public ConfigBuilder debug(boolean b) {
        config.debug = b;
        return this;
    }

    public ConfigBuilder useDebugLibs(boolean b) {
        config.useDebugLibs = b;
        return this;
    }

    public ConfigBuilder dumpIntermediates(boolean b) {
        config.dumpIntermediates = b;
        return this;
    }

    public ConfigBuilder skipRuntimeLib(boolean b) {
        config.skipRuntimeLib = b;
        return this;
    }

    public ConfigBuilder skipLinking(boolean b) {
        config.skipLinking = b;
        return this;
    }

    public ConfigBuilder skipInstall(boolean b) {
        config.skipInstall = b;
        return this;
    }

    public ConfigBuilder useDynamicJni(boolean b) {
        config.useDynamicJni = b;
        return this;
    }

    public ConfigBuilder threads(int threads) {
        config.threads = threads;
        return this;
    }

    public ConfigBuilder mainClass(String mainClass) {
        config.mainClass = mainClass;
        return this;
    }

    public ConfigBuilder tmpDir(File tmpDir) {
        config.tmpDir = tmpDir;
        return this;
    }

    public ConfigBuilder logger(Logger logger) {
        config.logger = logger;
        return this;
    }

    public ConfigBuilder treeShakerMode(Config.TreeShakerMode treeShakerMode) {
        config.treeShakerMode = treeShakerMode;
        return this;
    }

    public ConfigBuilder clearForceLinkClasses() {
        if (config.forceLinkClasses != null) {
            config.forceLinkClasses.clear();
        }
        return this;
    }

    public ConfigBuilder addForceLinkClass(String pattern) {
        if (config.forceLinkClasses == null) {
            config.forceLinkClasses = new ArrayList<String>();
        }
        config.forceLinkClasses.add(pattern);
        return this;
    }

    public ConfigBuilder clearExportedSymbols() {
        if (config.exportedSymbols != null) {
            config.exportedSymbols.clear();
        }
        return this;
    }

    public ConfigBuilder addExportedSymbol(String symbol) {
        if (config.exportedSymbols == null) {
            config.exportedSymbols = new ArrayList<String>();
        }
        config.exportedSymbols.add(symbol);
        return this;
    }

    public ConfigBuilder clearUnhideSymbols() {
        if (config.unhideSymbols != null) {
            config.unhideSymbols.clear();
        }
        return this;
    }

    public ConfigBuilder addUnhideSymbol(String symbol) {
        if (config.unhideSymbols == null) {
            config.unhideSymbols = new ArrayList<String>();
        }
        config.unhideSymbols.add(symbol);
        return this;
    }

    public ConfigBuilder clearLibs() {
        if (config.libs != null) {
            config.libs.clear();
        }
        return this;
    }

    public ConfigBuilder addLib(Config.Lib lib) {
        if (config.libs == null) {
            config.libs = new ArrayList<Config.Lib>();
        }
        config.libs.add(lib);
        return this;
    }

    public ConfigBuilder clearFrameworks() {
        if (config.frameworks != null) {
            config.frameworks.clear();
        }
        return this;
    }

    public ConfigBuilder addFramework(String framework) {
        if (config.frameworks == null) {
            config.frameworks = new ArrayList<String>();
        }
        config.frameworks.add(framework);
        return this;
    }

    public ConfigBuilder clearWeakFrameworks() {
        if (config.weakFrameworks != null) {
            config.weakFrameworks.clear();
        }
        return this;
    }

    public ConfigBuilder addWeakFramework(String framework) {
        if (config.weakFrameworks == null) {
            config.weakFrameworks = new ArrayList<String>();
        }
        config.weakFrameworks.add(framework);
        return this;
    }

    public ConfigBuilder clearFrameworkPaths() {
        if (config.frameworkPaths != null) {
            config.frameworkPaths.clear();
        }
        return this;
    }

    public ConfigBuilder addFrameworkPath(File frameworkPath) {
        if (config.frameworkPaths == null) {
            config.frameworkPaths = new ArrayList<File>();
        }
        config.frameworkPaths.add(frameworkPath);
        return this;
    }

    public ConfigBuilder clearResources() {
        if (config.resources != null) {
            config.resources.clear();
        }
        return this;
    }

    public ConfigBuilder addResource(Resource resource) {
        if (config.resources == null) {
            config.resources = new ArrayList<Resource>();
        }
        config.resources.add(resource);
        return this;
    }

    public ConfigBuilder targetType(String targetType) {
        config.targetType = targetType;
        return this;
    }

    public ConfigBuilder clearProperties() {
        config.properties.clear();
        return this;
    }

    public ConfigBuilder addProperties(Properties properties) {
        config.properties.putAll(properties);
        return this;
    }

    public ConfigBuilder addProperties(File file) throws IOException {
        Properties props = new Properties();
        Reader reader = null;
        try {
            reader = new InputStreamReader(new FileInputStream(file), "utf-8");
            props.load(reader);
            addProperties(props);
        } finally {
            IOUtils.closeQuietly(reader);
        }
        return this;
    }

    public ConfigBuilder addProperty(String name, String value) {
        config.properties.put(name, value);
        return this;
    }

    public ConfigBuilder cacerts(Config.Cacerts cacerts) {
        config.cacerts = cacerts;
        return this;
    }

    public ConfigBuilder tools(Tools tools) {
        config.tools = tools;
        return this;
    }

    public ConfigBuilder iosSdkVersion(String sdkVersion) {
        config.iosSdkVersion = sdkVersion;
        return this;
    }

    public ConfigBuilder iosDeviceType(String deviceType) {
        config.iosDeviceType = deviceType;
        return this;
    }

    public ConfigBuilder iosInfoPList(File infoPList) {
        config.iosInfoPListFile = infoPList;
        return this;
    }

    public ConfigBuilder infoPList(File infoPList) {
        config.infoPListFile = infoPList;
        return this;
    }

    public ConfigBuilder iosSkipSigning(boolean b) {
        config.iosSkipSigning = b;
        return this;
    }

    public ConfigBuilder addCompilerPlugin(CompilerPlugin compilerPlugin) {
        config.plugins.add(compilerPlugin);
        return this;
    }

    public ConfigBuilder addLaunchPlugin(LaunchPlugin plugin) {
        config.plugins.add(plugin);
        return this;
    }

    public ConfigBuilder addTargetPlugin(TargetPlugin plugin) {
        config.plugins.add(plugin);
        return this;
    }

    public void addPluginArgument(String argName) {
        if (config.pluginArguments == null) {
            config.pluginArguments = new ArrayList<>();
        }
        config.pluginArguments.add(argName);
    }

    public Config build() throws IOException {
        for (CompilerPlugin plugin : config.getCompilerPlugins()) {
            plugin.beforeConfig(this, config);
        }

        return config.build();
    }

    /**
     * Reads properties from a project basedir. If {@code isTest} is
     * {@code true} this method will first attempt to load a
     * {@code robovm.test.properties} file in {@code basedir}.
     * <p>
     * If no test specific file is found or if {@code isTest} is
     * {@code false} this method attempts to load a
     * {@code robovm.properties} and a {@code robovm.local.properties} file
     * in {@code basedir} and merges them so that properties from the local
     * file (if it exists) override properties in the non-local file.
     * <p>
     * If {@code isTest} is {@code true} and no test specific properties
     * file was found this method will append {@code Test} to the
     * {@code app.id} and {@code app.name} properties (if they exist).
     * <p>
     * If none of the files can be found found this method does nothing.
     */
    public void readProjectProperties(File basedir, boolean isTest) throws IOException {
        File testPropsFile = new File(basedir, "robovm.test.properties");
        File localPropsFile = new File(basedir, "robovm.local.properties");
        File propsFile = new File(basedir, "robovm.properties");
        if (isTest && testPropsFile.exists()) {
            config.logger.info("Loading test RoboVM config properties file: "
                    + testPropsFile.getAbsolutePath());
            addProperties(testPropsFile);
        } else {
            Properties props = new Properties();
            if (propsFile.exists()) {
                config.logger.info("Loading default RoboVM config properties file: "
                        + propsFile.getAbsolutePath());
                try (Reader reader = new InputStreamReader(new FileInputStream(propsFile), "utf-8")) {
                    props.load(reader);
                }
            }
            if (localPropsFile.exists()) {
                config.logger.info("Loading local RoboVM config properties file: "
                        + localPropsFile.getAbsolutePath());
                try (Reader reader = new InputStreamReader(new FileInputStream(localPropsFile), "utf-8")) {
                    props.load(reader);
                }
            }
            if (isTest) {
                modifyPropertyForTest(props, "app.id");
                modifyPropertyForTest(props, "app.name");
                modifyPropertyForTest(props, "app.executable");
            }
            addProperties(props);
        }
    }

    private void modifyPropertyForTest(Properties props, String propName) {
        String propValue = props.getProperty(propName);
        if (propValue != null && !propValue.endsWith("Test")) {
            String newPropValue = propValue + "Test";
            config.logger.info("Changing %s property from '%s' to '%s'", propName, propValue, newPropValue);
            props.setProperty(propName, newPropValue);
        }
    }

    /**
     * Reads a config file from a project basedir. If {@code isTest} is
     * {@code true} this method will first attempt to load a
     * {@code aura.test.xml} file in {@code basedir}.
     * <p>
     * If no test-specific file is found or if {@code isTest} is
     * {@code false} this method attempts to load a {@code aura.xml} file
     * in {@code basedir}.
     * <p>
     * If none of the files can be found found this method does nothing.
     */
    public void readProjectConfig(File basedir, boolean isTest) throws IOException {
        File testConfigFile = new File(basedir, "aura.test.xml");
        File configFile = new File(basedir, "aura.xml");
        if (isTest && testConfigFile.exists()) {
            config.logger.info("Loading test Aura config file: "
                    + testConfigFile.getAbsolutePath());
            read(testConfigFile);
        } else if (configFile.exists()) {
            config.logger.info("Loading default Aura config file: "
                    + configFile.getAbsolutePath());
            read(configFile);
        }
    }

    public void read(File file) throws IOException {
        Reader reader = null;
        try {
            reader = new InputStreamReader(new FileInputStream(file), "utf-8");
            read(reader, file.getAbsoluteFile().getParentFile());
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    public void read(Reader reader, File wd) throws IOException {
        try {
            Serializer serializer = createSerializer(wd);
            serializer.read(config, reader);
        } catch (IOException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw (IOException) new IOException().initCause(e);
        }
        // <roots> was renamed to <forceLinkClasses> but we still support
        // <roots>. We need to copy <roots> to <forceLinkClasses> and set
        // <roots> to null.
        if (config.roots != null && !config.roots.isEmpty()) {
            if (config.forceLinkClasses == null) {
                config.forceLinkClasses = new ArrayList<String>();
            }
            config.forceLinkClasses.addAll(config.roots);
            config.roots = null;
        }
    }

    public void write(File file) throws IOException {
        Writer writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(file), "utf-8");
            write(writer, file.getAbsoluteFile().getParentFile());
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    public void write(Writer writer, File wd) throws IOException {
        try {
            Serializer serializer = createSerializer(wd);
            serializer.write(config, writer);
        } catch (IOException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw (IOException) new IOException().initCause(e);
        }
    }

    private Serializer createSerializer(final File wd) throws Exception {
        Config.RelativeFileConverter fileConverter = new Config.RelativeFileConverter(wd);

        Serializer resourceSerializer = new Persister(
                new RegistryStrategy(new Registry().bind(File.class, fileConverter)),
                new PlatformFilter(config.properties), new Format(2));

        Registry registry = new Registry();
        RegistryStrategy registryStrategy = new RegistryStrategy(registry);
        Serializer serializer = new Persister(registryStrategy,
                new PlatformFilter(config.properties), new Format(2));

        registry.bind(File.class, fileConverter);
        registry.bind(Config.Lib.class, new Config.RelativeLibConverter(fileConverter));
        registry.bind(Resource.class, new Config.ResourceConverter(fileConverter, resourceSerializer));

        return serializer;
    }

    /**
     * Fetches the {@link PluginArgument}s of all registered plugins for
     * parsing.
     */
    public Map<String, PluginArgument> fetchPluginArguments() {
        Map<String, PluginArgument> args = new TreeMap<>();
        for (Plugin plugin : config.plugins) {
            for (PluginArgument arg : plugin.getArguments().getArguments()) {
                args.put(plugin.getArguments().getPrefix() + ":" + arg.getName(), arg);
            }
        }
        return args;
    }

    public List<Plugin> getPlugins() {
        return config.getPlugins();
    }
}
