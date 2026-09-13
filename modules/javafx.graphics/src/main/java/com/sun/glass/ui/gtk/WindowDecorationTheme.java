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

package com.sun.glass.ui.gtk;

import com.sun.javafx.application.PlatformImpl;
import javafx.stage.StageStyle;
import java.util.Locale;
import java.util.Map;

/**
 * The client-side window decoration theme used for {@link StageStyle#EXTENDED} windows.
 */
enum WindowDecorationTheme {

    GNOME("WindowDecorationGnome.css"),
    KDE("WindowDecorationKDE.css");

    WindowDecorationTheme(String stylesheet) {
        this.stylesheet = stylesheet;
    }

    private static final String THEME_NAME_KEY = "GTK.theme_name";

    /**
     * A mapping of platform theme names to the most similar window decoration theme.
     */
    private static final Map<String, WindowDecorationTheme> SIMILAR_THEMES = Map.of(
        "adwaita", WindowDecorationTheme.GNOME,
        "yaru", WindowDecorationTheme.GNOME,
        "breeze", WindowDecorationTheme.KDE
    );

    private final String stylesheet;

    /**
     * Determines the best window decoration theme for the current platform theme and desktop environment.
     * <p>
     * Since we can't ship decorations for all possible platform themes, we need to choose the theme most
     * similar to the platform theme. If we can't choose a theme by name, we fall back to choosing a theme
     * by determining the current desktop environment.
     */
    public static WindowDecorationTheme findBestTheme() {
        return PlatformImpl.getPlatformPreferences()
            .getString(THEME_NAME_KEY)
            .map(name -> {
                for (Map.Entry<String, WindowDecorationTheme> entry : SIMILAR_THEMES.entrySet()) {
                    if (name.toLowerCase(Locale.ROOT).startsWith(entry.getKey())) {
                        return entry.getValue();
                    }
                }

                return null;
            })
            .orElse(switch (DesktopEnvironment.current()) {
                case GNOME -> WindowDecorationTheme.GNOME;
                case KDE -> WindowDecorationTheme.KDE;
                default -> WindowDecorationTheme.GNOME;
            });
    }

    public String getStylesheet() {
        var url = getClass().getResource(stylesheet);
        if (url == null) {
            throw new RuntimeException("Resource not found: " + stylesheet);
        }

        return url.toExternalForm();
    }
}
