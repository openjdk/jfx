/*
 * Copyright (c) 2010, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.glass.utils;

import com.sun.javafx.PlatformUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.AccessController;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class NativeLibLoader {

    private static final HashSet<String> loaded = new HashSet<>();

    public static synchronized void loadLibrary(String libname) {
        if (!loaded.contains(libname)) {
            @SuppressWarnings("removal")
            StackWalker walker = AccessController.doPrivileged((PrivilegedAction<StackWalker>) () ->
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE));
            Class caller = walker.getCallerClass();
            loadLibraryInternal(libname, null, caller);
            loaded.add(libname);
        }
    }

    public static synchronized void loadLibrary(String libname, List<String> dependencies) {
        if (!loaded.contains(libname)) {
            @SuppressWarnings("removal")
            StackWalker walker = AccessController.doPrivileged((PrivilegedAction<StackWalker>) () ->
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE));
            Class caller = walker.getCallerClass();
            loadLibraryInternal(libname, dependencies, caller);
            loaded.add(libname);
        }
    }

    private static boolean verbose = false;

    private static File libDir = null;
    private static String libPrefix = "";
    private static String libSuffix = "";

    static {
        @SuppressWarnings("removal")
        var dummy = AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
            verbose = Boolean.getBoolean("javafx.verbose");
            return null;
        });
    }

    private static String[] initializePath(String propname) {
        String ldpath = System.getProperty(propname, "");
        String ps = File.pathSeparator;
        int ldlen = ldpath.length();
        int i, j, n;
        // Count the separators in the path
        i = ldpath.indexOf(ps);
        n = 0;
        while (i >= 0) {
            n++;
            i = ldpath.indexOf(ps, i + 1);
        }

        // allocate the array of paths - n :'s = n + 1 path elements
        String[] paths = new String[n + 1];

        // Fill the array with paths from the ldpath
        n = i = 0;
        j = ldpath.indexOf(ps);
        while (j >= 0) {
            if (j - i > 0) {
                paths[n++] = ldpath.substring(i, j);
            } else if (j - i == 0) {
                paths[n++] = ".";
            }
            i = j + 1;
            j = ldpath.indexOf(ps, i);
        }
        paths[n] = ldpath.substring(i, ldlen);
        return paths;
    }

    private static void loadLibraryInternal(String libraryName, List<String> dependencies, Class caller) {
        // The search order for native library loading is:
        // - try to load the native library from either ${java.home}
        //   (for jlinked javafx modules) or from the same folder as
        //   this jar (if using modular jars)
        // - if the native library comes bundled as a resource it is extracted
        //   and loaded
        // - the java.library.path is searched for the library in definition
        //   order
        // - the library is loaded via System#loadLibrary
        // - on iOS native library is staticly linked and detected from the
        //   existence of a JNI_OnLoad_libraryname funtion
        try {
            // FIXME: JIGSAW -- We should eventually remove this legacy path,
            // since it isn't applicable to Jigsaw.
            loadLibraryFullPath(libraryName);
        } catch (UnsatisfiedLinkError ex) {
            if (verbose) {
                System.err.println("WARNING: " + ex);
            }

            // if the library is available in the jar, copy it to cache and load it from there
            if (loadLibraryFromResource(libraryName, dependencies, caller)) {
                return;
            }

            // NOTE: First attempt to load the libraries from the java.library.path.
            // This allows FX to find more recent versions of the shared libraries
            // from java.library.path instead of ones that might be part of the JRE
            //
            String [] libPath = initializePath("java.library.path");
            for (int i=0; i<libPath.length; i++) {
                try {
                    String path = libPath[i];
                    if (!path.endsWith(File.separator)) path += File.separator;
                    String fileName = System.mapLibraryName(libraryName);
                    File libFile = new File(path + fileName);
                    System.load(libFile.getAbsolutePath());
                    if (verbose) {
                        System.err.println("Loaded " + libFile.getAbsolutePath()
                                + " from java.library.path");
                    }
                    return;
                } catch (UnsatisfiedLinkError ex3) {
                    // Fail silently and try the next directory in java.library.path
                }
            }

            // Finally we will use System.loadLibrary.
            try {
                System.loadLibrary(libraryName);
                if (verbose) {
                    System.err.println("System.loadLibrary("
                            + libraryName + ") succeeded");
                }
            } catch (UnsatisfiedLinkError ex2) {
                //On iOS we link all libraries staticaly. Presence of library
                //is recognized by existence of JNI_OnLoad_libraryname() C function.
                //If libraryname contains hyphen, it needs to be translated
                //to underscore to form valid C function indentifier.
                if (PlatformUtil.isIOS() && libraryName.contains("-")) {
                    libraryName = libraryName.replace("-", "_");
                    try {
                        System.loadLibrary(libraryName);
                        return;
                    } catch (UnsatisfiedLinkError ex3) {
                        throw ex3;
                    }
                }
                // Rethrow exception
                throw ex2;
            }
        }
    }

   /**
    * If there is a library with the platform-correct name at the
    * root of the resources in this jar, use that.
    */
    private static boolean loadLibraryFromResource(String libraryName, List<String> dependencies, Class caller) {
        return installLibraryFromResource(libraryName, dependencies, caller, true);
    }

   /**
    * If there is a library with the platform-correct name at the
    * root of the resources in this jar, install it. If load is true, also load it.
    */
    private static boolean installLibraryFromResource(String libraryName, List<String> dependencies, Class caller, boolean load) {
        try {
            // first preload dependencies
            if (dependencies != null) {
                for (String dep: dependencies) {
                    boolean hasdep = installLibraryFromResource(dep, null, caller, false);
                }
            }
            String reallib = "/"+System.mapLibraryName(libraryName);
            InputStream is = caller.getResourceAsStream(reallib);
            if (is != null) {
                String fp = cacheLibrary(is, reallib, caller);
                if (load) {
                    System.load(fp);
                    if (verbose) {
                        System.err.println("Loaded library " + reallib + " from resource");
                    }
                } else if (verbose) {
                    System.err.println("Unpacked library " + reallib + " from resource");
                }
                return true;
            }
        } catch (Throwable t) {
            // we should only be here if the resource exists in the module, but
            // for some reasons it can't be loaded.
            System.err.println("Loading library " + libraryName + " from resource failed: " + t);
            t.printStackTrace();
        }
        return false;
    }

    private static String cacheLibrary(InputStream is, String name, Class caller) throws IOException {
        String jfxVersion = System.getProperty("javafx.runtime.version", "versionless");
        String userCache = System.getProperty("javafx.cachedir", "");
        String arch = System.getProperty("os.arch");
        if (userCache.isEmpty()) {
            userCache = System.getProperty("user.home") + "/.openjfx/cache/" + jfxVersion + "/" + arch;
        }
        File cacheDir = new File(userCache);
        boolean cacheDirOk = true;
        if (cacheDir.exists()) {
            if (!cacheDir.isDirectory()) {
                System.err.println("Cache exists but is not a directory: "+cacheDir);
                cacheDirOk = false;
            }
        } else {
            if (!cacheDir.mkdirs()) {
                System.err.println("Can not create cache at "+cacheDir);
                cacheDirOk = false;
            }
        }
        if (!cacheDir.canRead()) {
            // on some systems, directories in user.home can be written but not read.
            cacheDirOk = false;
        }
        if (!cacheDirOk) {
            String username = System.getProperty("user.name", "anonymous");
            String tmpCache = System.getProperty("java.io.tmpdir") + "/.openjfx_" + username
                    + "/cache/" + jfxVersion + "/" + arch;
            cacheDir = new File(tmpCache);
            if (cacheDir.exists()) {
                if (!cacheDir.isDirectory()) {
                    throw new IOException("Cache exists but is not a directory: "+cacheDir);
                }
            } else {
                if (!cacheDir.mkdirs()) {
                    throw new IOException("Can not create cache at "+cacheDir);
                }
            }
        }
        // we have a cache directory. Add the file here
        File f = new File(cacheDir, name);
        // if it exists, calculate checksum and keep if same as inputstream.
        boolean write = true;
        if (f.exists()) {
            byte[] isHash;
            byte[] fileHash;
            try {
                DigestInputStream dis = new DigestInputStream(is, MessageDigest.getInstance("MD5"));
                dis.getMessageDigest().reset();
                byte[] buffer = new byte[4096];
                while (dis.read(buffer) != -1) { /* empty loop body is intentional */ }
                isHash = dis.getMessageDigest().digest();
                is.close();
                is = caller.getResourceAsStream(name); // mark/reset not supported, we have to reread
            }
            catch (NoSuchAlgorithmException nsa) {
                isHash = new byte[1];
            }
            fileHash = calculateCheckSum(f);
            if (!Arrays.equals(isHash, fileHash)) {
                Files.delete(f.toPath());
            } else {
                // hashes are the same, we already have the file.
                write = false;
            }
        }
        if (write) {
            Path path = f.toPath();
            Files.copy(is, path);
        }

        String fp = f.getAbsolutePath();
        return fp;
    }

    static byte[] calculateCheckSum(File file) {
        try {
                // not looking for security, just a checksum. MD5 should be faster than SHA
                try (final InputStream stream = new FileInputStream(file);
                    final DigestInputStream dis = new DigestInputStream(stream, MessageDigest.getInstance("MD5")); ) {
                    dis.getMessageDigest().reset();
                    byte[] buffer = new byte[4096];
                    while (dis.read(buffer) != -1) { /* empty loop body is intentional */ }
                    return dis.getMessageDigest().digest();
                }

        } catch (IllegalArgumentException | NoSuchAlgorithmException | IOException | SecurityException e) {
            // IOException also covers MalformedURLException
            // SecurityException means some untrusted app

            // Fall through...
        }
        return new byte[0];
    }


    private static File libDirForJRT() {
        String javaHome = System.getProperty("java.home");

        if (javaHome == null || javaHome.isEmpty()) {
            throw new UnsatisfiedLinkError("Cannot find java.home");
        }

        // Set the native directory based on the OS
        String relativeDir = null;
        if (PlatformUtil.isWindows()) {
            relativeDir = "bin/javafx";
        } else if (PlatformUtil.isMac()) {
            relativeDir = "lib";
        } else if (PlatformUtil.isLinux()) {
            relativeDir = "lib";
        }

        // Location of native libraries relative to java.home
        return new File(javaHome + "/" + relativeDir);
    }

    private static File libDirForJarFile(String classUrlString) throws Exception {
        // Strip out the "jar:" and everything after and including the "!"
        String tmpStr = classUrlString.substring(4, classUrlString.lastIndexOf('!'));
        // Strip everything after the last "/" or "\" to get rid of the jar filename
        int lastIndexOfSlash = Math.max(tmpStr.lastIndexOf('/'), tmpStr.lastIndexOf('\\'));

        // Set the native directory based on the OS
        String relativeDir = null;
        if (PlatformUtil.isWindows()) {
            relativeDir = "../bin";
        } else if (PlatformUtil.isMac()) {
            relativeDir = ".";
        } else if (PlatformUtil.isLinux()) {
            relativeDir = ".";
        }

        // Location of native libraries relative to jar file
        String libDirUrlString = tmpStr.substring(0, lastIndexOfSlash)
                + "/" + relativeDir;
        return new File(new URI(libDirUrlString).getPath());
    }

    /**
     * Load the native library either from the same directory as the jar file
     * containing this class, or from the Java runtime.
     */
    private static void loadLibraryFullPath(String libraryName) {
        try {
            if (libDir == null) {
                // Get the URL for this class, if it is a jar URL, then get the
                // filename associated with it.
                String theClassFile = "NativeLibLoader.class";
                Class theClass = NativeLibLoader.class;
                String classUrlString = theClass.getResource(theClassFile).toString();
                if (classUrlString.startsWith("jrt:")) {
                    libDir = libDirForJRT();
                } else if (classUrlString.startsWith("jar:file:") && classUrlString.indexOf('!') > 0) {
                    libDir = libDirForJarFile(classUrlString);
                } else {
                    throw new UnsatisfiedLinkError("Invalid URL for class: " + classUrlString);
                }

                // Set the lib prefix and suffix based on the OS
                if (PlatformUtil.isWindows()) {
                    libPrefix = "";
                    libSuffix = ".dll";
                } else if (PlatformUtil.isMac()) {
                    libPrefix = "lib";
                    libSuffix = ".dylib";
                } else if (PlatformUtil.isLinux()) {
                    libPrefix = "lib";
                    libSuffix = ".so";
                }
            }

            File libFile = new File(libDir, libPrefix + libraryName + libSuffix);
            String libFileName = libFile.getCanonicalPath();
            try {
                System.load(libFileName);
                if (verbose) {
                    System.err.println("Loaded " + libFile.getAbsolutePath()
                            + " from relative path");
                }
            } catch(UnsatisfiedLinkError ex) {
                throw ex;
            }
        } catch (Exception e) {
            // Throw UnsatisfiedLinkError for best compatibility with System.loadLibrary()
            throw (UnsatisfiedLinkError) new UnsatisfiedLinkError().initCause(e);
        }
    }

}
