package aura.compiler;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

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
public class CommandArgs {

    public static final String BOOTCLASSPATH = "b";
    public static final String BOOTCLASSPATH_LONG = "bootclasspath";
    public static final String BUILD_AS_LIB_LONG = "buildlib";
    public static final String CACHE = "C";
    public static final String CACHE_LONG = "cache";
    public static final String CC_COMPILER = "cc";
    public static final String CLASSPATH = "c";
    public static final String CLASSPATH_LONG = "classpath";
    public static final String CLEAN_LONG = "clean";
    public static final String DEBUG_LIBS = "g";
    public static final String DEBUG_LIBS_LOGS = "use-debug-libs";
    public static final String FORCE_LINK = "forcelink";
    public static final String HELP = "?";
    public static final String HELP_LONG = "help";
    public static final String HOME = "h";
    public static final String HOME_LONG = "home";
    public static final String JAR = "J";
    public static final String LIBS = "l";
    public static final String LIBS_LONG = "libs";
    public static final String OUTPUT_DIR = "d";
    public static final String OUT_BIN = "o";
    public static final String OUT_BIN_LONG = "out";
    public static final String PROPERTIES = "p";
    public static final String PROPERTIES_LONG = "properties";
    public static final String RESOURCES = "r";
    public static final String RESOURCES_LONG = "resources";
    public static final String RUN = "run";
    public static final String TEMP = "t";
    public static final String TEMP_LONG = "tmp";
    public static final String THREADS = "j";
    public static final String THREADS_LONG = "threads";
    public static final String TREESHAKE = "m";
    public static final String TREESHAKE_LONG = "treeshake";
    public static final String VERBOSE = "v";
    public static final String VERBOSE_LONG = "verbose";
    public static final String VERSION_LONG = "version";
    public static final String CONFIG = "f";
    public static final String CONFIG_LONG = "config";
    public static final String DUMP_CONFIG = "D";
    public static final String DUMP_CONFIG_LONG = "dump";
    public static final String DUMP_INTERMEDIATES = "I";
    public static final String DUMP_INTERMEDIATES_LONG = "dumpintermediates";
    public static final String DYNAMIC_JNI = "n";
    public static final String DYNAMIC_JNI_LONG = "dynamicjni";
    public static final String SKIP_RUNTIME = "s";
    public static final String SKIP_RUNTIME_LONG = "skipruntime";
    public static final String EXPORTED_SYMBOLS = "x";
    public static final String EXPORTED_SYMBOLS_LONG = "exportedsymbols";
    public static final String UNHIDE_SYMBOLS = "u";
    public static final String UNHIDE_SYMBOLS_LONG = "unhidesymbols";
    public static final String FRAMEWORKS = "F";
    public static final String FRAMEWORKS_LONG = "frameworks";
    public static final String WEAK_FRAMEWORKS = "w";
    public static final String WEAK_FRAMEWORKS_LONG = "weakframeworks";


