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

import com.sun.javafx.application.preferences.PlatformPreferences;
import test.javafx.collections.MockMapObserver;
import javafx.beans.InvalidationListener;
import javafx.collections.MapChangeListener;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static test.javafx.collections.MockMapObserver.Tuple.tup;

public class PlatformPreferencesTest {

    PlatformPreferences prefs;

    @BeforeEach
    void setup() {
        prefs = new PlatformPreferences(Map.of(
            "test.foregroundColor", "foregroundColor",
            "test.backgroundColor", "backgroundColor",
            "test.accentColor", "accentColor"
        ));
    }

    @Test
    void testMapIsImmutable() {
        prefs.update(Map.of("k1", 5, "k2", 7.5));
        assertThrows(UnsupportedOperationException.class, () -> prefs.put("k1", 4));
        assertThrows(UnsupportedOperationException.class, () -> prefs.remove("k1"));
        assertThrows(UnsupportedOperationException.class, () -> prefs.clear());
        assertThrows(UnsupportedOperationException.class, () -> prefs.entrySet().remove(prefs.entrySet().iterator().next()));
        assertThrows(UnsupportedOperationException.class, () -> prefs.entrySet().clear());
        assertThrows(UnsupportedOperationException.class, () -> prefs.keySet().remove(prefs.keySet().iterator().next()));
        assertThrows(UnsupportedOperationException.class, () -> prefs.keySet().clear());
    }

    @Test
    void testUnknownKeyReturnsEmptyValue() {
        assertEquals(Optional.empty(), prefs.getInteger("does_not_exist"));
        assertEquals(Optional.empty(), prefs.getDouble("does_not_exist"));
        assertEquals(Optional.empty(), prefs.getBoolean("does_not_exist"));
        assertEquals(Optional.empty(), prefs.getString("does_not_exist"));
        assertEquals(Optional.empty(), prefs.getColor("does_not_exist"));
        assertEquals(Optional.empty(), prefs.getValue("does_not_exist", String.class));
    }

    @Test
    void testGetValueWithWrongTypeFails() {
        prefs.update(Map.of("k", 5));
        assertThrows(IllegalArgumentException.class, () -> prefs.getValue("k", Double.class));
    }

    @Test
    void testOptionalKeys() {
        prefs.update(Map.of(
            "k1", 5,
            "k2", 7.5,
            "k3", true,
            "k4", "foo",
            "k5", Color.RED));

        assertEquals(5, prefs.getInteger("k1").orElseThrow());
        assertEquals(7.5, prefs.getDouble("k2").orElseThrow(), 0.001);
        assertEquals(true, prefs.getBoolean("k3").orElseThrow());
        assertEquals("foo", prefs.getString("k4").orElseThrow());
        assertEquals(Color.RED, prefs.getColor("k5").orElseThrow());
    }

    @Test
    void testUpdatePreferencesWithNewContent() {
        var content = Map.of(
            "red", Color.RED,
            "blue", Color.BLUE,
            "str", "foo",
            "bool", true);
        prefs.update(content);
        assertEquals(content, prefs);
    }

    @Test
    void testUpdatePreferencesWithSameContent() {
        var content = Map.of(
            "red", Color.RED,
            "blue", Color.BLUE,
            "str", "foo",
            "bool", true);
        prefs.update(content);
        prefs.update(content);
        assertEquals(content, prefs);
    }

    @Test
    void testPlatformPreferencesInvalidationListener() {
        int[] count = new int[1];
        InvalidationListener listener = observable -> count[0]++;
        prefs.addListener(listener);

        prefs.update(Map.of("foo", "bar"));
        assertEquals(1, count[0]);

        prefs.update(Map.of("qux", "quux", "quz", "quuz"));
        assertEquals(3, count[0]);
    }

    @Test
    void testPlatformPreferencesChangeListener() {
        var observer = new MockMapObserver<String, Object>();
        prefs.addListener(observer);

        // Two added keys are included in the change notification
        prefs.update(Map.of("foo", "bar", "baz", "qux"));
        observer.assertAdded(0, tup("foo", "bar"));
        observer.assertAdded(1, tup("baz", "qux"));
        observer.clear();

        // Mappings that haven't changed are not included in the change notification (baz=qux)
        prefs.update(Map.of("foo", "bar2", "baz", "qux"));
        observer.assertRemoved(0, tup("foo", "bar"));
        observer.assertAdded(0, tup("foo", "bar2"));
        observer.clear();

        // Change the second mapping
        prefs.update(Map.of("baz", "qux2"));
        observer.assertRemoved(0, tup("baz", "qux"));
        observer.assertAdded(0, tup("baz", "qux2"));
        observer.clear();

        // If no mapping was changed, no change notification is fired
        prefs.update(Map.of("foo", "bar2", "baz", "qux2"));
        observer.check0();
        observer.clear();
    }

    @Test
    void testPreferenceUpdatesAreAtomicWhenObserved() {
        var trace = new ArrayList<Color[]>();
        Color[] expectedColors;

        prefs.addListener((MapChangeListener<? super String, ? super Object>) change ->
            trace.add(new Color[] {prefs.getBackgroundColor(), prefs.getForegroundColor(), prefs.getAccentColor()}));

        prefs.update(Map.of(
            "test.foregroundColor", Color.RED,
            "test.backgroundColor", Color.BLUE,
            "test.accentColor", Color.GREEN));
        assertEquals(3, trace.size());
        expectedColors = new Color[] { Color.BLUE, Color.RED, Color.GREEN };
        assertArrayEquals(expectedColors, trace.get(0));
        assertArrayEquals(expectedColors, trace.get(1));
        assertArrayEquals(expectedColors, trace.get(2));

        prefs.update(Map.of(
            "test.backgroundColor", Color.YELLOW,
            "test.foregroundColor", Color.BLUE,
            "test.accentColor", Color.PURPLE));
        assertEquals(6, trace.size());
        expectedColors = new Color[] { Color.YELLOW, Color.BLUE, Color.PURPLE };
        assertArrayEquals(expectedColors, trace.get(3));
        assertArrayEquals(expectedColors, trace.get(4));
        assertArrayEquals(expectedColors, trace.get(5));
    }

}
