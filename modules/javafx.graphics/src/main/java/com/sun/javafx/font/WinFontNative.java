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

import com.sun.javafx.PlatformUtil;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

/**
 * The Win32 calls behind the Windows font enumeration of {@link PrismFontFactory} and
 * {@code DWFactory}, bound directly from Java: what {@code fontpath.c} used to wrap in JNI. It owns
 * the {@link Linker}, the {@link SymbolLookup}s of the four system libraries involved (kernel32,
 * user32, gdi32, advapi32), every downcall handle, the four struct layouts and the one upcall stub of
 * a font enumeration, and it is the only class in this package that uses a restricted
 * {@code java.lang.foreign} method. {@link WinFontPath} implements the eight former natives over the
 * typed static wrappers below and never touches native memory itself.
 * <p>
 * Each wrapper is one Win32 function, called with the arguments {@code fontpath.c} passed - buffer
 * sizes included, so that the same inputs meet the same limits. Strings cross as NUL-terminated
 * UTF-16 ({@code LPWSTR}) code units, in both directions and never through a charset, as JNI moved
 * them; fixed-size {@code WCHAR} arrays are read up to their first NUL or their
 * capacity, as {@code wcslen} read them. Nothing in {@code fontpath.c} consulted {@code GetLastError},
 * so no call captures it. {@code GetVersionEx} is not bound: it only chose the Windows XP size of
 * {@code NONCLIENTMETRICSW}, which no JDK that can run this code will ever see.
 *
 * <h2>Font enumeration</h2>
 * {@code EnumFontFamiliesExW} calls back synchronously, on the calling thread, once per matching
 * font, and {@code fontpath.c} enumerated recursively: every family enumerates its faces, and every
 * face enumerates once more to check its true family. A {@link FontEnumeration} is one such session:
 * a confined arena holding one upcall stub bound to the session, a stack of Java callbacks for the
 * nesting, and a pending-exception slot. The stub's target decodes the {@code ENUMLOGFONTEXW} into an
 * {@link EnumLogFont}, hands it to the innermost callback and returns its verdict (1 continue, 0
 * stop). A callback that throws stops the enumeration - the JNI code returned 0 whenever an exception
 * was pending - and the exception is rethrown from {@link FontEnumeration#enumerate} once
 * {@code EnumFontFamiliesExW} has returned, so nothing ever crosses the native frame. The stub is
 * closed with the session; GDI holds no pointer to it afterwards.
 * <p>
 * Line numbers into {@code fontpath.c} refer to it at commit {@code 8492cb03b0}
 * ({@code git show 8492cb03b0:modules/javafx.graphics/src/main/native-font/fontpath.c}).
 */
final class WinFontNative {

    /* wingdi.h: the FontType flags EnumFontFamExProc receives, and the LOGFONT limits. */
    static final int TRUETYPE_FONTTYPE = 0x0004;
    static final int DEVICE_FONTTYPE = 0x0002;
    static final int DEFAULT_CHARSET = 1;
    static final int LF_FACESIZE = 32;
    static final int LF_FULLFACESIZE = 64;
    static final int LOGPIXELSY = 90;

    /* winuser.h */
    static final int SPI_GETNONCLIENTMETRICS = 0x0029;
    static final int SPI_GETFONTSMOOTHINGCONTRAST = 0x200C;
    static final int USER_DEFAULT_SCREEN_DPI = 96;

    /* winnt.h / winreg.h / winerror.h */
    static final int MAX_PATH = 260;
    static final int ERROR_SUCCESS = 0;
    static final int REG_SZ = 1;
    static final int KEY_READ = 0x20019;

    /* winnls.h */
    static final int LOCALE_ILANGUAGE = 0x00000001;
    static final int LOCALE_RETURN_NUMBER = 0x20000000;

    /**
     * The predefined root keys as winreg.h defines them: {@code (HKEY)(ULONG_PTR)((LONG)0x80000001)},
     * that is, sign-extended to 64 bits.
     */
    static final MemorySegment HKEY_CURRENT_USER = MemorySegment.ofAddress(0xFFFFFFFF80000001L);
    static final MemorySegment HKEY_LOCAL_MACHINE = MemorySegment.ofAddress(0xFFFFFFFF80000002L);

