/*
 * Copyright (C) 2013 RoboVM AB
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

import aura.compiler.target.ConsoleTarget;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import aura.compiler.config.Config.Home;
import aura.compiler.config.Config.Lib;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Tests {@link Config}.
 */
public class ConfigTest {
    String savedUserDir;
    File tmp;
    File wd;
    Home fakeHome;
    
    @Before
    public void setUp() throws Exception {
        savedUserDir = System.getProperty("user.dir");
        tmp = new File("/tmp");
        wd = new File(tmp, "wd");
        System.setProperty("user.dir", wd.getAbsolutePath());
        
        fakeHome = new FakeHome();
    }
    
    @After
    public void tearDown() {
        System.setProperty("user.dir", savedUserDir);
    }
    
    @Test
    public void testReadConsole() throws Exception {
        ConfigBuilder configBuilder = new ConfigBuilder();
        configBuilder.read(new InputStreamReader(getClass().getResourceAsStream("ConfigTest.console.xml"), "utf-8"), wd);
        Config config = configBuilder.config;
        assertEquals(Arrays.asList(new File(wd, "foo1.jar"), new File(tmp, "foo2.jar")), config.getClasspath());
        assertEquals(Arrays.asList("Foundation", "AppKit"), config.getFrameworks());
        assertEquals(Arrays.asList(
                new Config.Lib("dl", true),
                new Config.Lib("/tmp/wd/libs/libmy.a", true),
                new Config.Lib("/tmp/wd/libs/foo.o", true),
                new Config.Lib("/usr/lib/libbar.a", false)
                ), config.getLibs());
        assertEquals(Arrays.asList(
                new Resource(new File(wd, "resources")), 
                new Resource(new File("/usr/share/resources")),
                new Resource(null, null).include("data/**/*"),
                new Resource(null, null).include("videos/**/*.avi"),
                new Resource(new File(wd, "resources"), "data")
                    .include("**/*.png")
                    .exclude("**/foo.png")
                    .flatten(true)
                ), config.getResources());
        assertEquals(Arrays.asList("javax.**.*"), config.getForceLinkClasses());
        assertEquals(OS.macosx, config.getOs());
        assertEquals(2, config.getArchs().size());
        assertEquals(Arch.x86, config.getArchs().get(0));
        assertEquals(Arch.x86_64, config.getArchs().get(1));
    }
    
    @Test
    public void testReadOldConsole() throws Exception {
        ConfigBuilder configBuilder = new ConfigBuilder();
        configBuilder.read(new InputStreamReader(getClass().getResourceAsStream("ConfigTest.old.console.xml"), "utf-8"), wd);
        Config config = configBuilder.config;
        assertEquals(Arrays.asList(new File(wd, "foo1.jar"), new File(tmp, "foo2.jar")), config.getClasspath());
        assertEquals(Arrays.asList("Foundation", "AppKit"), config.getFrameworks());
        assertEquals(Arrays.asList(
                new Config.Lib("dl", true),
                new Config.Lib("/tmp/wd/libs/libmy.a", true),
                new Config.Lib("/tmp/wd/libs/foo.o", true),
                new Config.Lib("/usr/lib/libbar.a", false)
                ), config.getLibs());
        assertEquals(Arrays.asList(new Resource(new File("/tmp/wd/resources")), 
                new Resource(new File("/usr/share/resources"))),
                config.getResources());
        assertEquals(Arrays.asList("javax.**.*"), config.getForceLinkClasses());
        assertEquals(OS.macosx, config.getOs());
        assertEquals(1, config.getArchs().size());
        assertEquals(Arch.x86, config.getArchs().get(0));
    }
    
