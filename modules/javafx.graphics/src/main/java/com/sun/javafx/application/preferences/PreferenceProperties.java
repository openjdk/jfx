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

import com.sun.javafx.util.Utils;
import javafx.application.ColorScheme;
import javafx.beans.InvalidationListener;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectPropertyBase;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.paint.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Contains {@link Property}-based preference implementations.
 */
final class PreferenceProperties {

    private final Map<String, DeferredProperty<?>> deferredProperties = new HashMap<>();
    private final DeferredProperty<Color> backgroundColor = new DeferredProperty<>("backgroundColor", Color.WHITE);
    private final DeferredProperty<Color> foregroundColor = new DeferredProperty<>("foregroundColor", Color.BLACK);
    private final DeferredProperty<Color> accentColor = new DeferredProperty<>("accentColor", Color.rgb(21, 126, 251));
    private final ColorSchemeProperty colorScheme = new ColorSchemeProperty();
    private final DeferredProperty<Boolean> reducedMotion = new DeferredProperty<>("reducedMotion", false);
    private final DeferredProperty<Boolean> reducedTransparency = new DeferredProperty<>("reducedTransparency", false);
    private final ReadOnlyBooleanWrapper reducedMotionFlag;
    private final ReadOnlyBooleanWrapper reducedTransparencyFlag;
    private final Object bean;

    PreferenceProperties(Object bean) {
        this.bean = bean;

        reducedMotionFlag = new ReadOnlyBooleanWrapper(bean, reducedMotion.getName());
        reducedMotionFlag.bind(reducedMotion);

        reducedTransparencyFlag = new ReadOnlyBooleanWrapper(bean, reducedTransparency.getName());
        reducedTransparencyFlag.bind(reducedTransparency);
    }

    public ReadOnlyBooleanProperty reducedMotionProperty() {
        return reducedMotionFlag.getReadOnlyProperty();
    }

    public boolean isReducedMotion() {
        return reducedMotion.get();
    }

    public void setReducedMotion(boolean value) {
        reducedMotion.setValueOverride(value);
    }

    public ReadOnlyBooleanProperty reducedTransparencyProperty() {
        return reducedTransparencyFlag.getReadOnlyProperty();
    }

    public boolean isReducedTransparency() {
        return reducedTransparency.get();
    }

    public void setReducedTransparency(boolean value) {
        reducedTransparency.setValueOverride(value);
    }

    public ReadOnlyObjectProperty<ColorScheme> colorSchemeProperty() {
        return colorScheme.getReadOnlyProperty();
    }

    public ColorScheme getColorScheme() {
        return colorScheme.get();
    }

    public void setColorScheme(ColorScheme value) {
        colorScheme.setValueOverride(value);
    }

    public ReadOnlyObjectProperty<Color> backgroundColorProperty() {
        return backgroundColor;
    }

    public Color getBackgroundColor() {
        return backgroundColor.get();
    }

    public void setBackgroundColor(Color color) {
        backgroundColor.setValueOverride(color);
    }

    public ReadOnlyObjectProperty<Color> foregroundColorProperty() {
        return foregroundColor;
    }

    public Color getForegroundColor() {
        return foregroundColor.get();
    }

    public void setForegroundColor(Color color) {
        foregroundColor.setValueOverride(color);
    }

    public ReadOnlyObjectProperty<Color> accentColorProperty() {
        return accentColor;
    }

    public Color getAccentColor() {
        return accentColor.get();
    }

    public void setAccentColor(Color color) {
        accentColor.setValueOverride(color);
    }

    public void update(Map<String, ChangedValue> changedPreferences,
                       Map<String, PreferenceMapping<?>> platformKeyMappings) {
        for (Map.Entry<String, ChangedValue> entry : changedPreferences.entrySet()) {
            if (platformKeyMappings.get(entry.getKey()) instanceof PreferenceMapping<?> mapping
                    && deferredProperties.get(mapping.keyName()) instanceof DeferredProperty<?> property) {
                property.setPlatformValue(mapping.map(entry.getValue().newValue()));
            }
        }

        for (DeferredProperty<?> property : deferredProperties.values()) {
            property.fireValueChangedIfNecessary();
        }
    }

    /**
     * DeferredProperty implements a deferred notification mechanism, where change notifications
     * are only fired after changes of all properties have been applied.
     * This ensures that observers will never see a transient state where two properties
     * are inconsistent (for example, both foreground and background could be the same color
     * when going from light to dark mode).
     */
    private final class DeferredProperty<T> extends ReadOnlyObjectPropertyBase<T> {
        private final String name;
        private final T defaultValue;
        private T overrideValue;
        private T platformValue;
        private T effectiveValue;
        private T lastEffectiveValue;

        DeferredProperty(String name, T initialValue) {
            Objects.requireNonNull(initialValue);
            PreferenceProperties.this.deferredProperties.put(name, this);
            this.name = name;
            this.defaultValue = initialValue;
            this.platformValue = initialValue;
            this.effectiveValue = initialValue;
            this.lastEffectiveValue = initialValue;
        }

        @Override
        public Object getBean() {
            return bean;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public T get() {
            return effectiveValue;
        }

        /**
         * Only called from {@link PreferenceProperties#update}, this method doesn't fire a change notification.
         * Change notifications are fired after the new values of all deferred properties have been set.
         */
        @SuppressWarnings("unchecked")
        public void setPlatformValue(Object value) {
            Class<?> expectedType = defaultValue.getClass();
            this.platformValue = expectedType.isInstance(value) ? (T)value : null;
            updateEffectiveValue();
        }

        public void setValueOverride(T value) {
            this.overrideValue = value;
            updateEffectiveValue();
            fireValueChangedEvent();
        }

        public void fireValueChangedIfNecessary() {
            if (!Objects.equals(lastEffectiveValue, effectiveValue)) {
                lastEffectiveValue = effectiveValue;
                fireValueChangedEvent();
            }
        }

        private void updateEffectiveValue() {
            // Choose the first non-null value in this order: overrideValue, platformValue, defaultValue.
            effectiveValue = Objects.requireNonNullElse(
                overrideValue != null ? overrideValue : platformValue,
                defaultValue);
        }
    }

    private class ColorSchemeProperty extends ReadOnlyObjectWrapper<ColorScheme> {
        private ColorScheme colorSchemeOverride;

        ColorSchemeProperty() {
            super(bean, "colorScheme");
            InvalidationListener listener = observable -> update();
            backgroundColor.addListener(listener);
            foregroundColor.addListener(listener);
            update();
        }

        public void setValueOverride(ColorScheme colorScheme) {
            colorSchemeOverride = colorScheme;
            update();
        }

        private void update() {
            if (colorSchemeOverride != null) {
                set(colorSchemeOverride);
            } else {
                Color background = backgroundColor.get();
                Color foreground = foregroundColor.get();
                boolean isDark = Utils.calculateBrightness(background) < Utils.calculateBrightness(foreground);
                set(isDark ? ColorScheme.DARK : ColorScheme.LIGHT);
            }
        }
    }
}
