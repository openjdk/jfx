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

package com.sun.javafx.application.preferences;

import javafx.application.Appearance;
import javafx.application.Application;
import javafx.scene.paint.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Contains the implementation of a map of application preferences.
 * <p>
 * Like {@link PlatformPreferences}, this map is updated when the operating system signals that a
 * preference has changed. This map also supports overriding existing mappings with the
 * {@link #put(String, Object)} operation. Overridden mappings can be reset to their platform
 * defaults by invoking {@link #reset(String)} or {@link #reset()}.
 */
public final class ApplicationPreferences extends PlatformPreferences implements Application.Preferences {

    private final Map<String, Object> platformPreferences = new HashMap<>();
    private final Map<String, Object> userPreferences = new HashMap<>();

    public ApplicationPreferences(Map<String, String> wellKnownKeys) {
        super(wellKnownKeys);
    }

    @Override
    public Object put(String key, Object value) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(value, "value cannot be null");
        Object effectiveValue = effectivePreferences.get(key);

        if (effectiveValue != null && !effectiveValue.getClass().isInstance(value)) {
            throw new IllegalArgumentException(
                "Cannot override a value of type " + effectiveValue.getClass().getName() +
                " with a value of type " + value.getClass().getName());
        }

        userPreferences.put(key, value);
        effectivePreferences.put(key, value);

        if (!Objects.equals(effectiveValue, value)) {
            var changedPreferences = Map.of(key, new ChangedValue(effectiveValue, value));
            properties.update(changedPreferences, wellKnownKeys);
            fireValueChangedEvent(changedPreferences);
        }

        return effectiveValue;
    }

    @Override
    public void reset(String key) {
        Objects.requireNonNull(key, "key cannot be null");
        Object oldValue = effectivePreferences.get(key);
        Object newValue;

        userPreferences.remove(key);

        if (platformPreferences.containsKey(key)) {
            newValue = platformPreferences.get(key);
            effectivePreferences.put(key, newValue);
        } else {
            newValue = null;
            effectivePreferences.remove(key);
        }

        boolean changed = oldValue instanceof Object[] array ?
            !Arrays.equals(array, (Object[])newValue) : !Objects.equals(oldValue, newValue);

        if (changed) {
            var changedPreferences = Map.of(key, new ChangedValue(oldValue, newValue));
            properties.update(changedPreferences, wellKnownKeys);
            fireValueChangedEvent(changedPreferences);
        }
    }

    @Override
    public void reset() {
        forEach((key, value) -> reset(key));
    }

    /**
     * Updates this map of preferences with a new set of application preferences.
     * The specified preferences may include all available preferences, or only the changed preferences.
     */
    @Override
    public void update(Map<String, Object> preferences) {
        Map<String, Object> currentEffectivePreferences = Map.copyOf(effectivePreferences);

        // The given preference map may contain null values, which indicates that a mapping was removed.
        platformPreferences.putAll(preferences);
        platformPreferences.entrySet().removeIf(entry -> entry.getValue() == null);
        effectivePreferences.clear();
        effectivePreferences.putAll(platformPreferences);
        effectivePreferences.putAll(userPreferences);

        // Only fire change notifications if any preference has effectively changed.
        Map<String, ChangedValue> effectivelyChangedPreferences =
            ChangedValue.getEffectiveChanges(currentEffectivePreferences, effectivePreferences);

        if (!effectivelyChangedPreferences.isEmpty()) {
            properties.update(effectivelyChangedPreferences, wellKnownKeys);
            fireValueChangedEvent(effectivelyChangedPreferences);
        }
    }

    @Override
    public void setAppearance(Appearance appearance) {
        properties.setAppearance(appearance);
    }

    @Override
    public void setBackgroundColor(Color color) {
        properties.setBackgroundColor(color);
    }

    @Override
    public void setForegroundColor(Color color) {
        properties.setForegroundColor(color);
    }

    @Override
    public void setAccentColor(Color color) {
        properties.setAccentColor(color);
    }

}
