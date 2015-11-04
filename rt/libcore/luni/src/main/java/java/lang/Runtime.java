/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package java.lang;

import dalvik.system.VMStack;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.FinalizerReference;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import aura.rt.VM;

import libcore.io.IoUtils;
import libcore.io.Libcore;
import libcore.util.EmptyArray;
import static libcore.io.OsConstants._SC_NPROCESSORS_CONF;

/**
 * Allows Java applications to interface with the environment in which they are
 * running. Applications can not create an instance of this class, but they can
 * get a singleton instance by invoking {@link #getRuntime()}.
 *
 * @see System
 */
public class Runtime {

    /**
     * Holds the Singleton global instance of Runtime.
     */
    private static final Runtime mRuntime = new Runtime();

    /**
     * Holds the library paths, used for native library lookup.
     */
    private final String[] mLibPaths = initLibPaths();

    private static String[] initLibPaths() {
        String javaLibraryPath = System.getProperty("java.library.path");
        if (javaLibraryPath == null) {
            return EmptyArray.STRING;
        }
        String[] paths = javaLibraryPath.split(":");
        // Add a '/' to the end of each directory so we don't have to do it every time.
        for (int i = 0; i < paths.length; ++i) {
            if (!paths[i].endsWith("/")) {
                paths[i] += "/";
            }
        }
        return paths;
    }

    /**
     * Holds the list of threads to run when the VM terminates
     */
    private List<Thread> shutdownHooks = new ArrayList<Thread>();

    /**
     * Reflects whether finalization should be run for all objects
     * when the VM terminates.
     */
    private static boolean finalizeOnExit;

    /**
     * Reflects whether we are already shutting down the VM.
     */
    private boolean shuttingDown;

    /**
     * Reflects whether we are tracing method calls.
     */
    private boolean tracingMethods;

    /**
     * Prevent this class from being instantiated.
     */
    private Runtime() {
    }

    /**
     * Executes the specified command and its arguments in a separate native
     * process. The new process inherits the environment of the caller. Calling
     * this method is equivalent to calling {@code exec(progArray, null, null)}.
     *
     * @param progArray
     *            the array containing the program to execute as well as any
     *            arguments to the program.
     * @return the new {@code Process} object that represents the native
     *         process.
     * @throws IOException
     *             if the requested program can not be executed.
     */
    public Process exec(String[] progArray) throws java.io.IOException {
        return exec(progArray, null, null);
    }

    /**
     * Executes the specified command and its arguments in a separate native
     * process. The new process uses the environment provided in {@code envp}.
     * Calling this method is equivalent to calling
     * {@code exec(progArray, envp, null)}.
     *
     * @param progArray
     *            the array containing the program to execute as well as any
     *            arguments to the program.
     * @param envp
     *            the array containing the environment to start the new process
     *            in.
     * @return the new {@code Process} object that represents the native
     *         process.
     * @throws IOException
     *             if the requested program can not be executed.
     */
    public Process exec(String[] progArray, String[] envp) throws java.io.IOException {
        return exec(progArray, envp, null);
    }

    /**
     * Executes the specified command and its arguments in a separate native
     * process. The new process uses the environment provided in {@code envp}
     * and the working directory specified by {@code directory}.
     *
     * @param progArray
     *            the array containing the program to execute as well as any
     *            arguments to the program.
     * @param envp
     *            the array containing the environment to start the new process
     *            in.
     * @param directory
     *            the directory in which to execute the program. If {@code null},
     *            execute if in the same directory as the parent process.
     * @return the new {@code Process} object that represents the native
     *         process.
     * @throws IOException
     *             if the requested program can not be executed.
     */
    public Process exec(String[] progArray, String[] envp, File directory) throws IOException {
        // ProcessManager is responsible for all argument checking.
        return ProcessManager.getInstance().exec(progArray, envp, directory, false);
    }

    /**
     * Executes the specified program in a separate native process. The new
     * process inherits the environment of the caller. Calling this method is
     * equivalent to calling {@code exec(prog, null, null)}.
     *
     * @param prog
     *            the name of the program to execute.
     * @return the new {@code Process} object that represents the native
     *         process.
     * @throws IOException
     *             if the requested program can not be executed.
     */
    public Process exec(String prog) throws java.io.IOException {
        return exec(prog, null, null);
    }

