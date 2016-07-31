package aura.compiler;


import aura.compiler.config.*;
import aura.compiler.log.ConsoleLogger;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static aura.compiler.CommandArgs.*;

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
public class ConfigBuilderArgParser extends ArgParser<ConfigBuilder> {
    String[] args;
    Options options;
    private boolean run;
    private boolean archive;

    public ConfigBuilderArgParser(Options options, String[] args) {
        super(options, args);
        this.args = getArgs();
        this.options = getOptions();
    }

    @Override
    public ConfigBuilder populateObject(ConfigBuilder configBuilder) {
        try {
            validate();
        } catch (ParseException e) {
            e.printStackTrace();
            return configBuilder;
        }
        for (Option option : getCmd().getOptions()) {
            configBuilder = processArg(option, option.getArgName(), configBuilder);
            configBuilder = processArg(option, option.getLongOpt(), configBuilder);
        }
        return configBuilder;
    }

    public ConfigBuilder processArg(Option option, String argName,ConfigBuilder configBuilder) {
        if (argName == null) {
            return configBuilder;
        }

        switch (argName) {
            case BOOTCLASSPATH:
            case BOOTCLASSPATH_LONG:
                return setBootClass(configBuilder, option.getValuesList());

            case CLASSPATH:
            case CLASSPATH_LONG:
                return setClasspath(configBuilder, option.getValuesList());

            case CACHE:
            case CACHE_LONG:
                return setCache(configBuilder, option.getValue());

            case CLEAN_LONG:
                  return enableClean(configBuilder);

            case CONFIG:
            case CONFIG_LONG:
                 return setConfig(configBuilder, option.getValuesList());

            case DUMP_INTERMEDIATES:
            case DUMP_INTERMEDIATES_LONG:
                 return dumpIntermediates(configBuilder);

            case DUMP_CONFIG:
            case DUMP_CONFIG_LONG:
                 return dumpConfiguration(configBuilder, option.getValue());

            case EXPORTED_SYMBOLS:
            case EXPORTED_SYMBOLS_LONG:
                 return setExportedSymbols(configBuilder, option.getValuesList());

            case UNHIDE_SYMBOLS:
            case UNHIDE_SYMBOLS_LONG:
                 return setUnhideSymbols(configBuilder, option.getValuesList());

            case WEAK_FRAMEWORKS:
            case WEAK_FRAMEWORKS_LONG:
                 return setWeakFrameworks(configBuilder, option.getValuesList());

            case FRAMEWORKS:
            case FRAMEWORKS_LONG:
                return setFrameworks(configBuilder, option.getValuesList());


            case SKIP_RUNTIME:
            case SKIP_RUNTIME_LONG:
                 return skipRuntime(configBuilder);

            case DYNAMIC_JNI:
            case DYNAMIC_JNI_LONG:
                 return enableDynamicJNI(configBuilder);

            case DEBUG_LIBS:
            case DEBUG_LIBS_LOGS:
                return setDebugLibs(configBuilder);

            case OUTPUT_DIR:
                return setOutDir(configBuilder, option.getValue());

            case CC_COMPILER:
                return setCCompiler(configBuilder, option.getValue());

            case HOME:
            case HOME_LONG:
                return setHome(configBuilder, option.getValue());

            case TEMP:
            case TEMP_LONG:
                return setTemp(configBuilder, option.getValue());

            case JAR:
                return setJar(configBuilder, option.getValuesList());

            case OUT_BIN:
            case OUT_BIN_LONG:
                return setOutBin(configBuilder, option.getValue());

            case PROPERTIES:
            case PROPERTIES_LONG:
                return setProperties(configBuilder, option.getValuesList());

            case RESOURCES:
            case RESOURCES_LONG:
                 return setResources(configBuilder, option.getValuesList());

            case BUILD_AS_LIB_LONG:
                return setBuildAsLib(configBuilder);

            case TREESHAKE:
            case TREESHAKE_LONG:
                return setTreeshake(configBuilder, option.getValue());

            case FORCE_LINK:
                return setForceLink(configBuilder, option.getValuesList());

            case THREADS:
            case THREADS_LONG:
                return setThreads(configBuilder, option.getValue());

            case RUN:
                run = true;
                return setRun(configBuilder);

            case VERBOSE:
            case VERBOSE_LONG:
                return setVerbose(configBuilder);

            case VERSION_LONG:
                printVersionAndExit();
                break;

            case HELP:
            case HELP_LONG:
                usage(options);
                break;

            case LIBS:
            case LIBS_LONG:
                return setLibs(configBuilder, option.getValuesList());

            default:
                invalidOption(option);
                break;
        }

        return configBuilder;
    }

