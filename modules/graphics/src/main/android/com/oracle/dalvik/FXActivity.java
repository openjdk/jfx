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
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

public class FXActivity extends Activity implements SurfaceHolder.Callback, 
        SurfaceHolder.Callback2 {
	
    private static final String TAG     = "FXActivity";
    private static final String TAG_JVM = "JVM";
    private static final String JAR     = ".jar";
    
    private static final String META_DATA_MAIN_CLASS = "main.class";
    private static final String META_DATA_JVM_ARGS   = "jvm.args";
    private static final String META_DATA_APP_ARGS   = "app.args";
    private static final String META_DATA_DEBUG_PORT = "debug.port";
    private static final String ANDROID_WEBVIEW = "android_webview";
    private static final String GLASS_LENS_ANDROID = "glass-lens-android";

    static {        
        System.loadLibrary("vmlauncher");
    }
    private static FXActivity instance;
    private static FrameLayout mViewGroup;
    private static SurfaceView mView;

    private String            appDataDir;
    private String            storageDir;    
    private NativePipeReader  reader;
    private InputMethodManager imm;   
    private String ldPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        getWindow().setFormat(PixelFormat.RGBA_8888);

        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        mView = new InternalSurfaceView(this);
        mView.getHolder().addCallback(this);        
        mViewGroup = new FrameLayout(this);
        mViewGroup.addView(mView);
        setContentView(mViewGroup);
        instance = this;
        reader = NativePipeReader.getDefaultReader(textListener);
        reader.start();
        initDirInfo();
        installJVMIfNeeded();
        System.loadLibrary(GLASS_LENS_ANDROID);
        System.loadLibrary(ANDROID_WEBVIEW);
    }

    public static FXActivity getInstance() {
        return instance;
    }

    public String getLDPath() {
        if (appDataDir == null) {
            appDataDir = this.getApplicationInfo().dataDir;
        }
        if (ldPath == null) {
            ldPath = appDataDir + "/lib";
        }
        return ldPath;
    }

    public static ViewGroup getViewGroup() {
        return mViewGroup;
    }    
	
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
            _surfaceChanged(holder.getSurface());
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                    int height) {
            _surfaceChanged(holder.getSurface(), format, width, height);

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
            _surfaceChanged(null);
    }

    @Override
    public void surfaceRedrawNeeded(SurfaceHolder holder) {
            _surfaceRedrawNeeded(holder.getSurface());		
    }

    private native void _surfaceChanged(Surface surface);

    private native void _surfaceChanged(Surface surface, int format, int width, int height);

    private native void _surfaceRedrawNeeded(Surface surface);

    private void showIME() {
        mView.requestFocus();
        imm.showSoftInput(mView, 0);
    }

    private void hideIME() {
        mView.requestFocus();
        imm.hideSoftInputFromWindow(mView.getWindowToken(), 0);
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
                    FXActivity.this.getApplicationContext().getAssets());
            installer.install();
            return null;
        }
    protected void onPostExecute(Void result) {
            runJVM();
        }
    }
	
    private NativePipeReader.OnTextReceivedListener textListener = 
            new NativePipeReader.OnTextReceivedListener() {
                public void onTextReceived(String text) {
                    Log.v(TAG_JVM, text);
                }
            };

    private void initDirInfo() {
        if (appDataDir == null) {
            appDataDir = this.getApplicationInfo().dataDir;
        }
        storageDir = appDataDir + "/storage";
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
                ActivityInfo ai = FXActivity.this.getPackageManager().getActivityInfo(
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
	
    class InternalSurfaceView extends SurfaceView {

        public InternalSurfaceView(Context context) {
            super(context);
            setFocusableInTouchMode(true);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
            onTouchEventNative(event.getAction(), (int) event.getX(), (int) event.getY());
            return true;
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            onKeyEventNative(event.getAction(), event.getKeyCode(), event.getCharacters());
            return true;
        }

        private native void onTouchEventNative(int action, int absx, int absy);

        private native void onKeyEventNative(int action, int keycode, String characters);

    }

}
