/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.scenario;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.sun.javafx.tk.Toolkit;
import javafx.util.Callback;

/**
 * A simple class to store and retrieve internal Scenario settings in the form
 * of String key/value pairs. It is meant to be used in a similar way to System
 * Properties, but without the security restrictions. This class is designed
 * primarily to aid in testing and benchmarking Scenario itself.
 *
 * If you are running in an environment that allows System Property access, this
 * class will attempt to look for a key's value in the System Properties if none
 * is found in Settings. This allows Settings to be set on the command line as
 * well as via the Settings API.
 *
 */
public class Settings {

    private final Map<String, String> settings = new HashMap<>(5);
    private final CopyOnWriteArrayList<Callback<String, Void>> listeners = new CopyOnWriteArrayList<>();
    private static final Object SETTINGS_KEY;
    static {
        SETTINGS_KEY = new StringBuilder("SettingsKey");

        // It seems no longer necessary to force loading of MasterTimer to pick
        // up the hi-res timer workaround. Also, this is causing some init
        // order problems (RT-5572), so it's being commented out.
        // Object obj = ToolkitAccessor.getMasterTimer();
    }

    private static synchronized Settings getInstance() {
        Map<Object, Object> contextMap = Toolkit.getToolkit().getContextMap();
        Settings instance = (Settings) contextMap.get(SETTINGS_KEY);
        if (instance == null) {
            instance = new Settings();
            contextMap.put(SETTINGS_KEY, instance);
        }
        return instance;
    }

    /**
     * Add a new key-value setting.
     *
     * Passing a value of null indicates that the value for this key should be
     * looked for in the System Properties.
     *
     * If PropertyChangeListeners have been registered for the given key, they
     * will be notified of a change in value.
     *
     * If key is "" or null, this methods throws an IllegalArgumentException.
     */
    public static void set(String key, String value) {
        getInstance().setImpl(key, value);
    }

    private void setImpl(String key, String value) {
        checkKeyArg(key);
        settings.put(key, value);
        for (Callback<String, Void> l : listeners) {
            l.call(key);
        }
     }

    /**
     * Retrieve the value for the given key.
     *
     * If the key is not present in Settings or its value is null, this methods
     * then checks to see if a value for this key is present in the System
     * Properties (provided you have sufficient privileges).
     *
     * If no value can be found for the given key, this method returns null.
     *
     * If key is "" or null, this methods throws an IllegalArgumentException.
     */
    public static String get(String key) {
        return getInstance().getImpl(key);
    }

    private String getImpl(String key) {
        checkKeyArg(key);
        String retVal = settings.get(key);
        if (retVal == null) {
            try {
                retVal = System.getProperty(key);
            } catch (SecurityException ignore) {
            }
        }
        return retVal;
    }

    /**
     * Convenience method for boolean settings.
     *
     * If the setting exists and its value is "true", true is returned.
     * Otherwise, false is returned.
     *
     * If key is "" or null, this methods throws an IllegalArgumentException.
     */
    public static boolean getBoolean(String key) {
        return getInstance().getBooleanImpl(key);
    }

    private boolean getBooleanImpl(String key) {
        // get() will call checkKeyArg(), so don't check it here
        String value = getImpl(key);
        return "true".equals(value);
    }

    /**
     * Convenience method for boolean settings.
     *
     * If the setting is set to "true", true is returned. If the setting is set
     * to "false", false is returned. It the setting is set to anything else,
     * defaultVal is returned.
     *
     * If key is "" or null, this methods throws an IllegalArgumentException.
     */
    public static boolean getBoolean(String key, boolean defaultVal) {
        return getInstance().getBooleanImpl(key, defaultVal);
    }

    private boolean getBooleanImpl(String key, boolean defaultVal) {
        // get() will call checkKeyArg(), so don't check it here
        String value = getImpl(key);
        boolean retVal = defaultVal;
        if (value != null) {
            if ("false".equals(value)) {
                retVal = false;
            } else if ("true".equals(value)) {
                retVal = true;
            }
        }
        return retVal;
    }

    /**
     * Convenience method for int settings.
     *
     * If the setting exists and its value can be parsed to an int, the int
     * value is returned. Otherwise, the default value is returned.
     *
     * If key is "" or null, this methods throws an IllegalArgumentException.
     */
    public static int getInt(String key, int defaultVal) {
        return getInstance().getIntImpl(key, defaultVal);
    }

    private int getIntImpl(String key, int defaultVal) {
        // get() will call checkKeyArg(), so don't check it here
        String value = getImpl(key);
        int retVal = defaultVal;
        try {
            retVal = Integer.parseInt(value);
        } catch (NumberFormatException ignore) {
            // ignore.printStackTrace();
        }
        return retVal;
    }

    /**
     * Add a PropertyChangeListener for the specified setting
     *
     * Note that the PropertyChangeEvent will contain old and new values as they
     * would be returned from get(), meaning they may come from the System
     * Properties.
     *
     * If key is "" or null, this methods throws an IllegalArgumentException. If
     * listener is null no exception is thrown and no action is taken.
     */
    public static void addPropertyChangeListener(Callback<String, Void> pcl) {
        getInstance().addPropertyChangeListenerImpl(pcl);
    }

    private void addPropertyChangeListenerImpl(Callback<String, Void> pcl) {
        listeners.add(pcl);
    }

    /**
     * Remove the specified PropertyChangeListener.
     *
     * If listener is null, or was never added, no exception is thrown and no
     * action is taken.
     */
    public static void removePropertyChangeListener(Callback<String, Void> pcl) {
        getInstance().removePropertyChangeListenerImpl(pcl);
    }

    private void removePropertyChangeListenerImpl(Callback<String, Void> pcl) {
        listeners.remove(pcl);
    }

    /*
     * Check that key is a valid Settings key. If not, throw an
     * IllegalArgumentException.
     */
    private void checkKeyArg(String key) {
        if (null == key || "".equals(key)) {
            throw new IllegalArgumentException("null key not allowed");
        }
    }

    private Settings() {
    }
}
