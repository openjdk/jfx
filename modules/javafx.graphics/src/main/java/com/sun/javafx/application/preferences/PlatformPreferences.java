/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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
import javafx.application.ColorScheme;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.MapChangeListener;
import javafx.scene.paint.Color;
import java.io.Serializable;
import java.lang.reflect.Modifier;
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
public final class PlatformPreferences extends AbstractMap<String, Object> implements Platform.Preferences {

    /**
     * Contains mappings from platform-specific keys to their types. This information is
     * used to catch misuse of typed getters even if the preferences map doesn't contain
     * the preference mapping at runtime.
     */
    private final Map<String, Class<?>> platformKeys;

    /**
     * Contains mappings from platform-specific keys to well-known keys, which are used
     * in the implementation of the property-based API in {@link PreferenceProperties}.
     */
    private final Map<String, PreferenceMapping<?>> platformKeyMappings;

    /**
     * Contains the current set of effective preferences, i.e. the set of preferences that
     * we know to be the current state of the world, and are exposed to users of this map.
     */
    private final Map<String, Object> effectivePreferences = new HashMap<>();
    private final Map<String, Object> unmodifiableEffectivePreferences = Collections.unmodifiableMap(effectivePreferences);

    /** Contains the implementation of the property-based API. */
    private final PreferenceProperties properties = new PreferenceProperties(this);

    private final List<InvalidationListener> invalidationListeners = new CopyOnWriteArrayList<>();
    private final List<MapChangeListener<? super String, Object>> mapChangeListeners = new CopyOnWriteArrayList<>();

    /**
     * Initializes a new {@code PlatformPreferences} instance with the given platform-specific keys and key mappings.
     *
     * @param platformKeys the platform-specific keys and the types of their values
     * @param platformKeyMappings the platform-specific key mappings
     * @throws NullPointerException if {@code platformKeys} or {@code platformKeyMappings} is {@code null} or
     *                              contains {@code null} keys or values
     */
    public PlatformPreferences(Map<String, Class<?>> platformKeys,
                               Map<String, PreferenceMapping<?>> platformKeyMappings) {
        this.platformKeys = Map.copyOf(platformKeys);
        this.platformKeyMappings = Map.copyOf(platformKeyMappings);
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
    public <T> Optional<T> getValue(String key, Class<T> type) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        Class<?> platformType = platformKeys.get(key);
        Object value = effectivePreferences.get(key);

        if (platformType == null) {
            // Well-behaved toolkits shouldn't report values for keys that are not listed in the
            // platform key-type map. However, if they do, we need to respect the invariant that
            // Map.getValue(key, type) should only return an empty value if Map.get(key) would
            // return null. In all other cases we need to return the value if the cast succeeds.
            if (value != null) {
                if (type.isInstance(value)) {
                    @SuppressWarnings("unchecked")
                    T v = (T)value;
                    return Optional.of(v);
                }

                throw new IllegalArgumentException(
                    "Incompatible types: requested = " + type.getName() +
                    ", actual = " + value.getClass().getName());
            }

            return Optional.empty();
        }

        // Check whether the declared platform type is convertible to the requested type.
        // This check validates that a casting conversion exists at all, even if we don't have a
        // value that we would need in order to use Class.isInstance to check if the conversion
        // succeeds at runtime.
        if (!isConvertible(platformType, type)) {
            throw new IllegalArgumentException(
                "Incompatible types: requested = " + type.getName() +
                ", actual = " + platformType.getName());
        }

        if (value == null) {
            return Optional.empty();
        }

        // The runtime type of the value might be a subtype of the platform type, which necessitates
        // checking whether the actual type is convertible to the requested type.
        if (!type.isInstance(value)) {
            throw new IllegalArgumentException(
                "Incompatible types: requested = " + type.getName() +
                ", actual = " + value.getClass().getName());
        }

        @SuppressWarnings("unchecked")
        T v = (T)value;
        return Optional.of(v);
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
    public ReadOnlyBooleanProperty reducedMotionProperty() {
        return properties.reducedMotionProperty();
    }

    @Override
    public boolean isReducedMotion() {
        return properties.isReducedMotion();
    }

    @Override
    public ReadOnlyBooleanProperty reducedTransparencyProperty() {
        return properties.reducedTransparencyProperty();
    }

    @Override
    public boolean isReducedTransparency() {
        return properties.isReducedTransparency();
    }

    @Override
    public ReadOnlyObjectProperty<ColorScheme> colorSchemeProperty() {
        return properties.colorSchemeProperty();
    }

    @Override
    public ColorScheme getColorScheme() {
        return properties.getColorScheme();
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
     * The absence of a mapping in the specified preferences does not indicate that it should be removed;
     * instead, a key must be explicitly mapped to {@code null} to remove the mapping. Consequently, this
     * map will never contain {@code null} values.
     *
     * @param preferences the new preference mappings
     * @throws NullPointerException if {@code preferences} is {@code null}
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
            properties.update(effectivelyChangedPreferences, platformKeyMappings);
            fireValueChangedEvent(effectivelyChangedPreferences);
        }
    }

