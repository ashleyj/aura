/*
 * Copyright (C) 2012 RoboVM AB
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/gpl-2.0.html>.
 */
package aura.compiler.config;

import aura.compiler.*;
import aura.compiler.clazz.Clazz;
import aura.compiler.clazz.Clazzes;
import aura.compiler.clazz.Path;
import aura.compiler.config.OS.Family;
import aura.compiler.config.tools.Tools;
import aura.compiler.llvm.DataLayout;
import aura.compiler.log.Logger;
import aura.compiler.plugin.*;
import aura.compiler.plugin.annotation.AnnotationImplPlugin;
import aura.compiler.plugin.lambda.LambdaPlugin;
import aura.compiler.plugin.shadowframe.ShadowFramePlugin;
import aura.compiler.target.ConsoleTarget;
import aura.compiler.target.Target;
import aura.compiler.util.DigestUtil;
import aura.compiler.util.InfoPList;
import aura.compiler.util.io.RamDiskTools;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Holds compiler configuration.
 */
@Root
public class Config {

    /**
     * The max file name length of files stored in the cache. OS X has a limit
     * of 255 characters. Class names are very unlikely to be this long but some
     * JVM language compilers (e.g. the Scala compiler) are known to generate
     * very long class names for auto-generated classes. See #955.
     */
    private static final int MAX_FILE_NAME_LENGTH = 255;
    public boolean verbose;

    public enum Cacerts {
        full
    };

    public enum TreeShakerMode {
        none, conservative, aggressive
    };

    @Element(required = false)
    protected Boolean archive = null;
    @Element(required = false)
    protected Boolean buildAsLib = null;
    @Element(required = false)
    protected File installDir = null;
    @Element(required = false)
    protected String executableName = null;
    @Element(required = false)
    protected String imageName = null;
    @Element(required = false)
    protected Boolean useDynamicJni = null;
    @Element(required = false)
    protected Boolean skipRuntimeLib = null;
    @Element(required = false)
    protected File mainJar;
    @Element(required = false)
    protected String mainClass;
    @Element(required = false)
    protected Cacerts cacerts = null;
    @Element(required = false)
    protected OS os = null;
    @ElementList(required = false, inline = true)
    protected ArrayList<Arch> archs = null;
    @ElementList(required = false, entry = "root")
    protected ArrayList<String> roots;
    @ElementList(required = false, entry = "runArgs")
    protected ArrayList<String> runArgs;
    @ElementList(required = false, entry = "pattern")
    protected ArrayList<String> forceLinkClasses;
    @ElementList(required = false, entry = "lib")
    protected ArrayList<Lib> libs;
    @ElementList(required = false, entry = "symbol")
    protected ArrayList<String> exportedSymbols;
    @ElementList(required = false, entry = "symbol")
    protected ArrayList<String> unhideSymbols;
    @ElementList(required = false, entry = "framework")
    protected ArrayList<String> frameworks;
    @ElementList(required = false, entry = "framework")
    protected ArrayList<String> weakFrameworks;
    @ElementList(required = false, entry = "path")
    protected ArrayList<File> frameworkPaths;
    @ElementList(required = false, entry = "resource")
    protected ArrayList<Resource> resources;
    @ElementList(required = false, entry = "classpathentry")
    protected ArrayList<File> bootclasspath;
    @ElementList(required = false, entry = "classpathentry")
    protected ArrayList<File> classpath;
    @ElementList(required = false, entry = "argument")
    protected ArrayList<String> pluginArguments;
    @Element(required = false, name = "target")
    protected String targetType;
    @Element(required = false, name = "treeShaker")
    protected TreeShakerMode treeShakerMode;
    @Element(required = false)
    protected String iosSdkVersion;
    @Element(required = false, name = "iosInfoPList")
    protected File iosInfoPListFile = null;
    @Element(required = false, name = "infoPList")
    protected File infoPListFile = null;
    @Element(required = false)
    protected File iosEntitlementsPList;

    @Element(required = false)
    protected Tools tools;

    protected String iosDeviceType;
    protected InfoPList infoPList;

    protected boolean iosSkipSigning = false;

    protected Properties properties = new Properties();

    protected Home home = null;
    protected File tmpDir;
    protected File cacheDir = new File(System.getProperty("user.home"), ".aura/cache");
    protected File ccBinPath = null;

