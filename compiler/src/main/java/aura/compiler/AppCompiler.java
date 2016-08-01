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
package aura.compiler;

import aura.compiler.clazz.Clazz;
import aura.compiler.clazz.Clazzes;
import aura.compiler.clazz.Path;
import aura.compiler.config.*;
import aura.compiler.plugin.LaunchPlugin;
import aura.compiler.plugin.PluginArgument;
import aura.compiler.target.LaunchParameters;
import aura.compiler.util.AntPathMatcher;
import org.apache.commons.cli.ParseException;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOExceptionWithCause;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import static aura.compiler.AuraConstants.*;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.*;

/**
 *
 * @version $Id$
 */
public class AppCompiler {

    /**
     * An {@link Executor} which runs tasks immediately without creating a
     * separate thread.
     */
    static final Executor SAME_THREAD_EXECUTOR = new Executor() {
        public void execute(Runnable r) {
            r.run();
        }
    };

    private final Config config;
    private final ClassCompiler classCompiler;
    private final Linker linker;

    public AppCompiler(Config config) {
        this.config = config;
        this.classCompiler = new ClassCompiler(config);
        this.linker = new Linker(config);
    }

    public Config getConfig() {
        return config;
    }

    /**
     * Returns all {@link Clazz}es in all {@link Path}s matching the specified
     * ANT-style pattern.
     */
    private Collection<Clazz> getMatchingClasses(String pattern) {
        AntPathMatcher matcher = new AntPathMatcher(pattern, ".");
        Map<String, Clazz> matches = new HashMap<String, Clazz>();
        for (Path path : config.getClazzes().getPaths()) {
            for (Clazz clazz : path.listClasses()) {
                if (!matches.containsKey(clazz.getClassName())
                        && matcher.matches(clazz.getClassName())) {

                    matches.put(clazz.getClassName(), clazz);
                }
            }
        }
        return matches.values();
    }

    /**
     * Returns all root classes. These are the minimum set of classes that needs
     * to be compiled and linked. The compiler will use this set to determine
     * which classes need to be recompiled and linked in through the root
     * classes' dependencies.
     * 
     * The classes matching {@link #ROOT_CLASS_PATTERNS} and
     * {@link #ROOT_CLASSES} will always be included. If a main class has been
     * specified it will also become a root. Any root class pattern specified on
     * the command line (as returned by {@link Config#getRoots()} will also be
     * used to find root classes. If no main class has been specified and
     * {@link Config#getRoots()} returns an empty set all classes available on
     * the bootclasspath and the classpath will become roots.
     */
    private TreeSet<Clazz> getRootClasses() {
        TreeSet<Clazz> classes = new TreeSet<Clazz>();
        for (String rootClassName : ROOT_CLASSES) {
            Clazz clazz = config.getClazzes().load(rootClassName);
            if (clazz == null) {
                throw new CompilerException("Root class " + rootClassName + " not found");
            }
            classes.add(clazz);
        }

        if (config.getMainClass() != null && !config.isBuildAsLib()) {
            Clazz clazz = config.getClazzes().load(config.getMainClass().replace('.', '/'));
            if (clazz == null) {
                throw new CompilerException("Main class " + config.getMainClass() + " not found");
            }
            classes.add(clazz);
        }

        if (config.getForceLinkClasses().isEmpty()) {
            if (config.getMainClass() == null) {
                classes.addAll(config.getClazzes().listClasses());
            }
        } else {
            for (String pattern : config.getForceLinkClasses()) {
                if (pattern == null || pattern.trim().isEmpty()) {
                    continue;
                }
                pattern = pattern.trim();
                if (pattern.indexOf('*') == -1) {
                    Clazz clazz = config.getClazzes().load(pattern.replace('.', '/'));
                    if (clazz == null) {
                        throw new CompilerException("Root class " + pattern + " not found");
                    }
                    classes.add(clazz);
                } else {
                    Collection<Clazz> matches = getMatchingClasses(pattern);
                    if (matches.isEmpty()) {
                        config.getLogger().warn("Root pattern %s matches no classes", pattern);
                    } else {
                        classes.addAll(matches);
                    }
                }
            }
        }
        return classes;
    }

    private boolean compile(Executor executor, ClassCompilerListener listener,
            Clazz clazz, Set<Clazz> compileQueue, Set<Clazz> compiled) throws IOException {

        boolean result = false;
        if (config.isClean() || classCompiler.mustCompile(clazz)) {
            classCompiler.compile(clazz, executor, listener);
            result = true;
        }
        return result;
    }

