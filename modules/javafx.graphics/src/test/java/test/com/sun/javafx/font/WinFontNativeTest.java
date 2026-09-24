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

package test.com.sun.javafx.font;

import com.sun.javafx.font.WinFontNativeShim;
import com.sun.javafx.font.WinFontNativeShim.EnumeratedFont;
import com.sun.javafx.font.WinFontNativeShim.Enumeration;
import com.sun.javafx.font.WinFontNativeShim.NonClientMetrics;
import com.sun.javafx.font.WinFontNativeShim.RegEnumValue;
import com.sun.javafx.font.WinFontNativeShim.RegKeyInfo;
import com.sun.javafx.font.WinFontNativeShim.RegValue;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Binding tests for {@code com.sun.javafx.font.WinFontNative}, the FFM facade over the Win32 functions
 * that {@code fontpath.c} used to call through JNI: symbol resolution in the four system libraries, the
 * struct layouts against the Windows SDK headers (and, for {@code NONCLIENTMETRICSW}, against what
 * {@code SystemParametersInfoW} itself accepts as {@code cbSize}), the {@code EnumFontFamiliesExW}
 * upcall including nesting, early stop, exception transport and session closure, the registry reads
 * with the buffer sizes the C used, and the kernel32 directory and locale queries.
 * <p>
 * What the Java built on top of these bindings returns is pinned separately, against the output of
 * the C, by {@link FontEnumerationGoldenTest}.
 * <p>
 * Line numbers into {@code fontpath.c} refer to it at commit {@code 8492cb03b0}
 * ({@code git show 8492cb03b0:modules/javafx.graphics/src/main/native-font/fontpath.c}).
 */
@EnabledOnOs(OS.WINDOWS)
public class WinFontNativeTest {

    /** Every Win32 function {@code fontpath.c} called, as the facade binds them, in binding order. */
    static final List<String> BOUND_SYMBOLS = List.of(
            "kernel32!GetSystemDirectoryW", "kernel32!GetWindowsDirectoryW", "kernel32!GetSystemDefaultLangID",
            "kernel32!GetSystemDefaultLCID", "kernel32!GetLocaleInfoW",
            "user32!SystemParametersInfoW", "user32!GetDesktopWindow", "user32!GetDC", "user32!ReleaseDC",
            "gdi32!GetDeviceCaps", "gdi32!EnumFontFamiliesExW",
            "advapi32!RegOpenKeyExW", "advapi32!RegCloseKey", "advapi32!RegQueryInfoKeyW", "advapi32!RegEnumValueW",
            "advapi32!RegQueryValueExW");

    static final int DEFAULT_CHARSET = 1;
    static final int TRUETYPE_FONTTYPE = 4;
    static final int DEVICE_FONTTYPE = 2;
    static final int ERROR_SUCCESS = 0;
    static final int ERROR_FILE_NOT_FOUND = 2;
    static final int ERROR_MORE_DATA = 234;
    static final int REG_SZ = 1;
    static final int MAX_PATH = 260;
    static final int SPI_GETFONTSMOOTHINGCONTRAST = 0x200C;
    static final int LOCALE_ILANGUAGE_AS_NUMBER = 0x00000001 | 0x20000000;

    static final String CURRENT_VERSION_KEY = "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion";
    static final String FONTS_KEY = CURRENT_VERSION_KEY + "\\Fonts";

    @BeforeAll
    static void bindWin32() {
        WinFontNativeShim.ensureLoaded();
    }

    // ---------------------------------------------------------------------------------------------
    // Symbols and constants
    // ---------------------------------------------------------------------------------------------

    @Test
    public void facadeBindsEveryWin32FunctionFontpathUsedInOrder() {
        assertEquals(BOUND_SYMBOLS, WinFontNativeShim.boundSymbols());
    }

    @Test
    public void everyBoundSymbolResolvesInItsSystemLibrary() {
        for (String symbol : BOUND_SYMBOLS) {
            assertTrue(WinFontNativeShim.resolves(symbol), symbol);
        }
        assertFalse(WinFontNativeShim.resolves("kernel32!NoSuchFunctionForJfx"));
        assertThrows(IllegalArgumentException.class, () -> WinFontNativeShim.resolves("shell32!SHGetFolderPathW"));
    }

