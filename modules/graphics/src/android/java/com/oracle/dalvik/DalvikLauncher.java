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

package com.oracle.dalvik;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map.Entry;
import java.util.Properties;

public class DalvikLauncher implements Launcher {

    private static final String TAG                                     = "DalvikLauncher";
    private static final String JAVAFX_APPLICATION_APPLICATION          = "javafx.application.Application";
	private static final String COM_SUN_JAVAFX_APPLICATION_LAUNCHERIMPL = "com.sun.javafx.application.LauncherImpl";
	private static final String LAUNCH_APPLICATION_METHOD               = "launchApplication";
	private static final String MAIN_METHOD                             = "main";
    private static final String META_DATA_MAIN_CLASS                    = "main.class";
	private static final String META_DATA_PRELOADER_CLASS               = "preloader.class";    
    private static final String ANDROID_PROPERTY_PREFIX                 = "android.";
    
    private static final Class[] LAUNCH_APPLICATION_ARGS = new Class[] {
			Class.class, Class.class, (new String[0]).getClass() };

	private static final Class[] MAIN_METHOD_ARGS = new Class[] { 
			(new String[0]).getClass() };
    
    private static boolean fxApplicationLaunched = false;
	private static boolean fxApplicationLaunching = false;
    
    private Activity    activity;
    private Bundle      metadata;
    
    public void launchApp(Activity a, Bundle metadata) {
        this.activity = a;
        this.metadata = metadata;
        Properties userProperties = new Properties();
		try {
			userProperties.load(DalvikLauncher.class.getResourceAsStream("/javafx.platform.properties"));
			String key = null;
            for(Entry<Object, Object> e:userProperties.entrySet()) {
                key = (String)e.getKey();
                System.setProperty(key.startsWith(ANDROID_PROPERTY_PREFIX) ?
                    key.substring(ANDROID_PROPERTY_PREFIX.length()) : key,
                    (String)e.getValue());
			}
			System.getProperties().list(System.out);
			
		} catch (IOException e) {
			throw new RuntimeException("Can't load properties", e);
		}
        
        Log.v(TAG, "Launch JavaFX application on dalvik vm.");
        try {            
			final Class applicationClass = resolveApplicationClass();
			final Class preloaderClass = resolvePreloaderClass();
			final Class javafxApplicationClass = Class.forName(JAVAFX_APPLICATION_APPLICATION);
			final Class javafxLauncherClass = Class.forName(COM_SUN_JAVAFX_APPLICATION_LAUNCHERIMPL);
	
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
							launchMethod.invoke(null, new Object[] {
									applicationClass, preloaderClass,
									new String[] {} });
						} else {
							Method mainMethod = applicationClass.getMethod(
									MAIN_METHOD, MAIN_METHOD_ARGS);
							mainMethod.invoke(null,
									new Object[] { new String[] {} });
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					fxApplicationLaunched = true;
					fxApplicationLaunching = false;
				}
			}, "Prelauncher Thread").start();
	
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    private Class resolveApplicationClass()
			throws PackageManager.NameNotFoundException, ClassNotFoundException {

		Class clazz = null;
        String applicationClassName = metadata.getString(META_DATA_MAIN_CLASS);
		if (applicationClassName != null && applicationClassName.length() > 0) {
			clazz = Class.forName(applicationClassName);
		}
		return clazz;
	}
    
    private Class resolvePreloaderClass()
			throws PackageManager.NameNotFoundException, ClassNotFoundException {

		Class clazz = null;
        String className = metadata.getString(META_DATA_PRELOADER_CLASS);
		if (className != null && className.length() > 0) {
			clazz = Class.forName(className);
		}
		return clazz;
	}
    
}
