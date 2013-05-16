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

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.NativeActivity;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;


public class MainActivity extends NativeActivity {

    public static final String  TAG     = "MainActivity";
    private static final String TAG_JVM = "JVM";
    private static final String JAR     = ".jar";

    private static final String META_DATA_MAIN_CLASS = "main.class";
    private static final String META_DATA_JVM_ARGS   = "jvm.args";
    private static final String META_DATA_APP_ARGS   = "app.args";
    private static final String META_DATA_DEBUG_PORT = "debug.port";

    private String                  appDataDir;
    private String                  storageDir;    
    private NativePipeReader        reader;
    
    private ViewGroup mViewGroup;
    private View      mView;

    static {
        // load npr and vmlauncher
        System.loadLibrary("vmlauncher");
    }
        
    private NativePipeReader.OnTextReceivedListener textListener = 
            new NativePipeReader.OnTextReceivedListener() {
                public void onTextReceived(String text) {
                    Log.v(TAG_JVM, text);
                }
            };

    private void initDirInfo() {
        appDataDir = this.getApplicationInfo().dataDir;
        storageDir = appDataDir + "/storage";
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFormat(PixelFormat.RGBA_8888);
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED
              | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);

        mViewGroup = (ViewGroup)getWindow().getDecorView()
                    .findViewById(Window.ID_ANDROID_CONTENT);
        mView = mViewGroup.getChildAt(0);
        mView.setFocusableInTouchMode(true);

        reader = NativePipeReader.getDefaultReader(textListener);
        reader.start();
        initDirInfo();
        installJVMIfNeeded();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
    	if (event.getAction() == KeyEvent.ACTION_MULTIPLE) {
    		if (event.getKeyCode() == KeyEvent.KEYCODE_UNKNOWN) {
    			Log.w(TAG, "KeyEvent (MULTIPLE) :" + event.getCharacters());
    			Log.w(TAG, "Predictive text (T9) not supported!");
    		}
    	}
    	return super.dispatchKeyEvent(event);
    }
    
    private void installJVMIfNeeded() {
        new InstallerTask().execute();
    }

    private void runJVM() {
        Log.i(TAG, "Launch JVM + application");
        JvmArgs args = new JvmArgs(appDataDir);
        VMLauncher.initialize(args.getJavaHome());
        VMLauncher.runOnDebugPort(args.getDebugPort(),
                                  args.getArgArray());
    }

    private class InstallerTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... args) {
           Log.i(TAG, "Installing JVM");
            AppDataInstaller installer = 
                new AppDataInstaller(storageDir,
                                     MainActivity.this.getApplicationContext().getAssets());
            installer.install();
            return null;
        }
        protected void onPostExecute(Void result) {
           Log.i(TAG, "JVM Installed");
            runJVM();
        }
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
           String[] libfiles = listFiles(libDir, JAR);
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
           return getMetadata().getString(META_DATA_MAIN_CLASS);
       }

       private String getCustomJVMArgs() {
           return getMetadata().getString(META_DATA_JVM_ARGS);
       }

       private String getApplicationArgs() {
           return getMetadata().getString(META_DATA_APP_ARGS);
       }

       private int getDebugPort() {            
           return getMetadata().getInt(META_DATA_DEBUG_PORT, 0);
       }

       private Bundle getMetadata() {
           try {
               ActivityInfo ai = MainActivity.this.getPackageManager().getActivityInfo(
                       getIntent().getComponent(), PackageManager.GET_META_DATA);
               return ai.metaData;
               
           } catch(NameNotFoundException e) {
               throw new RuntimeException("Error getting activity info", e);
           }
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