    static void addMetaInfImplementations(Clazzes clazzes, Clazz clazz, Set<Clazz> compiled, Set<Clazz> compileQueue)
            throws IOException {
        String metaInfName = "META-INF/services/" + clazz.getClassName();
        IOException throwLater = null;
        for (InputStream is : clazzes.loadResources(metaInfName)) {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(is, "UTF8"))) {
                for (;;) {
                    String line = r.readLine();
                    if (line == null) {
                        break;
                    }
                    if (line.startsWith("#")) {
                        continue;
                    }
                    String implClazzName = line.replace('.', '/');
                    Clazz implClazz = clazzes.load(implClazzName);
                    if (implClazz != null && !compiled.contains(implClazz)) {
                        compileQueue.add(implClazz);
                    }
                }
            } catch (IOException ex) {
                throwLater = ex;
            }
        }
        if (throwLater != null) {
            throw throwLater;
        }
    }

    public Set<Clazz> compile(Set<Clazz> rootClasses, boolean compileDependencies,
            final ClassCompilerListener listener) throws IOException {

        config.getLogger().info("Compiling classes using %d threads", config.getThreads());

        final Executor executor = (config.getThreads() <= 1)
                ? SAME_THREAD_EXECUTOR
                : new ThreadPoolExecutor(config.getThreads() - 1, config.getThreads() - 1,
                        0L, TimeUnit.MILLISECONDS,
                        // Use a bounded queue to avoid memory problems if the
                        // worker threads are slower than the enqueuing thread.
                        // The optimal thread pool size and queue size have been
                        // determined by trial and error.
                        new ArrayBlockingQueue<Runnable>((config.getThreads() - 1) * 20));
        class HandleFailureListener implements ClassCompilerListener {
            volatile Throwable t;

            @Override
            public void success(Clazz clazz) {
                if (listener != null) {
                    listener.success(clazz);
                }
            }

            @Override
            public void failure(Clazz clazz, Throwable t) {
                // Compilation failed. Save the error and stop the executor.
                this.t = t;
                if (executor instanceof ExecutorService) {
                    ((ExecutorService) executor).shutdown();
                }
                if (listener != null) {
                    listener.failure(clazz, t);
                }
            }
        };
        HandleFailureListener listenerWrapper = new HandleFailureListener();

        DependencyGraph dependencyGraph = config.getDependencyGraph();

        TreeSet<Clazz> compileQueue = new TreeSet<>(rootClasses);
        long start = System.currentTimeMillis();
        Set<Clazz> linkClasses = new HashSet<Clazz>();
        int compiledCount = 0;
        outer: while (!compileQueue.isEmpty() && !Thread.currentThread().isInterrupted()) {
            while (!compileQueue.isEmpty() && !Thread.currentThread().isInterrupted()) {
                Clazz clazz = compileQueue.pollFirst();
                if (!linkClasses.contains(clazz)) {
                    if (compile(executor, listenerWrapper, clazz, compileQueue, linkClasses)) {
                        compiledCount++;
                        if (listenerWrapper.t != null) {
                            // We have a failed compilation. Stop compiling.
                            break outer;
                        }
                    }

                    dependencyGraph.add(clazz, rootClasses.contains(clazz));
                    linkClasses.add(clazz);

                    if (compileDependencies) {
                        addMetaInfImplementations(config.getClazzes(), clazz, linkClasses, compileQueue);
                    }
                }
            }

            if (compileDependencies) {
                for (String className : dependencyGraph.findReachableClasses()) {
                    Clazz depClazz = config.getClazzes().load(className);
                    if (depClazz != null && !linkClasses.contains(depClazz)) {
                        compileQueue.add(depClazz);
                    }
                }
            }
        }

        // Shutdown the executor and wait for running tasks to complete.
        if (executor instanceof ExecutorService) {
            ExecutorService executorService = (ExecutorService) executor;
            executorService.shutdown();
            try {
                executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            } catch (InterruptedException e) {
            }
        }

        if (listenerWrapper.t != null) {
            // The compilation failed. Rethrow the exception in the callback.
            if (listenerWrapper.t instanceof IOException) {
                throw (IOException) listenerWrapper.t;
            }
            if (listenerWrapper.t instanceof RuntimeException) {
                throw (RuntimeException) listenerWrapper.t;
            }
            if (listenerWrapper.t instanceof Error) {
                throw (Error) listenerWrapper.t;
            }
            throw new CompilerException(listenerWrapper.t);
        }

        long duration = System.currentTimeMillis() - start;
        config.getLogger().info("Compiled %d classes in %.2f seconds", compiledCount, duration / 1000.0);

        return linkClasses;
    }

    private void compile() throws IOException {
        Set<Clazz> linkClasses = compile(getRootClasses(), true, null);

        if (Thread.currentThread().isInterrupted()) {
            return;
        }

        if (linkClasses.contains(config.getClazzes().load(TRUSTED_CERTIFICATE_STORE_CLASS))) {
            if (config.getCacerts() != null) {
                config.addResourcesPath(config.getClazzes().createResourcesBootclasspathPath(
                        config.getHome().getCacertsPath(config.getCacerts())));
            }
        }

        long start = System.currentTimeMillis();
        linker.link(linkClasses);
        long duration = System.currentTimeMillis() - start;
        if (config.isBuildAsLib()) {
            System.out.println("\nBuild Args: " + config.getTarget().getBuildCommand());
        } else {
            config.getLogger().info("Linked %d classes in %.2f seconds", linkClasses.size(), duration / 1000.0);
        }
    }

    public static void main(String[] args) throws IOException {

        AppCompiler compiler = null;
        ConfigBuilder configBuilder = null;

        configBuilder = new ConfigBuilder();

        ConfigBuilderArgParser argParser = new ConfigBuilderArgParser(CommandArgs.options(), args);

        try {
            argParser.validate();
            if (!argParser.validateArgs(argParser.getCmd())) {
                argParser.usage(CommandArgs.options());
                return;
            }
        } catch (IllegalArgumentException iae) {
            System.err.println(iae.getMessage());
            return;
        } catch (ParseException e) {
            System.out.println("Error processing options -- this is a bug. Please report");
            e.printStackTrace();
        }

        configBuilder = argParser.populateObject(configBuilder);
        configBuilder = processArgs(configBuilder, args);

        compiler = new AppCompiler(configBuilder.build());
        boolean run = false;

        try {
            if (configBuilder.getConfig().isArchive()) {
                compiler.build();
                compiler.archive();
            } else {
                if (run && !compiler.config.getTarget().canLaunch()) {
                    throw new IllegalArgumentException("Cannot launch when building " 
                            + compiler.config.getTarget().getType() + " binaries");
                }
                if (run) {
                    compiler.compile(); // Just compile the first slice if multiple archs have been specified
                    LaunchParameters launchParameters = compiler.config.getTarget().createLaunchParameters();
                    launchParameters.setArguments(configBuilder.getConfig().getRunArgs());
                    compiler.launch(launchParameters);
                } else if(compiler.config.isBuildAsLib()) {
                    compiler.compile();
                    /* output make file */
                } else {
                    compiler.build();
                    compiler.config.getTarget().install();
                }
            }
        } catch (Throwable t) {
            String message = t.getMessage();
            if (configBuilder.getConfig().verbose && !(t instanceof ExecuteException)) {
                t.printStackTrace();
            }
            printAndExit(message);
        }

    }

    public static ConfigBuilder processArgs(ConfigBuilder configBuilder, String[] args) throws IOException {
        List<Arch> archs = new ArrayList<>();
        List<String> runArgs = new ArrayList<>();
        Map<String, PluginArgument> pluginArguments = configBuilder.fetchPluginArguments();

        int i = 0;
        while (i < args.length) {
            if ("-properties".equals(args[i])) {
                configBuilder.addProperties(new File(args[++i]));
            } else if (args[i].startsWith("-P")) {
                int index = args[i].indexOf('=');
                if (index <= 0) {
                    throw new IllegalArgumentException("Malformed property: " + args[i]);
                }
                String name = args[i].substring(2, index);
                String value = args[i].substring(index + 1);
                configBuilder.addProperty(name, value);
            } else if ("-resources".equals(args[i])) {
                for (String p : args[++i].split(":")) {
                    if (AntPathMatcher.isPattern(p)) {
                        File dir = new File(AntPathMatcher.rtrimWildcardTokens(p));
                        String pattern = AntPathMatcher.extractPattern(p);
                        configBuilder.addResource(new Resource(dir, null).include(pattern));
                    } else {
                        configBuilder.addResource(new Resource(new File(p)));
                    }
                }
            } else if ("-cacerts".equals(args[i])) {
                String name = args[++i];
                Config.Cacerts cacerts = null;
                if (!"none".equals(name)) {
                    try {
                        cacerts = Config.Cacerts.valueOf(name);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Illegal -cacerts value: " + name);
                    }
                }
                configBuilder.cacerts(cacerts);
            } else if ("-archive".equals(args[i])) {
                configBuilder.archive(true);
            } else if (args[i].startsWith("-D")) {
            } else if (args[i].startsWith("-X")) {
            } else if (args[i].startsWith("-rvm:")) {
                runArgs.add(args[i]);
            } else if (args[i].startsWith("-")) {
                String argName = args[i].substring(1, args[i].length());
                if (argName.contains("=")) {
                    argName = argName.substring(0, argName.indexOf('='));
                }
                PluginArgument arg = pluginArguments.get(argName);
                if (arg != null) {
                    configBuilder.addPluginArgument(args[i].substring(1));
                } else {
                    //throw new IllegalArgumentException("Unrecognized option: " + args[i]);
                }
            }
            i++;
        }
        configBuilder.mainClass(args[args.length -1]);

        configBuilder.archs(archs.toArray(new Arch[archs.size()]));

        while (i < args.length) {
            runArgs.add(args[i++]);
        }
        return configBuilder;
    }

    /**
     * Builds the binary (possibly a fat binary with multiple archs).
     */
    public void build() throws IOException {
        List<Arch> archs = this.config.getArchs();
        if (archs.isEmpty()) {
            archs = config.getTarget().getDefaultArchs();
        }
        if (archs.isEmpty()) {
            throw new IllegalArgumentException("No archs specified in config");
        }
        if (archs.size() == 1 && this.config.getArch().equals(archs.get(0))) {
            // No need to clone configs for each slice.
            compile();
        } else {
            Map<Arch, File> slices = new TreeMap<>();
            for (Arch arch : archs) {
                this.config.getLogger().info("Building %s slice", arch);
                Config sliceConfig = this.config.builder()
                        .arch(arch)
                        .tmpDir(new File(this.config.getTmpDir(), arch.toString()))
                        .build();
                new AppCompiler(sliceConfig).compile();
                slices.put(arch, new File(sliceConfig.getTmpDir(), sliceConfig.getExecutableName()));
                for (Path path : sliceConfig.getResourcesPaths()) {
                    if (!this.config.getResourcesPaths().contains(path)) {
                        this.config.addResourcesPath(path);
                    }
                }
            }
            this.config.getTarget().buildFat(slices);
        }
    }

    /**
     * Archives the binary previously built using {@link #build()} along with
     * all resources specified in the {@link Config} and supporting files and
     * stores the archive in the {@link Config#getInstallDir()}.
     */
    public void archive() throws IOException {
        config.getTarget().archive();
    }

    /**
     * Installs the binary previously built using {@link #build()} along with
     * all resources specified in the {@link Config} and supporting files into
     * the {@link Config#getInstallDir()}.
     */
    public void install() throws IOException {
        config.getTarget().install();
    }

    public int launch(LaunchParameters launchParameters) throws Throwable {
        return launch(launchParameters, null);
    }

    public int launch(LaunchParameters launchParameters, InputStream inputStream) throws Throwable {
        try {
            return launchAsync(launchParameters, inputStream).waitFor();
        } finally {
            launchAsyncCleanup();
        }
    }

    public Process launchAsync(LaunchParameters launchParameters) throws Throwable {
        return launchAsync(launchParameters, null);
    }

    public Process launchAsync(LaunchParameters launchParameters, InputStream inputStream) throws Throwable {
        for (LaunchPlugin plugin : config.getLaunchPlugins()) {
            plugin.beforeLaunch(config, launchParameters);
        }
        try {
            Process process = config.getTarget().launch(launchParameters);
            for (LaunchPlugin plugin : config.getLaunchPlugins()) {
                plugin.afterLaunch(config, launchParameters, process);
            }
            return process;
        } catch (Throwable e) {
            for (LaunchPlugin plugin : config.getLaunchPlugins()) {
                plugin.launchFailed(config, launchParameters);
            }
            throw e;
        }
    }

    public void launchAsyncCleanup() {
        for (LaunchPlugin plugin : config.getLaunchPlugins()) {
            plugin.cleanup();
        }
    }

    private static void printDeviceTypesAndExit() throws IOException {
        System.exit(0);
    }


    private static void printAndExit(String errorMessage) {
        System.err.format("aura: %s\n", errorMessage);
    }


    private static String repeat(String s, int n) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < n; i++) {
            builder.append(s);
        }
        return builder.toString();
    }

    private class UpdateChecker extends Thread {
        private final String address;
        private volatile JSONObject result;

        public UpdateChecker(String address) {
            this.address = address;
            setDaemon(true);
        }

        @Override
        public void run() {
            result = fetchJson(address);
        }
    }

    private String getInstallUuid() throws IOException {
        File uuidFile = new File(new File(System.getProperty("user.home"), ".robovm"), "uuid");
        uuidFile.getParentFile().mkdirs();
        String uuid = uuidFile.exists() ? FileUtils.readFileToString(uuidFile, "UTF-8") : null;
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
            FileUtils.writeStringToFile(uuidFile, uuid, "UTF-8");
        }
        uuid = uuid.trim();
        if (uuid.matches("[0-9a-fA-F-]{36}")) {
            return uuid;
        }
        return null;
    }


    private JSONObject fetchJson(String address) {
        try {
            URL url = new URL(address);
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(5 * 1000);
            conn.setReadTimeout(5 * 1000);
            try (InputStream in = new BufferedInputStream(conn.getInputStream())) {
                return (JSONObject) JSONValue.parseWithException(IOUtils.toString(in, "UTF-8"));
            }
        } catch (Exception e) {
            if (config.getHome().isDev()) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