    @Test
    public void testWriteConsole() throws Exception {
        ConfigBuilder configBuilder = new ConfigBuilder();
        configBuilder.addClasspathEntry(new File("foo1.jar"));
        configBuilder.addClasspathEntry(new File(tmp, "foo2.jar"));
        configBuilder.addFramework("Foundation");
        configBuilder.addFramework("AppKit");
        configBuilder.addLib(new Config.Lib("dl", true));
        configBuilder.addLib(new Config.Lib("libs/libmy.a", true));
        configBuilder.addLib(new Config.Lib("libs/foo.o", true));
        configBuilder.addLib(new Config.Lib("/usr/lib/libbar.a", false));
        configBuilder.addResource(new Resource(new File("/tmp/wd/resources")));
        configBuilder.addResource(new Resource(new File("/usr/share/resources")));
        configBuilder.addResource(new Resource(new File("/tmp/wd"), null).include("data/**/*"));
        configBuilder.addResource(new Resource(null, null).include("videos/**/*.avi"));
        configBuilder.addResource(
                new Resource(new File("/tmp/wd/resources"), "data")
                    .include("**/*.png")
                    .exclude("**/foo.png")
                    .flatten(true));
        configBuilder.addForceLinkClass("javax.**.*");
        configBuilder.os(OS.macosx);
        configBuilder.archs(Arch.x86, Arch.x86_64);
        
        StringWriter out = new StringWriter();
        configBuilder.write(out, wd);
        assertEquals(IOUtils.toString(getClass().getResourceAsStream("ConfigTest.console.xml")), out.toString());
    }

    

    private File createMergeConfig(File tmpDir, String dir, String id, OS os, Arch arch, boolean jar) throws Exception {
        File p = new File(tmpDir, dir);
        for (OS os2 : OS.values()) {
            for (Arch arch2 : Arch.values()) {
                File root = new File(p, "META-INF/robovm/" + os2 + "/" + arch2);
                root.mkdirs();
                if (!new File(root, "robovm.xml").exists()) {
                    new ConfigBuilder().write(new File(root, "robovm.xml"));
                }
            }
        }

        File root = new File(p, "META-INF/robovm/" + os + "/" + arch);
        new ConfigBuilder()
            .addExportedSymbol(id.toUpperCase() + "*")
            .addForceLinkClass("com." + id.toLowerCase() + ".**")
            .addFrameworkPath(new File(root, id.toLowerCase() + "/bar"))
            .addFramework(id)
            .addLib(new Lib(id.toLowerCase(), true))
            .addLib(new Lib(new File(root, "lib" + id.toLowerCase() + ".a").getAbsolutePath(), true))
            .addResource(new Resource(new File(root, "resources")))
            .addWeakFramework("Weak" + id)
            .write(new File(root, "robovm.xml"));

        if (jar) {
            File jarFile = new File(tmpDir, p.getName() + ".jar");
            ZipUtil.pack(p, jarFile);
            FileUtils.deleteDirectory(p);
            return jarFile;
        } else {
            return p;
        }
    }
    
