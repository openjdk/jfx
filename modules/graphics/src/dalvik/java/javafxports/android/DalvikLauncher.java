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
import android.content.pm.PackageManager;
import android.util.Log;
import dalvik.system.DexClassLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Map.Entry;
import java.util.Properties;
import com.sun.javafx.application.PlatformImpl;
import com.sun.javafx.application.PlatformImpl.FinishListener;

public class DalvikLauncher implements Launcher {

    private static final String TAG = "DalvikLauncher";
    private static final String JAVAFX_APPLICATION_APPLICATION = "javafx.application.Application";
    private static final String COM_SUN_JAVAFX_APPLICATION_LAUNCHERIMPL = "com.sun.javafx.application.LauncherImpl";
    private static final String LAUNCH_APPLICATION_METHOD = "launchApplication";
    private static final String MAIN_METHOD = "main";
    private static final String ANDROID_PROPERTY_PREFIX = "android.";
    private static final String JAVAFX_PLATFORM_PROPERTIES = "javafx.platform.properties";
    private static final String JAVA_CUSTOM_PROPERTIES = "java.custom.properties";

    private static final Class[] LAUNCH_APPLICATION_ARGS = new Class[]{
        Class.class, Class.class, (new String[0]).getClass()};

    private static final Class[] MAIN_METHOD_ARGS = new Class[]{
        (new String[0]).getClass()};

    private static boolean fxApplicationLaunched = false;
    private static boolean fxApplicationLaunching = false;

    private Activity activity;
    private String preloaderClassName, mainClassName;
    private FXDalvikEntity fxDalvikEntity;

