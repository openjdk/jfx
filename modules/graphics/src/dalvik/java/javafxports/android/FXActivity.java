/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

package javafxports.android;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;



public class FXActivity extends Activity  {

    private static final String TAG = "FXActivity";
    private static final String JFX_BUILD = "8u60-b2-SNAPSHOT";
    
    private static final String ACTIVITY_LIB = "activity";
    private static final String META_DATA_LAUNCHER_CLASS = "launcher.class";
    private static final String DEFAULT_LAUNCHER_CLASS = "javafxports.android.DalvikLauncher";
    private static final String META_DATA_MAIN_CLASS = "main.class";
    private static final String META_DATA_PRELOADER_CLASS = "preloader.class";
    private static final String META_DATA_DEBUG_PORT = "debug.port";

    private static final String APPLICATION_DEX_NAME = "Application_dex.jar";
    private static final String APPLICATION_RESOURCES_NAME = "Application_resources.jar";
    private static final String CLASSLOADER_PROPERTIES_NAME = "classloader.properties";
    private static final String BUILD_TIME_NAME = "buildtime";
    private static final int BUF_SIZE = 8 * 1024;
    public static String dexClassPath = new String();

    private static FXActivity instance;
    private static Launcher launcher;
    private static FrameLayout mViewGroup;
    private static SurfaceView mView;

    private static String appDataDir;
    private static DeviceConfiguration configuration;



    // Cache method handles
    // Can not access com.sun.glass.ui.android.DalvikInput directly, because the javafx classes are loaded with a different classloader 
 //   private Method onMultiTouchEventMethod;
    private Method onKeyEventMethod;
    private Method onSurfaceChangedNativeMethod1;
    private Method onSurfaceChangedNativeMethod2;
    private Method onSurfaceRedrawNeededNativeMethod;
    private Method onConfigurationChangedNativeMethod;

    //configurations
    private int SCREEN_ORIENTATION = 1;
    
    private String launcherClassName;
    private String mainClassName;
    private String preloaderClassName;
    
    private String currentBuildStamp;
    private Properties classLoaderProperties;
    private File dexInternalStoragePath;
    
    private static final Bundle metadata = new Bundle();
    private FXDalvikEntity fxDalvikEntity;

    static {
        Log.v(TAG, "Initializing JavaFX Platform, Using "+JFX_BUILD);
        System.loadLibrary(ACTIVITY_LIB);
    }
        
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.fxDalvikEntity = new FXDalvikEntity(metadata, this);
        Log.v(TAG, "onCreate called, Using "+JFX_BUILD);
        if (launcher != null) {
            Log.v(TAG, "JavaFX application is already running");
            return;
        }
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED);
        getWindow().setFormat(PixelFormat.RGBA_8888);


        mView = fxDalvikEntity.createView();
        
        mViewGroup = new FrameLayout(this);
        mViewGroup.addView(mView);
        setContentView(mViewGroup);
        instance = this;

        configuration = new DeviceConfiguration();
        configuration.setConfiguration(getResources().getConfiguration());
        Log.v(TAG, String.format("Confiuration orientation: %s",
                configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                ? "LANDSCAPE" : configuration.orientation == Configuration.ORIENTATION_PORTRAIT
                                ? "PORTRAIT" : "UNDEFINED"));
        appDataDir = getApplicationInfo().dataDir;
        instance = this;
        _setDataDir(appDataDir);
        try {
            ApplicationInfo appi = getPackageManager().getApplicationInfo(
                    getPackageName(), PackageManager.GET_META_DATA);
            if (appi != null && appi.metaData != null) {
                metadata.putAll(appi.metaData);
            }

        } catch (NameNotFoundException e) {
            Log.w(TAG, "Error getting Application info.");
        }

        try {            
            ActivityInfo ai = FXActivity.this.getPackageManager().getActivityInfo(
                    getIntent().getComponent(), PackageManager.GET_META_DATA);
            if (ai != null && ai.metaData != null) {
                metadata.putAll(ai.metaData);           
            }

        } catch (NameNotFoundException e) {
            Log.w(TAG, "Error getting Activity info.");
        }
        
        int dport = metadata.getInt(META_DATA_DEBUG_PORT);
        if (dport > 0) {
            android.os.Debug.waitForDebugger();
        }

    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "onDestroy");
        super.onDestroy();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    @Override
    protected void onPause() {
        Log.v(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.v(TAG, "onResume");
        super.onResume();
    }

    @Override
    protected void onStart() {
        Log.v(TAG, "onStart");
        super.onStart();
    }

    @Override
    protected void onRestart() {
        Log.v(TAG, "onRestart");
        super.onRestart();
    }

    @Override
    protected void onStop() {
        Log.v(TAG, "onStop");
        super.onStop();
    }

    public static FXActivity getInstance() {
        return instance;
    }

    public static ViewGroup getViewGroup() {
        return mViewGroup;
    }


    public static String getDataDir() {
        return appDataDir;
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.v(TAG, "Called onConfigurationChanged");
        configuration.setConfiguration(getResources().getConfiguration());
    }

    private native void _jfxEventsLoop();

    private native void _setDataDir(String dir);

    private native void _setSurface(Surface surface);

    class DeviceConfiguration {

        private static final int ORIENTATION_CHANGE = 1;
        private int change = 0;
        private int orientation;

        DeviceConfiguration() {
        }

        void setConfiguration(Configuration config) {
            if (orientation != config.orientation) {
                orientation = config.orientation;
                change |= ORIENTATION_CHANGE;
            }
        }

        int getOrientation() {
            return orientation;
        }

        boolean isChanged() {
            return change > 0;
        }

        void dispatch() {
            if ((change & ORIENTATION_CHANGE) > 0) {
                Log.v(TAG, "Dispatching orientation change to");
                try {
                    onConfigurationChangedNativeMethod.invoke(null, SCREEN_ORIENTATION);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to invoke com.sun.glass.ui.android.DalvikInput.onConfigurationChangedNative method by reflection", e);
                }
       
            }
            change = 0;
        }
    }




}