    /**
     * Executes the specified program in a separate native process. The new
     * process uses the environment provided in {@code envp}. Calling this
     * method is equivalent to calling {@code exec(prog, envp, null)}.
     *
     * @param prog
     *            the name of the program to execute.
     * @param envp
     *            the array containing the environment to start the new process
     *            in.
     * @return the new {@code Process} object that represents the native
     *         process.
     * @throws IOException
     *             if the requested program can not be executed.
     */
    public Process exec(String prog, String[] envp) throws java.io.IOException {
        return exec(prog, envp, null);
    }

    /**
     * Executes the specified program in a separate native process. The new
     * process uses the environment provided in {@code envp} and the working
     * directory specified by {@code directory}.
     *
     * @param prog
     *            the name of the program to execute.
     * @param envp
     *            the array containing the environment to start the new process
     *            in.
     * @param directory
     *            the directory in which to execute the program. If {@code null},
     *            execute if in the same directory as the parent process.
     * @return the new {@code Process} object that represents the native
     *         process.
     * @throws IOException
     *             if the requested program can not be executed.
     */
    public Process exec(String prog, String[] envp, File directory) throws java.io.IOException {
        // Sanity checks
        if (prog == null) {
            throw new NullPointerException("prog == null");
        } else if (prog.isEmpty()) {
            throw new IllegalArgumentException("prog is empty");
        }

        // Break down into tokens, as described in Java docs
        StringTokenizer tokenizer = new StringTokenizer(prog);
        int length = tokenizer.countTokens();
        String[] progArray = new String[length];
        for (int i = 0; i < length; i++) {
            progArray[i] = tokenizer.nextToken();
        }

        // Delegate
        return exec(progArray, envp, directory);
    }

    /**
     * Causes the VM to stop running and the program to exit.
     * If {@link #runFinalizersOnExit(boolean)} has been previously invoked with a
     * {@code true} argument, then all objects will be properly
     * garbage-collected and finalized first.
     * Use 0 to signal success to the calling process and 1 to signal failure.
     * This method is unlikely to be useful to an Android application.
     */
    public void exit(int code) {
        // Make sure we don't try this several times
        synchronized(this) {
            if (!shuttingDown) {
                shuttingDown = true;

                Thread[] hooks;
                synchronized (shutdownHooks) {
                    // create a copy of the hooks
                    hooks = new Thread[shutdownHooks.size()];
                    shutdownHooks.toArray(hooks);
                }

                // Start all shutdown hooks concurrently
                for (Thread hook : hooks) {
                    hook.start();
                }

                // Wait for all shutdown hooks to finish
                for (Thread hook : hooks) {
                    try {
                        hook.join();
                    } catch (InterruptedException ex) {
                        // Ignore, since we are at VM shutdown.
                    }
                }

                // Ensure finalization on exit, if requested
                if (finalizeOnExit) {
                    runFinalization();
                }

                // Get out of here finally...
                nativeExit(code);
            }
        }
    }

    /**
     * Indicates to the VM that it would be a good time to run the
     * garbage collector. Note that this is a hint only. There is no guarantee
     * that the garbage collector will actually be run.
     */
    public native void gc();

    /**
     * Returns the single {@code Runtime} instance for the current application.
     */
    public static Runtime getRuntime() {
        return mRuntime;
    }

    /**
     * Loads and links the dynamic library that is identified through the
     * specified path. This method is similar to {@link #loadLibrary(String)},
     * but it accepts a full path specification whereas {@code loadLibrary} just
     * accepts the name of the library to load.
     *
     * @param pathName
     *            the absolute (platform dependent) path to the library to load.
     * @throws UnsatisfiedLinkError
     *             if the library can not be loaded.
     */
    public void load(String pathName) {
        load(pathName, VMStack.getCallingClassLoader());
    }

    /*
     * Loads and links the given library without security checks.
     */
    void load(String pathName, ClassLoader loader) {
        if (pathName == null) {
            throw new NullPointerException("pathName == null");
        }
        // RoboVM note: See note on nativeLoad() method.
        nativeLoad(pathName, loader);
    }

    /**
     * Loads and links the library with the specified name. The mapping of the
     * specified library name to the full path for loading the library is
     * implementation-dependent.
     *
     * @param libName
     *            the name of the library to load.
     * @throws UnsatisfiedLinkError
     *             if the library can not be loaded.
     */
    public void loadLibrary(String libName) {
        loadLibrary(libName, VMStack.getCallingClassLoader());
    }

