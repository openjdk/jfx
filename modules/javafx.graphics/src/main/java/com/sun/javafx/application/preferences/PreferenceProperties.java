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

import com.sun.javafx.util.Logging;
import com.sun.javafx.util.Utils;
import javafx.application.Appearance;
import javafx.beans.InvalidationListener;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectPropertyBase;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.paint.Color;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Contains {@link Property}-based preference implementations.
 */
final class PreferenceProperties {

    private final ColorProperty backgroundColor = new ColorProperty("backgroundColor", Color.WHITE);
    private final ColorProperty foregroundColor = new ColorProperty("foregroundColor", Color.BLACK);
    private final ColorProperty accentColor = new ColorProperty("accentColor", Color.rgb(21, 126, 251));
    private final List<ColorProperty> allColors = List.of(backgroundColor, foregroundColor, accentColor);
    private final AppearanceProperty appearance = new AppearanceProperty();
    private final Object bean;

    public PreferenceProperties(Object bean) {
        this.bean = bean;
    }

    public ReadOnlyObjectProperty<Appearance> appearanceProperty() {
        return appearance.getReadOnlyProperty();
    }

    public Appearance getAppearance() {
        return appearance.get();
    }

    public void setAppearance(Appearance value) {
        appearance.setValueOverride(value);
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

    public void update(Map<String, ChangedValue> changedPreferences, Map<String, String> wellKnownKeys) {
        for (Map.Entry<String, ChangedValue> entry : changedPreferences.entrySet()) {
            String key = wellKnownKeys.get(entry.getKey());
            if (key != null) {
                for (ColorProperty colorProperty : allColors) {
                    if (colorProperty.getName().equals(key)) {
                        updateColorPreference(colorProperty, entry.getValue().newValue());
                    }
                }
            }
        }
    }

    private void updateColorPreference(ColorProperty property, Object value) {
        if (value instanceof Color color) {
            property.setValue(color);
        } else {
            if (value != null) {
                Logging.getJavaFXLogger().warning(
                    "Unexpected value of " + property.getName() + " platform preference, " +
                    "using default value instead (expected = " + Color.class.getName() +
                    ", actual = " + value.getClass().getName() + ")");
            }

            property.setValue(null);
        }
    }

    private final class ColorProperty extends ReadOnlyObjectPropertyBase<Color> {
        private final String name;
        private final Color defaultValue;
        private Color overrideValue;
        private Color effectiveValue;
        private Color platformValue;

        ColorProperty(String name, Color initialValue) {
            this.name = name;
            this.defaultValue = initialValue;
            this.effectiveValue = initialValue;
            this.platformValue = initialValue;
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
        public Color get() {
            return effectiveValue;
        }

        public void setValue(Color value) {
            this.platformValue = value;
            update();
        }

        public void setValueOverride(Color value) {
            this.overrideValue = value;
            update();
        }

        private void update() {
            Color newValue = Objects.requireNonNullElse(
                overrideValue != null ? overrideValue : platformValue,
                defaultValue);

            if (!Objects.equals(effectiveValue, newValue)) {
                effectiveValue = newValue;
                fireValueChangedEvent();
            }
        }
    }

    private class AppearanceProperty extends ReadOnlyObjectWrapper<Appearance> {
        private Appearance appearanceOverride;

        AppearanceProperty() {
            super(bean, "appearance");
            InvalidationListener listener = observable -> update();
            backgroundColor.addListener(listener);
            foregroundColor.addListener(listener);
            update();
        }

        public void setValueOverride(Appearance appearance) {
            appearanceOverride = appearance;
            update();
        }

        private void update() {
            if (appearanceOverride != null) {
                set(appearanceOverride);
            } else {
                Color background = backgroundColor.get();
                Color foreground = foregroundColor.get();
                boolean isDark = Utils.calculateBrightness(background) < Utils.calculateBrightness(foreground);
                set(isDark ? Appearance.DARK : Appearance.LIGHT);
            }
        }
    }

}
