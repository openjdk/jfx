/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.ScenePreferences;
import com.sun.javafx.tk.Toolkit;
import com.sun.javafx.util.Utils;
import javafx.application.ColorScheme;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanPropertyBase;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectPropertyBase;
import javafx.beans.property.ReadOnlyProperty;
import javafx.scene.paint.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Contains {@link Property}-based preference implementations.
 * <p>
 * All properties in this class can be read from any thread in order to allow {@link ScenePreferences} to safely
 * initialize its values on a background thread (creating and configuring a {@code Scene} on a background thread
 * is allowed by its specification). This is not a specified capability for users, it is an implementation detail.
 * <p>
 * Importantly, even though properties can be read from any thread, changes always happen on the FX thread.
 */
final class PreferenceProperties {

    private final Object mutex = new Object();
    private final Object bean;
    private final Map<String, DeferredProperty<?>> deferredProperties = new HashMap<>();
    private final DeferredProperty<Color> backgroundColor = new DeferredProperty<>("backgroundColor", Color.WHITE);
    private final DeferredProperty<Color> foregroundColor = new DeferredProperty<>("foregroundColor", Color.BLACK);
    private final DeferredProperty<Color> accentColor = new DeferredProperty<>("accentColor", Color.rgb(21, 126, 251));
    private final ColorSchemeProperty colorScheme = new ColorSchemeProperty();
    private final DeferredProperty<Boolean> reducedMotion = new DeferredProperty<>("reducedMotion", false);
    private final DeferredProperty<Boolean> reducedTransparency = new DeferredProperty<>("reducedTransparency", false);
    private final DeferredProperty<Boolean> reducedData = new DeferredProperty<>("reducedData", false);
    private final DeferredProperty<Boolean> persistentScrollBars = new DeferredProperty<>("persistentScrollBars", false);
    private final ReadOnlyBooleanWrapperImpl reducedMotionFlag = new ReadOnlyBooleanWrapperImpl(reducedMotion);
    private final ReadOnlyBooleanWrapperImpl reducedTransparencyFlag = new ReadOnlyBooleanWrapperImpl(reducedTransparency);
    private final ReadOnlyBooleanWrapperImpl reducedDataFlag = new ReadOnlyBooleanWrapperImpl(reducedData);
    private final ReadOnlyBooleanWrapperImpl persistentScrollBarsFlag = new ReadOnlyBooleanWrapperImpl(persistentScrollBars);

    PreferenceProperties(Object bean) {
        this.bean = bean;
    }

    public ReadOnlyBooleanProperty reducedMotionProperty() {
        return reducedMotionFlag;
    }

    public boolean isReducedMotion() {
        return reducedMotion.get();
    }

    public void setReducedMotion(boolean value) {
        reducedMotion.setValueOverride(value);
    }

    public ReadOnlyBooleanProperty reducedTransparencyProperty() {
        return reducedTransparencyFlag;
    }

    public boolean isReducedTransparency() {
        return reducedTransparency.get();
    }

    public void setReducedTransparency(boolean value) {
        reducedTransparency.setValueOverride(value);
    }

    public ReadOnlyBooleanProperty reducedDataProperty() {
        return reducedDataFlag;
    }

    public boolean isReducedData() {
        return reducedData.get();
    }

    public void setReducedData(boolean value) {
        reducedData.setValueOverride(value);
    }

    public ReadOnlyBooleanProperty persistentScrollBarsProperty() {
        return persistentScrollBarsFlag;
    }

    public boolean isPersistentScrollBars() {
        return persistentScrollBars.get();
    }

    public void setPersistentScrollBars(boolean value) {
        persistentScrollBars.setValueOverride(value);
    }

    public ReadOnlyObjectProperty<ColorScheme> colorSchemeProperty() {
        return colorScheme;
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
                       Map<String, PreferenceMapping<?, ?>> platformKeyMappings) {
        synchronized (mutex) {
            for (Map.Entry<String, ChangedValue> entry : changedPreferences.entrySet()) {
                if (platformKeyMappings.get(entry.getKey()) instanceof PreferenceMapping<?, ?> mapping
                        && deferredProperties.get(mapping.keyName()) instanceof DeferredProperty<?> property) {
                    property.setPlatformValue(mapping.map(entry.getValue().newValue()));
                }
            }

            colorScheme.updateEffectiveValue();
        }

        for (DeferredProperty<?> property : deferredProperties.values()) {
            property.fireValueChangedIfNecessary();
        }

        colorScheme.fireValueChangeIfNecessary();
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
            // We need to synchronized on 'mutex' to see 'effectiveValue', because get() may be called
            // on a thread other than the FX application thread.
            synchronized (mutex) {
                return effectiveValue;
            }
        }