    @Override
    public void launchApp(FXDalvikEntity fxDalvikEntity, String mainClassName, String preloaderClassName) {
        this.fxDalvikEntity = fxDalvikEntity;
        this.activity = fxDalvikEntity.getActivity();
        this.preloaderClassName = preloaderClassName;
        this.mainClassName = mainClassName;

        InputStream isJavafxPlatformProperties = null;
        try {
            isJavafxPlatformProperties = DalvikLauncher.class.getResourceAsStream("/" + JAVAFX_PLATFORM_PROPERTIES);
            if (isJavafxPlatformProperties == null) {
                throw new RuntimeException("Could not find /" + JAVAFX_PLATFORM_PROPERTIES + " on classpath.");
            }

            Properties javafxPlatformProperties = new Properties();
            javafxPlatformProperties.load(isJavafxPlatformProperties);
            for (Entry<Object, Object> e : javafxPlatformProperties.entrySet()) {
                String key = (String) e.getKey();
                System.setProperty(key.startsWith(ANDROID_PROPERTY_PREFIX)
                        ? key.substring(ANDROID_PROPERTY_PREFIX.length()) : key,
                        (String) e.getValue());
            }
        } catch (IOException e) {
            Log.v(TAG, "Can't load " + JAVAFX_PLATFORM_PROPERTIES);
            throw new RuntimeException("Can't load " + JAVAFX_PLATFORM_PROPERTIES, e);
        } finally {
            try {
                if (isJavafxPlatformProperties != null) {
                    isJavafxPlatformProperties.close();
                }
            } catch (Exception e) {
                 Log.v(TAG, "Exception closing " + JAVAFX_PLATFORM_PROPERTIES + " InputStream");
            }
        }

        // try loading java custom properties
        InputStream isJavaCustomProperties = null;
        try {
            isJavaCustomProperties = DalvikLauncher.class.getResourceAsStream("/" + JAVA_CUSTOM_PROPERTIES);
            if (isJavaCustomProperties != null) {
                Properties javaCustomProperties = new Properties();
                javaCustomProperties.load(isJavaCustomProperties);
                for (Entry<Object, Object> entry : javaCustomProperties.entrySet()) {
                    System.setProperty((String) entry.getKey(), (String) entry.getValue());
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Can't load " + JAVA_CUSTOM_PROPERTIES, e);
        } finally {
            if (isJavaCustomProperties != null) {
                try {
                    isJavaCustomProperties.close();
                } catch (IOException e) {
                    Log.v(TAG, "Exception closing " + JAVA_CUSTOM_PROPERTIES + " InputStream", e);
                }
            }
        }

        System.getProperties().list(System.out);

        Log.v(TAG, "Launch JavaFX application on DALVIK vm.");
        try {
            initMethodHandles();
        } catch (Exception e) {
            throw new RuntimeException("Failed to init method handles", e);
        }

        try {
            final Class applicationClass = resolveApplicationClass();
            final Class preloaderClass = resolvePreloaderClass();
            final Class javafxApplicationClass = getApplicationClassLoader().loadClass(JAVAFX_APPLICATION_APPLICATION);
            final Class javafxLauncherClass = getApplicationClassLoader().loadClass(COM_SUN_JAVAFX_APPLICATION_LAUNCHERIMPL);

            final Method launchMethod = javafxLauncherClass.getMethod(
                    LAUNCH_APPLICATION_METHOD, LAUNCH_APPLICATION_ARGS);

            Log.v(TAG, String.format("application class: [%s]\n"
                    + "preloader class: [%s]\n"
                    + "javafx application class: [%s]\n"
                    + "javafx launcher class: [%s]\n"
                    + "launch application method: [%s]",
                    applicationClass,
                    preloaderClass,
                    javafxApplicationClass,
                    javafxLauncherClass,
                    launchMethod));

            new Thread(new Runnable() {
                public void run() {
                    fxApplicationLaunching = true;
                    try {
                        if (javafxApplicationClass.isAssignableFrom(applicationClass)) {
                            launchMethod.invoke(null, new Object[]{
                                applicationClass, preloaderClass,
                                new String[]{}});
                        } else {
                            Method mainMethod = applicationClass.getMethod(
                                    MAIN_METHOD, MAIN_METHOD_ARGS);
                            mainMethod.invoke(null,
                                    new Object[]{new String[]{}});
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    fxApplicationLaunched = true;
                    fxApplicationLaunching = false;
                }
            }, "Prelauncher Thread").start();

        } catch (Exception e) {
            Log.e(TAG, "Launch failed with exception.", e);
        }
    }

    private static ClassLoader applicationClassLoader;

    private ClassLoader getApplicationClassLoader() {
        return this.getClass().getClassLoader();
    }

    private void initMethodHandles() throws ClassNotFoundException, NoSuchMethodException, SecurityException {
        Class<?> androidInputDeviceRegistry = getApplicationClassLoader().loadClass("com.sun.glass.ui.monocle.AndroidInputDeviceRegistry");
        Method registerDevice = androidInputDeviceRegistry.getMethod("registerDevice");
        fxDalvikEntity.setInitializeMonocleMethod(registerDevice);
        Class<?> dalvikInputClass = getApplicationClassLoader().loadClass("com.sun.glass.ui.android.DalvikInput");
        fxDalvikEntity.setOnMultiTouchEventMethod(dalvikInputClass.getMethod("onMultiTouchEvent", int.class, int[].class, int[].class, int[].class, int[].class));
        fxDalvikEntity.setOnKeyEventMethod(dalvikInputClass.getMethod("onKeyEvent", int.class, int.class, String.class));
        fxDalvikEntity.setOnGlobalLayoutChangedMethod(dalvikInputClass.getMethod("onGlobalLayoutChanged"));
        fxDalvikEntity.setOnSurfaceChangedNativeMethod1(dalvikInputClass.getMethod("onSurfaceChangedNative"));
        fxDalvikEntity.setOnSurfaceChangedNativeMethod2(dalvikInputClass.getMethod("onSurfaceChangedNative", int.class, int.class, int.class));
        fxDalvikEntity.setOnSurfaceRedrawNeededNativeMethod(dalvikInputClass.getMethod("onSurfaceRedrawNeededNative"));
        fxDalvikEntity.setOnConfigurationChangedNativeMethod(dalvikInputClass.getMethod("onConfigurationChangedNative", int.class));
        boolean hasAccessToFXClasses = false;
        try {
            // this.getClass().getClassLoader().loadClass("com.sun.javafx.application.PlatformImpl.FinishListener");
            registerExitListener();
            Log.v(TAG, "We have JavaFX on our current (base) classpath, registered exit listener");
        }
        catch (Throwable t) {
            Log.v(TAG, "No JavaFX on our current (base) classpath, don't register exit listener");
            t.printStackTrace();
        }
    }

    private void registerExitListener() {
        FinishListener l = new FinishListener() {
            public void idle(boolean implicitExit) {
                Log.v (TAG, "FinishListener idle called with exit = "+implicitExit);
                activity.finish();
            }
            public void exitCalled() {
                Log.v (TAG, "FinishListener exit called");
                activity.finish();
            }
        };
        PlatformImpl.addListener(l);
    }


    private Class resolveApplicationClass()
            throws PackageManager.NameNotFoundException, ClassNotFoundException {

        ClassLoader cl = getApplicationClassLoader();

        Class clazz = null;
        if (mainClassName != null && mainClassName.length() > 0) {
            clazz = cl.loadClass(mainClassName);
        }
        // we set the contextClassLoader of the current thread to the one that loads the
        // application code. Doing so, the FXMLLoader can resolve application classes
        Thread.currentThread().setContextClassLoader(cl);
        return clazz;
    }

    private Class resolvePreloaderClass()
            throws PackageManager.NameNotFoundException, ClassNotFoundException {

        ClassLoader cl = getApplicationClassLoader();

        Class clazz = null;
        if (preloaderClassName != null && preloaderClassName.length() > 0) {
            clazz = cl.loadClass(preloaderClassName);
        }
        return clazz;
    }

}