    public ConfigBuilder setResources(ConfigBuilder configBuilder, List<String> values) {
        Optional.ofNullable(values).ifPresent(l -> l.forEach(value -> {
            File resourceFile = new File(value);
            if (!resourceFile.exists()) {
                System.err.println("Resource file " + resourceFile + " does not exist, skipping");
            } else {
                configBuilder.addResource(new Resource(resourceFile));
            }
        }));
        return configBuilder;
    }

    public ConfigBuilder setFrameworks(ConfigBuilder configBuilder, List<String> values) {
        Optional.ofNullable(values).ifPresent( l -> l.forEach(configBuilder::addFramework));
        return configBuilder;
    }

    public ConfigBuilder setWeakFrameworks(ConfigBuilder configBuilder, List<String> values) {
        Optional.ofNullable(values).ifPresent( l -> l.forEach(configBuilder::addWeakFramework));
        return configBuilder;
    }

    public ConfigBuilder setUnhideSymbols(ConfigBuilder configBuilder, List<String> values) {
        Optional.ofNullable(values).ifPresent( l -> l.forEach(configBuilder::addUnhideSymbol));
        return configBuilder;
    }

    public ConfigBuilder setExportedSymbols(ConfigBuilder configBuilder, List<String> values) {
        Optional.ofNullable(values).ifPresent( l -> l.forEach(configBuilder::addExportedSymbol));
        return configBuilder;
    }

    public void printVersionAndExit() {
        System.err.println(Version.getVersion());
        System.exit(0);
    }

    public ConfigBuilder setVerbose(ConfigBuilder configBuilder) {
        configBuilder.logger(new ConsoleLogger(true));
        return configBuilder;
    }

    public ConfigBuilder setThreads(ConfigBuilder configBuilder, String value) {
        String s = value;
        try {
            int n = Integer.parseInt(s);
            // Make sure n > 0 and cap at 128 threads.
            n = Math.max(n, 1);
            n = Math.min(n, 128);
            configBuilder.threads(n);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Unparsable thread count: " + s);
        }
        return configBuilder;
    }

    public ConfigBuilder setForceLink(ConfigBuilder configBuilder, List<String> values) {
        Optional.ofNullable(values).ifPresent(l ->l.forEach(value -> {
            if (value.contains(":")) {
                for (String s : value.split(":")) {
                    s = s.replace('#', '*');
                    configBuilder.addForceLinkClass(s);
                }
            } else {
                value = value.replace('#','*');
                configBuilder.addForceLinkClass(value);
            }
        }));

        return configBuilder;
    }

    public ConfigBuilder setTreeshake(ConfigBuilder configBuilder, String value) {
        configBuilder.treeShakerMode(Config.TreeShakerMode.valueOf(value));
        return configBuilder;
    }


