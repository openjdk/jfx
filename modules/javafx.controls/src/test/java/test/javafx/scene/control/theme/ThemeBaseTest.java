/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.control.theme;

import com.sun.javafx.application.PlatformImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import javafx.scene.control.theme.ThemeBase;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ThemeBaseTest {

    private static Color originalForegroundColor, originalAccentColor;

    @BeforeAll
    static void beforeAll() {
        originalForegroundColor = setSystemForegroundColor(Color.BLACK);
        originalAccentColor = setSystemAccentColor(Color.BLUE);
    }

    @AfterAll
    static void afterAll(){
        setSystemForegroundColor(originalForegroundColor);
        setSystemAccentColor(originalAccentColor);
    }

    private static Color setSystemForegroundColor(Color color) {
        var prefs = new HashMap<>(PlatformImpl.getPlatformPreferences());
        Color original = (Color)prefs.put("Windows.UIColor.Foreground", color);
        PlatformImpl.updatePreferences(prefs);
        return original;
    }

    private static Color setSystemAccentColor(Color color) {
        var prefs = new HashMap<>(PlatformImpl.getPlatformPreferences());
        Color original = (Color)prefs.put("Windows.UIColor.Accent", color);
        PlatformImpl.updatePreferences(prefs);
        return original;
    }

    @Test
    public void testDarkModeOverride() {
        var theme = new ThemeBase() {};
        var trace = new ArrayList<Boolean>();
        theme.darkModeProperty().addListener((obs, oldValue, newValue) -> trace.add(newValue));
        assertFalse(theme.isDarkMode());
        assertNull(theme.getDarkModeOverride());

        theme.setDarkModeOverride(true);
        assertEquals(List.of(true), trace);
        assertTrue(theme.isDarkMode());

        theme.setDarkModeOverride(null);
        assertEquals(List.of(true, false), trace);
        assertFalse(theme.isDarkMode());

        Color original = setSystemForegroundColor(Color.WHITE);
        assertEquals(List.of(true, false, true), trace);
        assertTrue(theme.isDarkMode());

        theme.setDarkModeOverride(false);
        assertEquals(List.of(true, false, true, false), trace);
        assertFalse(theme.isDarkMode());

        setSystemForegroundColor(original);
    }

    @Test
    public void testAccentColorOverride() {
        var theme = new ThemeBase() {};
        var trace = new ArrayList<Color>();
        theme.accentColorProperty().addListener((obs, oldValue, newValue) -> trace.add(newValue));
        assertEquals(Color.BLUE, theme.getAccentColor());
        assertNull(theme.getAccentColorOverride());

        theme.setAccentColorOverride(Color.FUCHSIA);
        assertEquals(List.of(Color.FUCHSIA), trace);
        assertEquals(Color.FUCHSIA, theme.getAccentColor());

        theme.setAccentColorOverride(null);
        assertEquals(List.of(Color.FUCHSIA, Color.BLUE), trace);
        assertEquals(Color.BLUE, theme.getAccentColor());

        Color original = setSystemAccentColor(Color.RED);
        assertEquals(List.of(Color.FUCHSIA, Color.BLUE, Color.RED), trace);
        assertEquals(Color.RED, theme.getAccentColor());

        theme.setAccentColorOverride(Color.YELLOW);
        assertEquals(List.of(Color.FUCHSIA, Color.BLUE, Color.RED, Color.YELLOW), trace);
        assertEquals(Color.YELLOW, theme.getAccentColor());

        setSystemForegroundColor(original);
    }

}