    @Test
    public void constantsMatchTheWindowsSdkHeaders() {
        assertEquals(TRUETYPE_FONTTYPE, constant("TRUETYPE_FONTTYPE"));
        assertEquals(DEVICE_FONTTYPE, constant("DEVICE_FONTTYPE"));
        assertEquals(DEFAULT_CHARSET, constant("DEFAULT_CHARSET"));
        assertEquals(32, constant("LF_FACESIZE"));
        assertEquals(64, constant("LF_FULLFACESIZE"));
        assertEquals(90, constant("LOGPIXELSY"));
        assertEquals(0x29, constant("SPI_GETNONCLIENTMETRICS"));
        assertEquals(SPI_GETFONTSMOOTHINGCONTRAST, constant("SPI_GETFONTSMOOTHINGCONTRAST"));
        assertEquals(96, constant("USER_DEFAULT_SCREEN_DPI"));
        assertEquals(MAX_PATH, constant("MAX_PATH"));
        assertEquals(ERROR_SUCCESS, constant("ERROR_SUCCESS"));
        assertEquals(REG_SZ, constant("REG_SZ"));
        assertEquals(0x20019, constant("KEY_READ"));
        assertEquals(LOCALE_ILANGUAGE_AS_NUMBER, constant("LOCALE_ILANGUAGE") | constant("LOCALE_RETURN_NUMBER"));
        // winreg.h: (HKEY)(ULONG_PTR)((LONG)0x8000000n), i.e. sign-extended
        assertEquals(0xFFFFFFFF80000001L, WinFontNativeShim.hkeyCurrentUser().address());
        assertEquals(0xFFFFFFFF80000002L, WinFontNativeShim.hkeyLocalMachine().address());
    }

    // ---------------------------------------------------------------------------------------------
    // Layouts (wingdi.h / winuser.h, x64)
    // ---------------------------------------------------------------------------------------------

    @Test
    public void logFontLayoutMatchesWingdi() {
        assertEquals(92, size("LOGFONTW"));
        assertEquals(0, offset("LOGFONTW", "lfHeight"));
        assertEquals(16, offset("LOGFONTW", "lfWeight"));
        assertEquals(20, offset("LOGFONTW", "lfItalic"));
        assertEquals(23, offset("LOGFONTW", "lfCharSet"));
        assertEquals(27, offset("LOGFONTW", "lfPitchAndFamily"));
        assertEquals(28, offset("LOGFONTW", "lfFaceName"));
    }

    @Test
    public void enumLogFontExLayoutMatchesWingdi() {
        assertEquals(348, size("ENUMLOGFONTEXW"));
        assertEquals(0, offset("ENUMLOGFONTEXW", "elfLogFont"));
        assertEquals(92, offset("ENUMLOGFONTEXW", "elfFullName"));
        assertEquals(220, offset("ENUMLOGFONTEXW", "elfStyle"));
        assertEquals(284, offset("ENUMLOGFONTEXW", "elfScript"));
    }

    @Test
    public void newTextMetricExLayoutMatchesWingdi() {
        assertEquals(76, size("NEWTEXTMETRICW"));
        assertEquals(44, offset("NEWTEXTMETRICW", "tmFirstChar"));
        assertEquals(52, offset("NEWTEXTMETRICW", "tmItalic"));
        assertEquals(56, offset("NEWTEXTMETRICW", "tmCharSet"));
        assertEquals(60, offset("NEWTEXTMETRICW", "ntmFlags"), "three bytes of padding before ntmFlags");
        assertEquals(72, offset("NEWTEXTMETRICW", "ntmAvgWidth"));
        assertEquals(24, size("FONTSIGNATURE"));
        assertEquals(100, size("NEWTEXTMETRICEXW"));
        assertEquals(76, offset("NEWTEXTMETRICEXW", "ntmFontSig"));
    }

    @Test
    public void nonClientMetricsLayoutIsTheOneWindowsAccepts() {
        assertEquals(504, size("NONCLIENTMETRICSW"));
        assertEquals(24, offset("NONCLIENTMETRICSW", "lfCaptionFont"));
        assertEquals(124, offset("NONCLIENTMETRICSW", "lfSmCaptionFont"));
        assertEquals(224, offset("NONCLIENTMETRICSW", "lfMenuFont"));
        assertEquals(316, offset("NONCLIENTMETRICSW", "lfStatusFont"));
        assertEquals(408, offset("NONCLIENTMETRICSW", "lfMessageFont"));
        assertEquals(500, offset("NONCLIENTMETRICSW", "iPaddedBorderWidth"));
        // SystemParametersInfoW refuses SPI_GETNONCLIENTMETRICS for any other cbSize: a live sizeof check.
        NonClientMetrics metrics = WinFontNativeShim.nonClientMetrics();
        assertNotNull(metrics, "SPI_GETNONCLIENTMETRICS refused cbSize = 504");
        assertNotEquals(0, metrics.messageFontHeight(), "lfMessageFont.lfHeight");
        assertFalse(metrics.messageFontFaceName().isEmpty(), "lfMessageFont.lfFaceName");
        assertTrue(metrics.messageFontFaceName().length() < 32, metrics.messageFontFaceName());
    }