    protected boolean clean = false;
    protected boolean debug = false;
    protected boolean useDebugLibs = false;
    protected boolean skipLinking = false;
    protected boolean skipInstall = false;
    protected boolean dumpIntermediates = false;
    protected int threads = Runtime.getRuntime().availableProcessors();
    protected Logger logger = Logger.NULL_LOGGER;

    /*
     * The fields below are all initialized in build() and must not be included
     * when constructing Config clone. We mark them as transient which will make
     * the builder() method skip them.
     */

    protected transient List<Plugin> plugins = new ArrayList<>();
    protected transient Target target = null;
    protected transient File osArchDepLibDir;
    protected transient File osArchCacheDir;
    protected transient Clazzes clazzes;
    protected transient VTable.Cache vtableCache;
    protected transient ITable.Cache itableCache;
    protected transient List<Path> resourcesPaths = new ArrayList<Path>();
    protected transient DataLayout dataLayout;
    protected transient MarshalerLookup marshalerLookup;
    protected transient Config configBeforeBuild;
    protected transient DependencyGraph dependencyGraph;
    protected transient Arch sliceArch;

    protected Config() throws IOException {
        // Add standard plugins
        this.plugins.addAll(0, Arrays.asList(
                new AnnotationImplPlugin(),
                new LambdaPlugin(),
                new ShadowFramePlugin()
                ));
        this.loadPluginsFromClassPath();
    }

    /**
     * Returns a new {@link ConfigBuilder} which builds exactly this {@link Config}
     * when {@link ConfigBuilder#build()} is called.
     */
    public ConfigBuilder builder() throws IOException {
        return new ConfigBuilder(clone(configBeforeBuild));
    }

    public Home getHome() {
        return home;
    }

    public File getInstallDir() {
        return installDir;
    }

    public String getExecutableName() {
        return executableName;
    }

    public String getImageName() {
        return imageName;
    }

    public File getExecutablePath() {
        return new File(installDir, getExecutableName());
    }

    public File getImagePath() {
        return getExecutablePath();
    }

    public File getCacheDir() {
        return osArchCacheDir;
    }

    public File getCcBinPath() {
        return ccBinPath;
    }

    public OS getOs() {
        return os;
    }

    public Arch getArch() {
        return sliceArch;
    }

    public List<Arch> getArchs() {
        return archs == null ? Collections.<Arch> emptyList()
                : Collections.unmodifiableList(archs);
    }
    
    public String getTriple() {
        return sliceArch.getLlvmName() + "-unknown-" + os.getLlvmName();
    }

    public String getClangTriple() {
        return sliceArch.getClangName() + "-unknown-" + os.getLlvmName();
    }

    public DataLayout getDataLayout() {
        return dataLayout;
    }