    private void fireValueChangedEvent(Map<String, ChangedValue> changedEntries) {
        invalidationListeners.forEach(listener -> listener.invalidated(this));
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

    /**
     * Determines whether a compile-time casting conversion exists from {@code source} to {@code target}.
     * This is an implementation of JLS 5.5.1 (Reference Type Casting).
     *
     * @param source the source type
     * @param target the target type
     * @return {@code true} if a casting conversion exists, {@code false} otherwise
     */
    private boolean isConvertible(Class<?> source, Class<?> target) {
        if (source.isArray()) {
            return isArrayConvertible(source, target);
        }

        if (source.isInterface()) {
            return isInterfaceConvertible(source, target);
        }

        return isClassConvertible(source, target);
    }

    // Assuming S is a class type:
    private boolean isClassConvertible(Class<?> source, Class<?> target) {
        // If T is an interface type:
        //   1. If S is final, then S must implement T.
        //   2. If S is not final, the cast is always legal (because even if S does not
        //      implement T, a subclass of S might).
        if (target.isInterface()) {
            return !Modifier.isFinal(source.getModifiers()) || target.isAssignableFrom(source);
        }

        // If T is an array type, then S must be the class Object.
        if (target.isArray()) {
            return source == Object.class;
        }

        // If T is a class type, then either S<:T, or T<:S.
        return target.isAssignableFrom(source) || source.isAssignableFrom(target);
    }

    // Assuming S is an interface type:
    private boolean isInterfaceConvertible(Class<?> source, Class<?> target) {
        // If T is an array type, then S must be the type Serializable or Cloneable.
        if (target.isArray()) {
            return source == Serializable.class || source == Cloneable.class;
        }

        // If T is not final, the cast is always legal (because even if S does not
        // implement T, a subclass of S might).
        if (!Modifier.isFinal(target.getModifiers())) {
            return true;
        }

        // If T is a class type that is final, then T must implement S.
        return source.isAssignableFrom(target);
    }

    // Assuming S is an array type SC[], that is, an array of components of type SC:
    private boolean isArrayConvertible(Class<?> source, Class<?> target) {
        // If T is an interface type, then it must be the type Serializable or Cloneable,
        // which are the only interfaces implemented by arrays.
        if (target.isInterface()) {
            return target == Serializable.class || target == Cloneable.class;
        }

        // If T is an array type TC[], that is, an array of components of type TC,
        // then one of the following must be true:
        //   1. TC and SC are the same primitive type
        //   2. TC and SC are reference types and type SC can undergo casting conversion to TC
        if (target.isArray()) {
            Class<?> sourceComponentType = source.getComponentType();
            Class<?> targetComponentType = target.getComponentType();

            if (sourceComponentType.isPrimitive() && targetComponentType.isPrimitive()) {
                return sourceComponentType == targetComponentType;
            }

            if (!sourceComponentType.isPrimitive() && !targetComponentType.isPrimitive()) {
                return isConvertible(sourceComponentType, targetComponentType);
            }

            return false;
        }

        // If T is a class type, then T must be Object because Object is the only
        // class type to which arrays can be assigned.
        return target == Object.class;
    }
}