    /*
     * Searches for a library, then loads and links it without security checks.
     */
    void loadLibrary(String libraryName, ClassLoader loader) {
        if (libraryName == null) {
            throw new NullPointerException("libraryName");
        }

        // RoboVM note: First check if the library has been statically linked in.
        for (String l : VM.staticLibs()) {
            if (libraryName.equals(l)) {
                return;
            }
        }
        
        if (loader != null) {
            String filename = loader.findLibrary(libraryName);
            if (filename == null) {
                throw new UnsatisfiedLinkError("Couldn't load " + libraryName +
                                               " from loader " + loader +
                                               ": findLibrary returned null");
            }
            // RoboVM note: See note on nativeLoad() method.
            nativeLoad(filename, loader);
            return;
        }

        String filename = System.mapLibraryName(libraryName);
        List<String> candidates = new ArrayList<String>();
        String lastError = null;
        for (String directory : mLibPaths) {
            String candidate = directory + filename;
            candidates.add(candidate);

            if (IoUtils.canOpenReadOnly(candidate)) {
                try {
                    // RoboVM note: See note on nativeLoad() method.
                    nativeLoad(candidate, loader);
                    return; // We successfully loaded the library. Job done.
                } catch (UnsatisfiedLinkError e) {
                    lastError = e.getMessage();
                }
            }
        }

        if (lastError != null) {
            throw new UnsatisfiedLinkError(lastError);
        }
        throw new UnsatisfiedLinkError("Library " + libraryName + " not found; tried " + candidates);
    }

    private static native void nativeExit(int code);

    // RoboVM note: On Android nativeLoad() returns an error message String 
    // on errors which is then wrapped in an UnsatisfiedLinkError. Our 
    // nativeLoad() throws UnsatisfiedLinkError directly.
    // TODO: Synchronize?
    private static native void nativeLoad(String filename, ClassLoader loader);

