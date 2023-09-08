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

import com.sun.javafx.binding.MapExpressionHelper;
import javafx.application.Appearance;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.MapChangeListener;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Contains the implementation of a read-only map of platform preferences.
 * <p>
 * When the operating system signals that a preference has changed, the mappings are updated
 * by calling the {@link #update(Map)} method.
 */
public class PlatformPreferences extends AbstractMap<String, Object> implements Platform.Preferences {

    /**
     * Contains mappings from platform-specific keys to well-known keys, which are used
     * in the implementation of the property-based API in {@link PreferenceProperties}.
     */
    final Map<String, String> wellKnownKeys;
    final Map<String, Object> effectivePreferences = new HashMap<>();
    final Map<String, Object> unmodifiableEffectivePreferences = Collections.unmodifiableMap(effectivePreferences);
    final PreferenceProperties properties = new PreferenceProperties(this);

    private final List<InvalidationListener> invalidationListeners = new CopyOnWriteArrayList<>();
    private final List<MapChangeListener<? super String, Object>> mapChangeListeners = new CopyOnWriteArrayList<>();

    public PlatformPreferences(Map<String, String> wellKnownKeys) {
        this.wellKnownKeys = wellKnownKeys;
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return unmodifiableEffectivePreferences.entrySet();
    }

    @Override
    public Object remove(Object key) {
        throw new UnsupportedOperationException();
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
        mapChangeListeners.add(listener);
    }

    @Override
    public void removeListener(MapChangeListener<? super String, ? super Object> listener) {
        mapChangeListeners.remove(listener);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getValue(String key, Class<T> type) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(key, "type cannot be null");
        Object value = effectivePreferences.get(key);

        if (value == null) {
            return Optional.empty();
        }

        if (type.isInstance(value)) {
            return Optional.of((T)value);
        }

        throw new IllegalArgumentException(
            "Incompatible types: requested = " + type.getName() +
            ", actual = " + value.getClass().getName());
    }

    @Override
    public Optional<Integer> getInteger(String key) {
        return getValue(key, Integer.class);
    }

    @Override
    public Optional<Double> getDouble(String key) {
        return getValue(key, Double.class);
    }

    @Override
    public Optional<Boolean> getBoolean(String key) {
        return getValue(key, Boolean.class);
    }

    @Override
    public Optional<String> getString(String key) {
        return getValue(key, String.class);
    }

    @Override
    public Optional<Color> getColor(String key) {
        return getValue(key, Color.class);
    }

    @Override
    public Optional<Paint> getPaint(String key) {
        return getValue(key, Paint.class);
    }

    @Override
    public ReadOnlyObjectProperty<Appearance> appearanceProperty() {
        return properties.appearanceProperty();
    }

    @Override
    public Appearance getAppearance() {
        return properties.getAppearance();
    }

    @Override
    public ReadOnlyObjectProperty<Color> backgroundColorProperty() {
        return properties.backgroundColorProperty();
    }

    @Override
    public Color getBackgroundColor() {
        return properties.getBackgroundColor();
    }

    @Override
    public ReadOnlyObjectProperty<Color> foregroundColorProperty() {
        return properties.foregroundColorProperty();
    }

    @Override
    public Color getForegroundColor() {
        return properties.getForegroundColor();
    }

    @Override
    public ReadOnlyObjectProperty<Color> accentColorProperty() {
        return properties.accentColorProperty();
    }

    @Override
    public Color getAccentColor() {
        return properties.getAccentColor();
    }

    /**
     * Updates this map of preferences with a new set of platform preferences.
     * The specified preferences may include all available preferences, or only the changed preferences.
     *
     * @param preferences the new preference mappings
     */
    public void update(Map<String, Object> preferences) {
        Map<String, Object> currentPreferences = Map.copyOf(effectivePreferences);
        effectivePreferences.putAll(preferences);

        // Only fire change notifications if any preference has effectively changed.
        Map<String, ChangedValue> effectivelyChangedPreferences =
            ChangedValue.getEffectiveChanges(currentPreferences, effectivePreferences);

        // The new mappings may contain null values, which indicates that a mapping was removed.
        effectivePreferences.entrySet().removeIf(entry -> entry.getValue() == null);

        if (!effectivelyChangedPreferences.isEmpty()) {
            properties.update(effectivelyChangedPreferences, wellKnownKeys);
            fireValueChangedEvent(effectivelyChangedPreferences);
        }
    }

    void fireValueChangedEvent(Map<String, ChangedValue> changedEntries) {
        for (var listener : invalidationListeners) {
            listener.invalidated(this);
        }

        var change = new MapExpressionHelper.SimpleChange<>(this);

        for (Map.Entry<String, ChangedValue> entry : changedEntries.entrySet()) {
            Object oldValue = entry.getValue().oldValue();
            Object newValue = entry.getValue().newValue();

            if (oldValue == null && newValue != null) {
                change.setAdded(entry.getKey(), newValue);
            } else if (oldValue != null && newValue == null) {
                change.setRemoved(entry.getKey(), oldValue);
            } else {
                change.setPut(entry.getKey(), oldValue, newValue);
            }

            for (var listener : mapChangeListeners) {
                listener.onChanged(change);
            }
        }
    }

}
