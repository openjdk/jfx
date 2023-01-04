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

package test.javafx.scene.control.theme;

import com.sun.javafx.application.PlatformImpl;
import com.sun.javafx.application.PlatformPreferencesImpl;
import org.junit.jupiter.api.Test;
import javafx.scene.control.theme.CaspianTheme;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CaspianThemeTest {

    @Test
    public void testHighContrastThemeWithSystemProperty() {
        var theme = new CaspianTheme();
        assertFalse(theme.getStylesheets().stream().anyMatch(fileName -> fileName.contains("highcontrast.css")));
        System.setProperty("com.sun.javafx.highContrastTheme", "ANY_VALUE_HERE");
        theme = new CaspianTheme();
        assertTrue(theme.getStylesheets().stream().anyMatch(fileName -> fileName.contains("highcontrast.css")));
        System.clearProperty("com.sun.javafx.highContrastTheme");
    }

    @Test
    public void testHighContrastThemeWithPlatformPreference() {
        var theme = new CaspianTheme();
        assertFalse(theme.getStylesheets().stream().anyMatch(fileName -> fileName.contains("highcontrast.css")));

        Map<String, Object> map = ((PlatformPreferencesImpl)PlatformImpl.getPlatformPreferences()).getModifiableMap();
        Object originalOn = map.put("Windows.SPI.HighContrastOn", true);
        Object originalName = map.put("Windows.SPI.HighContrastColorScheme", "ANY_VALUE_HERE");

        theme = new CaspianTheme();
        assertTrue(theme.getStylesheets().stream().anyMatch(fileName -> fileName.contains("highcontrast.css")));

        map.put("Windows.SPI.HighContrastOn", originalOn);
        map.put("Windows.SPI.HighContrastColorScheme", originalName);
    }

}
