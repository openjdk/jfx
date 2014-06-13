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
import java.io.BufferedInputStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Map.Entry;
import java.util.Properties;

public class DalvikLauncher implements Launcher {

    private static final String TAG = "DalvikLauncher";
    private static final String JAVAFX_APPLICATION_APPLICATION = "javafx.application.Application";
    private static final String COM_SUN_JAVAFX_APPLICATION_LAUNCHERIMPL = "com.sun.javafx.application.LauncherImpl";
    private static final String LAUNCH_APPLICATION_METHOD = "launchApplication";
    private static final String MAIN_METHOD = "main";
    private static final String ANDROID_PROPERTY_PREFIX = "android.";

    private static final Class[] LAUNCH_APPLICATION_ARGS = new Class[]{
        Class.class, Class.class, (new String[0]).getClass()};

    private static final Class[] MAIN_METHOD_ARGS = new Class[]{
        (new String[0]).getClass()};

    private static boolean fxApplicationLaunched = false;
    private static boolean fxApplicationLaunching = false;

    private Activity activity;
    private String preloaderClassName, mainClassName;

    public void launchApp(Activity a, String mainClassName, String preloaderClassName) {
        this.activity = a;
        this.preloaderClassName = preloaderClassName;
        this.mainClassName = mainClassName;

        InputStream is = null;
        Properties userProperties = new Properties();
        try {
            is = new BufferedInputStream(this.activity.getAssets().open("javafx.platform.properties"));
            userProperties.load(is);
            String key = null;
            for (Entry<Object, Object> e : userProperties.entrySet()) {
                key = (String) e.getKey();
                System.setProperty(key.startsWith(ANDROID_PROPERTY_PREFIX)
                        ? key.substring(ANDROID_PROPERTY_PREFIX.length()) : key,
                        (String) e.getValue());
            }
            System.getProperties().list(System.out);

        } catch (IOException e) {
            Log.v(TAG, "Can't load properties");
            throw new RuntimeException("Can't load properties", e);
        } finally {
            try {
                is.close();
            } catch (Exception e) {
                 Log.v(TAG, "Exception closing properties InputStream");
            }

        }

        Log.v(TAG, "Launch JavaFX application on dalvik vm.");
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
        if (applicationClassLoader == null) {
            // Internal storage where the DexClassLoader writes the optimized dex file to.
            final File optimizedDexOutputPath = activity.getDir("outdex", Context.MODE_PRIVATE);

            // Initialize the class loader with the secondary dex file.
            // This includes the javafx, compatibility and application classes
            ClassLoader cl = new DexClassLoader(FXActivity.dexClassPath,
                    optimizedDexOutputPath.getAbsolutePath(),
                    FXActivity.getInstance().getApplicationInfo().nativeLibraryDir,
                    activity.getClassLoader());
            Thread.currentThread().setContextClassLoader(cl);
            applicationClassLoader = cl;
            Log.v(TAG, "Application classloader initialized: " + applicationClassLoader);
        }
        return applicationClassLoader;
    }

    private void initMethodHandles() throws ClassNotFoundException, NoSuchMethodException, SecurityException {
        Class<?> dalvikInputClass = getApplicationClassLoader().loadClass("com.sun.glass.ui.android.DalvikInput");
        FXActivity.getInstance().setOnMultiTouchEventMethod(dalvikInputClass.getMethod("onMultiTouchEvent", int.class, int[].class, int[].class, int[].class, int[].class));
        FXActivity.getInstance().setOnKeyEventMethod(dalvikInputClass.getMethod("onKeyEvent", int.class, int.class, String.class));
        FXActivity.getInstance().setOnSurfaceChangedNativeMethod1(dalvikInputClass.getMethod("onSurfaceChangedNative"));
        FXActivity.getInstance().setOnSurfaceChangedNativeMethod2(dalvikInputClass.getMethod("onSurfaceChangedNative", int.class, int.class, int.class));
        FXActivity.getInstance().setOnSurfaceRedrawNeededNativeMethod(dalvikInputClass.getMethod("onSurfaceRedrawNeededNative"));
        FXActivity.getInstance().setOnConfigurationChangedNativeMethod(dalvikInputClass.getMethod("onConfigurationChangedNative", int.class));
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