    /**
     * Provides a hint to the VM that it would be useful to attempt
     * to perform any outstanding object finalization.
     */
    public void runFinalization() {
        try {
            FinalizerReference.finalizeAllEnqueued();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Sets the flag that indicates whether all objects are finalized when the
     * VM is about to exit. Note that all finalization which occurs
     * when the system is exiting is performed after all running threads have
     * been terminated.
     *
     * @param run
     *            {@code true} to enable finalization on exit, {@code false} to
     *            disable it.
     * @deprecated This method is unsafe.
     */
    @Deprecated
    public static void runFinalizersOnExit(boolean run) {
        finalizeOnExit = run;
    }

    /**
     * Switches the output of debug information for instructions on or off.
     * On Android, this method does nothing.
     */
    public void traceInstructions(boolean enable) {
    }

    /**
     * Switches the output of debug information for methods on or off.
     */
    public void traceMethodCalls(boolean enable) {
        // RoboVM note: Method tracing is not supported by RoboVM.
//        if (enable != tracingMethods) {
//            if (enable) {
//                VMDebug.startMethodTracing();
//            } else {
//                VMDebug.stopMethodTracing();
//            }
//            tracingMethods = enable;
//        }
    }

    /**
     * Returns the localized version of the specified input stream. The input
     * stream that is returned automatically converts all characters from the
     * local character set to Unicode after reading them from the underlying
     * stream.
     *
     * @param stream
     *            the input stream to localize.
     * @return the localized input stream.
     * @deprecated Use {@link InputStreamReader} instead.
     */
    @Deprecated
    public InputStream getLocalizedInputStream(InputStream stream) {
        String encoding = System.getProperty("file.encoding", "UTF-8");
        if (!encoding.equals("UTF-8")) {
            throw new UnsupportedOperationException("Cannot localize " + encoding);
        }
        return stream;
    }

    /**
     * Returns the localized version of the specified output stream. The output
     * stream that is returned automatically converts all characters from
     * Unicode to the local character set before writing them to the underlying
     * stream.
     *
     * @param stream
     *            the output stream to localize.
     * @return the localized output stream.
     * @deprecated Use {@link OutputStreamWriter} instead.
     */
    @Deprecated
    public OutputStream getLocalizedOutputStream(OutputStream stream) {
        String encoding = System.getProperty("file.encoding", "UTF-8");
        if (!encoding.equals("UTF-8")) {
            throw new UnsupportedOperationException("Cannot localize " + encoding);
        }
        return stream;
    }

    /**
     * Registers a VM shutdown hook. A shutdown hook is a
     * {@code Thread} that is ready to run, but has not yet been started. All
     * registered shutdown hooks will be executed when the VM
     * terminates normally (typically when the {@link #exit(int)} method is called).
     *
     * <p><i>Note that on Android, the application lifecycle does not include VM termination,
     * so calling this method will not ensure that your code is run</i>. Instead, you should
     * use the most appropriate lifecycle notification ({@code Activity.onPause}, say).
     *
     * <p>Shutdown hooks are run concurrently and in an unspecified order. Hooks
     * failing due to an unhandled exception are not a problem, but the stack
     * trace might be printed to the console. Once initiated, the whole shutdown
     * process can only be terminated by calling {@code halt()}.
     *
     * <p>If {@link #runFinalizersOnExit(boolean)} has been called with a {@code
     * true} argument, garbage collection and finalization will take place after
     * all hooks are either finished or have failed. Then the VM
     * terminates.
     *
     * <p>It is recommended that shutdown hooks do not do any time-consuming
     * activities, in order to not hold up the shutdown process longer than
     * necessary.
     *
     * @param hook
     *            the shutdown hook to register.
     * @throws IllegalArgumentException
     *             if the hook has already been started or if it has already
     *             been registered.
     * @throws IllegalStateException
     *             if the VM is already shutting down.
     */
    public void addShutdownHook(Thread hook) {
        // Sanity checks
        if (hook == null) {
            throw new NullPointerException("hook == null");
        }

        if (shuttingDown) {
            throw new IllegalStateException("VM already shutting down");
        }

        if (hook.started) {
            throw new IllegalArgumentException("Hook has already been started");
        }

        synchronized (shutdownHooks) {
            if (shutdownHooks.contains(hook)) {
                throw new IllegalArgumentException("Hook already registered.");
            }

            shutdownHooks.add(hook);
        }
    }

    /**
     * Unregisters a previously registered VM shutdown hook.
     *
     * @param hook
     *            the shutdown hook to remove.
     * @return {@code true} if the hook has been removed successfully; {@code
     *         false} otherwise.
     * @throws IllegalStateException
     *             if the VM is already shutting down.
     */
    public boolean removeShutdownHook(Thread hook) {
        // Sanity checks
        if (hook == null) {
            throw new NullPointerException("hook == null");
        }

        if (shuttingDown) {
            throw new IllegalStateException("VM already shutting down");
        }

        synchronized (shutdownHooks) {
            return shutdownHooks.remove(hook);
        }
    }

    /**
     * Causes the VM to stop running, and the program to exit with the given return code.
     * Use 0 to signal success to the calling process and 1 to signal failure.
     * Neither shutdown hooks nor finalizers are run before exiting.
     * This method is unlikely to be useful to an Android application.
     */
    public void halt(int code) {
        // Get out of here...
        nativeExit(code);
    }

    /**
     * Returns the number of processor cores available to the VM, at least 1.
     * Traditionally this returned the number currently online,
     * but many mobile devices are able to take unused cores offline to
     * save power, so releases newer than Android 4.2 (Jelly Bean) return the maximum number of
     * cores that could be made available if there were no power or heat
     * constraints.
     */
    public int availableProcessors() {
        return (int) Libcore.os.sysconf(_SC_NPROCESSORS_CONF);
    }

    /**
     * Returns the number of bytes currently available on the heap without expanding the heap. See
     * {@link #totalMemory} for the heap's current size. When these bytes are exhausted, the heap
     * may expand. See {@link #maxMemory} for that limit.
     */
    public native long freeMemory();

    /**
     * Returns the number of bytes taken by the heap at its current size. The heap may expand or
     * contract over time, as the number of live objects increases or decreases. See
     * {@link #maxMemory} for the maximum heap size, and {@link #freeMemory} for an idea of how much
     * the heap could currently contract.
     */
    public native long totalMemory();

    /**
     * Returns the maximum number of bytes the heap can expand to. See {@link #totalMemory} for the
     * current number of bytes taken by the heap, and {@link #freeMemory} for the current number of
     * those bytes actually used by live objects.
     */
    public native long maxMemory();
}
