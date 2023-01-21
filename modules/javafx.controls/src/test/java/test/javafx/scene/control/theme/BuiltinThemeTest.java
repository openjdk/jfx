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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import javafx.css.StyleTheme;
import javafx.scene.control.theme.CaspianTheme;
import javafx.scene.control.theme.ModenaTheme;

import static com.sun.javafx.application.PlatformImpl.*;
import static org.junit.jupiter.api.Assertions.*;

public class BuiltinThemeTest {

    private static String originalUAStylesheet;
    private static StyleTheme originalTheme;

    @BeforeAll
    static void beforeAll() {
        originalUAStylesheet = platformUserAgentStylesheetProperty().get();
        originalTheme = platformUserAgentStyleThemeProperty().get();
    }

    @AfterAll
    static void afterAll() {
        platformUserAgentStyleThemeProperty().set(originalTheme);
        platformUserAgentStylesheetProperty().set(originalUAStylesheet);
    }

    /*
       When platformUserAgentStylesheet is set to a built-in theme name, platformTheme is implicitly
       set to the corresponding theme class:

       ┌───────────────────────────────────────────────────────────────┐
       │ platformUserAgentStylesheet    platformTheme                  │
       ├───────────────────────────────────────────────────────────────┤
       │ null --> foo.css               null                           │
       │ foo.css --> CASPIAN            null --> CaspianTheme          │
       │ CASPIAN --> MODENA             CaspianTheme --> ModenaTheme   │
       └───────────────────────────────────────────────────────────────┘
     */
    @Test
    public void testThemeIsImplicitlySet() {
        platformUserAgentStylesheetProperty().set(null);
        platformUserAgentStyleThemeProperty().set(null);

        platformUserAgentStylesheetProperty().set("foo.css");
        assertNull(platformUserAgentStyleThemeProperty().get());

        platformUserAgentStylesheetProperty().set("CASPIAN");
        assertEquals("CaspianTheme", platformUserAgentStyleThemeProperty().get().getClass().getSimpleName());

        platformUserAgentStylesheetProperty().set("MODENA");
        assertEquals("ModenaTheme", platformUserAgentStyleThemeProperty().get().getClass().getSimpleName());
    }

    /*
       When platformTheme is explicitly set to one of the built-in themes, platformUserAgentStylesheet
       is cleared when it was previously set to one of the built-in theme constants.
     */
    @Test
    public void testUAConstantIsClearedWhenBuiltinThemeIsExplicitlySet() {
        platformUserAgentStylesheetProperty().set("CASPIAN");
        platformUserAgentStyleThemeProperty().set(new CaspianTheme());
        assertNull(platformUserAgentStylesheetProperty().get());
        assertTrue(platformUserAgentStyleThemeProperty().get() instanceof CaspianTheme);

        platformUserAgentStylesheetProperty().set("MODENA");
        platformUserAgentStyleThemeProperty().set(new ModenaTheme());
        assertNull(platformUserAgentStylesheetProperty().get());
        assertTrue(platformUserAgentStyleThemeProperty().get() instanceof ModenaTheme);
    }

    /*
       When platformTheme is explicitly set to one of the built-in themes, platformUserAgentStylesheet
       is NOT cleared when its value does not represent a built-in theme constant.
     */
    @Test
    public void testUAStylesheetIsNotClearedWhenBuiltinThemeIsExplicitlySet() {
        platformUserAgentStylesheetProperty().set("foo.css");
        platformUserAgentStyleThemeProperty().set(new CaspianTheme());
        assertEquals("foo.css", platformUserAgentStylesheetProperty().get());
        assertTrue(platformUserAgentStyleThemeProperty().get() instanceof CaspianTheme);
    }

}
