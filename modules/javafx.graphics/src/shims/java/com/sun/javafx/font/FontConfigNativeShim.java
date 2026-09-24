/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.font;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Test access to what {@link FontConfigNative} keeps package-private beyond the two entry points
 * {@link FontConfigManagerShim} reaches by name: the sonames it tries and the library-name-taking forms of
 * those entry points, for the cases where fontconfig cannot be opened. No golden goes through this class, and
 * {@code LinuxFontGoldens.requireSameBinding} does not read it. Compiled into the module by the
 * {@code compile-shims} execution of the javafx.graphics pom; reached by the tests through the
 * {@code --add-exports javafx.graphics/com.sun.javafx.font=ALL-UNNAMED} line of {@code src/test/addExports}.
 * No restricted {@code java.lang.foreign} call is made here: every one stays in {@code FontConfigNative}.
 */
public final class FontConfigNativeShim {

    private FontConfigNativeShim() {
    }

    /** The library names {@code FontConfigNative} tries, in order. */
    public static List<String> libraries() {
        return FontConfigNative.LIBRARIES;
    }

    /** {@code FontConfigNative.getLogicalFonts} over the given library names instead of {@link #libraries()}. */
    public static boolean getLogicalFonts(List<String> libraries, String locale,
                                          FontConfigManager.FcCompFont[] fonts, boolean includeFallbacks) {
        return FontConfigNative.sortLogicalFonts(libraries, locale, fonts, includeFallbacks);
    }

    /** {@code FontConfigNative.populateFontMaps} over the given library names instead of {@link #libraries()}. */
    public static boolean populateFontMaps(List<String> libraries, HashMap<String, String> fontToFileMap,
                                           HashMap<String, String> fontToFamilyNameMap,
                                           HashMap<String, ArrayList<String>> familyToFontListMap,
                                           Locale locale) {
        return FontConfigNative.listFonts(libraries, fontToFileMap, fontToFamilyNameMap, familyToFontListMap,
                                          locale);
    }
}
