/*
 * Copyright (c) 2023 Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.application.preferences;

import com.sun.javafx.application.preferences.ApplicationPreferences;
import javafx.scene.paint.Color;
import javafx.stage.Appearance;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ApplicationPreferencesTest {

    ApplicationPreferences prefs;

    @BeforeEach
    void setup() {
        prefs = new ApplicationPreferences(Map.of(
            "test.foregroundColor", "foregroundColor",
            "test.backgroundColor", "backgroundColor",
            "test.accentColor", "accentColor"
        ));
    }

    @Test
    void testDefaultValues() {
        assertEquals(Color.WHITE, prefs.getBackgroundColor());
        assertEquals(Color.BLACK, prefs.getForegroundColor());
        assertEquals(Color.web("#157EFB"), prefs.getAccentColor());
        assertEquals(Appearance.LIGHT, prefs.getAppearance());
    }

    @Test
    void testResetSingleMapping() {
        prefs.update(Map.of("k1", 5, "k2", 7.5));

        // Override the "k1" mapping with a user value
        assertEquals(5, prefs.put("k1", 10));
        assertEquals(10, prefs.get("k1"));

        // Clear the user value
        prefs.reset("k1");
        assertEquals(5, prefs.get("k1"));
    }

    @Test
    void testResetAllMappings() {
        prefs.update(Map.of("k1", 5, "k2", 7.5));

        prefs.put("k1", 10);
        prefs.put("k2", 0.123);
        assertEquals(10, prefs.getInteger("k1").orElseThrow());
        assertEquals(0.123, prefs.getDouble("k2").orElseThrow(), 0.001);

        prefs.reset();
        assertEquals(5, prefs.getInteger("k1").orElseThrow());
        assertEquals(7.5, prefs.getDouble("k2").orElseThrow(), 0.001);
    }

    @Test
    void testCannotOverrideValueWithDifferentType() {
        prefs.update(Map.of("k", 5));
        assertThrows(IllegalArgumentException.class, () -> prefs.put("k", 3.141));
    }

    @Test
    void testCannotOverrideWithNullValue() {
        prefs.update(Map.of("k", 5));
        assertThrows(NullPointerException.class, () -> prefs.put("k", null));
    }

    @Test
    void testAppearanceReflectsForegroundAndBackgroundColors() {
        prefs.update(Map.of("test.foregroundColor", Color.BLACK, "test.backgroundColor", Color.WHITE));
        assertEquals(Appearance.LIGHT, prefs.getAppearance());

        prefs.update(Map.of("test.foregroundColor", Color.WHITE, "test.backgroundColor", Color.BLACK));
        assertEquals(Appearance.DARK, prefs.getAppearance());

        prefs.update(Map.of("test.foregroundColor", Color.DARKGRAY, "test.backgroundColor", Color.LIGHTGRAY));
        assertEquals(Appearance.LIGHT, prefs.getAppearance());

        prefs.update(Map.of("test.foregroundColor", Color.RED, "test.backgroundColor", Color.BLUE));
        assertEquals(Appearance.DARK, prefs.getAppearance());
    }

    @Test
    void testOverriddenAppearanceIsNotAffectedByBackgroundAndForegroundColors() {
        prefs.setAppearance(Appearance.DARK);
        prefs.setBackgroundColor(Color.WHITE);
        prefs.setForegroundColor(Color.BLACK);
        assertEquals(Appearance.DARK, prefs.getAppearance());
        prefs.setAppearance(null);
        assertEquals(Appearance.LIGHT, prefs.getAppearance());
    }

    @Test
    void testOverrideAppearance() {
        assertEquals(Appearance.LIGHT, prefs.getAppearance());
        prefs.setAppearance(Appearance.DARK);
        assertEquals(Appearance.DARK, prefs.getAppearance());
        prefs.setAppearance(null);
        assertEquals(Appearance.LIGHT, prefs.getAppearance());
    }

    @Test
    void testOverrideBackgroundColor() {
        assertEquals(Color.WHITE, prefs.getBackgroundColor());
        prefs.setBackgroundColor(Color.GREEN);
        assertEquals(Color.GREEN, prefs.getBackgroundColor());
        prefs.setBackgroundColor(null);
        assertEquals(Color.WHITE, prefs.getBackgroundColor());
    }

    @Test
    void testOverrideForegroundColor() {
        assertEquals(Color.BLACK, prefs.getForegroundColor());
        prefs.setForegroundColor(Color.GREEN);
        assertEquals(Color.GREEN, prefs.getForegroundColor());
        prefs.setForegroundColor(null);
        assertEquals(Color.BLACK, prefs.getForegroundColor());
    }

}
