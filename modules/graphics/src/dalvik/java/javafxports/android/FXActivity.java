/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;

import javafx.application.Platform;

public class FXActivity extends Activity implements SurfaceHolder.Callback,
        SurfaceHolder.Callback2 {

    private static final String TAG = "FXActivity";

    private static final String ACTIVITY_LIB = "activity";
    private static final String META_DATA_LAUNCHER_CLASS = "launcher.class";
    private static final String DEFAULT_LAUNCHER_CLASS = "javafxports.android.DalvikLauncher";
    private static final String META_DATA_DEBUG_PORT = "debug.port";

    public static final String APPLICATION_DEX_NAME = "Application_dex.jar";
    static final int BUF_SIZE = 8 * 1024;

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

    //configurations
    private int SCREEN_ORIENTATION = 1;

    static {
        System.loadLibrary(ACTIVITY_LIB);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.v(TAG, "onCreate");
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
        File dexInternalStoragePath = new File(getDir("dex", Context.MODE_PRIVATE), APPLICATION_DEX_NAME);
        BufferedInputStream bis = null;
        OutputStream dexWriter = null;

        try {
            bis = new BufferedInputStream(getAssets().open(APPLICATION_DEX_NAME));
            Log.v(TAG, "copy secondary dex file '" + APPLICATION_DEX_NAME + "' from asset resource to storage location");
            dexWriter = new BufferedOutputStream(new FileOutputStream(dexInternalStoragePath));
            byte[] buf = new byte[BUF_SIZE];
            int len;
            while ((len = bis.read(buf, 0, BUF_SIZE)) > 0) {
                dexWriter.write(buf, 0, len);
            }
        } catch (FileNotFoundException e) {
            Log.v(TAG, "no secondary dex file '" + APPLICATION_DEX_NAME + "' found");
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy secondary dex file.", e);
        } finally {
            try {
                if (dexWriter != null) {
                    dexWriter.close();
                }
                if (bis != null) {
                    bis.close();
                }
            } catch (Exception e2) {
            }
        }
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

    private Bundle getMetadata() {
        try {
            ActivityInfo ai = FXActivity.this.getPackageManager().getActivityInfo(
                    getIntent().getComponent(), PackageManager.GET_META_DATA);
            return ai.metaData;

        } catch (NameNotFoundException e) {
            throw new RuntimeException("Error getting activity info", e);
        }
    }

    private void getLauncherAndLaunchApplication() {
        Bundle metaData = getMetadata();
        int dport = metaData.getInt(META_DATA_DEBUG_PORT);
        if (dport > 0) {
            android.os.Debug.waitForDebugger();
        }
        String launcherClass = metaData.getString(
                META_DATA_LAUNCHER_CLASS);
        if (launcherClass == null) {
            launcherClass = DEFAULT_LAUNCHER_CLASS;
        }
        try {
            Class clazz = Class.forName(launcherClass);
            launcher = (Launcher) clazz.newInstance();
            launcher.launchApp(this, metaData);

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
            _surfaceChanged(surfaceDetails.surface);
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
            _surfaceChanged(surfaceDetails.surface,
                    surfaceDetails.format,
                    surfaceDetails.width,
                    surfaceDetails.height);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.v(TAG, "Called Surface destroyed");
        surfaceDetails = new SurfaceDetails();
        _setSurface(surfaceDetails.surface);
        if (glassHasStarted) {
            _surfaceChanged(null);
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
            _surfaceRedrawNeeded(surfaceDetails.surface);
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

    private native void _surfaceChanged(Surface surface);

    private native void _surfaceChanged(Surface surface, int format, int width, int height);

    private native void _surfaceRedrawNeeded(Surface surface);

    private native void _configurationChanged(int flag);

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
                FXActivity.this._configurationChanged(SCREEN_ORIENTATION);
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
            Platform.runLater(new Runnable() {
                public void run() {
                    onMultiTouchEventNative(pcount, actions, ids, touchXs, touchYs);
                }
            });
            return true;
        }

        @Override
        public boolean dispatchKeyEvent(final KeyEvent event) {
            if (!glassHasStarted) {
                return false;
            }
            Platform.runLater(new Runnable() {
                public void run() {
                    onKeyEventNative(event.getAction(), event.getKeyCode(), event.getCharacters());
                }
            });
            return true;
        }

        private native void onMultiTouchEventNative(int count, int[] actions,
                int[] ids, int[] touchXs, int[] touchYs);

        private native void onKeyEventNative(int action, int keycode, String characters);

    }

}
