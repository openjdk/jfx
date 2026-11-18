/*
 * Copyright (c) 2023, 2026, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.PlatformUtil;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Enumeration of possible high contrast scheme values.
 * <p>
 * For each scheme, a theme key is defined. These keys can be
 * used in a resource bundle that defines the theme name values
 * for supported locales.
 * <p>
 * The high contrast feature may not be available on all platforms.
 */
enum WindowsHighContrastScheme {

    NONE(null),
    HIGH_CONTRAST_BLACK("high.contrast.black.theme"),
    HIGH_CONTRAST_WHITE("high.contrast.white.theme"),
    HIGH_CONTRAST_1("high.contrast.1.theme"),
    HIGH_CONTRAST_2("high.contrast.2.theme");

    private final String themeKey;

    WindowsHighContrastScheme(String themeKey) {
        this.themeKey = themeKey;
    }

    /**
     * Given a theme name string, this method finds the possible enum constant
     * for which the result of a function, applying its theme key, matches the theme name.
     *
     * @param themeName a string with the localized theme name (for the locale of the OS, not the JVM)
     * @return the enum constant or {@code NONE} if not found
     */
    static WindowsHighContrastScheme fromThemeName(String themeName) {
        if (themeName == null || !PlatformUtil.isWindows()) {
            return NONE;
        }

        // The OS gives us the theme name in the OS display language. In most cases, the default locale
        // of the JVM matches that language, so we first try the resource bundle for the default locale.
        // This is much faster than loading the resource bundles for all locales.
        WindowsHighContrastScheme scheme = find(themeName, List.of(ResourceBundle.getBundle(BUNDLE_NAME)));
        if (scheme != NONE) {
            return scheme;
        }

        // The JVM might run with a locale that is different from the OS, so we try all resource bundles.
        return find(themeName, ResourceBundleHolder.BUNDLES);
    }

    private static WindowsHighContrastScheme find(String themeName, List<ResourceBundle> resourceBundles) {
        for (WindowsHighContrastScheme item : values()) {
            for (ResourceBundle resourceBundle : resourceBundles) {
                if (item != NONE && themeName.equalsIgnoreCase(resourceBundle.getString(item.themeKey))) {
                    return item;
                }
            }
        }

        return NONE;
    }

    /**
     * Holds the theme resource bundles for all locales. Loading them takes a noticeable amount of time,
     * so we only do it when {@link #fromThemeName} actually needs to look up a theme name.
     */
    private static final class ResourceBundleHolder {
        static final List<ResourceBundle> BUNDLES = Arrays.stream(Locale.getAvailableLocales())
            .map(locale -> ResourceBundle.getBundle(BUNDLE_NAME, locale))
            .distinct()
            .toList();
    }

    private static final String BUNDLE_NAME = "com/sun/glass/ui/win/themes";
}
