/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene;

import com.sun.javafx.application.PlatformImpl;
import com.sun.javafx.beans.property.NullCoalescingPropertyBase;
import com.sun.javafx.css.media.MediaQueryContext;
import java.util.List;
import javafx.application.ColorScheme;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Window;

public final class ScenePreferences implements Scene.Preferences, MediaQueryContext {

    private final Scene scene;

    public ScenePreferences(Scene scene) {
        this.scene = scene;

        scene.windowProperty()
            .flatMap(Window::showingProperty)
            .orElse(false)
            .subscribe(this::onShowingChanged);
    }

    private final MediaProperty<ColorScheme> colorScheme = new MediaProperty<>(
            "colorScheme", PlatformImpl.getPlatformPreferences().colorSchemeProperty());

    @Override
    public ObjectProperty<ColorScheme> colorSchemeProperty() {
        return colorScheme;
    }

    @Override
    public ColorScheme getColorScheme() {
        return colorScheme.get();
    }

    @Override
    public void setColorScheme(ColorScheme colorScheme) {
        this.colorScheme.set(colorScheme);
    }

    private final MediaProperty<Boolean> persistentScrollBars = new MediaProperty<>(
            "persistentScrollBars", PlatformImpl.getPlatformPreferences().persistentScrollBarsProperty());

    @Override
    public ObjectProperty<Boolean> persistentScrollBarsProperty() {
        return persistentScrollBars;
    }

    @Override
    public boolean isPersistentScrollBars() {
        return persistentScrollBars.get();
    }

    @Override
    public void setPersistentScrollBars(Boolean value) {
        this.persistentScrollBars.set(value);
    }

    private final MediaProperty<Boolean> reducedMotion = new MediaProperty<>(
            "reducedMotion", PlatformImpl.getPlatformPreferences().reducedMotionProperty());

    @Override
    public ObjectProperty<Boolean> reducedMotionProperty() {
        return reducedMotion;
    }

    @Override
    public boolean isReducedMotion() {
        return reducedMotion.get();
    }

    @Override
    public void setReducedMotion(Boolean value) {
        this.reducedMotion.set(value);
    }

    private final MediaProperty<Boolean> reducedTransparency = new MediaProperty<>(
            "reducedTransparency", PlatformImpl.getPlatformPreferences().reducedTransparencyProperty());

    @Override
    public ObjectProperty<Boolean> reducedTransparencyProperty() {
        return reducedTransparency;
    }

    @Override
    public boolean isReducedTransparency() {
        return reducedTransparency.get();
    }

    @Override
    public void setReducedTransparency(Boolean value) {
        this.reducedTransparency.set(value);
    }

    private final MediaProperty<Boolean> reducedData = new MediaProperty<>(
            "reducedData", PlatformImpl.getPlatformPreferences().reducedDataProperty());

    @Override
    public ObjectProperty<Boolean> reducedDataProperty() {
        return reducedData;
    }

    @Override
    public boolean isReducedData() {
        return reducedData.get();
    }

    @Override
    public void setReducedData(Boolean value) {
        this.reducedData.set(value);
    }

    private void onShowingChanged(Boolean showing) {
        for (var property : List.of(colorScheme, persistentScrollBars, reducedData,
                                    reducedMotion, reducedTransparency)) {
            if (showing) {
                property.connect();
            } else {
                property.disconnect();
            }
        }
    }

    /**
     * Property implementation for media features that causes CSS to be re-applied when the property
     * value is changed. This is required to re-evaluate media queries in stylesheets.
     */
    private class MediaProperty<T> extends NullCoalescingPropertyBase<T> {
        private final String name;

        MediaProperty(String name, ObservableValue<T> defaultValue) {
            super(defaultValue);
            this.name = name;
        }

        @Override
        public Object getBean() {
            return scene;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        protected void onInvalidated() {
            Node root = scene.getRoot();
            if (root != null) {
                NodeHelper.reapplyCSS(root);
            }
        }
    }
}