    private File createTempDir() throws IOException {
        final File tmp = File.createTempFile(getClass().getName(), ".tmp");
        tmp.delete();
        FileUtils.deleteDirectory(tmp);
        tmp.mkdirs();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    FileUtils.deleteDirectory(tmp);
                } catch (IOException e) {
                }
            }
        });
        return tmp;
    }
    
    @Test
    public void testMergeConfigsFromClasspath() throws Exception {
        File tmpDir = createTempDir();
        File cacheDir = new File(tmpDir, "cache");
        File p1 = createMergeConfig(tmpDir, "p1", "Foo", OS.macosx, Arch.x86, false);
        File p2 = createMergeConfig(tmpDir, "p2", "Wooz", OS.linux, Arch.x86, false);
        // Create a jar file with both x86 and x86_64 by first creating a folder for x86 in p3/ 
        // and then passing p3/ again but this time compress it to a jar.
                  createMergeConfig(tmpDir, "p3", "Baaz", OS.macosx, Arch.x86, false);
        File p3 = createMergeConfig(tmpDir, "p3", "Raaz", OS.macosx, Arch.x86_64, true);
        
        ConfigBuilder configBuilder = new ConfigBuilder();
        configBuilder.cacheDir(cacheDir);
        configBuilder.os(OS.macosx);
        configBuilder.arch(Arch.x86);
        configBuilder.targetType(ConsoleTarget.TYPE);
        configBuilder.mainClass("Main");
        configBuilder.addClasspathEntry(p1);
        configBuilder.addClasspathEntry(p2);
        configBuilder.addClasspathEntry(p3);
        configBuilder.addExportedSymbol("YADA*");
        configBuilder.addFrameworkPath(new File(p1, "yada"));
        configBuilder.addFramework("Yada");
        configBuilder.addForceLinkClass("org.yada.**");
        configBuilder.addLib(new Lib("yada", true));
        configBuilder.addResource(new Resource(new File(p1, "resources")));
        configBuilder.addWeakFramework("WeakYada");
        configBuilder.home(fakeHome);
        Config config = configBuilder.build();

        File p1X86Root = new File(p1, "META-INF/robovm/macosx/x86");
        File p3X86Cache = config.getCacheDir(config.getClazzes().getClasspathPaths().get(2));
        File p3X86Root = new File(p3X86Cache.getParentFile(), p3X86Cache.getName() + ".extracted/META-INF/robovm/macosx/x86");

        assertEquals(Arrays.asList("FOO*", "BAAZ*", "YADA*"), config.getExportedSymbols());
        assertEquals(Arrays.asList("com.foo.**", "com.baaz.**", "org.yada.**"), config.getForceLinkClasses());
        assertEquals(Arrays.asList(
                new File(p1X86Root, "foo/bar"), 
                new File(p3X86Root, "baaz/bar"), 
                new File(p1, "yada")), 
                config.getFrameworkPaths());
        assertEquals(Arrays.asList("Foo", "Baaz", "Yada"), config.getFrameworks());
        assertEquals(Arrays.asList(
                new Lib("foo", true), 
                new Lib(new File(p1X86Root, "libfoo.a").getAbsolutePath(), true), 
                new Lib("baaz", true), 
                new Lib(new File(p3X86Root, "libbaaz.a").getAbsolutePath(), true), 
                new Lib("yada", true)), 
                config.getLibs());
        assertEquals(Arrays.asList(
                new Resource(new File(p1X86Root, "resources")), 
                new Resource(new File(p3X86Root, "resources")), 
                new Resource(new File(p1, "resources"))), 
                config.getResources());
        assertEquals(Arrays.asList("WeakFoo", "WeakBaaz", "WeakYada"), config.getWeakFrameworks());
        
        // Make sure builder() returns a config which merges in x86_64 configs instead
        config = config.builder().arch(Arch.x86_64).build();
        
        File p3X86_64Cache = config.getCacheDir(config.getClazzes().getClasspathPaths().get(2));
        File p3X86_64Root = new File(p3X86_64Cache.getParentFile(), p3X86_64Cache.getName() + ".extracted/META-INF/robovm/macosx/x86_64");
        
        assertEquals(Arrays.asList("RAAZ*", "YADA*"), config.getExportedSymbols());
        assertEquals(Arrays.asList("com.raaz.**", "org.yada.**"), config.getForceLinkClasses());
        assertEquals(Arrays.asList(
                new File(p3X86_64Root, "raaz/bar"), 
                new File(p1, "yada")), 
                config.getFrameworkPaths());
        assertEquals(Arrays.asList("Raaz", "Yada"), config.getFrameworks());
        assertEquals(Arrays.asList(
                new Lib("raaz", true), 
                new Lib(new File(p3X86_64Root, "libraaz.a").getAbsolutePath(), true), 
                new Lib("yada", true)), 
                config.getLibs());
        assertEquals(Arrays.asList(
                new Resource(new File(p3X86_64Root, "resources")), 
                new Resource(new File(p1, "resources"))), 
                config.getResources());
        assertEquals(Arrays.asList("WeakRaaz", "WeakYada"), config.getWeakFrameworks());
    }

    @Test
    public void testCreateBuilderFromConfig() throws Exception {
        File tmpDir = createTempDir();
        File cacheDir = new File(tmpDir, "cache");
        
        ConfigBuilder configBuilder = new ConfigBuilder();
        configBuilder.tmpDir(tmpDir);
        configBuilder.cacheDir(cacheDir);
        configBuilder.os(OS.macosx);
        configBuilder.arch(Arch.x86);
        configBuilder.targetType(ConsoleTarget.TYPE);
        configBuilder.mainClass("Main");
        configBuilder.addBootClasspathEntry(new File(tmpDir, "bcp1"));
        configBuilder.addBootClasspathEntry(new File(tmpDir, "bcp2"));
        configBuilder.addBootClasspathEntry(new File(tmpDir, "bcp3"));
        configBuilder.addClasspathEntry(new File(tmpDir, "cp1"));
        configBuilder.addClasspathEntry(new File(tmpDir, "cp2"));
        configBuilder.addClasspathEntry(new File(tmpDir, "cp3"));
        configBuilder.addExportedSymbol("YADA*");
        configBuilder.addFrameworkPath(new File(tmpDir, "yada"));
        configBuilder.addFramework("Yada");
        configBuilder.addForceLinkClass("org.yada.**");
        configBuilder.addLib(new Lib("yada", true));
        configBuilder.addResource(new Resource(new File(tmpDir, "resources")));
        configBuilder.addWeakFramework("WeakYada");
        configBuilder.addPluginArgument("foo:bar=yada");
        configBuilder.home(fakeHome);

        Config config = configBuilder.build();
        
        ConfigBuilder configBuilder2 = config.builder();
        configBuilder2.arch(Arch.arm64);
        Config config2 = configBuilder2.build();
        
        assertNotSame(config, config2);
        assertEquals(config.getTmpDir(), config2.getTmpDir());
        assertEquals(config.getCacheDir().getParentFile().getParentFile(), 
                config2.getCacheDir().getParentFile().getParentFile());
        assertEquals(config.getOs(), config2.getOs());
        assertEquals(config.getMainClass(), config2.getMainClass());
        assertEquals(config.getBootclasspath(), config2.getBootclasspath());
        assertNotSame(config.getBootclasspath(), config2.getBootclasspath());
        assertEquals(config.getClasspath(), config2.getClasspath());
        assertNotSame(config.getClasspath(), config2.getClasspath());
        assertEquals(config.getExportedSymbols(), config2.getExportedSymbols());
        assertNotSame(config.getExportedSymbols(), config2.getExportedSymbols());
        assertEquals(config.getFrameworkPaths(), config2.getFrameworkPaths());
        assertNotSame(config.getFrameworkPaths(), config2.getFrameworkPaths());
        assertEquals(config.getFrameworks(), config2.getFrameworks());
        assertNotSame(config.getFrameworks(), config2.getFrameworks());
        assertEquals(config.getForceLinkClasses(), config2.getForceLinkClasses());
        assertNotSame(config.getForceLinkClasses(), config2.getForceLinkClasses());
        assertEquals(config.getLibs(), config2.getLibs());
        assertNotSame(config.getLibs(), config2.getLibs());
        assertEquals(config.getResources(), config2.getResources());
        assertNotSame(config.getResources(), config2.getResources());
        assertEquals(config.getPluginArguments(), config2.getPluginArguments());
        assertNotSame(config.getPluginArguments(), config2.getPluginArguments());
        
        assertEquals(Arch.arm64, config2.getArch());
        
        assertFalse(config.getPlugins().equals(config2.getPlugins()));
        assertNotSame(config.getTarget(), config2.getTarget());
        assertNotSame(config.getClazzes(), config2.getClazzes());
    }

    @Test
    public void testGetFileName() throws Exception {
        assertEquals("201a6b3053cc1422d2c3670b62616221d2290929.class.o", Config.getFileName("Foo", "class.o", 0));
        assertEquals("201a6b3053cc1422d2c3670b62616221d2290929.class.o", Config.getFileName("Foo", "class.o", 1));
        assertEquals("201a6b3053cc1422d2c3670b62616221d2290929.class.o", Config.getFileName("Foo", "class.o", 10));
        assertEquals("Foo.class.o", Config.getFileName("Foo", "class.o", 11));

        assertEquals("com/example/201a6b3053cc1422d2c3670b62616221d2290929.class.o",
                Config.getFileName("com/example/Foo", "class.o", 0));
        assertEquals("com/example/201a6b3053cc1422d2c3670b62616221d2290929.class.o",
                Config.getFileName("com/example/Foo", "class.o", 1));
        assertEquals("com/example/201a6b3053cc1422d2c3670b62616221d2290929.class.o",
                Config.getFileName("com/example/Foo", "class.o", 10));
        assertEquals("com/example/Foo.class.o", Config.getFileName("com/example/Foo", "class.o", 11));

        assertEquals("com/example/AB9ca44297c0e0d22df654119dce73ee52d3d51c71.class.o",
                Config.getFileName("com/example/ABCDEFGIHJABCDEFGIHJABCDEFGIHJABCDEFGIHJABCDEFGIHJ", "class.o", 50));
    }
}