    public boolean isClean() {
        return clean;
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isUseDebugLibs() {
        return useDebugLibs;
    }

    public boolean isDumpIntermediates() {
        return dumpIntermediates;
    }

    public boolean isBuildAsLib() {
        return buildAsLib != null && buildAsLib.booleanValue();
    }

    public boolean isArchive() {
        return archive != null && archive.booleanValue();
    }

    public boolean isSkipRuntimeLib() {
        return skipRuntimeLib != null && skipRuntimeLib.booleanValue();
    }

    public boolean isSkipLinking() {
        return skipLinking;
    }

    public boolean isSkipInstall() {
        return skipInstall;
    }

    public boolean isUseDynamicJni() {
        return useDynamicJni != null && useDynamicJni.booleanValue();
    }

    public int getThreads() {
        return threads;
    }

    public File getMainJar() {
        return mainJar;
    }

    public String getMainClass() {
        return mainClass;
    }

    public Cacerts getCacerts() {
        return cacerts == null ? Cacerts.full : cacerts;
    }

    public List<Path> getResourcesPaths() {
        return resourcesPaths;
    }

    public void addResourcesPath(Path path) {
        resourcesPaths.add(path);
    }

    public DependencyGraph getDependencyGraph() {
        return dependencyGraph;
    }
    
    public File getTmpDir() {
        if (tmpDir == null) {
            try {
                tmpDir = File.createTempFile("aura", ".tmp");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            tmpDir.delete();
            tmpDir.mkdirs();
        }
        return tmpDir;
    }

    public List<String> getForceLinkClasses() {
        return forceLinkClasses == null ? Collections.<String> emptyList()
                : Collections.unmodifiableList(forceLinkClasses);
    }

    public List<String> getExportedSymbols() {
        return exportedSymbols == null ? Collections.<String> emptyList()
                : Collections.unmodifiableList(exportedSymbols);
    }

    public List<String> getUnhideSymbols() {
        return unhideSymbols == null ? Collections.<String> emptyList()
                : Collections.unmodifiableList(unhideSymbols);
    }
    
    public List<Lib> getLibs() {
        return libs == null ? Collections.<Lib> emptyList()
                : Collections.unmodifiableList(libs);
    }

    public List<String> getFrameworks() {
        return frameworks == null ? Collections.<String> emptyList()
                : Collections.unmodifiableList(frameworks);
    }

    public List<String> getWeakFrameworks() {
        return weakFrameworks == null ? Collections.<String> emptyList()
                : Collections.unmodifiableList(weakFrameworks);
    }

    public List<File> getFrameworkPaths() {
        return frameworkPaths == null ? Collections.<File> emptyList()
                : Collections.unmodifiableList(frameworkPaths);
    }

    public List<Resource> getResources() {
        return resources == null ? Collections.<Resource> emptyList()
                : Collections.unmodifiableList(resources);
    }

    public File getOsArchDepLibDir() {
        return osArchDepLibDir;
    }

    public Clazzes getClazzes() {
        return clazzes;
    }

    public VTable.Cache getVTableCache() {
        return vtableCache;
    }

    public ITable.Cache getITableCache() {
        return itableCache;
    }

    public MarshalerLookup getMarshalerLookup() {
        return marshalerLookup;
    }

    public List<CompilerPlugin> getCompilerPlugins() {
        List<CompilerPlugin> compilerPlugins = new ArrayList<>();
        for (Plugin plugin : plugins) {
            if (plugin instanceof CompilerPlugin) {
                compilerPlugins.add((CompilerPlugin) plugin);
            }
        }
        return compilerPlugins;
    }

    public List<LaunchPlugin> getLaunchPlugins() {
        List<LaunchPlugin> launchPlugins = new ArrayList<>();
        for (Plugin plugin : plugins) {
            if (plugin instanceof LaunchPlugin) {
                launchPlugins.add((LaunchPlugin) plugin);
            }
        }
        return launchPlugins;
    }

    public List<TargetPlugin> getTargetPlugins() {
        List<TargetPlugin> targetPlugins = new ArrayList<>();
        for (Plugin plugin : plugins) {
            if (plugin instanceof TargetPlugin) {
                targetPlugins.add((TargetPlugin) plugin);
            }
        }
        return targetPlugins;
    }

    public List<String> getRunArgs() {
        return runArgs;
    }

    public List<Plugin> getPlugins() {
        return plugins;
    }

    public List<String> getPluginArguments() {
        return pluginArguments == null ? Collections.<String> emptyList()
                : Collections.unmodifiableList(pluginArguments);
    }

    public List<File> getBootclasspath() {
        return bootclasspath == null ? Collections.<File> emptyList()
                : Collections.unmodifiableList(bootclasspath);
    }

    public List<File> getClasspath() {
        return classpath == null ? Collections.<File> emptyList()
                : Collections.unmodifiableList(classpath);
    }

    public Properties getProperties() {
        return properties;
    }

    public Logger getLogger() {
        return logger;
    }

    public Target getTarget() {
        return target;
    }

    public String getTargetType() {
        return targetType;
    }

    public TreeShakerMode getTreeShakerMode() {
        return treeShakerMode == null ? TreeShakerMode.none : treeShakerMode;
    }

    public String getIosSdkVersion() {
        return iosSdkVersion;
    }

    public String getIosDeviceType() {
        return iosDeviceType;
    }

    public InfoPList getIosInfoPList() {
        return getInfoPList();
    }

    public InfoPList getInfoPList() {
        if (infoPList == null && iosInfoPListFile != null) {
            infoPList = new InfoPList(iosInfoPListFile);
        } else if (infoPList == null && infoPListFile != null) {
            infoPList = new InfoPList(infoPListFile);
        }
        return infoPList;
    }

    public File getIosEntitlementsPList() {
        return iosEntitlementsPList;
    }


    public boolean isIosSkipSigning() {
        return iosSkipSigning;
    }

    public Tools getTools() {
        return tools;
    }

    private static File makeFileRelativeTo(File dir, File f) {
        if (f.getParentFile() == null) {
            return dir;
        }
        return new File(makeFileRelativeTo(dir, f.getParentFile()), f.getName());
    }

    public String getArchiveName(Path path) {
        if (path.getFile().isFile()) {
            return path.getFile().getName();
        } else {
            return "classes" + path.getIndex() + ".jar";
        }
    }

    static String getFileName(Clazz clazz, String ext) {
        return getFileName(clazz.getInternalName(), ext, MAX_FILE_NAME_LENGTH);
    }

    static String getFileName(String internalName, String ext, int maxFileNameLength) {
        String packagePath = internalName.substring(0, internalName.lastIndexOf('/') + 1);
        String className = internalName.substring(internalName.lastIndexOf('/') + 1);
        String suffix = ext.startsWith(".") ? ext : "." + ext;

        int length = className.length() + suffix.length();
        if (length > maxFileNameLength) {
            String sha1 = DigestUtil.sha1(className);
            className = className.substring(0, Math.max(0, maxFileNameLength - suffix.length() - sha1.length())) + sha1;
        }
        return packagePath.replace('/', File.separatorChar) + className + suffix;
    }

    public File getLlFile(Clazz clazz) {
        return new File(getCacheDir(clazz.getPath()), getFileName(clazz, "class.ll"));
    }

    public File getCFile(Clazz clazz) {
        return new File(getCacheDir(clazz.getPath()), getFileName(clazz, "class.c"));
    }

    public File getBcFile(Clazz clazz) {
        return new File(getCacheDir(clazz.getPath()), getFileName(clazz, "class.bc"));
    }

    public File getSFile(Clazz clazz) {
        return new File(getCacheDir(clazz.getPath()), getFileName(clazz, "class.s"));
    }

    public File getOFile(Clazz clazz) {
        return new File(getCacheDir(clazz.getPath()), getFileName(clazz, "class.o"));
    }

    public File getLinesOFile(Clazz clazz) {
        return new File(getCacheDir(clazz.getPath()), getFileName(clazz, "class.lines.o"));
    }

    public File getLinesLlFile(Clazz clazz) {
        return new File(getCacheDir(clazz.getPath()), getFileName(clazz, "class.lines.ll"));
    }

    public File getInfoFile(Clazz clazz) {
        return new File(getCacheDir(clazz.getPath()), getFileName(clazz, "class.info"));
    }

    public File getCacheDir(Path path) {
        File srcRoot = path.getFile().getParentFile();
        String name = path.getFile().getName();
        try {
            return new File(makeFileRelativeTo(osArchCacheDir, srcRoot.getCanonicalFile()), name);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the directory where generated classes are stored for the
     * specified {@link Path}. Generated classes are stored in the cache
     * directory in a dir at the same level as the cache dir for the
     * {@link Path} with <code>.generated</code> appended to the dir name.
     */
    public File getGeneratedClassDir(Path path) {
        File pathCacheDir = getCacheDir(path);
        return new File(pathCacheDir.getParentFile(), pathCacheDir.getName() + ".generated");
    }

    private static Map<Object, Object> getManifestAttributes(File jarFile) throws IOException {
        JarFile jf = null;
        try {
            jf = new JarFile(jarFile);
            return new HashMap<Object, Object>(jf.getManifest().getMainAttributes());
        } finally {
            jf.close();
        }
    }

    private static String getImplementationVersion(File jarFile) throws IOException {
        return (String) getManifestAttributes(jarFile).get(Attributes.Name.IMPLEMENTATION_VERSION);
    }

    private static String getMainClass(File jarFile) throws IOException {
        return (String) getManifestAttributes(jarFile).get(Attributes.Name.MAIN_CLASS);
    }

    private File extractIfNeeded(Path path) throws IOException {
        if (path.getFile().isFile()) {
            File pathCacheDir = getCacheDir(path);
            File target = new File(pathCacheDir.getParentFile(), pathCacheDir.getName() + ".extracted");

            if (!target.exists() || path.getFile().lastModified() > target.lastModified()) {
                FileUtils.deleteDirectory(target);
                target.mkdirs();
                try (ZipFile zipFile = new ZipFile(path.getFile())) {
                    Enumeration<? extends ZipEntry> entries = zipFile.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        if (entry.getName().startsWith("META-INF/aura/") && !entry.isDirectory()) {
                            File f = new File(target, entry.getName());
                            f.getParentFile().mkdirs();
                            try (InputStream in = zipFile.getInputStream(entry);
                                 OutputStream out = new FileOutputStream(f)) {

                                IOUtils.copy(in, out);
                                if (entry.getTime() != -1) {
                                    f.setLastModified(entry.getTime());
                                }
                            }
                        }
                    }
                }
                target.setLastModified(path.getFile().lastModified());
            }

            return target;
        } else {
            return path.getFile();
        }
    }

    private <T> ArrayList<T> mergeLists(ArrayList<T> from, ArrayList<T> to) {
        if (from == null) {
            return to;
        }
        to = to != null ? to : new ArrayList<T>();
        for (T o : from) {
            if (!to.contains(o)) {
                to.add(o);
            }
        }
        return to;
    }

    private void mergeConfig(Config from, Config to) {
        to.exportedSymbols = mergeLists(from.exportedSymbols, to.exportedSymbols);
        to.unhideSymbols = mergeLists(from.unhideSymbols, to.unhideSymbols);
        to.forceLinkClasses = mergeLists(from.forceLinkClasses, to.forceLinkClasses);
        to.frameworkPaths = mergeLists(from.frameworkPaths, to.frameworkPaths);
        to.frameworks = mergeLists(from.frameworks, to.frameworks);
        to.libs = mergeLists(from.libs, to.libs);
        to.resources = mergeLists(from.resources, to.resources);
        to.weakFrameworks = mergeLists(from.weakFrameworks, to.weakFrameworks);
    }

    private void mergeConfigsFromClasspath() throws IOException {
        List<String> dirs = Arrays.asList(
                "META-INF/aura/" + os + "/" + sliceArch,
                "META-INF/aura/" + os);

        // The algorithm below preserves the order of config data from the
        // classpath. Last the config from this object is added.

        // First merge all configs on the classpath to an empty Config
        Config config = new Config();
        for (Path path : clazzes.getPaths()) {
            for (String dir : dirs) {
                if (path.contains(dir + "/robovm.xml")) {
                    File configXml = new File(new File(extractIfNeeded(path), dir), "robovm.xml");
                    ConfigBuilder configBuilder = new ConfigBuilder();
                    configBuilder.read(configXml);
                    mergeConfig(configBuilder.config, config);
                    break;
                }
            }
        }

        // Then merge with this Config
        mergeConfig(this, config);

        // Copy back to this Config
        this.exportedSymbols = config.exportedSymbols;
        this.unhideSymbols = config.unhideSymbols;
        this.forceLinkClasses = config.forceLinkClasses;
        this.frameworkPaths = config.frameworkPaths;
        this.frameworks = config.frameworks;
        this.libs = config.libs;
        this.resources = config.resources;
        this.weakFrameworks = config.weakFrameworks;
    }

    private static <T> List<T> toList(Iterator<T> it) {
        List<T> l = new ArrayList<T>();
        while (it.hasNext()) {
            l.add(it.next());
        }
        return l;
    }

    private void loadPluginsFromClassPath() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        ServiceLoader<CompilerPlugin> compilerPluginLoader = ServiceLoader.load(CompilerPlugin.class, classLoader);
        ServiceLoader<LaunchPlugin> launchPluginLoader = ServiceLoader.load(LaunchPlugin.class, classLoader);
        ServiceLoader<TargetPlugin> targetPluginLoader = ServiceLoader.load(TargetPlugin.class, classLoader);

        plugins.addAll(toList(compilerPluginLoader.iterator()));
        plugins.addAll(toList(launchPluginLoader.iterator()));
        plugins.addAll(toList(targetPluginLoader.iterator()));
    }

    private static Config clone(Config config) throws IOException {
        Config clone = new Config();
        for (Field f : Config.class.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers()) && !Modifier.isTransient(f.getModifiers())) {
                f.setAccessible(true);
                try {
                    Object o = f.get(config);
                    if (o instanceof Collection && o instanceof Cloneable) {
                        // Clone collections. Assume the class has a public
                        // clone() method.
                        Method m = o.getClass().getMethod("clone");
                        o = m.invoke(o);
                    }
                    f.set(clone, o);
                } catch (Throwable t) {
                    throw new Error(t);
                }
            }
        }
        return clone;
    }