    /** {@code LOGFONTW} (wingdi.h): 92 bytes. */
    static final StructLayout LOGFONTW_LAYOUT = MemoryLayout.structLayout(
            JAVA_INT.withName("lfHeight"),
            JAVA_INT.withName("lfWidth"),
            JAVA_INT.withName("lfEscapement"),
            JAVA_INT.withName("lfOrientation"),
            JAVA_INT.withName("lfWeight"),
            JAVA_BYTE.withName("lfItalic"),
            JAVA_BYTE.withName("lfUnderline"),
            JAVA_BYTE.withName("lfStrikeOut"),
            JAVA_BYTE.withName("lfCharSet"),
            JAVA_BYTE.withName("lfOutPrecision"),
            JAVA_BYTE.withName("lfClipPrecision"),
            JAVA_BYTE.withName("lfQuality"),
            JAVA_BYTE.withName("lfPitchAndFamily"),
            MemoryLayout.sequenceLayout(LF_FACESIZE, JAVA_CHAR).withName("lfFaceName"));

    /** {@code ENUMLOGFONTEXW} (wingdi.h): 348 bytes; what {@code EnumFontFamExProc} receives first. */
    static final StructLayout ENUMLOGFONTEXW_LAYOUT = MemoryLayout.structLayout(
            LOGFONTW_LAYOUT.withName("elfLogFont"),
            MemoryLayout.sequenceLayout(LF_FULLFACESIZE, JAVA_CHAR).withName("elfFullName"),
            MemoryLayout.sequenceLayout(LF_FACESIZE, JAVA_CHAR).withName("elfStyle"),
            MemoryLayout.sequenceLayout(LF_FACESIZE, JAVA_CHAR).withName("elfScript"));

    /** {@code NEWTEXTMETRICW} (wingdi.h): 76 bytes, three bytes of padding before {@code ntmFlags}. */
    static final StructLayout NEWTEXTMETRICW_LAYOUT = MemoryLayout.structLayout(
            JAVA_INT.withName("tmHeight"),
            JAVA_INT.withName("tmAscent"),
            JAVA_INT.withName("tmDescent"),
            JAVA_INT.withName("tmInternalLeading"),
            JAVA_INT.withName("tmExternalLeading"),
            JAVA_INT.withName("tmAveCharWidth"),
            JAVA_INT.withName("tmMaxCharWidth"),
            JAVA_INT.withName("tmWeight"),
            JAVA_INT.withName("tmOverhang"),
            JAVA_INT.withName("tmDigitizedAspectX"),
            JAVA_INT.withName("tmDigitizedAspectY"),
            JAVA_CHAR.withName("tmFirstChar"),
            JAVA_CHAR.withName("tmLastChar"),
            JAVA_CHAR.withName("tmDefaultChar"),
            JAVA_CHAR.withName("tmBreakChar"),
            JAVA_BYTE.withName("tmItalic"),
            JAVA_BYTE.withName("tmUnderlined"),
            JAVA_BYTE.withName("tmStruckOut"),
            JAVA_BYTE.withName("tmPitchAndFamily"),
            JAVA_BYTE.withName("tmCharSet"),
            MemoryLayout.paddingLayout(3),
            JAVA_INT.withName("ntmFlags"),
            JAVA_INT.withName("ntmSizeEM"),
            JAVA_INT.withName("ntmCellHeight"),
            JAVA_INT.withName("ntmAvgWidth"));

    /** {@code FONTSIGNATURE} (wingdi.h): 24 bytes. */
    static final StructLayout FONTSIGNATURE_LAYOUT = MemoryLayout.structLayout(
            MemoryLayout.sequenceLayout(4, JAVA_INT).withName("fsUsb"),
            MemoryLayout.sequenceLayout(2, JAVA_INT).withName("fsCsb"));

    /**
     * {@code NEWTEXTMETRICEXW} (wingdi.h): 100 bytes; what {@code EnumFontFamExProc} receives second.
     * {@code fontpath.c} never read it; the layout documents the callback's contract.
     */
    static final StructLayout NEWTEXTMETRICEXW_LAYOUT = MemoryLayout.structLayout(
            NEWTEXTMETRICW_LAYOUT.withName("ntmTm"),
            FONTSIGNATURE_LAYOUT.withName("ntmFontSig"));

    /**
     * {@code NONCLIENTMETRICSW} (winuser.h, {@code WINVER >= 0x0600}): 504 bytes including
     * {@code iPaddedBorderWidth}, the size {@code fontpath.c} passed as {@code cbSize} on every Windows
     * release since Vista.
     */
    static final StructLayout NONCLIENTMETRICSW_LAYOUT = MemoryLayout.structLayout(
            JAVA_INT.withName("cbSize"),
            JAVA_INT.withName("iBorderWidth"),
            JAVA_INT.withName("iScrollWidth"),
            JAVA_INT.withName("iScrollHeight"),
            JAVA_INT.withName("iCaptionWidth"),
            JAVA_INT.withName("iCaptionHeight"),
            LOGFONTW_LAYOUT.withName("lfCaptionFont"),
            JAVA_INT.withName("iSmCaptionWidth"),
            JAVA_INT.withName("iSmCaptionHeight"),
            LOGFONTW_LAYOUT.withName("lfSmCaptionFont"),
            JAVA_INT.withName("iMenuWidth"),
            JAVA_INT.withName("iMenuHeight"),
            LOGFONTW_LAYOUT.withName("lfMenuFont"),
            LOGFONTW_LAYOUT.withName("lfStatusFont"),
            LOGFONTW_LAYOUT.withName("lfMessageFont"),
            JAVA_INT.withName("iPaddedBorderWidth"));

