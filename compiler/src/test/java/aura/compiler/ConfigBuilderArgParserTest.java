package aura.compiler;

import aura.compiler.config.*;
import aura.compiler.target.ConsoleTarget;
import org.apache.commons.cli.*;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/*
 * Copyright (C) 2016 Aura Project
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
public class ConfigBuilderArgParserTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Rule
    public TemporaryFolder folder= new TemporaryFolder();



    ConfigBuilder configBuilder;
    Options options;
    ConfigBuilderArgParser configBuilderArgParser;

    @Before
    public void setUp() throws IOException {
        configBuilder = new ConfigBuilder();
        options = CommandArgs.options();
        configBuilderArgParser = new ConfigBuilderArgParser(new Options(), new String[] {});
    }

    @Test
    public void testSetVerbose() throws Exception {
        configBuilder = configBuilderArgParser.setVerbose(configBuilder);
        Config config = configBuilder.getConfig();
        assert(config.getLogger() != null);
    }

    @Test
    public void testSetThreads() throws Exception {
        configBuilder = configBuilderArgParser.setThreads(configBuilder,"10");
        assert(configBuilder.getConfig().getThreads() == 10);
        /* should cap at 128 */
        configBuilder = configBuilderArgParser.setThreads(configBuilder,"200");
        assert(configBuilder.getConfig().getThreads() == 128);
    }

    @Test
    public void testSetForceLink() throws Exception {
        String filePath = "/tmp/filePath";
        configBuilder = configBuilderArgParser.setForceLink(configBuilder, Collections.singletonList(filePath));
        assert(configBuilder.getConfig().getForceLinkClasses().contains(filePath));
    }

    @Test
    public void testSetTreeshake() throws Exception {
        configBuilder = configBuilderArgParser.setTreeshake(configBuilder, Config.TreeShakerMode.aggressive.toString());
        assert(configBuilder.getConfig().getTreeShakerMode().equals(Config.TreeShakerMode.aggressive));

        exception.expect(IllegalArgumentException.class);
        configBuilder = configBuilderArgParser.setTreeshake(configBuilder, "invalid");

    }

    @Test
    public void testSetRun() throws Exception {
        configBuilder = configBuilderArgParser.setRun(configBuilder);
        assert(configBuilder.getConfig().isSkipInstall());
    }

    @Test
    public void testSetOutBin() throws Exception {
        String binName = "MyBinName";
        configBuilder = configBuilderArgParser.setOutBin(configBuilder,binName);
        assert(configBuilder.getConfig().getExecutableName().equals(binName));
    }

    @Test
    public void testSetProperties() throws Exception {
        List<String> validProperties = Arrays.asList(
                "arch=x86",
                "os=linux",
                "target=console"
        );

        configBuilder = configBuilderArgParser.setProperties(configBuilder, validProperties);
        assert(configBuilder.getConfig().getArchs().contains(Arch.x86));
        assert(configBuilder.getConfig().getOs() == OS.linux);
        assert(configBuilder.getConfig().getTargetType().equals(ConsoleTarget.TYPE));

        exception.expect(IllegalArgumentException.class);
        configBuilder = configBuilderArgParser.setProperties(configBuilder, Collections.singletonList("arch=invalid"));

        exception.expect(IllegalArgumentException.class);
        configBuilder = configBuilderArgParser.setProperties(configBuilder, Collections.singletonList("os=invalid"));

        exception.expect(IllegalArgumentException.class);
        configBuilder = configBuilderArgParser.setProperties(configBuilder, Collections.singletonList("invalid=invalid"));

        exception.expect(IllegalArgumentException.class);
        configBuilder = configBuilderArgParser.setProperties(configBuilder, Collections.singletonList("invalid"));

    }

    @Test
    public void testSetJar() throws Exception {
        String jarFile = "/tmp/test.jar";
        configBuilder = configBuilderArgParser.setJar(configBuilder, Collections.singletonList(jarFile));
        assert(configBuilder.getConfig().getBootclasspath().contains(new File(jarFile)));
    }

    @Test
    public void testSetDebugLibs() throws Exception {
        configBuilder = configBuilderArgParser.setDebugLibs(configBuilder);
        assert(configBuilder.getConfig().isDebug());
        assert(configBuilder.getConfig().isUseDebugLibs());
    }

    @Test
    public void testUsage() throws Exception {
        final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        System.setOut(new PrintStream(stdout));
        configBuilderArgParser.usage(options);
        assert(stdout.toString().contains("separated list of directories"));
    }

    @Test
    public void testSetLibs() throws Exception {
        configBuilder = configBuilderArgParser.setLibs(configBuilder, Collections.singletonList("libtest"));
        List<Config.Lib> configList = configBuilder.getConfig().getLibs();
        assert(configList.contains(new Config.Lib("libtest", true)));
        configBuilder = configBuilderArgParser.setLibs(configBuilder, Arrays.asList("libtest2:libtest3", "libtest4"));
        configList = configBuilder.getConfig().getLibs();
        assert(configList.contains(new Config.Lib("libtest2", true)));
        assert(configList.contains(new Config.Lib("libtest3", true)));
        assert(configList.contains(new Config.Lib("libtest4", true)));
    }

    @Test
    public void testSetTemp() throws Exception {
        String tempDir = "/tmp";
        configBuilder = configBuilderArgParser.setTemp(configBuilder, tempDir);
        assert(configBuilder.getConfig().getTmpDir().getAbsolutePath().equals(tempDir));
    }

    @Test
    public void testSetHome() throws Exception {
        String homeDir = "/tmp/homeDir";
        try {
            Config.Home.validate(new File(homeDir));
            configBuilder = configBuilderArgParser.setHome(configBuilder, homeDir);
            assert(configBuilder.getConfig().getHome() == new Config.Home(new File(homeDir)));
        } catch (Exception e) {
            Assume.assumeNoException(e);
        }
    }

    @Test
    public void testSetConfig() throws Exception {
        String testConfig = this.getClass().getResource("config/ConfigTest.console.xml").getFile();
        configBuilderArgParser.setConfig(configBuilder, Collections.singletonList(testConfig));
        assert(configBuilder.getConfig().getClasspath().contains(new File("/tmp/foo2.jar")));
    }

    @Test
    public void testDumpConfiguration() throws Exception {
        File tempFile = folder.newFile("test");
        configBuilderArgParser.dumpConfiguration(configBuilder, tempFile.getAbsolutePath());
        assert(tempFile.getTotalSpace() > 0);
    }

    @Test
    public void testSkipRuntime() throws Exception {
        configBuilderArgParser.skipRuntime(configBuilder);
        assert(configBuilder.getConfig().isSkipRuntimeLib());
    }
    @Test
    public void testDynamicJNI() throws Exception {
        configBuilderArgParser.enableDynamicJNI(configBuilder);
        assert(configBuilder.getConfig().isUseDynamicJni());
    }

    @Test
    public void testDumpIntermediates() throws Exception {
        configBuilderArgParser.dumpIntermediates(configBuilder);
        assert(configBuilder.getConfig().isDumpIntermediates());
    }

    @Test
    public void testSetCCompiler() throws Exception {
        String ccCompiler = "/usr/local/bin/clang";
        configBuilder = configBuilderArgParser.setCCompiler(configBuilder, ccCompiler);
        assert(configBuilder.getConfig().getCcBinPath().equals(new File(ccCompiler)));
    }

    @Test
    public void testEnableClean() throws Exception {
        configBuilderArgParser.enableClean(configBuilder);
        assert(configBuilder.getConfig().isClean());
    }

    @Test
    public void testSetOutDir() throws Exception {
        String outDir = "/tmp/outDir";
        configBuilder = configBuilderArgParser.setOutDir(configBuilder, outDir);
        assert(configBuilder.getConfig().getInstallDir().equals(new File(outDir)));
    }

    @Test
    public void testSetClasspath() throws Exception {
        String additionalPath = "/tmp/additionalClassPath";
        configBuilder = configBuilderArgParser.setClasspath(configBuilder, Collections.singletonList(additionalPath));
        assertTrue(configBuilder.getConfig().getClasspath().contains(new File(additionalPath)));
    }

    @Test
    public void testSetBootClass() throws Exception {
        String bootClass = "bootClass";
        configBuilder = configBuilderArgParser.setBootClass(configBuilder, Collections.singletonList(bootClass));
        assertTrue(configBuilder.getConfig().getBootclasspath().contains(new File(bootClass)));
    }


    @Test
    public void setSetFramework() throws Exception {
        String framework = "MyFramework";
        configBuilder = configBuilderArgParser.setFrameworks(configBuilder, Collections.singletonList(framework));
        assert(configBuilder.getConfig().getFrameworks().contains(framework));

    }

    @Test
    public void testSetWeakFramework() {
        String framework = "MyFramework";
        configBuilder = configBuilderArgParser.setWeakFrameworks(configBuilder, Collections.singletonList(framework));
        assert(configBuilder.getConfig().getWeakFrameworks().contains(framework));
    }

    @Test
    public void testSetUnhideSymbols() {
        String symbol = "testSymbol";
        configBuilder = configBuilderArgParser.setUnhideSymbols(configBuilder, Collections.singletonList(symbol));
        assert(configBuilder.getConfig().getUnhideSymbols().contains(symbol));
    }

    @Test
    public void testSetExportedSymbols() {
        String symbol = "testSymbol";
        configBuilder = configBuilderArgParser.setExportedSymbols(configBuilder, Collections.singletonList(symbol));
        assert(configBuilder.getConfig().getExportedSymbols().contains(symbol));
    }

    @Test
    public void testValidateArgs() throws ParseException {
        CommandLineParser commandLineParser = new DefaultParser();
        CommandLine cmd = commandLineParser.parse( options, new String[]{});

        /* Blank args should call usage */
        assert(!configBuilderArgParser.validateArgs(cmd));

        cmd = commandLineParser.parse( options, new String[]{"-c", "/tmp/classPath"});
        assert(configBuilderArgParser.validateArgs(cmd));
    }

    @Test
    public void testSetResources() throws IOException {
        String invalidFile = "/tmp/doesntexist";
        configBuilder = configBuilderArgParser.setResources(configBuilder, Collections.singletonList(invalidFile));
        assertFalse(configBuilder.getConfig().getResources().contains(new Resource(new File(invalidFile))));

        File testFile = folder.newFile("testFile");
        configBuilder = configBuilderArgParser.setResources(configBuilder, Collections.singletonList(testFile.getAbsolutePath()));
        assert(configBuilder.getConfig().getResources().contains(new Resource(testFile)));
    }

}