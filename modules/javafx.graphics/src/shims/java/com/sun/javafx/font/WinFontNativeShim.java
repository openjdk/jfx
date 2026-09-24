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

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Test access to {@link WinFontNative}, the Win32 binding layer behind the Windows font enumeration.
 * <p>
 * Compiled into the module by the {@code compile-shims} execution of the javafx.graphics pom, so it
 * may call the package-private facade; {@code test.com.sun.javafx.font.WinFontNativeTest} reaches it
 * through the {@code --add-exports javafx.graphics/com.sun.javafx.font=ALL-UNNAMED} line of
 * {@code src/test/addExports}. Every downcall and upcall a test makes goes through here, so the
 * restricted {@code java.lang.foreign} calls stay inside the module that
 * {@code --enable-native-access} names. The package-private records of the facade are re-exposed as
 * public ones of the same shape.
 */
public final class WinFontNativeShim {

    private WinFontNativeShim() {
    }

    /** Runs the facade's class initializer: looks the four system libraries up and binds every symbol. */
    public static void ensureLoaded() {
        WinFontNative.ensureLoaded();
    }

    /** The symbols the facade bound, as {@code <dll>!<name>}, in binding order. */
    public static List<String> boundSymbols() {
        return WinFontNative.boundSymbols();
    }

    public static boolean resolves(String qualifiedName) {
        return WinFontNative.resolves(qualifiedName);
    }

    /**
     * The {@code WCHAR}s the facade writes for a string it hands Win32 (a registry key or value name),
     * read back from native memory, the terminating NUL included.
     */
    public static char[] wideCodeUnits(String text) {
        return WinFontNative.wideCodeUnits(text);
    }

    /** A {@code wingdi.h}/{@code winuser.h}/{@code winreg.h} constant the facade defines, by name. */
    public static int constant(String name) {
        return switch (name) {
            case "TRUETYPE_FONTTYPE" -> WinFontNative.TRUETYPE_FONTTYPE;
            case "DEVICE_FONTTYPE" -> WinFontNative.DEVICE_FONTTYPE;
            case "DEFAULT_CHARSET" -> WinFontNative.DEFAULT_CHARSET;
            case "LF_FACESIZE" -> WinFontNative.LF_FACESIZE;
            case "LF_FULLFACESIZE" -> WinFontNative.LF_FULLFACESIZE;
            case "LOGPIXELSY" -> WinFontNative.LOGPIXELSY;
            case "SPI_GETNONCLIENTMETRICS" -> WinFontNative.SPI_GETNONCLIENTMETRICS;
            case "SPI_GETFONTSMOOTHINGCONTRAST" -> WinFontNative.SPI_GETFONTSMOOTHINGCONTRAST;
            case "USER_DEFAULT_SCREEN_DPI" -> WinFontNative.USER_DEFAULT_SCREEN_DPI;
            case "MAX_PATH" -> WinFontNative.MAX_PATH;
            case "ERROR_SUCCESS" -> WinFontNative.ERROR_SUCCESS;
            case "REG_SZ" -> WinFontNative.REG_SZ;
            case "KEY_READ" -> WinFontNative.KEY_READ;
            case "LOCALE_ILANGUAGE" -> WinFontNative.LOCALE_ILANGUAGE;
            case "LOCALE_RETURN_NUMBER" -> WinFontNative.LOCALE_RETURN_NUMBER;
            default -> throw new IllegalArgumentException("no such constant: " + name);
        };
    }

    /* ---------------------------------------------------------------------------------------------
     * Layouts
     * ------------------------------------------------------------------------------------------- */

    public static long layoutByteSize(String struct) {
        return layout(struct).byteSize();
    }

    public static long layoutOffset(String struct, String field) {
        return layout(struct).byteOffset(MemoryLayout.PathElement.groupElement(field));
    }

    private static StructLayout layout(String struct) {
        return switch (struct) {
            case "LOGFONTW" -> WinFontNative.LOGFONTW_LAYOUT;
            case "ENUMLOGFONTEXW" -> WinFontNative.ENUMLOGFONTEXW_LAYOUT;
            case "NEWTEXTMETRICW" -> WinFontNative.NEWTEXTMETRICW_LAYOUT;
            case "FONTSIGNATURE" -> WinFontNative.FONTSIGNATURE_LAYOUT;
            case "NEWTEXTMETRICEXW" -> WinFontNative.NEWTEXTMETRICEXW_LAYOUT;
            case "NONCLIENTMETRICSW" -> WinFontNative.NONCLIENTMETRICSW_LAYOUT;
            default -> throw new IllegalArgumentException("no such layout: " + struct);
        };
    }

    /* ---------------------------------------------------------------------------------------------
     * kernel32 / user32 / gdi32 scalars
     * ------------------------------------------------------------------------------------------- */

    public static String systemDirectory() {
        return WinFontNative.systemDirectory();
    }

    public static String windowsDirectory() {
        return WinFontNative.windowsDirectory();
    }

    public static String windowsDirectory(int capacity) {
        return WinFontNative.windowsDirectory(capacity);
    }

    public static int systemDefaultLangID() {
        return WinFontNative.systemDefaultLangID();
    }

    public static int systemDefaultLCID() {
        return WinFontNative.systemDefaultLCID();
    }

