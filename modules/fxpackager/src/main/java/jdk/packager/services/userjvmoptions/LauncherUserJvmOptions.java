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

package jdk.packager.services.userjvmoptions;

import jdk.packager.services.UserJvmOptionsService;

import java.security.AllPermission;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * Access the UserJVMOptions via a native library provided by the launcher.
 * 
 * Do not instantiate this class directly, instead use 
 * {@see jdk.packager.services.UserJvmOptionsService#getUserJVMDefaults()}
 * to get an instance.
 *
 * @since 8u40
 */
public class LauncherUserJvmOptions implements UserJvmOptionsService {

    private static final Object semaphore = new Object();
    
    static {
        try {
            checkAllPermissions();
            System.loadLibrary("packager");
        } catch (SecurityException se) {
            // fail to load, we will also throw on all public methods
        }
    }

    private static void checkAllPermissions() {
        final SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(new AllPermission());
        }
    }

    /**
     * Access the default User JVM Option for a specific key
     *
     * @param option the key for the User JVM Option
     *
     * @return the default value of the user JVM Option.  Currently set user
     * values are not considered, and only the default is returned.  If there
     * is no default value then null is returned.
     */
    private static native String _getUserJvmOptionDefaultValue(String option);

    /**
     * This lists the keys for User JVM Options that will have a default
     * provided by the launcher.
     *
     * This list will be a subset of the keys used by the launcher and only
     * lists those values that will have a default value provided if the user
     * does not set a value of their own.
     *
     * @return an array of keys in no particular order.
     */
    private static native String[] _getUserJvmOptionDefaultKeys();
    /**
     * Access the current User JVM Option for a specific key
     *
     * @param option the key for the User JVM Option
     *
     * @return the current value of the user JVM Option.  If the user has not
     * set a value then the default value is returned, except in the case where
     * there is no default value, where null is returned.
     */
    private static native String _getUserJvmOptionValue(String option);

    /**
     * Update the all User JVM Options
     *
     * All option/value pairs will be replaced with the values provided. The
     * parameters options and values are paired at the same index.
     * Example: options[3] = -Xmx and values[3] = 999m
     * This cannot be used to adjust default values.
     *
     * @param options the keys for the User JVM Options
     * @param values the values for the User JVM Options
     */
    private static native void _setUserJvmKeysAndValues(String[] options, String[] values);

    /**
     * This lists the keys for all User JVM Options that will be used by the
     * launcher.
     *
     * This list will be a superset of the defaults as may also include user
     * values that do not have a default.
     *
     * @return an array of keys in no particular order.
     */
    private static native String[] _getUserJvmOptionKeys();

    @Override
    public Map<String, String> getUserJVMOptions() {
        checkAllPermissions();
        synchronized (semaphore) {
            Map<String, String> results = new LinkedHashMap<>();
            for (String s : _getUserJvmOptionKeys()) {
                results.put(s, _getUserJvmOptionValue(s));
            }
            return results;
        }
    }

    @Override
    public void setUserJVMOptions(Map<String, String> options) {
        checkAllPermissions();
        synchronized (semaphore) {
            List<String> keys = new LinkedList<>();
            List<String> values = new LinkedList<>();

            for (Map.Entry<String, String> option : options.entrySet()) {
                keys.add(option.getKey());
                values.add(option.getValue());
            }

            _setUserJvmKeysAndValues(keys.toArray(new String[keys.size()]),
                    values.toArray(new String[values.size()]));
        }
    }

    @Override
    public Map<String, String> getUserJVMOptionDefaults() {
        checkAllPermissions();
        synchronized (semaphore) {
            Map<String, String> results = new LinkedHashMap<>();
            for (String s : _getUserJvmOptionDefaultKeys()) {
                results.put(s, _getUserJvmOptionDefaultValue(s));
            }
            return results;
        }
    }
}
