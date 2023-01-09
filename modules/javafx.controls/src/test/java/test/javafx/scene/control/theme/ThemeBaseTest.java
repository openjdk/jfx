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
import org.junit.jupiter.api.Test;
import javafx.scene.control.theme.ThemeBase;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ThemeBaseTest {

    @Test
    public void testOnPreferencesChangedIsInvokedWhenPreferencesAreInvalidated() {
        int[] count = new int[1];
        var theme = new ThemeBase() {
            @Override
            protected void onPreferencesChanged() {
                count[0]++;
            }
        };

        PlatformImpl.updatePreferences(Map.of("foo", "bar"));
        assertEquals(1, count[0]);

        PlatformImpl.updatePreferences(Map.of("foo", "baz", "qux", "quz"));
        assertEquals(2, count[0]);
    }

    @Test
    public void testAddFirst() {
        var theme = new ThemeBase() {
            {
                addFirst("foo");
                addFirst("bar");
            }
        };

        assertEquals(List.of("bar", "foo"), theme.getStylesheets());
    }

    @Test
    public void testAddLast() {
        var theme = new ThemeBase() {
            {
                addLast("foo");
                addLast("bar");
            }
        };

        assertEquals(List.of("foo", "bar"), theme.getStylesheets());
    }

}
