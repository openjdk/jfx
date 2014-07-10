/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.net.URI;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashSet;

public class NativeLibLoader {

    private static final HashSet<String> loaded = new HashSet<String>();

    public static synchronized void loadLibrary(String libname) {
        if (!loaded.contains(libname)) {
            loadLibraryInternal(libname);
            loaded.add(libname);
        }
    }

    private static boolean verbose = false;

    private static File libDir = null;
    private static String libPrefix = "";
    private static String libSuffix = "";

    static {
        AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
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

    private static void loadLibraryInternal(String libraryName) {
        // Look for the library in the same directory as the jar file
        // containing this class.
        // If that fails, then try System.loadLibrary as a last resort.
        try {
            loadLibraryFullPath(libraryName);
        } catch (UnsatisfiedLinkError ex) {
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

            // Try System.loadLibrary as a last resort. If it succeeds, then
            // print a warning. If it fails, rethrow the exception from
            // the earlier System.load()
            try {
                System.loadLibrary(libraryName);
                if (verbose) {
                    System.err.println("WARNING: " + ex.toString());
                    System.err.println("    using System.loadLibrary("
                            + libraryName + ") as a fallback");
                }
            } catch (UnsatisfiedLinkError ex2) {
                //On iOS we link all libraries staticaly. Presence of library 
                //is recognized by existence of JNI_OnLoad_libraryname() C function.
                //If libraryname contains hyphen, it needs to be translated 
                //to underscore to form valid C function indentifier.
                if ("iOS".equals(System.getProperty("os.name"))
                        && libraryName.contains("-")) {
                    libraryName = libraryName.replace("-", "_");
                    try {
                        System.loadLibrary(libraryName);
                        return;
                    } catch (UnsatisfiedLinkError ex3) {
                        throw ex3;
                    }
                }
                // Rethrow original exception
                throw ex;
            }
        }
    }

    /**
     * Load the native library from the same directory as the jar file
     * containing this class.
     */
    private static void loadLibraryFullPath(String libraryName) {
        try {
            if (libDir == null) {
                // Get the URL for this class, if it is a jar URL, then get the
                // filename associated with it.
                String theClassFile = "NativeLibLoader.class";
                Class theClass = NativeLibLoader.class;
                String classUrlString = theClass.getResource(theClassFile).toString();
                if (!classUrlString.startsWith("jar:file:") || classUrlString.indexOf('!') == -1){
                    throw new UnsatisfiedLinkError("Invalid URL for class: " + classUrlString);
                }
                // Strip out the "jar:" and everything after and including the "!"
                String tmpStr = classUrlString.substring(4, classUrlString.lastIndexOf('!'));
                // Strip everything after the last "/" or "\" to get rid of the jar filename
                int lastIndexOfSlash = Math.max(tmpStr.lastIndexOf('/'), tmpStr.lastIndexOf('\\'));

                // Set the native directory based on the OS
                String osName = System.getProperty("os.name");
                String relativeDir = null;
                if (osName.startsWith("Windows")) {
                    relativeDir = "../../bin";
                } else if (osName.startsWith("Mac")) {
                    relativeDir = "..";
                } else if (osName.startsWith("Linux")) {
                    relativeDir = "../" + System.getProperty("os.arch");
                }

                // Location of native libraries relative to jar file
                String libDirUrlString = tmpStr.substring(0, lastIndexOfSlash)
                        + "/" + relativeDir;
                libDir = new File(new URI(libDirUrlString).getPath());

                // Set the lib prefix and suffix based on the OS
                if (osName.startsWith("Windows")) {
                    libPrefix = "";
                    libSuffix = ".dll";
                } else if (osName.startsWith("Mac")) {
                    libPrefix = "lib";
                    libSuffix = ".dylib";
                } else if (osName.startsWith("Linux")) {
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
