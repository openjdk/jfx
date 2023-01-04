/*
 * Copyright (c) 2021, 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.util.function.Function;

/**
 * Enumeration of possible high contrast scheme values.
 * <p>
 * For each scheme, a theme key is defined. These keys can be
 * used, for instance, in a resource bundle that defines the theme name values
 * for supported locales.
 * <p>
 * The high contrast feature may not be available on all platforms.
 */
public enum HighContrastScheme {
    HIGH_CONTRAST_BLACK("high.contrast.black.theme"),
    HIGH_CONTRAST_WHITE("high.contrast.white.theme"),
    HIGH_CONTRAST_1("high.contrast.1.theme"),
    HIGH_CONTRAST_2("high.contrast.2.theme");

    private final String themeKey;

    HighContrastScheme(String themeKey) {
        this.themeKey = themeKey;
    }

    public String getThemeKey() {
        return themeKey;
    }

    /**
     * Given a theme name string, this method finds the possible enum constant
     * for which the result of a function, applying its theme key, matches the theme name.
     * <p>
     * An example of such function can be {@code ResourceBundle::getString},
     * as {@link java.util.ResourceBundle#getString(String)} returns a string for
     * the given key.
     *
     * @param keyFunction a {@link Function} that returns a string for a given theme key string.
     * @param themeName   a string with the theme name
     * @return the name of the enum constant or null if not found
     */
    public static String fromThemeName(Function<String, String> keyFunction, String themeName) {
        if (keyFunction == null || themeName == null) {
            return null;
        }
        for (HighContrastScheme item : values()) {
            if (themeName.equalsIgnoreCase(keyFunction.apply(item.getThemeKey()))) {
                return item.toString();
            }
        }
        return null;
    }
}
