/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.dalvik;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;

import android.util.Log;

public class VMLauncher extends Thread {

   private static final String TAG = "VMLauncher";
    private static String sJavaHome;

    private String[] args;

    public static void initialize(String javaHome) {
        if (javaHome != null) {
            sJavaHome = javaHome;
            loadNativeLibraries();
        } else {
            throw new NullPointerException();
        }
    }
    
    // java libs are located at architecture dependent 
    // paths
    // i386 for x86 arch
    // arm  for ARM arch
    public static String getJvmArch() {
        String rawarch = System.getProperty("os.arch");
        if (rawarch != null && rawarch.contains("86")) {
            return "i386";
        }
        return "arm";
    }

    private static void loadNativeLibraries() {
        System.load(sJavaHome + "/lib/" + getJvmArch() + "/client/libjvm.so");
        System.load(sJavaHome + "/lib/" + getJvmArch() + "/jli/libjli.so");
    }
    
    private static String getCmdLine() {
        // /proc/self/cmdline contains apk
        // package name
        StringBuilder builder = new StringBuilder();
        try {
            Reader reader = new FileReader("/proc/self/cmdline");
            int c = 0;
            while ((c = reader.read()) > 0) {
                builder.append((char)c);
            }
            reader.close();
        } catch (Exception e) {
            builder = new StringBuilder("dalvik.package");
        }
        return builder.toString();        
    }

    private static void listDirToStandardOut(String dirpath) {
        File dir = new File(dirpath);
        try {
            for (File file : dir.listFiles()) {
                if (file.isDirectory()) {
                    listDirToStandardOut(file.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception listing dir " + dir);
        }
    }

    private VMLauncher(String[] args) {
        super("JVM");
        this.args = args;
    }

    public void run()
    {
        launchJVM(this.args);
    }

    private static void startJavaInBackground(String[] args)
    {
        new VMLauncher(args).start();
    }

    public static void runOnDebugPort(Integer debugPort,
                                      String[] args)
    {
        ArrayList<String> localArrayList = new ArrayList();
        
        // need to add apk pkg name to head of arg list
        localArrayList.add(getCmdLine()); 
        
        if (debugPort.intValue() > 0) {
            localArrayList.add("-Xdebug");
            localArrayList.add("-agentlib:jdwp=server=y,suspend=y,transport=dt_socket,address=" + debugPort);
        }

        for (String arg : args) {
            localArrayList.add(arg);
        }

        String[] processedArgs = localArrayList.toArray(new String[0]);

        for (String arg : processedArgs) {
            Log.v(TAG, "Processed JVM arg : " + arg);
        }

        startJavaInBackground(processedArgs);
    }

    private static native int launchJVM(String[] args);

}