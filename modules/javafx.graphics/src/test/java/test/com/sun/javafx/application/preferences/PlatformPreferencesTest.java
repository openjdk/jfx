/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.javafx.application.preferences.PreferenceMapping;
import javafx.animation.Interpolatable;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import test.javafx.collections.MockMapObserver;
import javafx.beans.InvalidationListener;
import javafx.collections.MapChangeListener;
import javafx.scene.paint.Color;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        prefs = new PlatformPreferences(
            // Well-known platform keys and their associated type
            Map.of(
                "test.anInt", Integer.class,
                "test.aDouble", Double.class,
                "test.aBoolean", Boolean.class,
                "test.aString", String.class,
                "test.aColor", Color.class,
                "test.aPaint", Paint.class,
                "test.aPaintArray", Paint[].class
            ),
            // Platform-specific key mappings
            Map.of(
                "test.foregroundColor", new PreferenceMapping<>("foregroundColor", Color.class),
                "test.backgroundColor", new PreferenceMapping<>("backgroundColor", Color.class),
                "test.accentColor", new PreferenceMapping<>("accentColor", Color.class),
                "test.reducedMotion", new PreferenceMapping<>("reducedMotion", Boolean.class),
                "test.enableTransparency", new PreferenceMapping<>("reducedTransparency", Boolean.class, b -> !b)
            ));
    }

    @Test
    void testWellKnownKeyThrowsExceptionWithWrongGetter() {
        assertThrows(IllegalArgumentException.class, () -> prefs.getDouble("test.anInt"));
        assertThrows(IllegalArgumentException.class, () -> prefs.getInteger("test.aDouble"));
        assertThrows(IllegalArgumentException.class, () -> prefs.getString("test.aBoolean"));
        assertThrows(IllegalArgumentException.class, () -> prefs.getColor("test.aString"));
        assertThrows(IllegalArgumentException.class, () -> prefs.getInteger("test.aColor"));
        assertThrows(IllegalArgumentException.class, () -> prefs.getString("test.aPaint"));
    }

    @Test
    void testWellKnownKeyReturnsEmptyValueWhenMappingNotPresent() {
        assertEquals(Optional.empty(), prefs.getInteger("test.anInt"));
        assertEquals(Optional.empty(), prefs.getDouble("test.aDouble"));
        assertEquals(Optional.empty(), prefs.getBoolean("test.aBoolean"));
        assertEquals(Optional.empty(), prefs.getString("test.aString"));
        assertEquals(Optional.empty(), prefs.getColor("test.aColor"));
        assertEquals(Optional.empty(), prefs.getValue("test.aColor", Color.class));
        assertEquals(Optional.empty(), prefs.getValue("test.aPaint", Paint.class));
        assertEquals(Optional.empty(), prefs.getValue("test.aPaint", Color.class));
    }

    @Test
    void testPolymorphicValues() {
        prefs.update(Map.of("test.aPaint", Color.RED));
        assertEquals(Color.RED, prefs.getColor("test.aPaint").orElseThrow());
        assertEquals(Color.RED, prefs.getValue("test.aPaint", Paint.class).orElseThrow());
        assertEquals(Color.RED, prefs.getValue("test.aPaint", Object.class).orElseThrow());
        assertEquals(Color.RED, prefs.getValue("test.aPaint", Interpolatable.class).orElseThrow());
        assertThrows(IllegalArgumentException.class, () -> prefs.getValue("test.aPaint", LinearGradient.class));

        var gradient = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE);
        prefs.update(Map.of("test.aPaint", gradient));
        assertEquals(gradient, prefs.getValue("test.aPaint", Paint.class).orElseThrow());
        assertEquals(gradient, prefs.getValue("test.aPaint", LinearGradient.class).orElseThrow());
        assertEquals(gradient, prefs.getValue("test.aPaint", Object.class).orElseThrow());
        assertThrows(IllegalArgumentException.class, () -> prefs.getColor("test.aPaint").orElseThrow());
    }

    @Test
    void testPolymorphicArrayValues() {
        var colors = new Color[] {Color.RED, Color.GREEN, Color.BLUE};
        prefs.update(Map.of("test.aPaintArray", colors));
        assertSame(colors, prefs.getValue("test.aPaintArray", Object[].class).orElseThrow());
        assertSame(colors, prefs.getValue("test.aPaintArray", Color[].class).orElseThrow());
        assertSame(colors, prefs.getValue("test.aPaintArray", Paint[].class).orElseThrow());
        assertSame(colors, prefs.getValue("test.aPaintArray", Interpolatable[].class).orElseThrow());
        assertSame(colors, prefs.getValue("test.aPaintArray", Object.class).orElseThrow());
        assertSame(colors, prefs.getValue("test.aPaintArray", Cloneable.class).orElseThrow());
        assertSame(colors, prefs.getValue("test.aPaintArray", Serializable.class).orElseThrow());
        assertThrows(IllegalArgumentException.class, () -> prefs.getValue("test.aPaintArray", Paint.class));
        assertThrows(IllegalArgumentException.class, () -> prefs.getValue("test.aPaintArray", LinearGradient[].class));
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
    void testUpdatePreferencesWithNullValueRemovesMapping() {
        var content = Map.of(
            "red", Color.RED,
            "blue", Color.BLUE,
            "str", "foo",
            "bool", true);
        prefs.update(content);
        assertEquals(content, prefs);

        prefs.update(new HashMap<>() {{ put("red", null); }});
        assertEquals(
            Map.of(
                "blue", Color.BLUE,
                "str", "foo",
                "bool", true),
            prefs);
    }

    @Test
    void testPlatformPreferencesInvalidationListener() {
        int[] count = new int[1];
        InvalidationListener listener = observable -> count[0]++;
        prefs.addListener(listener);

        prefs.update(Map.of("foo", "bar"));
        assertEquals(1, count[0]);

        prefs.update(Map.of("qux", "quux", "quz", "quuz"));
        assertEquals(2, count[0]);

        prefs.update(Map.of("qux", "quux", "quz", "quuz", "abc", "def"));
        assertEquals(3, count[0]);
    }

    @Test
    void testPlatformPreferencesChangeListener() {
        var observer = new MockMapObserver<String, Object>();
        prefs.addListener(observer);

        // Two added keys are included in the change notification
        prefs.update(Map.of("foo", "bar", "baz", "qux"));
        assertEquals(2, observer.getCallsNumber());
        observer.assertAdded(0, tup("foo", "bar"));
        observer.assertAdded(1, tup("baz", "qux"));
        observer.clear();

        // Mappings that haven't changed are not included in the change notification (baz=qux)
        prefs.update(Map.of("foo", "bar2", "baz", "qux"));
        assertEquals(1, observer.getCallsNumber());
        observer.assertRemoved(0, tup("foo", "bar"));
        observer.assertAdded(0, tup("foo", "bar2"));
        observer.clear();

        // Change the second mapping
        prefs.update(Map.of("baz", "qux2"));
        assertEquals(1, observer.getCallsNumber());
        observer.assertRemoved(0, tup("baz", "qux"));
        observer.assertAdded(0, tup("baz", "qux2"));
        observer.clear();

        // If no mapping was changed, no change notification is fired
        prefs.update(Map.of("foo", "bar2", "baz", "qux2"));
        observer.check0();
        observer.clear();

        // If a key is mapped to null, the mapping is removed
        prefs.update(new HashMap<>() {{ put("foo", null); }});
        assertEquals(1, observer.getCallsNumber());
        observer.assertRemoved(0, tup("foo", "bar2"));
        observer.clear();
    }

    @Test
    void testColorPropertyChangesAreAtomicWhenMapIsObserved() {
        var trace = new ArrayList<Color[]>();

        prefs.addListener((MapChangeListener<? super String, ? super Object>) change ->
            trace.add(new Color[] {prefs.getForegroundColor(), prefs.getBackgroundColor(), prefs.getAccentColor()}));

        testColorPropertyChangesAreAtomic(trace, 3);
    }

    @Test
    void testColorPropertyChangesAreAtomicWhenColorPropertiesAreObserved() {
        var trace = new ArrayList<Color[]>();

        InvalidationListener listener = observable -> trace.add(
            new Color[] { prefs.getForegroundColor(), prefs.getBackgroundColor(), prefs.getAccentColor() });
        prefs.foregroundColorProperty().addListener(listener);
        prefs.backgroundColorProperty().addListener(listener);
        prefs.accentColorProperty().addListener(listener);

        testColorPropertyChangesAreAtomic(trace, 3);
    }

    @Test
    void testColorPropertyChangesAreAtomicWhenColorSchemePropertyIsObserved() {
        var trace = new ArrayList<Color[]>();

        prefs.colorSchemeProperty().addListener((observable, oldValue, newValue) -> trace.add(
            new Color[] { prefs.getForegroundColor(), prefs.getBackgroundColor(), prefs.getAccentColor() }));

        testColorPropertyChangesAreAtomic(trace, 1);
    }

    /**
     * Asserts that color properties are never observed in a transient state: change notifications
     * are only received after all properties have been set to their new values.
     */
    private void testColorPropertyChangesAreAtomic(List<Color[]> trace, int listenerInvocations) {
        Color[] expectedColors;
        int i = 0;

        prefs.update(Map.of(
            "test.foregroundColor", Color.RED,
            "test.backgroundColor", Color.GREEN,
            "test.accentColor", Color.BLUE));
        assertEquals(listenerInvocations, trace.size());
        expectedColors = new Color[] { Color.RED, Color.GREEN, Color.BLUE };
        while (i < listenerInvocations) {
            assertArrayEquals(expectedColors, trace.get(i++));
        }

        prefs.update(Map.of(
            "test.foregroundColor", Color.BLUE,
            "test.backgroundColor", Color.YELLOW,
            "test.accentColor", Color.PURPLE));
        assertEquals(listenerInvocations * 2, trace.size());
        expectedColors = new Color[] { Color.BLUE, Color.YELLOW, Color.PURPLE };
        while (i < listenerInvocations * 2) {
            assertArrayEquals(expectedColors, trace.get(i++));
        }
    }

    @Test
    void testReducedMotionProperty() {
        var trace = new ArrayList<Boolean>();
        prefs.reducedMotionProperty().addListener((observable, ov, nv) -> trace.add(nv));

        assertFalse(prefs.isReducedMotion());
        prefs.update(Map.of("test.reducedMotion", true));

        assertEquals(1, trace.size());
        assertEquals(Boolean.TRUE, trace.get(0));
        assertTrue(prefs.isReducedMotion());

        prefs.update(new HashMap<>() {{ put("test.reducedMotion", null); }});
        assertEquals(2, trace.size());
        assertEquals(Boolean.FALSE, trace.get(1));
        assertFalse(prefs.isReducedMotion());
    }

    @Test
    void testReducedTransparencyPropertyWithInverseMapping() {
        var trace = new ArrayList<Boolean>();
        prefs.reducedTransparencyProperty().addListener((observable, ov, nv) -> trace.add(nv));

        assertFalse(prefs.isReducedTransparency());
        prefs.update(Map.of("test.enableTransparency", false));

        assertEquals(1, trace.size());
        assertEquals(Boolean.TRUE, trace.get(0));
        assertTrue(prefs.isReducedTransparency());

        prefs.update(new HashMap<>() {{ put("test.enableTransparency", null); }});
        assertEquals(2, trace.size());
        assertEquals(Boolean.FALSE, trace.get(1));
        assertFalse(prefs.isReducedTransparency());
    }
}
