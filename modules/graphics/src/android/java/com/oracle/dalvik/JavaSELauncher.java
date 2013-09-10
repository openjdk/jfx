/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

public class JavaSELauncher implements Launcher, NativePipeReader.OnTextReceivedListener {

    private static final String TAG                     = "JavaSELauncher";
    private static final String TAG_JVM                 = "JavaSE";
    
    private static final String VMLAUNCHER_LIB          = "vmlauncher";
    private static final String ANDROID_WEBVIEW_LIB     = "android_webview";
    
    private static final String META_DATA_MAIN_CLASS    = "main.class";
    private static final String META_DATA_JVM_ARGS      = "jvm.args";
    private static final String META_DATA_APP_ARGS      = "app.args";
    private static final String META_DATA_DEBUG_PORT    = "debug.port";
    private static final String JAR_EXT                 = ".jar";
    
    private Bundle              metaData;
    private Activity            activity;
    private NativePipeReader    reader;
    private String              appDataDir;
    private String              storageDir;
    
    public JavaSELauncher() {
        System.loadLibrary(VMLAUNCHER_LIB);
    }
    
    public void launchApp(Activity a, Bundle metadata) {
        this.activity = a;
        this.metaData = metadata;
        reader = NativePipeReader.getDefaultReader(this);
        reader.start();
        appDataDir = activity.getApplicationInfo().dataDir;
        storageDir = appDataDir + "/storage";
        installJVMIfNeeded();
        System.loadLibrary(ANDROID_WEBVIEW_LIB);
    }

    public void onTextReceived(String text) {
        Log.v(TAG_JVM, text);
    }
    
    private void installJVMIfNeeded() {
        new InstallerTask().execute();
    }
    
    private class InstallerTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... args) {
           Log.i(TAG, "Installing JVM");
            AppDataInstaller installer = 
                new AppDataInstaller(storageDir,
                    activity.getApplicationContext().getAssets());
            installer.install();
            return null;
        }
        
        protected void onPostExecute(Void result) {
            runJVM();
        }
    }
    
    private void runJVM() {
        Log.i(TAG, "Launch JVM + application");
        JvmArgs args = new JvmArgs(appDataDir);
        VMLauncher.initialize(args.getJavaHome());
        VMLauncher.runOnDebugPort(args.getDebugPort(),
                                  args.getArgArray());
    }
    
    private class JvmArgs {
        private List<String> argList = new ArrayList<String>();
        private String javaHome;

        public JvmArgs(String appDir) {
            String jvmRunCommand =
                   "-Djava.library.path="
                     + appDir + "/lib|"
                     + "-Djava.home="
                     + appDir + "/storage/jvm|"
                     + "-Dsun.boot.library.path="
                     + appDir + "/storage/jvm/lib|"
                     + "-cp|"
                     + getClasspath(appDir)
                     + "|"                                         
                     + "-Djavafx.platform=android|"
                     + "-Djavafx.runtime.path="
                     + appDir +"/storage/lib|"
                     + getCustomJVMArgs()
                     + "|"
                     + getMainClass()
                     + "|"
                     + getApplicationArgs();

            createArgList(jvmRunCommand);
        }

        private String[] listFiles(String dir, final String suffix) {
            File dirf = new File(dir);
            if (!dirf.exists()) {
                return new String[]{};
            }
            String[] files = dirf.list(new FilenameFilter() {           
                @Override
                public boolean accept(File dir, String filename) {      
                    return filename.endsWith(suffix);
                }
            });
            return files;
        }

        private String getClasspath(String appDir) {
            final String libDir = appDir + "/storage/lib/";
            String[] libfiles = listFiles(libDir, JAR_EXT);
            if (libfiles.length == 0) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            for(String file: libfiles) {
                sb.append(libDir);
                sb.append(file);
                sb.append(File.pathSeparatorChar);
            }
            int len = sb.length();
            return sb.substring(0, len - 1);
        }

        private String getMainClass() {
             return metaData.getString(META_DATA_MAIN_CLASS);
        }

        private String getCustomJVMArgs() {
             return metaData.getString(META_DATA_JVM_ARGS);
        }

        private String getApplicationArgs() {
             return metaData.getString(META_DATA_APP_ARGS);
        }

        private int getDebugPort() {            
             return metaData.getInt(META_DATA_DEBUG_PORT, 0);
        }

        public void createArgList(String args) {
            if (args != null) {
                String sep = (args.contains("|")) ? "\\|" : " ";
                for (String arg : args.split(sep)) {
                    arg = arg.trim();
                    if (arg.length() > 0) {
                        this.argList.add(arg);
                        if (javaHome == null) {
                            String[] pair = arg.split("\\=");
                            Log.v(TAG, "arg = " + arg);
                            Log.v(TAG, "pair.length = " + pair.length);
                            if (pair.length == 2) {
                                if ("-Djava.home".equals(pair[0])) {
                                    Log.v(TAG, "Setting javaHome to " + pair[1]);
                                    javaHome = pair[1];
                                }
                            }
                        }
                    }
                }
            }
        }
        
        public String[] getArgArray() {
            return argList.toArray(new String[0]);
        }

        public String getJavaHome() {
           return javaHome;
        }
    }
}