    protected Config build() throws IOException {
        // Create a clone of this Config before we have done anything with it so
        // that builder() has a fresh Config it can use.
        this.configBeforeBuild = clone(this);

        if (home == null) {
            home = Home.find();
        }

        if (bootclasspath == null) {
            bootclasspath = new ArrayList<File>();
        }
        if (classpath == null) {
            classpath = new ArrayList<File>();
        }

        if (mainJar != null) {
            mainClass = getMainClass(mainJar);
            classpath.add(mainJar);
        }

        if (executableName == null && imageName != null) {
            executableName = imageName;
        }

        if (!skipLinking && executableName == null && mainClass == null) {
            throw new IllegalArgumentException("No target and no main class specified");
        }

        if (!skipLinking && classpath.isEmpty()) {
            throw new IllegalArgumentException("No classpath specified");
        }

        if (skipLinking) {
            skipInstall = true;
        }

        if (executableName == null) {
            executableName = mainClass;
        }

        if (imageName == null || !imageName.equals(executableName)) {
            imageName = executableName;
        }

        List<File> realBootclasspath = bootclasspath == null ? new ArrayList<File>() : bootclasspath;
        if (!isSkipRuntimeLib()) {
            realBootclasspath = new ArrayList<File>(bootclasspath);
            realBootclasspath.add(0, home.rtPath);
        }

        this.vtableCache = new VTable.Cache();
        this.itableCache = new ITable.Cache();
        this.marshalerLookup = new MarshalerLookup(this);

        if (!skipInstall) {
            if (installDir == null) {
                installDir = new File(".", executableName);
            }
            installDir.mkdirs();
        }

        if (targetType != null) {
            if (ConsoleTarget.TYPE.equals(targetType)) {
                target = new ConsoleTarget();
            } else {
                for (TargetPlugin plugin : getTargetPlugins()) {
                    if (plugin.getTarget().getType().equals(targetType)) {
                        target = plugin.getTarget();
                        break;
                    }
                }
                if (target == null) {
                    throw new IllegalArgumentException("Unsupported target '" + targetType + "'");
                }
            }
        } else {
            target = new ConsoleTarget();
        }

        if (!getArchs().isEmpty()) {
            sliceArch = getArchs().get(0);
        }

        target.init(this);

        os = target.getOs();
        sliceArch = target.getArch();
        dataLayout = new DataLayout(getTriple());

        osArchDepLibDir = new File(new File(home.libVmDir, os.toString()),
                sliceArch.toString());

        if (treeShakerMode != null && treeShakerMode != TreeShakerMode.none 
                && os.getFamily() == Family.darwin && sliceArch == Arch.x86) {

            logger.warn("Tree shaking is not supported when building "
                    + "for OS X/iOS x86 32-bit due to a bug in Xcode's linker. No tree "
                    + "shaking will be performed. Run in 64-bit mode instead to "
                    + "use tree shaking.");
            treeShakerMode = TreeShakerMode.none;
        }
        dependencyGraph = new DependencyGraph(getTreeShakerMode());

        RamDiskTools ramDiskTools = new RamDiskTools();
        ramDiskTools.setupRamDisk(this, this.cacheDir, this.tmpDir);
        this.cacheDir = ramDiskTools.getCacheDir();
        this.tmpDir = ramDiskTools.getTmpDir();

        File osDir = new File(cacheDir, os.toString());
        File archDir = new File(osDir, sliceArch.toString());
        osArchCacheDir = new File(archDir, debug ? "debug" : "release");
        osArchCacheDir.mkdirs();

        this.clazzes = new Clazzes(this, realBootclasspath, classpath);

        mergeConfigsFromClasspath();

        return this;
    }