    public static Options options() {

        Options options = new Options();

        options.addOption(Option.builder(EXPORTED_SYMBOLS)
                .required(false)
                .desc("  : separated list of symbols that should be exported" +
                        " when linking the executable. This can be used when" +
                        " linking in function which will be called using bro." +
                        " Wildcards can be used. * matches zero or more characters," +
                        " ? matches one character. [abc], [a-z] matches one character" +
                        " from the specified set of characters.: separated list of directories, JAR archives, and ZIP ")
                .longOpt(EXPORTED_SYMBOLS_LONG)
                .hasArgs()
                .build());

        options.addOption(Option.builder(UNHIDE_SYMBOLS)
                .required(false)
                .desc("  : separated list of global hidden symbols in linked in static" +
                        " libraries or frameworks that should be unhidden to be" +
                        " accessible to bro @Bridge methods. Wildcards are not" +
                        " supported. Unhidden symbols will always be exported.")
                .longOpt(UNHIDE_SYMBOLS_LONG)
                .hasArgs()
                .build());

        options.addOption(Option.builder(FRAMEWORKS)
                .required(false)
                .desc(" : separated list of frameworks that should be included" +
                        " when linking the final executable.")
                .longOpt(FRAMEWORKS_LONG)
                .hasArgs()
                .build());


        options.addOption(Option.builder(WEAK_FRAMEWORKS)
                .required(false)
                .desc(" : separated list of frameworks that should be weakly linked" +
                        " into the final executable.: separated list of frameworks that should be included")
                .longOpt(WEAK_FRAMEWORKS_LONG)
                .hasArgs()
                .build());

        options.addOption(Option.builder(BOOTCLASSPATH)
                .required(false)
                .desc(": separated list of directories, JAR archives, and ZIP "
                        + " archives to search for class files. Used to locate the \n"
                        + "java.* and javax.* classes. Default is \n"
                        + "<aura-home>/lib/robovm-rt.jar.")
                .longOpt(BOOTCLASSPATH_LONG)
                .hasArgs()
                .build());

        options.addOption(Option.builder(CLASSPATH)
                .required(false)
                .desc("separated list of directories, JAR archives, and ZIP archives to search for class files.")
                .longOpt(CLASSPATH_LONG)
                .hasArgs()
                .build());

        options.addOption(Option.builder(DUMP_CONFIG)
                .required(false)
                .desc("Dumps a configuration XML file to the specified file. Specify" +
                        " '-' to dump the config to stdout.")
                .longOpt(DUMP_CONFIG_LONG)
                .hasArg()
                .build());

        options.addOption(Option.builder(DYNAMIC_JNI)
                .required(false)
                .desc("Dynamic JNI")
                .longOpt(DYNAMIC_JNI_LONG)
                .hasArg(false)
                .build());

        options.addOption(Option.builder(DUMP_INTERMEDIATES)
                .required(false)
                .desc("Dump intermediates")
                .longOpt(DUMP_INTERMEDIATES_LONG)
                .hasArg(false)
                .build());

        options.addOption(Option.builder(CONFIG)
                .required(false)
                .desc("Reads the specified configuration XML file. Values set in" +
                        " the file will override values set earlier in the command" +
                        " line. Later options will override values set in the XML file." +
                        " Can be specified multiple times to read multiple config files")
                .longOpt(CONFIG_LONG)
                .hasArgs()
                .build());

        options.addOption(Option.builder(CACHE)
                .required(false)
                .desc("Directory where cached compiled class files will be placed. Default is ~/.aura/cache")
                .longOpt(CACHE_LONG)
                .hasArg()
                .build());

        options.addOption(Option.builder()
                .required(false)
                .desc("Compile class files even if a compiled version already exists in the cache")
                .longOpt(CLEAN_LONG)
                .hasArg(false)
                .build());

        options.addOption(Option.builder(OUTPUT_DIR)
                .required(false)
                .desc("Install the generated executable and other files in <dir>. Default is <cwd>/<executableName>. " +
                        "Ignored if -run is specified")
                .hasArg()
                .build());

        options.addOption(Option.builder(CC_COMPILER)
                .required(false)
                .desc("Path to the c compiler binary. gcc and clang are supported")
                .hasArg()
                .build());

        options.addOption(Option.builder(HOME)
                .required(false)
                .desc("Directory where Aura runtime has been installed. Default search path: $AURA_HOME, " +
                        "~/Applications/aura~/.aura/home, /usr/local/lib/aura, /opt/aura, /usr/lib/aura")
                .longOpt(HOME_LONG)
                .hasArg()
                .build());

        options.addOption(Option.builder(TEMP)
                .required(false)
                .desc("Directory where temporary files will be placed during compilation. By default a new dir will " +
                        "be created under ${java.io.tmpdir}")
                .longOpt(TEMP_LONG)
                .hasArg()
                .build());

        options.addOption(Option.builder()
                .required(false)
                .desc("Use main class as specified by the manifest in this JAR archive.")
                .longOpt(JAR)
                .hasArg()
                .build());

        options.addOption(Option.builder(OUT_BIN)
                .required(false)
                .desc("The name of the target binary.")
                .longOpt(OUT_BIN_LONG)
                .hasArg()
                .build());

        options.addOption(Option.builder(PROPERTIES)
                .required(false)
                .desc("Properties [key=value]. Valid properties: os, arch, cpu,target")
                .longOpt(PROPERTIES_LONG)
                .hasArgs()
                .build());

        options.addOption(Option.builder(RESOURCES)
                .required(false)
                .desc("Add resource to application bundle")
                .longOpt(RESOURCES_LONG)
                .hasArgs()
                .build());

        options.addOption(Option.builder()
                .required(false)
                .desc("Build as library. This will skip the linking process(assumes -I)")
                .longOpt(BUILD_AS_LIB_LONG)
                .build());

        options.addOption(Option.builder()
                .required(false)
                .desc("':' separated list of class patterns matching" +
                        " classes that must be linked in even if not referenced" +
                        " (directly or indirectly) from the main class. If no main" +
                        " class is specified all classes will be linked in unless this" +
                        " option has been given. A pattern is an ANT style path pattern," +
                        " e.g. com.foo.**.bar.*.Main. An alternative syntax using # is" +
                        " also supported, e.g. com.##.#.Main.")
                .longOpt(FORCE_LINK)
                .hasArgs()
                .build());


        options.addOption(Option.builder(TREESHAKE)
                .required(false)
                .desc("The tree shaking algorithm to use. 'none', 'conservative' or" +
                        "'aggressive'. 'aggressive' will remove all unreachable method" +
                        " implementations when it's safe to do so. 'conservative' only" +
                        " removes unreachable methods marked as @WeaklyLinked. Methods" +
                        " in the main class and in force linked classes will never be" +
                        " stripped. Default is 'none'")
                .longOpt(TREESHAKE_LONG)
                .hasArg()
                .build());

        options.addOption(Option.builder(THREADS)
                .required(false)
                .desc("The number of threads to use during class compilation. By" +
                        " default the number returned by Runtime.availableProcessors()" +
                        " will be used (" + Runtime.getRuntime().availableProcessors() + " on this host).")
                .longOpt(THREADS_LONG)
                .hasArg()
                .build());


        options.addOption(Option.builder()
                .required(false)
                .desc("Run the executable directly without installing it (-d is ignored). The executable will" +
                        " be executed from the temporary dir specified with -tmp.")
                .longOpt(RUN)
                .hasArg(false)
                .build());


        options.addOption(Option.builder(SKIP_RUNTIME)
                .required(false)
                .desc("Do not add default runtime jar(aura-rt.jar) to bootclasspath")
                .longOpt(SKIP_RUNTIME_LONG)
                .hasArg(false)
                .build());

        options.addOption(Option.builder(VERBOSE)
                .required(false)
                .desc("Verbose output")
                .longOpt(VERBOSE_LONG)
                .hasArg(false)
                .build());

        options.addOption(Option.builder()
                .required(false)
                .desc("Print version information")
                .longOpt(VERSION_LONG)
                .hasArg(false)
                .build());

        options.addOption(Option.builder(HELP)
                .required(false)
                .desc("Print usage")
                .longOpt(HELP_LONG)
                .hasArg(false)
                .build());

        options.addOption(Option.builder(DEBUG_LIBS)
                .required(false)
                .desc("Link against debug libs")
                .longOpt(DEBUG_LIBS_LOGS)
                .hasArg(false)
                .build());

        options.addOption(Option.builder(LIBS)
                .required(false)
                .desc(": separated list of static library files (.a), object files (.o) and system libraries that " +
                        "should be included when linking the final executable")
                .longOpt(LIBS_LONG)
                .hasArg()
                .build());


        return options;

    }
}