    public ConfigBuilder dumpConfiguration(ConfigBuilder configBuilder, String value) {
        if (value != null && value.equals("-")) {
            try {
                configBuilder.write(new OutputStreamWriter(System.out), new File("."));
                configBuilder.write(new File(value));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return configBuilder;
    }

    public ConfigBuilder dumpIntermediates(ConfigBuilder configBuilder) {
        configBuilder.dumpIntermediates(true);
        return configBuilder;
    }

    public ConfigBuilder skipRuntime(ConfigBuilder configBuilder) {
        configBuilder.skipRuntimeLib(true);
        return configBuilder;
    }
    public ConfigBuilder enableDynamicJNI(ConfigBuilder configBuilder) {
        configBuilder.useDynamicJni(true);
        return configBuilder;
    }

    public ConfigBuilder setConfig(ConfigBuilder configBuilder, List<String> values) {
        for (String value : values) {
            try {
                configBuilder.read(new File(value));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return configBuilder;
    }

    public ConfigBuilder setRun(ConfigBuilder configBuilder) {
        configBuilder.skipInstall(true);
        return configBuilder;
    }

    public ConfigBuilder setOutBin(ConfigBuilder configBuilder, String value) {
        configBuilder.executableName(value);
        return configBuilder;
    }

    /**
     * Set configuration properties for os, arch, cpu, target
     * @param configBuilder
     * @param values Array of property=value. Valid keys - os, arch, cpu, target
     *               os - linux, macosx, ios, freebsd
     *               arch - x86, x86_64, thumbv7, arm64
     *               target - console, auto
     * @return
     */
    public ConfigBuilder setProperties(ConfigBuilder configBuilder, List<String> values) {
        List<Arch> archs = new ArrayList<>();
        OS os = null;
        String target = null;

        if (values == null) {
            return configBuilder;
        }

        for (String value : values) {
            if (isPropertyType(value, "os")) {
                os = getOSFromProperty(value);
            }
            if (isPropertyType(value, "target")) {
                target = getTargetFromProperty(value);
            }
            if (isPropertyType(value, "arch")) {
                archs.add(getArchProperties(value));
            }
        }

        if (archs.size() > 0) {
            configBuilder.archs(archs);
        }
        if (notNull(os)) {
            configBuilder.os(os);
        }
        if (notNull(target)) {
            configBuilder.targetType(target);
        }
        return configBuilder;
    }

    private boolean isPropertyType(String propertyString, String type) {
        return (propertyString.startsWith(type) && propertyString.contains("="));
    }


    private boolean notNull(Object o) {
        return o != null;
    }

    private String getTargetFromProperty(String propertyString) {
        String[] properties = propertyString.split("=");
        if (properties[0].equals("target")) {
            return (properties[1]);
        }
        return  null;
    }

    private OS getOSFromProperty(String propertyString) {
        String[] properties = propertyString.split("=");
        if (properties[0].equals("os")) {
            return ("auto".equals(properties[0]) ? null : OS.valueOf(properties[1]));
        }
        return null;
    }

    private Arch getArchProperties(String value) {
        String[] properties = value.split("=");
        if (properties[0].contains("arch")) {
            return Arch.valueOf(properties[1]);
        }
        return null;
    }

    public ConfigBuilder setJar(ConfigBuilder configBuilder, List<String> values) {
        Optional.ofNullable(values).ifPresent( l -> l.forEach(value -> {
            configBuilder.addBootClasspathEntry(new File(value));
        }));
        return configBuilder;
    }

    public ConfigBuilder setDebugLibs(ConfigBuilder configBuilder) {
        configBuilder.debug(true);
        configBuilder.useDebugLibs(true);
        return configBuilder;
    }

    @Override
    public void usage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( "aura [args] mainClass", options );
    }

    public ConfigBuilder setLibs(ConfigBuilder configBuilder, List<String> values) {
        Optional.ofNullable(values).ifPresent( l -> l.forEach(value -> {
            if (value.contains(":")) {
                for (String s : value.split(":")) {
                    configBuilder.addLib(new Config.Lib(s,true));
                }
            } else {
                configBuilder.addLib(new Config.Lib(value, true));
            }
        }));
        return configBuilder;
    }

    public ConfigBuilder setTemp(ConfigBuilder configBuilder, String value) {
        configBuilder.tmpDir(new File(value));
        return configBuilder;
    }

    public ConfigBuilder setHome(ConfigBuilder configBuilder, String value) {
        configBuilder.home(new Config.Home(new File(value)));
        return configBuilder;
    }

    public ConfigBuilder setCCompiler(ConfigBuilder configBuilder, String value) {
        configBuilder.ccBinPath(new File(value));
        return configBuilder;
    }

    public ConfigBuilder enableClean(ConfigBuilder configBuilder) {
        configBuilder.clean(true);
        return configBuilder;
    }

    public ConfigBuilder setCache(ConfigBuilder configBuilder, String value) {
        configBuilder.cacheDir(new File(value));
        return configBuilder;
    }

    public void invalidOption(Option option) {
        System.out.println("Invlid use for options: " + option.getLongOpt() + "(" +
                                                        option.getArgName() +") " + option.getDescription());
        System.exit(1);
    }

    public ConfigBuilder setOutDir(ConfigBuilder configBuilder, String value) {
        configBuilder.installDir(new File(value));
        return configBuilder;
    }

    public ConfigBuilder setClasspath(ConfigBuilder configBuilder, List<String> values) {
        Optional.ofNullable(values).ifPresent(l -> l.forEach( value -> {
            configBuilder.addClasspathEntry(new File(value));
        }));
        return configBuilder;
    }

    public ConfigBuilder setBootClass(ConfigBuilder configBuilder, List<String> values) {
        Optional.ofNullable(values).ifPresent(l -> l.forEach( value -> {
            configBuilder.addBootClasspathEntry(new File(value));
        }));
        return configBuilder;
    }

    public ConfigBuilder setBuildAsLib(ConfigBuilder configBuilder) {
        configBuilder.buildAsLibrary(true);
        return configBuilder;
    }


    public boolean validateArgs(CommandLine cli) throws IllegalArgumentException {
       if (archive && run) {
            throw new IllegalArgumentException("Specify either -run or -createipa/-archive, not both");
       } else {
           if (cli.getOptions() == null || cli.getOptions().length == 0 ) {
               return false;
           } else {
               if (cli.getOptionValue(CLASSPATH) == null) {
                   return false;
               }
           }
       }
        return true;
    }

}