    public static class Home {
        private File binDir = null;
        private File includeDir = null;
        private File libVmDir = null;
        private File rtPath = null;
        private Map<Cacerts, File> cacertsPath = null;
        private boolean dev = false;

        public Home(File homeDir) {
            this(homeDir, true);
        }

        protected Home(File homeDir, boolean validate) {
            if (validate) {
                try {
                    validate(homeDir);
                } catch (IllegalArgumentException iae) {
                    iae.printStackTrace();
                }
            }
            binDir = new File(homeDir, "bin");
            includeDir = new File(homeDir, "include");
            libVmDir = new File(homeDir, "lib/vm");
            rtPath = new File(homeDir, "lib/aura-rt.jar");
            cacertsPath = new HashMap<Cacerts, File>();
            cacertsPath.put(Cacerts.full, new File(homeDir, "lib/cacerts-full.jar"));
        }

        private Home(File devDir, File binDir, File includeDir, File libVmDir, File rtPath) {
            this.binDir = binDir;
            this.includeDir = includeDir;
            this.libVmDir = libVmDir;
            this.rtPath = rtPath;
            cacertsPath = new HashMap<Cacerts, File>();
            cacertsPath.put(Cacerts.full, new File(devDir,
                    "cacerts/full/target/cacerts-full-" + Version.getVersion() + ".jar"));
            this.dev = true;
        }

