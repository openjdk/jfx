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

package test.com.sun.javafx.application;

import com.sun.javafx.application.PlatformPreferencesImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.javafx.collections.MockMapObserver;
import javafx.beans.InvalidationListener;
import java.util.HashMap;
import java.util.Map;

import static com.sun.javafx.application.PlatformImpl.*;
import static org.junit.jupiter.api.Assertions.*;
import static test.javafx.collections.MockMapObserver.Tuple.*;

public class PlatformImplTest {

    private static Map<String, Object> preferences;
    private static Map<String, Object> originalPreferences;

    @BeforeEach
    void beforeEach() {
        preferences.clear();
    }

    @BeforeAll
    static void beforeAll() {
        originalPreferences = new HashMap<>(getPlatformPreferences());
        preferences = ((PlatformPreferencesImpl)getPlatformPreferences()).getModifiableMap();
    }

    @AfterAll
    static void afterAll() {
        preferences.clear();
        preferences.putAll(originalPreferences);
    }

    @Test
    public void testUpdatePlatformPreferences() {
        Map<String, Object> newPrefs = Map.of("foo", "bar", "baz", "qux");
        updatePreferences(newPrefs);
        assertEquals(newPrefs, getPlatformPreferences());
    }

    @Test
    public void testPlatformPreferencesInvalidationListener() {
        int[] count = new int[1];
        InvalidationListener listener = observable -> count[0]++;
        getPlatformPreferences().addListener(listener);

        updatePreferences(Map.of("foo", "bar"));
        assertEquals(1, count[0]);

        // InvalidationListener is invoked only once, even when multiple values are changed at the same time
        updatePreferences(Map.of("qux", "quux", "quz", "quuz"));
        assertEquals(2, count[0]);

        getPlatformPreferences().removeListener(listener);
    }

    @Test
    public void testPlatformPreferencesChangeListener() {
        var observer = new MockMapObserver<String, Object>();
        getPlatformPreferences().addListener(observer);

        // Two added keys are included in the change notification
        updatePreferences(Map.of("foo", "bar", "baz", "qux"));
        observer.assertAdded(0, tup("foo", "bar"));
        observer.assertAdded(1, tup("baz", "qux"));
        observer.clear();

        // Mappings that haven't changed are not included in the change notification
        updatePreferences(Map.of("foo", "bar2", "baz", "qux"));
        observer.assertAdded(0, tup("foo", "bar2"));
        observer.clear();

        // Change the second mapping
        updatePreferences(Map.of("baz", "qux2"));
        observer.assertAdded(0, tup("baz", "qux2"));
        observer.clear();

        // If no mapping was changed, no change notification is fired
        updatePreferences(Map.of("foo", "bar2", "baz", "qux2"));
        observer.check0();
        observer.clear();

        getPlatformPreferences().removeListener(observer);
    }

}
