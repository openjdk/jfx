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
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.inputmethod.InputMethodManager;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;

public class FXDalvikEntity implements SurfaceHolder.Callback,
        SurfaceHolder.Callback2 {
    private static final String ACTIVITY_LIB = "activity";
    private static final String META_DATA_LAUNCHER_CLASS = "launcher.class";
    private static final String DEFAULT_LAUNCHER_CLASS = "javafxports.android.DalvikLauncher";
    public static final String META_DATA_MAIN_CLASS = "main.class";
    private static final String META_DATA_PRELOADER_CLASS = "preloader.class";
    private static final String META_DATA_DEBUG_PORT = "debug.port";

    private static final String APPLICATION_DEX_NAME = "Application_dex.jar";
    private static final String APPLICATION_RESOURCES_NAME = "Application_resources.jar";
    private static final String CLASSLOADER_PROPERTIES_NAME = "classloader.properties";
        private int SCREEN_ORIENTATION = 1;
        private String launcherClassName;
    private String mainClassName;
    private String preloaderClassName;
    
    private static final String TAG = "FXEntity";
    private SurfaceDetails surfaceDetails;
    private static Launcher launcher;
    private final Bundle metadata;
    private final Activity activity;
    
    private static boolean glassHasStarted = false;
    private Method onMultiTouchEventMethod;
    private Method onKeyEventMethod;
    private static Method onGlobalLayoutChangedMethod;
    private Method onSurfaceChangedNativeMethod1;
    private Method onSurfaceChangedNativeMethod2;
    private Method onSurfaceRedrawNeededNativeMethod;
    private Method onConfigurationChangedNativeMethod;
    private static Method initializeMonocleMethod;
    
    private static InputMethodManager imm;
    private static InternalSurfaceView myView;
    private static CountDownLatch cdlEvLoopFinished;

    private float density;

    public FXDalvikEntity (Bundle metadata, Activity activity) {
        this.metadata = metadata;
        this.activity = activity;
        
        imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        jfxEventsLoop();
    }
    
    public Activity getActivity() {
        return this.activity;
    }
    
    public void getLauncherAndLaunchApplication() {


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


    public SurfaceView createView () {
        myView = new InternalSurfaceView(activity);
        myView.getHolder().addCallback(this);
        return myView;
    }
    
  @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.v(TAG, "Surface created.");
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        Log.v(TAG, "metrics = "+metrics);
        surfaceDetails = new SurfaceDetails(holder.getSurface(), metrics.density);
        _setSurface(surfaceDetails.surface);
        density = metrics.density;
        _setDensity(surfaceDetails.density);
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

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        Log.v(TAG, String.format("Called Surface changed [%d, %d], format %d", width, height, format));
        if (glassHasStarted) {
//            if (configuration.isChanged()) {
//                configuration.dispatch();
//            }
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
// this is dirty. We need to wait for at least 1 pulse.
                 // Thread.currentThread().sleep(250);
        Log.v(TAG, "Redraw...");
                onSurfaceRedrawNeededNativeMethod.invoke(null);
        Log.v(TAG, "Wait a while before doing this again...");
                 Thread.currentThread().sleep(200);
        Log.v(TAG, "Redraw again...");
                onSurfaceRedrawNeededNativeMethod.invoke(null);
            } catch (Exception e) {
                throw new RuntimeException("Failed to invoke com.sun.glass.ui.android.DalvikInput.onSurfaceRedrawNeededNative method by reflection", e);
            }
        }
    }
    protected void setInitializeMonocleMethod (Method v ) {
        initializeMonocleMethod  = v ;
    }
        
    protected void setOnMultiTouchEventMethod(Method onMultiTouchEventMethod) {
        this.onMultiTouchEventMethod = onMultiTouchEventMethod;
    }

    protected void setOnKeyEventMethod(Method onKeyEventMethod) {
        this.onKeyEventMethod = onKeyEventMethod;
    }

    protected void setOnGlobalLayoutChangedMethod(Method method) {
        onGlobalLayoutChangedMethod = method;
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
    
    
    public static void notify_glassHasStarted() {
        Log.v(TAG, "notify_glassHasStarted called in FXActivity. register device now.");
        glassHasStarted = true;
        try {
            initializeMonocleMethod.invoke(null);
        // com.sun.glass.ui.monocle.AndroidInputDeviceRegistry.registerDevice();
        }
        catch (Throwable t) {
            System.out.println ("throwable: "+t);
            t.printStackTrace();
        }
        System.out.println("register device done");
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
             //   instance.finish();
            }
        }).start();
    }

private static long softInput = 0L;
    private static void notify_showIME() {
        Log.v(TAG, "Called notify_showIME");
        // myView.requestFocus();
        imm.showSoftInput(myView, 0);
        softInput = System.currentTimeMillis();
        Log.v(TAG, "Done calling notify_showIME");
    }

    private static void notify_hideIME() {
        Log.v(TAG, "Called notify_hideIME");
        // myView.requestFocus();
        imm.hideSoftInputFromWindow(myView.getWindowToken(), 0);
        softInput = 0L;
        Log.v(TAG, "Done Calling notify_hideIME");
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

    private native void _jfxEventsLoop();

    private native void _setDataDir(String dir);

    private native void _setSurface(Surface surface);

    private native void _setDensity(float density);
    
    
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
                        touchXs[i] = (int) (event.getX(i)/density);
                        touchYs[i] = (int) (event.getY(i)/density);
                    }
                } else if (actionCode == MotionEvent.ACTION_MOVE) {
                    for (int i = 0; i < pcount; i++) {
                        touchXs[i] = (int) (event.getX(i)/density);
                        touchYs[i] = (int) (event.getY(i)/density);
                        actions[i] = MotionEvent.ACTION_MOVE;
                        ids[i] = event.getPointerId(i);
                    }
                }
            } else {
                //single touch
                actions[0] = actionCode;
                ids[0] = event.getPointerId(0);
                touchXs[0] = (int) (event.getX()/density);
                touchYs[0] = (int) (event.getY()/density);
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
        float density;

        SurfaceDetails() {
        }

        SurfaceDetails(Surface surface) {
            this.surface = surface;
        }

        SurfaceDetails(Surface surface, float density) {
            this.surface = surface;
            this.density = density;
        }

        SurfaceDetails(Surface surface, int format, int width, int height) {
            this.surface = surface;
            this.format = format;
            this.width = width;
            this.height = height;
        }
    }

}