        public boolean isDev() {
            return dev;
        }

        public File getBinDir() {
            return binDir;
        }

        public File getIncludeDir() {
            return includeDir;
        }

        public File getLibVmDir() {
            return libVmDir;
        }

        public File getRtPath() {
            return rtPath;
        }

        public File getCacertsPath(Cacerts cacerts) {
            return cacertsPath.get(cacerts);
        }

        public static Home find() {
            // Check if AURA_DEV_ROOT has been set. If set it should be
            // pointing at the root of a complete Aura source tree.
            if (System.getenv("AURA_DEV_ROOT") != null) {
                File dir = new File(System.getenv("AURA_DEV_ROOT"));
                return validateDevRootDir(dir);
            }
            if (System.getProperty("AURA_DEV_ROOT") != null) {
                File dir = new File(System.getProperty("AURA_DEV_ROOT"));
                return validateDevRootDir(dir);
            }

            if (System.getenv("AURA_HOME") != null) {
                File dir = new File(System.getenv("AURA_HOME"));
                return new Home(dir);
            }

            List<File> candidates = new ArrayList<File>();
            File userHome = new File(System.getProperty("user.home"));
            candidates.add(new File(userHome, "Applications/aura"));
            candidates.add(new File(userHome, ".aura/home"));
            candidates.add(new File("/usr/local/lib/aura"));
            candidates.add(new File("/opt/aura"));
            candidates.add(new File("/usr/lib/aura"));

            for (File dir : candidates) {
                if (dir.exists()) {
                    return new Home(dir);
                }
            }

            throw new IllegalArgumentException("AURA_HOME not set and no Aura "
                    + "installation found in " + candidates);
        }

