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
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
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

public class FXActivity extends Activity implements SurfaceHolder.Callback, 
        SurfaceHolder.Callback2 {
	
    private static final String TAG     = "FXActivity";
    
    private static final String GLASS_LENS_ANDROID_LIB      = "glass_lens_android";
    private static final String META_DATA_LAUNCHER_CLASS    = "launcher.class";
    private static final String DEFAULT_LAUNCHER_CLASS      = "com.oracle.dalvik.JavaSELauncher";
    
    private static FXActivity   instance;
    private static Launcher     launcher;
    private static FrameLayout  mViewGroup;
    private static SurfaceView  mView;

    private String              appDataDir;
    private InputMethodManager  imm;   

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
        Log.v(TAG, "Loading glass native library.");
        System.loadLibrary(GLASS_LENS_ANDROID_LIB);        
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

    private void getLauncherAndLaunchApplication() {
        Bundle metaData = getMetadata();
        String launcherClass = metaData.getString(
                META_DATA_LAUNCHER_CLASS,
                DEFAULT_LAUNCHER_CLASS);
        try {
            Class clazz = Class.forName(launcherClass);
            launcher = (Launcher)clazz.newInstance();
            launcher.launchApp(this, metaData);
            
        } catch (Exception ex) {
            throw new RuntimeException("Did not created correct launcher.", ex);
        }
    }
    
    @Override
    protected void onDestroy() {
        android.os.Process.killProcess(android.os.Process.myPid());
        super.onDestroy();
    }
    
    public static FXActivity getInstance() {
        return instance;
    }

    public String getDataDir() {
        if (appDataDir == null) {
            appDataDir = this.getApplicationInfo().dataDir;
        }        
        return appDataDir;
    }
    
    public static ViewGroup getViewGroup() {
        return mViewGroup;
    }    
	
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.v(TAG, "Surface created.");
        _surfaceChanged(holder.getSurface());
        if (launcher == null) {
            getLauncherAndLaunchApplication();
        }
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
    
    private void shutdown() {
        Log.e(TAG, "VM SHUTDOWN");
        finish();
    }
	
    class InternalSurfaceView extends SurfaceView {

        public InternalSurfaceView(Context context) {
            super(context);
            setFocusableInTouchMode(true);
        }        

        private static final int ACTION_POINTER_STILL = -1;
        
        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {      
            int action = event.getAction();
            int actionCode = action & MotionEvent.ACTION_MASK;
            int pcount = event.getPointerCount();
            int[] actions = new int[pcount];
            int[] ids = new int[pcount];
            int[] touchXs = new int[pcount];
            int[] touchYs = new int[pcount];

            if (pcount > 1) {                      
                //multitouch
                if (actionCode == MotionEvent.ACTION_POINTER_DOWN ||
                    actionCode == MotionEvent.ACTION_POINTER_UP) {

                    int pointerIndex = event.getActionIndex();
                    for (int i = 0;i <pcount; i++) {
                        actions[i] = pointerIndex == i ? actionCode : ACTION_POINTER_STILL;
                        ids[i] = event.getPointerId(i);
                        touchXs[i] = (int)event.getX(i);
                        touchYs[i] = (int)event.getY(i);                        
                    }                    
                } else if (actionCode == MotionEvent.ACTION_MOVE) {
                    for (int i = 0;i <pcount; i++) {
                        touchXs[i] = (int)event.getX(i);
                        touchYs[i] = (int)event.getY(i);                    
                        actions[i] = MotionEvent.ACTION_MOVE;
                        ids[i] = event.getPointerId(i);                        
                    }                    
                }                 
            } else {
                //single touch
                actions[0] = actionCode;
                ids[0] = event.getPointerId(0);
                touchXs[0] = (int)event.getX();
                touchYs[0] = (int)event.getY();                
            }                 
            onMultiTouchEventNative(pcount, actions, ids, touchXs, touchYs);
            return true;
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            onKeyEventNative(event.getAction(), event.getKeyCode(), event.getCharacters());
            return true;
        }
        
        private native void onMultiTouchEventNative(int count, int[] actions, 
                int[] ids, int[] touchXs, int[] touchYs);
        

        private native void onKeyEventNative(int action, int keycode, String characters);

    }

}
