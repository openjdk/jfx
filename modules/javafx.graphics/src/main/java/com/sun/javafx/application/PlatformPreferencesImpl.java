/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.collections.MapChangeListener;
import javafx.scene.paint.Color;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public final class PlatformPreferencesImpl extends AbstractMap<String, Object> implements Platform.Preferences {

    private final Map<String, Object> modifiableMap = new HashMap<>();
    private final Set<Entry<String, Object>> unmodifiableEntrySet = Collections.unmodifiableSet(modifiableMap.entrySet());
    private final List<InvalidationListener> invalidationListeners = new CopyOnWriteArrayList<>();
    private final List<MapChangeListener<? super String, ? super Object>> changeListeners = new CopyOnWriteArrayList<>();

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
    public void addListener(InvalidationListener listener) {
        invalidationListeners.add(listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        invalidationListeners.remove(listener);
    }

    @Override
    public void addListener(MapChangeListener<? super String, ? super Object> listener) {
        changeListeners.add(listener);
    }

    @Override
    public void removeListener(MapChangeListener<? super String, ? super Object> listener) {
        changeListeners.remove(listener);
    }

    void firePreferencesChanged(Map<String, Object> changed) {
        for (InvalidationListener listener : invalidationListeners) {
            try {
                listener.invalidated(this);
            } catch (Exception e) {
                Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
            }
        }

        for (Map.Entry<String, Object> entry : changed.entrySet()) {
            boolean keyExists = modifiableMap.containsKey(entry.getKey());
            Object oldValue = keyExists ? modifiableMap.get(entry.getKey()) : null;
            MapChangeListener.Change<String, Object> change = new MapChangeListener.Change<>(this) {
                @Override public boolean wasAdded() { return true; }
                @Override public boolean wasRemoved() { return keyExists; }
                @Override public String getKey() { return entry.getKey(); }
                @Override public Object getValueAdded() { return entry.getValue(); }
                @Override public Object getValueRemoved() { return oldValue; }
            };

            for (MapChangeListener<? super String, ? super Object> listener : changeListeners) {
                try {
                    listener.onChanged(change);
                } catch (Exception e) {
                    Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
                }
            }
        }
    }

}