        public static void validate(File dir) throws IllegalArgumentException {
            String error = "Path " + dir + " is not a valid Aura install directory: ";
            // Check for required dirs and match the compiler version with our
            // version.
            if (!dir.exists()) {
                throw new IllegalArgumentException(error + "no such path");
            }

            if (!dir.isDirectory()) {
                throw new IllegalArgumentException(error + "not a directory");
            }

            File libDir = new File(dir, "lib");
            if (!libDir.exists() || !libDir.isDirectory()) {
                throw new IllegalArgumentException(error + "lib/ missing or invalid");
            }
            File binDir = new File(dir, "bin");
            if (!binDir.exists() || !binDir.isDirectory()) {
                throw new IllegalArgumentException(error + "bin/ missing or invalid");
            }
            File includeDir = new File(dir, "include");
            if (!includeDir.exists() || !includeDir.isDirectory()) {
                throw new IllegalArgumentException(error + "include/ missing or invalid");
            }
            File libVmDir = new File(libDir, "vm");
            if (!libVmDir.exists() || !libVmDir.isDirectory()) {
                throw new IllegalArgumentException(error + "lib/vm/ missing or invalid");
            }
            File rtJarFile = new File(libDir, "aura-rt.jar");
            if (!rtJarFile.exists() || !rtJarFile.isFile()) {
                throw new IllegalArgumentException(error
                        + "lib/aura-rt.jar missing or invalid");
            }

            // Compare the version of this compiler with the version of the
            // aura-rt.jar in the home dir. They have to match.
            try {
                String thisVersion = Version.getVersion();
                String thatVersion = getImplementationVersion(rtJarFile);
                if (thisVersion == null || thatVersion == null || !thisVersion.equals(thatVersion)) {
                    throw new IllegalArgumentException(error + "version mismatch (expected: "
                            + thisVersion + ", was: " + thatVersion + ")");
                }
            } catch (IOException e) {
                throw new IllegalArgumentException(error
                        + "failed to get version of rt jar", e);
            }
        }

        private static Home validateDevRootDir(File dir) {
            String error = "Path " + dir + " is not a valid Aura source tree: ";
            // Check for required dirs.
            if (!dir.exists()) {
                throw new IllegalArgumentException(error + "no such path");
            }

            if (!dir.isDirectory()) {
                throw new IllegalArgumentException(error + "not a directory");
            }

            File vmBinariesDir = new File(dir, "vm/target/binaries");
            if (!vmBinariesDir.exists() || !vmBinariesDir.isDirectory()) {
                throw new IllegalArgumentException(error + "vm/target/binaries/ missing or invalid");
            }
            File binDir = new File(dir, "bin");
            if (!binDir.exists() || !binDir.isDirectory()) {
                throw new IllegalArgumentException(error + "bin/ missing or invalid");
            }

            File includeDir = new File(dir, "include");
            if (!includeDir.exists() || !includeDir.isDirectory()) {
                throw new IllegalArgumentException(error + "include/ missing or invalid");
            }

            String rtJarName = "aura-rt-" + Version.getVersion() + ".jar";
            File rtJar = new File(dir, "rt/target/" + rtJarName);
            File rtClasses = new File(dir, "rt/target/classes/");
            File rtSource = rtJar;
            if (!rtJar.exists() || rtJar.isDirectory()) {
                if (!rtClasses.exists() || rtClasses.isFile()) {
                    throw new IllegalArgumentException(error
                            + "rt/target/" + rtJarName + " missing or invalid");
                } else {
                    rtSource = rtClasses;
                }
            }

            return new Home(dir, binDir, includeDir, vmBinariesDir, rtSource);
        }
    }

