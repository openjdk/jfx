/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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



public class FXActivity extends Activity implements SurfaceHolder.Callback,
        SurfaceHolder.Callback2 {

    private static final String TAG = "FXActivity";

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

    private static boolean glassHasStarted = false;
    private static String appDataDir;
    private static SurfaceDetails surfaceDetails;
    private static DeviceConfiguration configuration;
    private static InputMethodManager imm;

    private static CountDownLatch cdlEvLoopFinished;

    // Cache method handles
    // Can not access com.sun.glass.ui.android.DalvikInput directly, because the javafx classes are loaded with a different classloader 
    private Method onMultiTouchEventMethod;
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

    static {
        System.loadLibrary(ACTIVITY_LIB);
        System.setProperty("javax.xml.stream.XMLInputFactory",
                "com.sun.xml.stream.ZephyrParserFactory");
        System.setProperty("javax.xml.stream.XMLOutputFactory",
                "com.sun.xml.stream.ZephyrWriterFactory");
        System.setProperty("javax.xml.stream.XMLEventFactory",
                "com.sun.xml.stream.events.ZephyrEventFactory");

    }
        
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.v(TAG, "onCreate FXActivity");
        if (launcher != null) {
            Log.v(TAG, "JavaFX application is already running");
            return;
        }
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED);
        getWindow().setFormat(PixelFormat.RGBA_8888);

        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        mView = new InternalSurfaceView(this);
        mView.getHolder().addCallback(this);
        mViewGroup = new FrameLayout(this);
        mViewGroup.addView(mView);
        setContentView(mViewGroup);
        instance = this;

        // Before the secondary dex file can be processed by the DexClassLoader,
        // it has to be first copied from asset resource to a storage location.
        dexInternalStoragePath = getDir("dex", Context.MODE_PRIVATE);
        
        try {
            if(isUptodate()) {
                dexClassPath = (String) getClassLoaderProperties().get("classpath");
            } else {
                extractDexFiles(APPLICATION_DEX_NAME);
                copy(APPLICATION_RESOURCES_NAME);
                writeClassLoaderProperties();
            }
        } catch (FileNotFoundException e) {
            Log.v(TAG, "Not found application dex and resources jars.", e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy a file.", e);
        }
        Log.v(TAG, "Secondary dex file classpath " + dexClassPath);
        configuration = new DeviceConfiguration();
        configuration.setConfiguration(getResources().getConfiguration());
        Log.v(TAG, String.format("Confiuration orientation: %s",
                configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                ? "LANDSCAPE" : configuration.orientation == Configuration.ORIENTATION_PORTRAIT
                ? "PORTRAIT" : "UNDEFINED"));
        appDataDir = getApplicationInfo().dataDir;
        instance = this;
        _setDataDir(appDataDir);
        jfxEventsLoop();
    }

    private boolean isUptodate() {
        
        try {
            String currentBuildTime = getCurrentBuildStamp();
            if (currentBuildTime == null) {
                return false;
            }
            Properties props = getClassLoaderProperties();
            if(props == null) {
                return false;
            }
            String extractedBuildTime = props.getProperty("buildStamp");
            return currentBuildTime.equals(extractedBuildTime);
        } catch (IOException e) {
            Log.d(TAG, "Failed to compare build timestamps. reason:" + e.getMessage());
        }
        return false;
    }

    private String getCurrentBuildStamp() {
        if(currentBuildStamp == null) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(getAssets().open(BUILD_TIME_NAME), "UTF-8"));
                currentBuildStamp = reader.readLine();
            } catch (IOException e) {
                Log.e(TAG, "Failed to read build timestamp.", e);
            } finally {
                if(reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) { }
                }
            }
        }
        return currentBuildStamp;
    }
    
    private Properties getClassLoaderProperties() throws IOException {
        if(classLoaderProperties == null) {
            BufferedReader reader = null;
            Properties props;
            try {
                props = new Properties();
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(dexInternalStoragePath, CLASSLOADER_PROPERTIES_NAME)), "UTF-8"));
                props.load(reader);
            } catch (FileNotFoundException e) {
                return null;
            } finally {
                if(reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) { }
                }
            }
            classLoaderProperties = props;
        }
        return classLoaderProperties;
    }
    
    private void extractDexFiles(String file) throws IOException {
        BufferedInputStream bis = null;
        ZipInputStream zis = null;
        try {
            Log.v(TAG, "extracting secondary dex files from '" + file + "' from asset resource to storage location");
            bis = new BufferedInputStream(getAssets().open(file));
            zis = new ZipInputStream(bis);
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                if(ze.getName().endsWith(".dex")) {
                    OutputStream dexWriter = null;
                    try {
                        File dexFile = new File(dexInternalStoragePath, ze.getName());
                        if(! dexClassPath.isEmpty()) {
                            dexClassPath += File.pathSeparator;
                        }
                        dexClassPath += dexFile.getAbsolutePath();
                        dexWriter = new BufferedOutputStream(new FileOutputStream(dexFile));
                        byte[] buf = new byte[BUF_SIZE];
                        int len;
                        while ((len = zis.read(buf, 0, BUF_SIZE)) > 0) {
                            dexWriter.write(buf, 0, len);
                        }
                    } finally {
                        if(dexWriter != null) { dexWriter.close(); };
                    }
                }
            }
        } finally {
            try {
                if (zis != null) {
                    zis.close();
                }
                if (bis != null) {
                    bis.close();
                }
            } catch (Exception e2) {
            }
        }
    }
    
    
    private void copy(String file) throws IOException, FileNotFoundException {
            BufferedOutputStream writer = null;
            BufferedInputStream bis = null;
            Log.v(TAG, "copy secondary resource file '" + file + 
                    "' from asset resource to storage location");
            bis = new BufferedInputStream(getAssets().open(file));
            try {
                File dexFile = new File(dexInternalStoragePath, file);
                if(! dexClassPath.isEmpty()) {
                    dexClassPath += File.pathSeparator;
                }
                dexClassPath += dexFile.getAbsolutePath();
                writer = new BufferedOutputStream(new FileOutputStream(dexFile));
                byte[] buf = new byte[BUF_SIZE];
                int len;
                while ((len = bis.read(buf, 0, BUF_SIZE)) > 0) {
                    writer.write(buf, 0, len);
                }
            
            } finally {
                  if(writer != null) { writer.close(); }
                  if (bis != null) {bis.close();}
            }
    }
    
    private void writeClassLoaderProperties() {
        Properties props =  new Properties();
        props.put("classpath", dexClassPath);
        props.put("buildStamp", getCurrentBuildStamp());

        BufferedOutputStream writer = null;
        File propertyFile = new File(dexInternalStoragePath, CLASSLOADER_PROPERTIES_NAME);
        Log.d(TAG, "writing " + propertyFile.getAbsolutePath());
        
        try {
            writer = new BufferedOutputStream(new FileOutputStream(propertyFile));
            props.store(writer, null);
        } catch (IOException e) {
            Log.e(TAG, "failed to write " + propertyFile.getAbsolutePath(), e);
        } finally {
            if (writer != null) { 
                try {
                    writer.close();
                } catch (IOException e) { }
            }
        }
    }
    
    private void getLauncherAndLaunchApplication() {
        //load metadata
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

        launcherClassName = metadata.containsKey(META_DATA_LAUNCHER_CLASS) ?
                metadata.getString(META_DATA_LAUNCHER_CLASS) : DEFAULT_LAUNCHER_CLASS;
        
        mainClassName = metadata.containsKey(META_DATA_MAIN_CLASS) ?
                metadata.getString(META_DATA_MAIN_CLASS) : null;
        
        preloaderClassName = metadata.containsKey(META_DATA_PRELOADER_CLASS) ?
                metadata.getString(META_DATA_PRELOADER_CLASS) : null;
        if (mainClassName == null || mainClassName.length() == 0) {
            throw new RuntimeException("Main application class must be defined.\n"
                    + "Use <meta-data android.name=\"main.class\" "
                    + "android.value=\"your.package.YourMainClass\"/>");
        }
        if (preloaderClassName != null && preloaderClassName.length() == 0) {
            preloaderClassName = null;
        }
        
        //launch application
        try {
            Class<Launcher> clazz = (Class<Launcher>) Thread.currentThread().getContextClassLoader().loadClass(launcherClassName);
            launcher = clazz.newInstance();
            launcher.launchApp(this, mainClassName, preloaderClassName);

        } catch (Exception ex) {
            throw new RuntimeException("Did not create correct launcher.", ex);
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

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.v(TAG, "Surface created.");
        surfaceDetails = new SurfaceDetails(holder.getSurface());
        _setSurface(surfaceDetails.surface);
        if (launcher == null) {
            //surface ready now is time to launch javafx
            getLauncherAndLaunchApplication();
        } else {
            try {
                onSurfaceChangedNativeMethod1.invoke(null);
            } catch (Exception e) {
                throw new RuntimeException("Failed to invoke com.sun.glass.ui.android.DalvikInput.onSurfaceChangedNative1 method by reflection", e);
            }
        }
    }

    public static Surface getSurface() {
        return surfaceDetails != null ? surfaceDetails.surface : null;
    }

    public static String getDataDir() {
        return appDataDir;
    }

    public static void notify_glassHasStarted() {
        Log.v(TAG, "notify_glassHasStarted");
        glassHasStarted = true;
    }

    private static void notify_glassShutdown() {
        Log.v(TAG, "notify_glassShutdown");
        new Thread(new Runnable() {
            public void run() {
                try {
                    cdlEvLoopFinished.await();
                } catch (InterruptedException ex) {
                    //SNH
                }
                instance.finish();
            }
        }).start();
    }

    private static void notify_showIME() {
        Log.v(TAG, "Called notify_showIME");
        mView.requestFocus();
        imm.showSoftInput(mView, 0);
    }

    private static void notify_hideIME() {
        Log.v(TAG, "Called notify_hideIME");
        mView.requestFocus();
        imm.hideSoftInputFromWindow(mView.getWindowToken(), 0);
    }

    public void jfxEventsLoop() {
        cdlEvLoopFinished = new CountDownLatch(1);
        Thread t = new Thread(new Runnable() {
            public void run() {
                _jfxEventsLoop();
                Log.v(TAG, "FXActivityEventsLoop finished.");
                cdlEvLoopFinished.countDown();
            }
        }, "FXActivityEventsLoop");
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        Log.v(TAG, String.format("Called Surface changed [%d, %d]", width, height));
        if (glassHasStarted) {
            if (configuration.isChanged()) {
                configuration.dispatch();
            }
        }
        surfaceDetails = new SurfaceDetails(holder.getSurface(), format, width, height);
        _setSurface(surfaceDetails.surface);
        if (glassHasStarted) {
            try {
                onSurfaceChangedNativeMethod2.invoke(null, surfaceDetails.format, surfaceDetails.width, surfaceDetails.height);
            } catch (Exception e) {
                throw new RuntimeException("Failed to invoke com.sun.glass.ui.android.DalvikInput.onSurfaceChangedNative2 method by reflection", e);
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.v(TAG, "Called Surface destroyed");
        surfaceDetails = new SurfaceDetails();
        _setSurface(surfaceDetails.surface);
        if (glassHasStarted) {
            try {
                onSurfaceChangedNativeMethod1.invoke(null);
            } catch (Exception e) {
                throw new RuntimeException("Failed to invoke com.sun.glass.ui.android.DalvikInput.onSurfaceChangedNative1 method by reflection", e);
            }
        }
    }

    @Override
    public void surfaceRedrawNeeded(SurfaceHolder holder) {
        Log.v(TAG, "Called Surface redraw needed");
        if (holder.getSurface() != surfaceDetails.surface) {
            surfaceDetails = new SurfaceDetails(holder.getSurface());
            _setSurface(surfaceDetails.surface);
        }
        if (glassHasStarted) {
            try {
                onSurfaceRedrawNeededNativeMethod.invoke(null);
            } catch (Exception e) {
                throw new RuntimeException("Failed to invoke com.sun.glass.ui.android.DalvikInput.onSurfaceRedrawNeededNative method by reflection", e);
            }
        }
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

    class SurfaceDetails {

        Surface surface;
        int format;
        int width;
        int height;

        SurfaceDetails() {
        }

        SurfaceDetails(Surface surface) {
            this.surface = surface;
        }

        SurfaceDetails(Surface surface, int format, int width, int height) {
            this.surface = surface;
            this.format = format;
            this.width = width;
            this.height = height;
        }
    }

    class InternalSurfaceView extends SurfaceView {

        public InternalSurfaceView(Context context) {
            super(context);
            setFocusableInTouchMode(true);
        }

        private static final int ACTION_POINTER_STILL = -1;

        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
            if (!glassHasStarted) {
                return false;
            }
            int action = event.getAction();
            int actionCode = action & MotionEvent.ACTION_MASK;
            final int pcount = event.getPointerCount();
            final int[] actions = new int[pcount];
            final int[] ids = new int[pcount];
            final int[] touchXs = new int[pcount];
            final int[] touchYs = new int[pcount];

            if (pcount > 1) {
                //multitouch
                if (actionCode == MotionEvent.ACTION_POINTER_DOWN
                        || actionCode == MotionEvent.ACTION_POINTER_UP) {

                    int pointerIndex = event.getActionIndex();
                    for (int i = 0; i < pcount; i++) {
                        actions[i] = pointerIndex == i ? actionCode : ACTION_POINTER_STILL;
                        ids[i] = event.getPointerId(i);
                        touchXs[i] = (int) event.getX(i);
                        touchYs[i] = (int) event.getY(i);
                    }
                } else if (actionCode == MotionEvent.ACTION_MOVE) {
                    for (int i = 0; i < pcount; i++) {
                        touchXs[i] = (int) event.getX(i);
                        touchYs[i] = (int) event.getY(i);
                        actions[i] = MotionEvent.ACTION_MOVE;
                        ids[i] = event.getPointerId(i);
                    }
                }
            } else {
                //single touch
                actions[0] = actionCode;
                ids[0] = event.getPointerId(0);
                touchXs[0] = (int) event.getX();
                touchYs[0] = (int) event.getY();
            }
            Log.e(TAG, "call native MultitouchEvent");
            try {
                onMultiTouchEventMethod.invoke(null, pcount, actions, ids, touchXs, touchYs);
            } catch (Exception e) {
                throw new RuntimeException("Failed to invoke com.sun.glass.ui.android.DalvikInput.onMultiTouchEvent method by reflection", e);
            }
            return true;
        }

        @Override
        public boolean dispatchKeyEvent(final KeyEvent event) {
            if (!glassHasStarted) {
                return false;
            }
            try {
                onKeyEventMethod.invoke(null, event.getAction(), event.getKeyCode(), event.getCharacters());
            } catch (Exception e) {
                throw new RuntimeException("Failed to invoke com.sun.glass.ui.android.DalvikInput.onKeyEventMethod method by reflection", e);
            }
            return true;
        }
    }

    protected void setOnMultiTouchEventMethod(Method onMultiTouchEventMethod) {
        this.onMultiTouchEventMethod = onMultiTouchEventMethod;
    }

    protected void setOnKeyEventMethod(Method onKeyEventMethod) {
        this.onKeyEventMethod = onKeyEventMethod;
    }

    protected void setOnSurfaceChangedNativeMethod1(
            Method onSurfaceChangedNativeMethod1) {
        this.onSurfaceChangedNativeMethod1 = onSurfaceChangedNativeMethod1;
    }

    protected void setOnSurfaceChangedNativeMethod2(
            Method onSurfaceChangedNativeMethod2) {
        this.onSurfaceChangedNativeMethod2 = onSurfaceChangedNativeMethod2;
    }

    protected void setOnSurfaceRedrawNeededNativeMethod(
            Method onSurfaceRedrawNeededNativeMethod) {
        this.onSurfaceRedrawNeededNativeMethod = onSurfaceRedrawNeededNativeMethod;
    }

    protected void setOnConfigurationChangedNativeMethod(
            Method onConfigurationChangedNativeMethod) {
        this.onConfigurationChangedNativeMethod = onConfigurationChangedNativeMethod;
    }

}