    // ---------------------------------------------------------------------------------------------
    // EnumFontFamiliesExW upcall
    // ---------------------------------------------------------------------------------------------

    @Test
    public void enumeratingArialReportsItsFacesUnderTheirOwnFamily() {
        try (Enumeration enumeration = new Enumeration()) {
            List<EnumeratedFont> faces = enumeration.faces("Arial", DEFAULT_CHARSET);
            assertFalse(faces.isEmpty(), "no Arial face enumerated");
            for (EnumeratedFont face : faces) {
                assertEquals("Arial", face.faceName(), face.toString());
                assertTrue(face.fullName().startsWith("Arial"), face.toString());
                assertTrue(face.charSet() >= 0 && face.charSet() <= 255, face.toString());
            }
        }
    }

    @Test
    public void fontTypeArrivesWithEveryCallback() {
        List<Integer> types = new ArrayList<>();
        try (Enumeration enumeration = new Enumeration()) {
            enumeration.enumerate("Arial", DEFAULT_CHARSET, (font, fontType) -> {
                types.add(fontType);
                return 1;
            });
        }
        assertFalse(types.isEmpty());
        for (int type : types) {
            // The filter fontpath.c applied with != on both values; Arial is in the golden, so it passes it.
            assertTrue(type == TRUETYPE_FONTTYPE || type == DEVICE_FONTTYPE, "Arial reported with FontType " + type);
        }
    }

    @Test
    public void enumerationsNestInsideACallbackOnTheSameSession() {
        List<String> nested = new ArrayList<>();
        try (Enumeration enumeration = new Enumeration()) {
            enumeration.enumerate("Arial", DEFAULT_CHARSET, (font, fontType) -> {
                // DifferentFamily: enumerate the full name and read the family GDI files it under.
                enumeration.enumerate(font.fullName(), DEFAULT_CHARSET, (inner, innerType) -> {
                    nested.add(font.fullName() + " -> " + inner.faceName());
                    return 0;
                });
                return 1;
            });
        }
        assertFalse(nested.isEmpty());
        for (String record : nested) {
            assertTrue(record.endsWith(" -> Arial"), record);
        }
    }

    @Test
    public void aCallbackReturningZeroStopsTheEnumeration() {
        int[] calls = new int[1];
        try (Enumeration enumeration = new Enumeration()) {
            enumeration.enumerate("", DEFAULT_CHARSET, (font, fontType) -> {
                calls[0]++;
                return 0;
            });
        }
        assertEquals(1, calls[0]);
    }

    @Test
    public void aCallbackExceptionStopsTheEnumerationAndIsRethrownAfterTheDowncall() {
        IllegalStateException failure = new IllegalStateException("thrown from EnumFontFamExProc");
        int[] calls = new int[1];
        try (Enumeration enumeration = new Enumeration()) {
            IllegalStateException thrown = assertThrows(IllegalStateException.class,
                    () -> enumeration.enumerate("", DEFAULT_CHARSET, (font, fontType) -> {
                        calls[0]++;
                        throw failure;
                    }));
            assertSame(failure, thrown);
            assertEquals(1, calls[0], "the enumeration went on after the exception");
            // The pending slot was cleared by the rethrow: the session is still usable.
            assertFalse(enumeration.faces("Arial", DEFAULT_CHARSET).isEmpty());
        }
    }

    @Test
    public void anErrorFromACallbackIsRethrownAsItself() {
        AssertionError failure = new AssertionError("error from EnumFontFamExProc");
        try (Enumeration enumeration = new Enumeration()) {
            AssertionError thrown = assertThrows(AssertionError.class,
                    () -> enumeration.enumerate("Arial", DEFAULT_CHARSET, (font, fontType) -> {
                        throw failure;
                    }));
            assertSame(failure, thrown);
        }
    }

    @Test
    public void aClosedSessionRefusesToEnumerate() {
        Enumeration enumeration = new Enumeration();
        enumeration.close();
        assertThrows(IllegalStateException.class, () -> enumeration.faces("Arial", DEFAULT_CHARSET));
    }