    public static final class Lib {
        private final String value;
        private final boolean force;

        public Lib(String value, boolean force) {
            this.value = value;
            this.force = force;
        }

        public String getValue() {
            return value;
        }

        public boolean isForce() {
            return force;
        }

        @Override
        public String toString() {
            return "Lib [value=" + value + ", force=" + force + "]";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (force ? 1231 : 1237);
            result = prime * result + ((value == null) ? 0 : value.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Lib other = (Lib) obj;
            if (force != other.force) {
                return false;
            }
            if (value == null) {
                if (other.value != null) {
                    return false;
                }
            } else if (!value.equals(other.value)) {
                return false;
            }
            return true;
        }
    }

    protected static final class RelativeLibConverter implements Converter<Lib> {
        private final RelativeFileConverter fileConverter;

        public RelativeLibConverter(RelativeFileConverter fileConverter) {
            this.fileConverter = fileConverter;
        }

        @Override
        public Lib read(InputNode node) throws Exception {
            String value = node.getValue();
            if (value == null) {
                return null;
            }
            InputNode forceNode = node.getAttribute("force");
            boolean force = forceNode == null || Boolean.valueOf(forceNode.getValue());
            if (value.endsWith(".a") || value.endsWith(".o")) {
                return new Lib(fileConverter.read(value).getAbsolutePath(), force);
            } else {
                return new Lib(value, force);
            }
        }

        @Override
        public void write(OutputNode node, Lib lib) throws Exception {
            String value = lib.getValue();
            boolean force = lib.isForce();
            if (value.endsWith(".a") || value.endsWith(".o")) {
                fileConverter.write(node, new File(value));
            } else {
                node.setValue(value);
            }
            if (!force) {
                node.setAttribute("force", "false");
            }
        }
    }

    protected static final class RelativeFileConverter implements Converter<File> {
        private final String wdPrefix;

        public RelativeFileConverter(File wd) {
            if (wd.isFile()) {
                wd = wd.getParentFile();
            }
            String prefix = wd.getAbsolutePath();
            if (prefix.endsWith(File.separator)) {
                prefix = prefix.substring(0, prefix.length() - 1);
            }
            wdPrefix = prefix;
        }

        File read(String value) {
            if (value == null) {
                return null;
            }
            File file = new File(value);
            if (!file.isAbsolute()) {
                file = new File(wdPrefix, value);
            }
            return file;
        }

        @Override
        public File read(InputNode node) throws Exception {
            return read(node.getValue());
        }

        @Override
        public void write(OutputNode node, File value) throws Exception {
            String path = value.getAbsolutePath();
            if (path.equals(wdPrefix)) {
                if ("directory".equals(node.getName())) {
                    // Skip
                    node.remove();
                } else {
                    node.setValue("");
                }
            } else if (path.startsWith(wdPrefix) && path.charAt(wdPrefix.length()) == File.separatorChar) {
                node.setValue(path.substring(wdPrefix.length() + 1));
            } else {
                node.setValue(path);
            }
        }
    }

    protected static final class ResourceConverter implements Converter<Resource> {
        private final RelativeFileConverter fileConverter;
        private final Serializer serializer;

        public ResourceConverter(RelativeFileConverter fileConverter, Serializer serializer) {
            this.fileConverter = fileConverter;
            this.serializer = serializer;
        }

        @Override
        public Resource read(InputNode node) throws Exception {
            String value = node.getValue();
            if (value != null && value.trim().length() > 0) {
                return new Resource(fileConverter.read(value));
            }
            return serializer.read(Resource.class, node);
        }

        @Override
        public void write(OutputNode node, Resource resource) throws Exception {
            File path = resource.getPath();
            if (path != null) {
                fileConverter.write(node, path);
            } else {
                node.remove();
                serializer.write(resource, node.getParent());
            }
        }
    }
}