    private static final long LF_HEIGHT_OFFSET = offset(LOGFONTW_LAYOUT, "lfHeight");
    private static final long LF_CHARSET_OFFSET = offset(LOGFONTW_LAYOUT, "lfCharSet");
    private static final long LF_FACE_NAME_OFFSET = offset(LOGFONTW_LAYOUT, "lfFaceName");
    private static final long ELF_FULL_NAME_OFFSET = offset(ENUMLOGFONTEXW_LAYOUT, "elfFullName");
    private static final long NCM_CB_SIZE_OFFSET = offset(NONCLIENTMETRICSW_LAYOUT, "cbSize");
    private static final long NCM_MESSAGE_FONT_OFFSET = offset(NONCLIENTMETRICSW_LAYOUT, "lfMessageFont");

    /** {@code int CALLBACK EnumFontFamExProc(const LOGFONTW*, const TEXTMETRICW*, DWORD FontType, LPARAM)}. */
    private static final FunctionDescriptor ENUM_FONT_FAM_EX_PROC_FD =
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, JAVA_LONG);

    static {
        if (!PlatformUtil.isWindows()) {
            throw new UnsupportedOperationException("WinFontNative binds Win32 and is only usable on Windows");
        }
    }

    private static final Linker LINKER = Linker.nativeLinker();
    private static final List<String> BOUND_SYMBOLS = new ArrayList<>();

    /** A system DLL the JDK does not load for us, looked up once for the life of the process. */
    private record SystemLibrary(String name, SymbolLookup lookup) {
        SystemLibrary(String name) {
            this(name, systemLibrary(name + ".dll"));
        }
    }

    private static final SystemLibrary KERNEL32 = new SystemLibrary("kernel32");
    private static final SystemLibrary USER32 = new SystemLibrary("user32");
    private static final SystemLibrary GDI32 = new SystemLibrary("gdi32");
    private static final SystemLibrary ADVAPI32 = new SystemLibrary("advapi32");

    private static final MethodHandle GET_SYSTEM_DIRECTORY_W = bind(KERNEL32, "GetSystemDirectoryW",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));
    private static final MethodHandle GET_WINDOWS_DIRECTORY_W = bind(KERNEL32, "GetWindowsDirectoryW",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));
    private static final MethodHandle GET_SYSTEM_DEFAULT_LANG_ID = bind(KERNEL32, "GetSystemDefaultLangID",
            FunctionDescriptor.of(JAVA_SHORT));
    private static final MethodHandle GET_SYSTEM_DEFAULT_LCID = bind(KERNEL32, "GetSystemDefaultLCID",
            FunctionDescriptor.of(JAVA_INT));
    private static final MethodHandle GET_LOCALE_INFO_W = bind(KERNEL32, "GetLocaleInfoW",
            FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT));

    private static final MethodHandle SYSTEM_PARAMETERS_INFO_W = bind(USER32, "SystemParametersInfoW",
            FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT));
    private static final MethodHandle GET_DESKTOP_WINDOW = bind(USER32, "GetDesktopWindow",
            FunctionDescriptor.of(ADDRESS));
    private static final MethodHandle GET_DC = bind(USER32, "GetDC",
            FunctionDescriptor.of(ADDRESS, ADDRESS));
    private static final MethodHandle RELEASE_DC = bind(USER32, "ReleaseDC",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));

    private static final MethodHandle GET_DEVICE_CAPS = bind(GDI32, "GetDeviceCaps",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));
    private static final MethodHandle ENUM_FONT_FAMILIES_EX_W = bind(GDI32, "EnumFontFamiliesExW",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, JAVA_LONG, JAVA_INT));

    private static final MethodHandle REG_OPEN_KEY_EX_W = bind(ADVAPI32, "RegOpenKeyExW",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS));
    private static final MethodHandle REG_CLOSE_KEY = bind(ADVAPI32, "RegCloseKey",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));
    private static final MethodHandle REG_QUERY_INFO_KEY_W = bind(ADVAPI32, "RegQueryInfoKeyW",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS,
                    ADDRESS, ADDRESS, ADDRESS, ADDRESS));
    private static final MethodHandle REG_ENUM_VALUE_W = bind(ADVAPI32, "RegEnumValueW",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS));
    private static final MethodHandle REG_QUERY_VALUE_EX_W = bind(ADVAPI32, "RegQueryValueExW",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS));

    private WinFontNative() {
    }

    @SuppressWarnings("restricted")
    private static SymbolLookup systemLibrary(String fileName) {
        return SymbolLookup.libraryLookup(fileName, Arena.global());
    }

    /**
     * Resolves {@code name} in {@code library} and binds it as a plain downcall. None of these calls
     * is {@link Linker.Option#critical critical}: {@code EnumFontFamiliesExW} calls back into Java,
     * and the rest are cold.
     *
     * @throws UnsatisfiedLinkError if the library does not export {@code name}
     */
    @SuppressWarnings("restricted")
    private static MethodHandle bind(SystemLibrary library, String name, FunctionDescriptor descriptor) {
        MemorySegment symbol = library.lookup().find(name).orElseThrow(
                () -> new UnsatisfiedLinkError("missing Win32 symbol: " + name + " in " + library.name() + ".dll"));
        BOUND_SYMBOLS.add(library.name() + "!" + name);
        return LINKER.downcallHandle(symbol, descriptor);
    }

    @SuppressWarnings("restricted")
    private static MemorySegment upcallStub(MethodHandle target, FunctionDescriptor descriptor, Arena arena) {
        return LINKER.upcallStub(target, descriptor, arena);
    }

    /** Gives a zero-length segment handed to an upcall the bound GDI promises; the only reinterpret here. */
    @SuppressWarnings("restricted")
    private static MemorySegment bounded(MemorySegment segment, long byteSize) {
        return segment.reinterpret(byteSize);
    }

    private static long offset(StructLayout layout, String field) {
        return layout.byteOffset(PathElement.groupElement(field));
    }

    private static Error unexpected(Throwable t) {
        if (t instanceof Error e) {
            return e;
        }
        if (t instanceof RuntimeException e) {
            throw e;
        }
        // Only linkage failures can land here: Win32 cannot throw.
        return new AssertionError(t);
    }

    /**
     * A NUL-terminated {@code LPCWSTR} copy of {@code text}: its UTF-16 code units, unit for unit, and a
     * NUL - what {@code GetStringChars} handed {@code RegQueryValueExW} in {@code regReadFontLink}
     * ({@code fontpath.c:815}; HotSpot's copy carries the terminator). No charset:
     * {@code allocateFrom(text, UTF_16LE)} runs an encoder that replaces an unpaired surrogate with
     * U+FFFD, so a font name carrying one would query a different registry value than the C did. An
     * embedded U+0000 is copied as it was, and Win32 stops reading there in both paths.
     */
    private static MemorySegment wide(Arena arena, String text) {
        char[] chars = text.toCharArray();
        MemorySegment segment = arena.allocate(JAVA_CHAR, chars.length + 1L);
        MemorySegment.copy(chars, 0, segment, JAVA_CHAR, 0, chars.length);
        segment.setAtIndex(JAVA_CHAR, chars.length, '\0');
        return segment;
    }

    /** Exactly {@code count} {@code WCHAR}s from {@code offset}. */
    private static char[] chars(MemorySegment segment, long offset, int count) {
        char[] chars = new char[count];
        MemorySegment.copy(segment, JAVA_CHAR, offset, chars, 0, count);
        return chars;
    }

    /** The {@code WCHAR[maxChars]} at {@code offset} read as {@code wcslen} would: up to its first NUL. */
    private static String wideString(MemorySegment segment, long offset, int maxChars) {
        char[] chars = chars(segment, offset, maxChars);
        int length = 0;
        while (length < maxChars && chars[length] != 0) {
            length++;
        }
        return new String(chars, 0, length);
    }

    /* ---------------------------------------------------------------------------------------------
     * kernel32
     * ------------------------------------------------------------------------------------------- */

    /**
     * {@code GetSystemDirectoryW} as {@code fontpath.c} called it (L100-119): once with size 0 for the
     * required length, then into a buffer of that length. {@code null} when either call reports
     * failure, or the second does not fit - the cases the C returned {@code NULL} for.
     */
    static String systemDirectory() {
        return directory(GET_SYSTEM_DIRECTORY_W);
    }

    /** {@code GetWindowsDirectoryW}, the same two-step way ({@code fontpath.c} L144-157). */
    static String windowsDirectory() {
        return directory(GET_WINDOWS_DIRECTORY_W);
    }

    private static String directory(MethodHandle api) {
        try (Arena scratch = Arena.ofConfined()) {
            // fontpath.c L85-92: a one-char dummy in case the API writes despite a count of 0.
            MemorySegment probe = scratch.allocate(JAVA_CHAR);
            int needed;
            try {
                needed = (int) api.invokeExact(probe, 0);
            } catch (Throwable t) {
                throw unexpected(t);
            }
            if (needed == 0) {
                return null;
            }
            MemorySegment buffer = scratch.allocate(JAVA_CHAR, needed);
            int written;
            try {
                written = (int) api.invokeExact(buffer, needed);
            } catch (Throwable t) {
                throw unexpected(t);
            }
            if (written == 0 || written >= needed) {
                return null;
            }
            return new String(chars(buffer, 0, written));
        }
    }

    /**
     * {@code GetWindowsDirectoryW(buffer, capacity)} into a fixed buffer ({@code fontpath.c} L927-928):
     * the path when the call returned a length below {@code capacity}, {@code null} when it returned 0
     * or the size it would have needed.
     */
    static String windowsDirectory(int capacity) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment buffer = scratch.allocate(JAVA_CHAR, capacity);
            int written;
            try {
                written = (int) GET_WINDOWS_DIRECTORY_W.invokeExact(buffer, capacity);
            } catch (Throwable t) {
                throw unexpected(t);
            }
            if (written == 0 || written >= capacity) {
                return null;
            }
            return new String(chars(buffer, 0, written));
        }
    }

    /** {@code GetSystemDefaultLangID()}: the 16-bit {@code LANGID}, zero-extended. */
    static int systemDefaultLangID() {
        try {
            return Short.toUnsignedInt((short) GET_SYSTEM_DEFAULT_LANG_ID.invokeExact());
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code GetSystemDefaultLCID()}. */
    static int systemDefaultLCID() {
        try {
            return (int) GET_SYSTEM_DEFAULT_LCID.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /**
     * {@code GetLocaleInfoW(lcid, lcType, &dword, 4)} for an {@code LCTYPE} that carries
     * {@link #LOCALE_RETURN_NUMBER}: {@code fontpath.c} L1023-1026 passed {@code cchData} as
     * {@code sizeof(DWORD) / sizeof(char)} and read the {@code DWORD} back, ignoring the return value;
     * so does this, and a call that wrote nothing leaves 0 where the C had an uninitialised local.
     */
    static int localeInfoNumber(int lcid, int lcType) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment value = scratch.allocate(8, 4);
            try {
                int ignored = (int) GET_LOCALE_INFO_W.invokeExact(lcid, lcType, value, 4);
            } catch (Throwable t) {
                throw unexpected(t);
            }
            return value.get(JAVA_INT, 0);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * user32 / gdi32
     * ------------------------------------------------------------------------------------------- */

    /**
     * {@code SystemParametersInfoW(action, 0, &uint, 0)} for an action that fills a {@code UINT}
     * ({@code fontpath.c} L982-983): the value, or {@code fallback} when the call returned
     * {@code FALSE}.
     */
    static int systemParametersInfoUInt(int action, int fallback) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment value = scratch.allocate(JAVA_INT);
            int ok;
            try {
                ok = (int) SYSTEM_PARAMETERS_INFO_W.invokeExact(action, 0, value, 0);
            } catch (Throwable t) {
                throw unexpected(t);
            }
            return ok != 0 ? value.get(JAVA_INT, 0) : fallback;
        }
    }

    /** The two {@code lfMessageFont} fields {@code fontpath.c} read from a {@code NONCLIENTMETRICSW}. */
    record NonClientMetrics(int messageFontHeight, String messageFontFaceName) {
    }

    /**
     * {@code getSysParams} ({@code fontpath.c} L944-968): {@code SystemParametersInfoW}
     * {@code (SPI_GETNONCLIENTMETRICS, sizeof(NONCLIENTMETRICSW), &ncm, FALSE)} on a zeroed struct
     * whose {@code cbSize} is that size. {@code null} when the call returned {@code FALSE}.
     */
    static NonClientMetrics nonClientMetrics() {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment metrics = scratch.allocate(NONCLIENTMETRICSW_LAYOUT);
            int cbSize = (int) NONCLIENTMETRICSW_LAYOUT.byteSize();
            metrics.set(JAVA_INT, NCM_CB_SIZE_OFFSET, cbSize);
            int ok;
            try {
                ok = (int) SYSTEM_PARAMETERS_INFO_W.invokeExact(SPI_GETNONCLIENTMETRICS, cbSize, metrics, 0);
            } catch (Throwable t) {
                throw unexpected(t);
            }
            if (ok == 0) {
                return null;
            }
            return new NonClientMetrics(
                    metrics.get(JAVA_INT, NCM_MESSAGE_FONT_OFFSET + LF_HEIGHT_OFFSET),
                    wideString(metrics, NCM_MESSAGE_FONT_OFFSET + LF_FACE_NAME_OFFSET, LF_FACESIZE));
        }
    }

    /** {@code GetDesktopWindow()}. */
    static MemorySegment getDesktopWindow() {
        try {
            return (MemorySegment) GET_DESKTOP_WINDOW.invokeExact();
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code GetDC(hwnd)}: the {@code HDC}, {@link MemorySegment#NULL} when GDI has none. */
    static MemorySegment getDC(MemorySegment hwnd) {
        try {
            return (MemorySegment) GET_DC.invokeExact(hwnd);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code ReleaseDC(hwnd, hdc)}. */
    static int releaseDC(MemorySegment hwnd, MemorySegment hdc) {
        try {
            return (int) RELEASE_DC.invokeExact(hwnd, hdc);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** {@code GetDeviceCaps(hdc, index)}; 0 for a {@code NULL} device context, as GDI answers. */
    static int getDeviceCaps(MemorySegment hdc, int index) {
        try {
            return (int) GET_DEVICE_CAPS.invokeExact(hdc, index);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * gdi32: EnumFontFamiliesExW
     * ------------------------------------------------------------------------------------------- */

    /** The fields of an {@code ENUMLOGFONTEXW} that {@code fontpath.c} read, decoded once per callback. */
    record EnumLogFont(String faceName, int charSet, String fullName) {
    }

    /** The Java side of {@code EnumFontFamExProc}: return 1 to continue, 0 to stop. */
    interface FontEnumProc {
        int accept(EnumLogFont font, int fontType);
    }

    /**
     * One recursive font enumeration: the upcall stub, in a confined arena, that every
     * {@code EnumFontFamiliesExW} of the session passes as its callback, the stack of Java callbacks
     * the nesting needs, and the slot that carries a callback's exception past the native frame. Used
     * on the thread that created it, exactly as long as the enumeration lasts.
     */
    static final class FontEnumeration implements AutoCloseable {

        private static final MethodHandle ENUM_TARGET;

        static {
            try {
                ENUM_TARGET = MethodHandles.lookup().findVirtual(FontEnumeration.class, "onEnumFont",
                        MethodType.methodType(int.class, MemorySegment.class, MemorySegment.class, int.class,
                                long.class));
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        private final Arena arena = Arena.ofConfined();
        private final MemorySegment stub;
        private final Deque<FontEnumProc> procs = new ArrayDeque<>();
        private Throwable pending;

        FontEnumeration() {
            stub = upcallStub(ENUM_TARGET.bindTo(this), ENUM_FONT_FAM_EX_PROC_FD, arena);
        }

        /**
         * {@code EnumFontFamiliesExW(hdc, &lf, proc, 0, 0)} with a zeroed {@code LOGFONTW} carrying only
         * {@code lfCharSet} and {@code lfFaceName}, the way every call in {@code fontpath.c} was made.
         * May be called from within {@code proc} of an enclosing enumeration on the same thread.
         *
         * @param faceName the family to enumerate, or {@code ""} for every family; at most
         *        {@link #LF_FACESIZE} - 1 chars, as the C {@code wcscpy} into the struct required
         * @return what {@code EnumFontFamiliesExW} returned; {@code fontpath.c} ignored it
         * @throws RuntimeException the exception {@code proc} threw, once the enumeration it stopped
         *         has returned
         * @throws IllegalStateException if the session is closed
         */
        int enumerate(MemorySegment hdc, String faceName, int charSet, FontEnumProc proc) {
            if (faceName.length() >= LF_FACESIZE) {
                throw new IllegalArgumentException("face name does not fit LOGFONTW.lfFaceName: " + faceName);
            }
            procs.push(proc);
            int result;
            try (Arena scratch = Arena.ofConfined()) {
                MemorySegment logFont = scratch.allocate(LOGFONTW_LAYOUT);
                logFont.set(JAVA_BYTE, LF_CHARSET_OFFSET, (byte) charSet);
                MemorySegment.copy(faceName.toCharArray(), 0, logFont, JAVA_CHAR, LF_FACE_NAME_OFFSET,
                        faceName.length());
                result = (int) ENUM_FONT_FAMILIES_EX_W.invokeExact(hdc, logFont, stub, 0L, 0);
            } catch (Throwable t) {
                throw unexpected(t);
            } finally {
                procs.pop();
            }
            Throwable failure = pending;
            if (failure != null) {
                pending = null;
                throw rethrow(failure);
            }
            return result;
        }

        /**
         * The upcall target. Returns 0 at once while an exception is pending, as the C callbacks did
         * on {@code ExceptionCheck}; otherwise decodes the struct and defers to the innermost callback.
         * Never lets a {@link Throwable} escape into GDI.
         */
        private int onEnumFont(MemorySegment logFont, MemorySegment textMetric, int fontType, long lParam) {
            if (pending != null) {
                return 0;
            }
            try {
                MemorySegment font = bounded(logFont, ENUMLOGFONTEXW_LAYOUT.byteSize());
                EnumLogFont decoded = new EnumLogFont(
                        wideString(font, LF_FACE_NAME_OFFSET, LF_FACESIZE),
                        Byte.toUnsignedInt(font.get(JAVA_BYTE, LF_CHARSET_OFFSET)),
                        wideString(font, ELF_FULL_NAME_OFFSET, LF_FULLFACESIZE));
                return procs.peek().accept(decoded, fontType);
            } catch (Throwable t) {
                pending = t;
                return 0;
            }
        }

        private static RuntimeException rethrow(Throwable failure) {
            if (failure instanceof RuntimeException e) {
                return e;
            }
            if (failure instanceof Error e) {
                throw e;
            }
            return new IllegalStateException(failure);
        }

        @Override
        public void close() {
            arena.close();
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * advapi32
     * ------------------------------------------------------------------------------------------- */

    /** {@code RegOpenKeyExW(root, subKey, 0, KEY_READ, &key)}: the key, or {@code null} on any status but success. */
    static MemorySegment regOpenKeyRead(MemorySegment root, String subKey) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment result = scratch.allocate(ADDRESS);
            int status;
            try {
                status = (int) REG_OPEN_KEY_EX_W.invokeExact(root, wide(scratch, subKey), 0, KEY_READ, result);
            } catch (Throwable t) {
                throw unexpected(t);
            }
            return status == ERROR_SUCCESS ? result.get(ADDRESS, 0) : null;
        }
    }

    /** {@code RegCloseKey(key)}. */
    static int regCloseKey(MemorySegment key) {
        try {
            return (int) REG_CLOSE_KEY.invokeExact(key);
        } catch (Throwable t) {
            throw unexpected(t);
        }
    }

    /** The three counts {@code fontpath.c} L645-647 asked {@code RegQueryInfoKeyW} for. */
    record RegKeyInfo(int values, int maxValueNameChars, int maxValueDataBytes) {
    }

    /** {@code RegQueryInfoKeyW} for the value counts; {@code null} on any status but success. */
    static RegKeyInfo regQueryInfoKey(MemorySegment key) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment values = scratch.allocate(JAVA_INT);
            MemorySegment maxValueNameLen = scratch.allocate(JAVA_INT);
            MemorySegment maxValueLen = scratch.allocate(JAVA_INT);
            MemorySegment none = MemorySegment.NULL;
            int status;
            try {
                status = (int) REG_QUERY_INFO_KEY_W.invokeExact(key, none, none, none, none, none, none,
                        values, maxValueNameLen, maxValueLen, none, none);
            } catch (Throwable t) {
                throw unexpected(t);
            }
            if (status != ERROR_SUCCESS) {
                return null;
            }
            return new RegKeyInfo(values.get(JAVA_INT, 0), maxValueNameLen.get(JAVA_INT, 0),
                    maxValueLen.get(JAVA_INT, 0));
        }
    }

    /**
     * One {@code RegEnumValueW} result. {@code name}, {@code type} and {@code data} are meaningful only
     * for {@link #ERROR_SUCCESS}: {@code name} is the NUL-terminated value name, {@code data} the first
     * {@code cbData / 2} {@code WCHAR}s of the value, raw - the caller applies {@code wcslen} to them.
     */
    record RegEnumValue(int status, String name, int type, char[] data) {
    }

    /**
     * {@code RegEnumValueW(key, index, name, &nameCapacityChars, NULL, &type, data, &dataCapacityBytes)}
     * with buffers of exactly the given capacities ({@code fontpath.c} L654-660).
     */
    static RegEnumValue regEnumValue(MemorySegment key, int index, int nameCapacityChars, int dataCapacityBytes) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment name = scratch.allocate(JAVA_CHAR, nameCapacityChars);
            MemorySegment nameChars = scratch.allocate(JAVA_INT);
            nameChars.set(JAVA_INT, 0, nameCapacityChars);
            MemorySegment type = scratch.allocate(JAVA_INT);
            MemorySegment data = scratch.allocate(dataCapacityBytes, 8);
            MemorySegment dataBytes = scratch.allocate(JAVA_INT);
            dataBytes.set(JAVA_INT, 0, dataCapacityBytes);
            int status;
            try {
                status = (int) REG_ENUM_VALUE_W.invokeExact(key, index, name, nameChars, MemorySegment.NULL, type,
                        data, dataBytes);
            } catch (Throwable t) {
                throw unexpected(t);
            }
            if (status != ERROR_SUCCESS) {
                return new RegEnumValue(status, null, 0, null);
            }
            int copied = Math.min(dataBytes.get(JAVA_INT, 0), dataCapacityBytes);
            return new RegEnumValue(status, wideString(name, 0, nameCapacityChars), type.get(JAVA_INT, 0),
                    chars(data, 0, copied / 2));
        }
    }

    /**
     * One {@code RegQueryValueExW} result: the status, the type and byte size it reported, and for a
     * successful read into a buffer the first {@code min(size, capacity) / 2} {@code WCHAR}s, raw.
     */
    record RegValue(int status, int type, int sizeBytes, char[] data) {
    }

    /**
     * {@code RegQueryValueExW(key, valueName, NULL, &type, data, &size)}. A negative
     * {@code dataCapacityBytes} passes {@code NULL} for {@code data} - the size query of
     * {@code fontpath.c} L818, {@code size} seeded with {@code sizeof(BYTE*)} as there - otherwise a
     * buffer of exactly that many bytes, which is how the C reached {@code ERROR_MORE_DATA} for values
     * larger than {@code MAX_PATH + 1} bytes (L869-903).
     */
    static RegValue regQueryValue(MemorySegment key, String valueName, int dataCapacityBytes) {
        try (Arena scratch = Arena.ofConfined()) {
            MemorySegment type = scratch.allocate(JAVA_INT);
            MemorySegment size = scratch.allocate(JAVA_INT);
            MemorySegment data;
            if (dataCapacityBytes < 0) {
                data = MemorySegment.NULL;
                size.set(JAVA_INT, 0, (int) ADDRESS.byteSize());
            } else {
                data = scratch.allocate(dataCapacityBytes, 8);
                size.set(JAVA_INT, 0, dataCapacityBytes);
            }
            int status;
            try {
                status = (int) REG_QUERY_VALUE_EX_W.invokeExact(key, wide(scratch, valueName), MemorySegment.NULL,
                        type, data, size);
            } catch (Throwable t) {
                throw unexpected(t);
            }
            int sizeBytes = size.get(JAVA_INT, 0);
            char[] chars = null;
            if (status == ERROR_SUCCESS && dataCapacityBytes >= 0) {
                chars = chars(data, 0, Math.min(sizeBytes, dataCapacityBytes) / 2);
            }
            return new RegValue(status, type.get(JAVA_INT, 0), sizeBytes, chars);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * Test hooks (reached through WinFontNativeShim)
     * ------------------------------------------------------------------------------------------- */

    /** Runs the class initializer: looks the four libraries up and binds every symbol. */
    static void ensureLoaded() {
    }

    /**
     * The native buffer {@link #wide} builds for {@code text}, read back unit for unit - every
     * {@code WCHAR} of it, the terminating NUL included - before its arena closes.
     */
    static char[] wideCodeUnits(String text) {
        try (Arena scratch = Arena.ofConfined()) {
            return wide(scratch, text).toArray(JAVA_CHAR);
        }
    }

    /** The symbols this class bound, as {@code <dll>!<name>}, in binding order. */
    static List<String> boundSymbols() {
        return Collections.unmodifiableList(new ArrayList<>(BOUND_SYMBOLS));
    }

    /** Whether {@code <dll>!<name>} resolves in that library's lookup today. */
    static boolean resolves(String qualifiedName) {
        int bang = qualifiedName.indexOf('!');
        if (bang < 0) {
            throw new IllegalArgumentException("expected <dll>!<name>: " + qualifiedName);
        }
        SystemLibrary library = switch (qualifiedName.substring(0, bang)) {
            case "kernel32" -> KERNEL32;
            case "user32" -> USER32;
            case "gdi32" -> GDI32;
            case "advapi32" -> ADVAPI32;
            default -> throw new IllegalArgumentException("not a library this class binds: " + qualifiedName);
        };
        return library.lookup().find(qualifiedName.substring(bang + 1)).isPresent();
    }
}