    public static int localeInfoNumber(int lcid, int lcType) {
        return WinFontNative.localeInfoNumber(lcid, lcType);
    }

    public static int systemParametersInfoUInt(int action, int fallback) {
        return WinFontNative.systemParametersInfoUInt(action, fallback);
    }

    public record NonClientMetrics(int messageFontHeight, String messageFontFaceName) {
    }

    /** {@code SPI_GETNONCLIENTMETRICS} with {@code cbSize = sizeof(NONCLIENTMETRICSW)}; {@code null} if refused. */
    public static NonClientMetrics nonClientMetrics() {
        WinFontNative.NonClientMetrics metrics = WinFontNative.nonClientMetrics();
        return metrics == null ? null
                : new NonClientMetrics(metrics.messageFontHeight(), metrics.messageFontFaceName());
    }

    /** {@code GetDeviceCaps(GetDC(GetDesktopWindow()), LOGPIXELSY)}, the DC released afterwards. */
    public static int desktopLogPixelsY() {
        MemorySegment desktop = WinFontNative.getDesktopWindow();
        MemorySegment hdc = WinFontNative.getDC(desktop);
        try {
            return WinFontNative.getDeviceCaps(hdc, WinFontNative.LOGPIXELSY);
        } finally {
            WinFontNative.releaseDC(desktop, hdc);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * EnumFontFamiliesExW
     * ------------------------------------------------------------------------------------------- */

    public record EnumeratedFont(String faceName, int charSet, String fullName) {
    }

    /** The Java side of {@code EnumFontFamExProc}: return 1 to continue, 0 to stop. */
    public interface FontEnumProc {
        int accept(EnumeratedFont font, int fontType);
    }

    /**
     * One {@link WinFontNative.FontEnumeration} over the screen device context, for the thread that
     * created it. {@link #enumerate} may be called from within a callback of the same session.
     */
    public static final class Enumeration implements AutoCloseable {

        private final MemorySegment screenDC;
        private final WinFontNative.FontEnumeration session;

        public Enumeration() {
            screenDC = WinFontNative.getDC(MemorySegment.NULL);
            if (screenDC.address() == 0) {
                throw new IllegalStateException("GetDC(NULL) returned NULL");
            }
            session = new WinFontNative.FontEnumeration();
        }

        /** {@code EnumFontFamiliesExW} for {@code faceName} ({@code ""} = every family) and {@code charSet}. */
        public int enumerate(String faceName, int charSet, FontEnumProc proc) {
            return session.enumerate(screenDC, faceName, charSet, (font, fontType) ->
                    proc.accept(new EnumeratedFont(font.faceName(), font.charSet(), font.fullName()), fontType));
        }

        /** Every font {@link #enumerate} reports for the arguments, in callback order. */
        public List<EnumeratedFont> faces(String faceName, int charSet) {
            List<EnumeratedFont> faces = new ArrayList<>();
            enumerate(faceName, charSet, (font, fontType) -> {
                faces.add(font);
                return 1;
            });
            return faces;
        }

        @Override
        public void close() {
            session.close();
            WinFontNative.releaseDC(MemorySegment.NULL, screenDC);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * advapi32
     * ------------------------------------------------------------------------------------------- */

    public static MemorySegment hkeyCurrentUser() {
        return WinFontNative.HKEY_CURRENT_USER;
    }

    public static MemorySegment hkeyLocalMachine() {
        return WinFontNative.HKEY_LOCAL_MACHINE;
    }

    /** {@code RegOpenKeyExW(root, subKey, 0, KEY_READ, &key)}: the key, or {@code null} when it fails. */
    public static MemorySegment regOpenKeyRead(MemorySegment root, String subKey) {
        return WinFontNative.regOpenKeyRead(root, subKey);
    }

    public static int regCloseKey(MemorySegment key) {
        return WinFontNative.regCloseKey(key);
    }

    public record RegKeyInfo(int values, int maxValueNameChars, int maxValueDataBytes) {
    }

    public static RegKeyInfo regQueryInfoKey(MemorySegment key) {
        WinFontNative.RegKeyInfo info = WinFontNative.regQueryInfoKey(key);
        return info == null ? null : new RegKeyInfo(info.values(), info.maxValueNameChars(), info.maxValueDataBytes());
    }

    public record RegEnumValue(int status, String name, int type, char[] data) {
    }

    public static RegEnumValue regEnumValue(MemorySegment key, int index, int nameCapacityChars,
                                            int dataCapacityBytes) {
        WinFontNative.RegEnumValue value = WinFontNative.regEnumValue(key, index, nameCapacityChars, dataCapacityBytes);
        return new RegEnumValue(value.status(), value.name(), value.type(), value.data());
    }

    public record RegValue(int status, int type, int sizeBytes, char[] data) {
    }

    /** {@code RegQueryValueExW}; a negative capacity is the size query with a {@code NULL} buffer. */
    public static RegValue regQueryValue(MemorySegment key, String valueName, int dataCapacityBytes) {
        WinFontNative.RegValue value = WinFontNative.regQueryValue(key, valueName, dataCapacityBytes);
        return new RegValue(value.status(), value.type(), value.sizeBytes(), value.data());
    }
}
