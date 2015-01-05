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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Access to old preferences based UserJvmOptions
 * 
 * Do not instantiate this class directly, instead use 
 * {@see jdk.packager.services.UserJvmOptionsService#getUserJVMDefaults()}
 * to get an instance.
 *
 * @since 8u40
 */
final public class PreferencesUserJvmOptions implements UserJvmOptionsService {

    Preferences node = Preferences.userRoot().node(System.getProperty("app.preferences.id").replace(".", "/")).node("JVMUserOptions");

    @Override
    public Map<String, String> getUserJVMOptions() {
        Map<String, String> result = new LinkedHashMap<>();
        try {
            for (String s : node.childrenNames()) {
                String o = node.get(s, null);
                if (o != null) {
                    result.put(s, o);
                }
            }
        } catch (BackingStoreException ignore) {
        }

        return result;
    }

    @Override
    public void setUserJVMOptions(Map<String, String> options) {
        try {
            node.clear();
            for (Map.Entry<String, String> entry : options.entrySet()) {
                node.put(entry.getKey(), entry.getValue());
            }
            node.flush();
        } catch (BackingStoreException ignore) {
        }

    }

    @Override
    public Map<String, String> getUserJVMOptionDefaults() {
        throw new UnsupportedOperationException("Preferences backed UserJvmOptions do not enumerate their defaults");
    }
}