        /**
         * Only called from {@link PreferenceProperties#update}, this method doesn't fire a change notification.
         * Change notifications are fired after the new values of all deferred properties have been set.
         */
        @SuppressWarnings("unchecked")
        public void setPlatformValue(Object value) {
            // No need to synchronize here, because the update() method already synchronizes on 'mutex'.
            Class<?> expectedType = defaultValue.getClass();
            this.platformValue = expectedType.isInstance(value) ? (T) value : null;
            updateEffectiveValue();
        }

        public void setValueOverride(T value) {
            // This method may be called by user code, so make sure that we are on the FX application thread.
            Toolkit.getToolkit().checkFxUserThread();

            synchronized (mutex) {
                this.overrideValue = value;
                updateEffectiveValue();
            }

            fireValueChangedIfNecessary();
        }

        // This method must only be called on the FX application thread.
        public void fireValueChangedIfNecessary() {
            if (!Objects.equals(lastEffectiveValue, effectiveValue)) {
                lastEffectiveValue = effectiveValue;
                fireValueChangedEvent();
            }
        }

        // This method must only be called when synchronized on 'mutex'.
        private void updateEffectiveValue() {
            // Choose the first non-null value in this order: overrideValue, platformValue, defaultValue.
            effectiveValue = Objects.requireNonNullElse(
                overrideValue != null ? overrideValue : platformValue,
                defaultValue);
        }
    }

    private final class ColorSchemeProperty extends ReadOnlyObjectPropertyBase<ColorScheme> {
        private ColorScheme overrideValue;
        private ColorScheme effectiveValue = ColorScheme.LIGHT;
        private ColorScheme lastEffectiveValue = ColorScheme.LIGHT;

        @Override
        public Object getBean() {
            return bean;
        }

        @Override
        public String getName() {
            return "colorScheme";
        }

        @Override
        public ColorScheme get() {
            // We need to synchronized on 'mutex' to see 'effectiveValue', because get() may be called
            // on a thread other than the FX application thread.
            synchronized (mutex) {
                return effectiveValue;
            }
        }

        public void setValueOverride(ColorScheme colorScheme) {
            // This method may be called by user code, so make sure that we are on the FX application thread.
            Toolkit.getToolkit().checkFxUserThread();

            synchronized (mutex) {
                overrideValue = colorScheme;
                updateEffectiveValue();
            }

            fireValueChangeIfNecessary();
        }

        // This method must only be called when synchronized on 'mutex'.
        public void updateEffectiveValue() {
            if (overrideValue != null) {
                effectiveValue = overrideValue;
            } else {
                Color background = backgroundColor.get();
                Color foreground = foregroundColor.get();
                boolean isDark = Utils.calculateBrightness(background) < Utils.calculateBrightness(foreground);
                effectiveValue = isDark ? ColorScheme.DARK : ColorScheme.LIGHT;
            }
        }

        // This method must only be called on the FX application thread.
        public void fireValueChangeIfNecessary() {
            if (lastEffectiveValue != effectiveValue) {
                lastEffectiveValue = effectiveValue;
                fireValueChangedEvent();
            }
        }
    }

    private static final class ReadOnlyBooleanWrapperImpl extends ReadOnlyBooleanPropertyBase {
        private final ReadOnlyProperty<Boolean> observable;

        ReadOnlyBooleanWrapperImpl(ReadOnlyProperty<Boolean> observable) {
            this.observable = observable;
            observable.addListener((_, _, _) -> fireValueChangedEvent());
        }

        @Override
        public Object getBean() {
            return observable.getBean();
        }

        @Override
        public String getName() {
            return observable.getName();
        }

        @Override
        public boolean get() {
            return observable.getValue();
        }
    }
}
