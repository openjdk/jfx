/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.application;

import javafx.application.PlatformPreferences;
import javafx.application.PlatformPreferencesListener;
import javafx.scene.paint.Color;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public final class PlatformPreferencesImpl extends AbstractMap<String, Object> implements PlatformPreferences {

    private final Map<String, Object> modifiableMap = new HashMap<>();
    private final Set<Entry<String, Object>> unmodifiableEntrySet = Collections.unmodifiableSet(modifiableMap.entrySet());
    private final List<PlatformPreferencesListener> listeners = new CopyOnWriteArrayList<>();

    public Map<String, Object> getModifiableMap() {
        return modifiableMap;
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return unmodifiableEntrySet;
    }

    @Override
    public String getString(String key) {
        Object value = modifiableMap.get(key);
        if (value instanceof String s) {
            return s;
        }

        return null;
    }

    @Override
    public Boolean getBoolean(String key) {
        Object value = modifiableMap.get(key);
        if (value instanceof Boolean b) {
            return b;
        }

        return null;
    }

    @Override
    public Color getColor(String key) {
        Object value = modifiableMap.get(key);
        if (value instanceof Color c) {
            return c;
        }

        return null;
    }

    @Override
    public void addListener(PlatformPreferencesListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(PlatformPreferencesListener listener) {
        listeners.remove(listener);
    }

    void firePreferencesChanged(Map<String, Object> changed) {
        for (PlatformPreferencesListener listener : listeners) {
            listener.onPreferencesChanged(this, changed);
        }
    }

}
