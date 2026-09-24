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

import java.util.HashMap;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Test access to the pure string logic of {@link WinFontPath}: the parts of {@code fontpath.c} that
 * never called Win32, so that their mirroring can be checked on every platform without a registry or
 * GDI. None of these touch {@link WinFontNative}, with one Windows-only exception: the two registry
 * passes {@link #populateFontFileNamesFromCurrentUser} and {@link #populateFontFileNamesFromLocalMachine},
 * exposed separately so that the HKCU-then-HKLM precedence of {@code populateFontFileNameMap} can be proved.
 * <p>
 * Line numbers into {@code fontpath.c} refer to it at commit {@code 8492cb03b0}
 * ({@code git show 8492cb03b0:modules/javafx.graphics/src/main/native-font/fontpath.c}).
 */
public final class WinFontPathShim {

    private WinFontPathShim() {
    }

    /** {@code fontpath.c} L121-182 on the raw system and Windows directories. */
    public static String fontPath(String systemDirectory, String windowsDirectory) {
        return WinFontPath.fontPath(systemDirectory, windowsDirectory);
    }

    /** {@code RegistryToBaseTTNameW}: the name less {@code " (TrueType)"}, or {@code null}. */
    public static String registryToBaseTTName(String name) {
        return WinFontPath.registryToBaseTTName(name);
    }

    /** The {@code .ttf}/{@code .otf} acceptance of L674-678. */
    public static boolean hasTrueTypeExtension(String fileName) {
        return WinFontPath.hasTrueTypeExtension(fileName);
    }

    /** {@code registerFontW}: the collection split and the lower-cased puts. */
    public static void registerFont(HashMap<String, String> fontToFileMap, String name, String data, Locale locale) {
        WinFontPath.registerFont(fontToFileMap, name, data, locale);
    }

    /**
     * {@code populateFontFileNameFromRegistryKey} on {@code HKEY_CURRENT_USER}: the first of the two registry
     * passes of {@code populateFontFileNameMap} ({@code fontpath.c} L786-791). Windows only.
     */
    public static void populateFontFileNamesFromCurrentUser(HashMap<String, String> fontToFileMap, Locale locale) {
        WinFontPath.populateFontFileNameFromRegistryKey(WinFontNative.HKEY_CURRENT_USER, fontToFileMap, locale);
    }

    /** The second pass, on {@code HKEY_LOCAL_MACHINE}; its puts overwrite the user pass by name. Windows only. */
    public static void populateFontFileNamesFromLocalMachine(HashMap<String, String> fontToFileMap, Locale locale) {
        WinFontPath.populateFontFileNameFromRegistryKey(WinFontNative.HKEY_LOCAL_MACHINE, fontToFileMap, locale);
    }

    /** The {@code EUDC\<code page>} key for a language ID, or {@code null}. */
    public static String eudcKey(int langID) {
        return WinFontPath.eudcKey(langID);
    }

    /** L911-941 on the registry value's characters. */
    public static String eudcFontFile(char[] fontPath, int fontPathLength, String systemRoot,
                                      Supplier<String> windowsDirectory) {
        return WinFontPath.eudcFontFile(fontPath, fontPathLength, systemRoot, windowsDirectory);
    }

    public static String cString(char[] chars) {
        return WinFontPath.cString(chars);
    }

    /** {@code _wcsnicmp(s1 + from1, s2, count) == 0} in the "C" locale. */
    public static boolean wcsnicmpEqual(String s1, int from1, String s2, int count) {
        return WinFontPath.wcsnicmpEqual(s1, from1, s2, count);
    }

    /** {@code _wcsicmp(s1, s2) == 0} in the "C" locale. */
    public static boolean wcsicmpEqual(String s1, String s2) {
        return WinFontPath.wcsicmpEqual(s1, s2);
    }
}