    @Test
    public void faceNamesAreBoundedByLogFontBeforeTheDowncall() {
        try (Enumeration enumeration = new Enumeration()) {
            assertThrows(IllegalArgumentException.class, () -> enumeration.faces("x".repeat(32), DEFAULT_CHARSET));
            assertTrue(enumeration.faces("x".repeat(31), DEFAULT_CHARSET).isEmpty(),
                    "31 characters fit lfFaceName and name no installed font");
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Registry
    // ---------------------------------------------------------------------------------------------

    @Test
    public void registryFontsKeyEnumeratesEveryValueWithinTheReportedCapacities() {
        MemorySegment key = WinFontNativeShim.regOpenKeyRead(WinFontNativeShim.hkeyLocalMachine(), FONTS_KEY);
        assertNotNull(key, "HKLM Fonts key");
        try {
            RegKeyInfo info = WinFontNativeShim.regQueryInfoKey(key);
            assertNotNull(info);
            assertTrue(info.values() > 10, "values: " + info.values());
            assertTrue(info.maxValueNameChars() > 0);
            assertTrue(info.maxValueDataBytes() > 0);
            int nameCapacity = info.maxValueNameChars() + 1;
            int regSz = 0;
            for (int i = 0; i < info.values(); i++) {
                RegEnumValue value = WinFontNativeShim.regEnumValue(key, i, nameCapacity, info.maxValueDataBytes());
                assertEquals(ERROR_SUCCESS, value.status(), "value " + i);
                assertFalse(value.name().isEmpty(), "value " + i);
                assertTrue(value.name().length() <= info.maxValueNameChars(), value.name());
                assertTrue(value.data().length * 2 <= info.maxValueDataBytes(), value.name());
                if (value.type() == REG_SZ) {
                    regSz++;
                }
            }
            assertTrue(regSz > 10, "REG_SZ values: " + regSz);
            RegEnumValue past = WinFontNativeShim.regEnumValue(key, info.values(), nameCapacity,
                    info.maxValueDataBytes());
            assertNotEquals(ERROR_SUCCESS, past.status(), "index past the last value");
            assertNull(past.name());
            assertNull(past.data());
        } finally {
            assertEquals(ERROR_SUCCESS, WinFontNativeShim.regCloseKey(key));
        }
    }

    @Test
    public void registryValueQueriesReportSizeTypeAndMisses() {
        MemorySegment key = WinFontNativeShim.regOpenKeyRead(WinFontNativeShim.hkeyLocalMachine(), CURRENT_VERSION_KEY);
        assertNotNull(key);
        try {
            RegValue size = WinFontNativeShim.regQueryValue(key, "ProductName", -1);
            assertEquals(ERROR_SUCCESS, size.status());
            assertEquals(REG_SZ, size.type());
            assertTrue(size.sizeBytes() > 2, "size: " + size.sizeBytes());
            assertNull(size.data(), "a size query reads nothing");

            RegValue value = WinFontNativeShim.regQueryValue(key, "ProductName", size.sizeBytes());
            assertEquals(ERROR_SUCCESS, value.status());
            assertEquals(size.sizeBytes(), value.sizeBytes());
            assertEquals(size.sizeBytes() / 2, value.data().length);
            String product = new String(value.data());
            assertTrue(product.startsWith("Windows"), product);
            assertEquals(0, product.charAt(product.length() - 1),
                    "the REG_SZ terminator is read back, as NewString kept it");

            RegValue truncated = WinFontNativeShim.regQueryValue(key, "ProductName", 2);
            assertEquals(ERROR_MORE_DATA, truncated.status());
            assertEquals(size.sizeBytes(), truncated.sizeBytes(), "the size needed is reported");
            assertNull(truncated.data());

            RegValue missing = WinFontNativeShim.regQueryValue(key, "NoSuchValueForJfx", -1);
            assertEquals(ERROR_FILE_NOT_FOUND, missing.status());
        } finally {
            WinFontNativeShim.regCloseKey(key);
        }
        assertNull(WinFontNativeShim.regOpenKeyRead(WinFontNativeShim.hkeyCurrentUser(), "SOFTWARE\\NoSuchKeyForJfx"));
    }

    /**
     * A key or value name crosses as its UTF-16 code units plus a NUL, which is what {@code GetStringChars}
     * handed {@code RegQueryValueExW} for {@code regReadFontLink}'s font name ({@code fontpath.c:815}). Windows
     * does not require well-formed UTF-16 in a name, so an unpaired surrogate must reach it as the unit it is:
     * a charset encoder writes U+FFFD in its place, and the query then names a different value.
     */
    @Test
    public void registryNamesCrossAsTheirExactCodeUnitsAndOneNul() {
        char[] name = {
            '\uDC00',           // a lone low surrogate, first
            'F', 'o', 'n', 't',
            '\uD83D', '\uDE00', // a valid pair, U+1F600
            '\uD800', 'x',      // a lone high surrogate followed by an ASCII char
            '\uDBFF'            // a lone high surrogate, last
        };
        char[] expected = Arrays.copyOf(name, name.length + 1);
        char[] units = WinFontNativeShim.wideCodeUnits(new String(name));
        // Hex first: an unpaired surrogate cannot be printed, and a failure message must show which unit changed.
        assertEquals(hex(expected), hex(units), "the WCHARs written, the terminator included");
        assertArrayEquals(expected, units);
        assertEquals(new String(name), new String(units, 0, units.length - 1), "the round trip back to a String");

        assertArrayEquals(new char[] {'a', '\0', 'b', '\0'}, WinFontNativeShim.wideCodeUnits("a\0b"),
                "an embedded U+0000 is copied as GetStringChars copied it");
        assertArrayEquals(new char[] {'\0'}, WinFontNativeShim.wideCodeUnits(""));
    }

    // ---------------------------------------------------------------------------------------------
    // kernel32 / user32 scalars
    // ---------------------------------------------------------------------------------------------

    @Test
    public void directoriesFollowTheTwoStepAndFixedBufferProtocols() {
        String system = WinFontNativeShim.systemDirectory();
        String windows = WinFontNativeShim.windowsDirectory();
        assertNotNull(system);
        assertNotNull(windows);
        assertTrue(system.toLowerCase(Locale.ROOT).endsWith("\\system32"), system);
        assertTrue(system.toLowerCase(Locale.ROOT).startsWith(windows.toLowerCase(Locale.ROOT) + "\\"),
                system + " under " + windows);
        assertEquals(windows, WinFontNativeShim.windowsDirectory(MAX_PATH));
        assertEquals(windows, WinFontNativeShim.windowsDirectory(windows.length() + 1), "exactly fits with the NUL");
        assertNull(WinFontNativeShim.windowsDirectory(windows.length()),
                "no room for the NUL: the size needed comes back");
        assertNull(WinFontNativeShim.windowsDirectory(1));
    }

    @Test
    public void localeQueriesAgreeWithEachOther() {
        int langID = WinFontNativeShim.systemDefaultLangID();
        int lcid = WinFontNativeShim.systemDefaultLCID();
        assertTrue(langID > 0 && langID <= 0xFFFF, Integer.toHexString(langID));
        assertEquals(langID, lcid & 0xFFFF, "LANGID is the low word of the LCID");
        assertEquals(langID, WinFontNativeShim.localeInfoNumber(lcid, LOCALE_ILANGUAGE_AS_NUMBER), "LOCALE_ILANGUAGE");
        assertEquals(0, WinFontNativeShim.localeInfoNumber(0x7FFFFFFF, LOCALE_ILANGUAGE_AS_NUMBER),
                "a failed query leaves 0 where the C had an uninitialised DWORD");
    }

    @Test
    public void systemParametersReturnTheValueOrTheFallback() {
        int contrast = WinFontNativeShim.systemParametersInfoUInt(SPI_GETFONTSMOOTHINGCONTRAST, 1300);
        assertTrue(contrast >= 1000 && contrast <= 2200, "SPI_GETFONTSMOOTHINGCONTRAST " + contrast);
        assertEquals(-7, WinFontNativeShim.systemParametersInfoUInt(0x7FFF, -7), "an unknown action: the fallback");
        assertTrue(WinFontNativeShim.desktopLogPixelsY() >= 96, "LOGPIXELSY of the desktop");
    }

    // ---------------------------------------------------------------------------------------------

    private static int constant(String name) {
        return WinFontNativeShim.constant(name);
    }

    /** {@code units} as space-separated four-digit hex, so that an unpaired surrogate survives into a message. */
    private static String hex(char[] units) {
        StringBuilder text = new StringBuilder();
        for (char unit : units) {
            text.append(text.isEmpty() ? "" : " ").append(String.format("%04X", (int) unit));
        }
        return text.toString();
    }

    private static long size(String struct) {
        return WinFontNativeShim.layoutByteSize(struct);
    }

    private static long offset(String struct, String field) {
        return WinFontNativeShim.layoutOffset(struct, field);
    }
}
