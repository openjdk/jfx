/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.application.theme;

import com.sun.javafx.PlatformUtil;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.application.Theme;
import javafx.beans.binding.ListBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.Objects;

public class Modena implements Theme {

    private static final String WINDOWS_HIGH_CONTRAST_THEME_KEY = "Windows.SPI_HighContrastColorScheme";

    private final ObservableList<String> baseStylesheets = FXCollections.observableArrayList();
    private final ObservableList<String> accessibilityStylesheets = FXCollections.observableArrayList();
    private final ObservableList<String> allStylesheets;
    private Map<String, String> platformThemeProperties;

    public Modena(Map<String, String> platformThemeProperties) {
        this.platformThemeProperties = platformThemeProperties;

        baseStylesheets.add("com/sun/javafx/scene/control/skin/modena/modena.css");

        if (Platform.isSupported(ConditionalFeature.INPUT_TOUCH)) {
            baseStylesheets.add("com/sun/javafx/scene/control/skin/modena/touch.css");
        }

        // when running on embedded add a extra stylesheet to tune performance of modena theme
        if (PlatformUtil.isEmbedded()) {
            baseStylesheets.add("com/sun/javafx/scene/control/skin/modena/modena-embedded-performance.css");
        }

        if (PlatformUtil.isAndroid()) {
            baseStylesheets.add("com/sun/javafx/scene/control/skin/modena/android.css");
        }

        if (PlatformUtil.isIOS()) {
            baseStylesheets.add("com/sun/javafx/scene/control/skin/modena/ios.css");
        }

        if (Platform.isSupported(ConditionalFeature.TWO_LEVEL_FOCUS)) {
            baseStylesheets.add("com/sun/javafx/scene/control/skin/modena/two-level-focus.css");
        }

        if (Platform.isSupported(ConditionalFeature.VIRTUAL_KEYBOARD)) {
            baseStylesheets.add("com/sun/javafx/scene/control/skin/caspian/fxvk.css");
        }

        if (!Platform.isSupported(ConditionalFeature.TRANSPARENT_WINDOW)) {
            baseStylesheets.add("com/sun/javafx/scene/control/skin/modena/modena-no-transparency.css");
        }

        updateAccessibilityStylesheets();

        allStylesheets = new ListBinding<>() {
            {
                bind(baseStylesheets, accessibilityStylesheets);
            }

            @Override
            @SuppressWarnings("unchecked")
            protected ObservableList<String> computeValue() {
                return FXCollections.concat(baseStylesheets, accessibilityStylesheets);
            }
        };
    }

    @Override
    public ObservableList<String> getStylesheets() {
        return allStylesheets;
    }

    @Override
    public void platformThemeChanged(Map<String, String> properties) {
        boolean accessibilityThemeChanged = !Objects.equals(
            platformThemeProperties.get(WINDOWS_HIGH_CONTRAST_THEME_KEY),
            properties.get(WINDOWS_HIGH_CONTRAST_THEME_KEY));

        this.platformThemeProperties = properties;

        if (accessibilityThemeChanged) {
            updateAccessibilityStylesheets();
        }
    }

    private void updateAccessibilityStylesheets() {
        // check to see if there is an override to enable a high-contrast theme
        final String userTheme = AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty("com.sun.javafx.highContrastTheme"));

        String platformThemeName = platformThemeProperties.get(WINDOWS_HIGH_CONTRAST_THEME_KEY);
        String accessibilityTheme = null;

        // User-defined property takes precedence
        if (userTheme != null) {
            switch (userTheme.toUpperCase()) {
                case "BLACKONWHITE":
                    accessibilityTheme = "com/sun/javafx/scene/control/skin/modena/blackOnWhite.css";
                    break;
                case "WHITEONBLACK":
                    accessibilityTheme = "com/sun/javafx/scene/control/skin/modena/whiteOnBlack.css";
                    break;
                case "YELLOWONBLACK":
                    accessibilityTheme = "com/sun/javafx/scene/control/skin/modena/yellowOnBlack.css";
                    break;
                default:
            }
        } else if (platformThemeName != null) {
            // The following names are Platform specific (Windows 7 and 8)
            switch (platformThemeName) {
                case "High Contrast White":
                    accessibilityTheme = "com/sun/javafx/scene/control/skin/modena/blackOnWhite.css";
                    break;
                case "High Contrast Black":
                    accessibilityTheme = "com/sun/javafx/scene/control/skin/modena/whiteOnBlack.css";
                    break;
                case "High Contrast #1":
                case "High Contrast #2": //TODO #2 should be green on black
                    accessibilityTheme = "com/sun/javafx/scene/control/skin/modena/yellowOnBlack.css";
                    break;
                default:
            }
        }

        if (accessibilityTheme != null) {
            accessibilityStylesheets.setAll(accessibilityTheme);
        } else {
            accessibilityStylesheets.clear();
        }
    }

}
