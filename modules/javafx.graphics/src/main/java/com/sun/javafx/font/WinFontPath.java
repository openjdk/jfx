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

import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * The Windows font-directory, font-map and system-font queries of {@link PrismFontFactory} and
 * {@code DWFactory}: the eight functions that {@code fontpath.c} implemented in C over GDI, the
 * registry and the user/kernel APIs, now Java over the Win32 bindings of {@link WinFontNative}.
 * <p>
 * Every method mirrors its C original statement by statement - the registry paths and value types,
 * the buffer sizes handed to Win32, the enumeration filters, the order in which the maps are filled and
 * the case folding of their keys, the early returns - so that the golden captured from the C
 * ({@code test.com.sun.javafx.font.FontEnumerationGoldenTest}) passes unchanged. Line numbers in the
 * comments refer to {@code fontpath.c} at commit {@code 8492cb03b0}, the version this class replaced
 * ({@code git show 8492cb03b0:modules/javafx.graphics/src/main/native-font/fontpath.c}). Where the C compared strings
 * with {@code _wcsnicmp}/{@code _wcsicmp} this class folds ASCII letters only, which is what those
 * functions do in the CRT's default "C" locale ({@link #wcsnicmpEqual}); everything else compares
 * exact UTF-16 code units, as {@code wcscmp}/{@code wcsstr} did.
 * <p>
 * Windows only: {@link WinFontNative} binds Win32 in its class initializer, and every caller guards on
 * {@code PrismFontFactory.isWindows}, as it did for the natives.
 */
public final class WinFontPath {

    /** L225-226: the font name to file map, under both {@code HKLM} and {@code HKCU}. */
    static final String FONTS_KEY = "Software\\Microsoft\\Windows NT\\CurrentVersion\\Fonts";

    /** L808. */
    static final String FONT_LINK_KEY = "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\FontLink\\SystemLink";

    /** L899. */
    static final String EUDC_VALUE = "SystemDefaultEUDCFont";

    private static final String FONTS_DIR = "\\Fonts";
    private static final String SYSTEM_DIR = "\\System";
    private static final String SYSTEM32_DIR = "\\System32";
    private static final String TRUETYPE_SUFFIX = " (TrueType)";
    private static final String COLLECTION_SEPARATOR = " & ";
    private static final String SYSTEM_ROOT_PREFIX = "%SystemRoot%";
    private static final String EUDC_TTE = "EUDC.TTE";
    private static final String EUDC_TTE_UNDER_WINDOWS = "\\FONTS\\EUDC.TTE";

    /** L980. */
    private static final int LCD_CONTRAST_DEFAULT = 1300;

    /** L999. */
    private static final float SYSTEM_FONT_SIZE_DEFAULT = 12.0f;

    /* L848-855 */
    private static final int LANGID_JA_JP = 0x411;
    private static final int LANGID_ZH_CN = 0x0804;
    private static final int LANGID_ZH_SG = 0x1004;
    private static final int LANGID_ZH_TW = 0x0404;
    private static final int LANGID_ZH_HK = 0x0c04;
    private static final int LANGID_ZH_MO = 0x1404;
    private static final int LANGID_KO_KR = 0x0412;
    private static final int LANGID_US = 0x409;

    private WinFontPath() {
    }

    /* ---------------------------------------------------------------------------------------------
     * PrismFontFactory.getFontPath (L66-201)
     * ------------------------------------------------------------------------------------------- */

    /**
     * The system font directory, or the system and the Windows font directories joined by {@code ;}
     * when they differ ("as in a shared Windows installation"). {@code null} when either directory
     * query fails, as the C returned {@code NULL} from every {@code goto finish}.
     */
    public static String getFontPath() {
        String systemDirectory = WinFontNative.systemDirectory();
        if (systemDirectory == null) {
            return null;
        }
        String windowsDirectory = WinFontNative.windowsDirectory();
        if (windowsDirectory == null) {
            return null;
        }
        return fontPath(systemDirectory, windowsDirectory);
    }

    /**
     * L121-182 on the two directories Win32 returned. If the last component of the system directory
     * is {@code \System} or {@code \System32} - or, as {@code _wcsnicmp(end, literal, endLen)} has it,
     * any case-insensitive prefix of one of them - it is replaced by {@code \Fonts}; otherwise the
     * system directory is used as it is. {@code \Fonts} is always appended to the Windows directory.
     * The two are joined only when they differ over the length of the shorter one, so a directory that
     * is a prefix of the other counts as the same.
     */
    static String fontPath(String systemDirectory, String windowsDirectory) {
        String systemFonts = systemDirectory;
        int end = systemDirectory.lastIndexOf('\\');
        if (end >= 0) {
            int endLength = systemDirectory.length() - end;
            // If the last directory in sysdir is "System" or "System32", strip that and replace it with "\Fonts"
            if (wcsnicmpEqual(systemDirectory, end, SYSTEM_DIR, endLength)
                    || wcsnicmpEqual(systemDirectory, end, SYSTEM32_DIR, endLength)) {
                systemFonts = systemDirectory.substring(0, end) + FONTS_DIR;
            }
        }
        // "Fonts" directory should be placed right inside Windows directory, so just append it to the path
        String windowsFonts = windowsDirectory + FONTS_DIR;
        /* JFX expects either one path, or two separated by a semicolon.
         * If sysdir and windir are different, form a complete path "list" by
         * joining them in "<sysdir>;<windir>" pattern. JVM side will unpack it.
         */
        if (wcsnicmpEqual(systemFonts, 0, windowsFonts, Math.min(systemFonts.length(), windowsFonts.length()))) {
            return systemFonts;
        }
        return systemFonts + ";" + windowsFonts;
    }

    /* ---------------------------------------------------------------------------------------------
     * PrismFontFactory.populateFontFileNameMap (L203-795)
     * ------------------------------------------------------------------------------------------- */

    /**
     * Fills the three maps from GDI and the registry: first {@code EnumFontFamiliesExW} over every
     * family of every charset, adding each family (lower-cased in {@code locale}) with the list of its
     * faces to {@code familyToFontListMap} and each face's full name (lower-cased) to
     * {@code fontToFamilyMap}; then the {@code Fonts} registry key under {@code HKEY_CURRENT_USER} and
     * then under {@code HKEY_LOCAL_MACHINE}, adding the lower-cased font name to file mappings to
     * {@code fontToFileMap}, later keys overwriting earlier ones. A {@code null} map, or no screen
     * device context, leaves everything untouched.
     */
    public static void populateFontFileNameMap(HashMap<String, String> fontToFileMap,
                                               HashMap<String, String> fontToFamilyMap,
                                               HashMap<String, ArrayList<String>> familyToFontListMap,
                                               Locale locale) {
        /* Check we were passed all the maps we need (L718-722) */
        if (fontToFileMap == null || fontToFamilyMap == null || familyToFontListMap == null) {
            return;
        }
        /* This HDC is initialised and released in this populate family map
         * entry point, and used within the call which would otherwise
         * create many DCs. (L773-776)
         */
        MemorySegment screenDC = WinFontNative.getDC(MemorySegment.NULL);
        if (screenDC.address() == 0) {
            return;
        }
        try {
            /* Enumerate fonts via GDI to build maps of fonts and families (L778-784) */
            try (WinFontNative.FontEnumeration enumeration = new WinFontNative.FontEnumeration()) {
                new GdiFontMap(enumeration, screenDC, fontToFamilyMap, familyToFontListMap, locale)
                        .enumerateFamilies();
            }
            /* Starting from Windows 10 Preview Build 17704
             * fonts are installed into user's home folder by default,
             * and are listed in user's registry section (L786-791)
             */
            populateFontFileNameFromRegistryKey(WinFontNative.HKEY_CURRENT_USER, fontToFileMap, locale);
            populateFontFileNameFromRegistryKey(WinFontNative.HKEY_LOCAL_MACHINE, fontToFileMap, locale);
        } finally {
            WinFontNative.releaseDC(MemorySegment.NULL, screenDC);
        }
    }

    /**
     * L208-478: the state the three {@code EnumFontFamExProc} callbacks shared through
     * {@code GdiFontMapInfo}, and the callbacks themselves. All three enumerations run on the same
     * {@link WinFontNative.FontEnumeration}, nested exactly as the C nested them.
     */
    private static final class GdiFontMap {

        private final WinFontNative.FontEnumeration enumeration;
        private final MemorySegment screenDC;
        private final HashMap<String, String> fontToFamilyMap;
        private final HashMap<String, ArrayList<String>> familyToFontListMap;
        private final Locale locale;

        GdiFontMap(WinFontNative.FontEnumeration enumeration, MemorySegment screenDC,
                   HashMap<String, String> fontToFamilyMap,
                   HashMap<String, ArrayList<String>> familyToFontListMap, Locale locale) {
            this.enumeration = enumeration;
            this.screenDC = screenDC;
            this.fontToFamilyMap = fontToFamilyMap;
            this.familyToFontListMap = familyToFontListMap;
            this.locale = locale;
        }

        /** L779-784: {@code DEFAULT_CHARSET} and an empty face name, one callback per family and charset. */
        void enumerateFamilies() {
            enumeration.enumerate(screenDC, "", WinFontNative.DEFAULT_CHARSET, this::enumFamilyNames);
        }

        /**
         * {@code EnumFamilyNamesW}, L370-478. Expects to be called for every charset of every font
         * family. If this is the first time we have been called for this family, add a new mapping to
         * the familyToFontListMap from this family to a list of its members. To populate that list,
         * further enumerate all faces in this family for the matched charset.
         */
        private int enumFamilyNames(WinFontNative.EnumLogFont font, int fontType) {
            /* Both Vista and XP return DEVICE_FONTTYPE for OTF fonts */
            if (fontType != WinFontNative.TRUETYPE_FONTTYPE && fontType != WinFontNative.DEVICE_FONTTYPE) {
                return 1;
            }
            /* Windows lists fonts which have a vmtx (vertical metrics) table twice.
             * Once using their normal name, and again preceded by '@'. These appear
             * in font lists in some windows apps, such as wordpad. We don't want
             * these so we skip any font where the first character is '@'
             */
            String family = font.faceName();
            if (family.startsWith("@")) {
                return 1;
            }
            String familyLC = family.toLowerCase(locale);
            /* check if already seen this family with a different charset */
            if (familyToFontListMap.containsKey(familyLC)) {
                return 1;
            }
            ArrayList<String> list = new ArrayList<>(4);
            familyToFontListMap.put(familyLC, list);
            enumeration.enumerate(screenDC, family, font.charSet(),
                    (face, faceType) -> enumFontFacesInFamily(family, list, face, faceType));
            return 1;
        }

        /**
         * {@code EnumFontFacesInFamilyProcW}, L287-356. Expects to be called once for each face name in
         * the family: the full name is added to the family's list and, lower-cased, mapped to the
         * canonical family name.
         */
        private int enumFontFacesInFamily(String family, ArrayList<String> list, WinFontNative.EnumLogFont face,
                                          int fontType) {
            /* Both Vista and XP return DEVICE_FONTTYPE for OTF fonts */
            if (fontType != WinFontNative.TRUETYPE_FONTTYPE && fontType != WinFontNative.DEVICE_FONTTYPE) {
                return 1;
            }
            /* Windows has font aliases and so may enumerate fonts from
             * the aliased family if any actual font of that family is installed.
             * To protect against it ignore fonts which aren't enumerated under
             * their true family.
             */
            if (differentFamily(face.faceName(), face.fullName())) {
                return 1;
            }
            String fullName = face.fullName();
            list.add(fullName);
            fontToFamilyMap.put(fullName.toLowerCase(locale), family);
            return 1;
        }

        /**
         * {@code DifferentFamily} and {@code CheckFontFamilyProcW}, L235-277: enumerate the full name
         * under {@code DEFAULT_CHARSET} and compare the face name of the first font reported with the
         * face's own. No font reported, or a full name too long for {@code lfFaceName}, means "same".
         */
        private boolean differentFamily(String family, String fullName) {
            /* If fullName can't be stored in the struct, assume correct family */
            if (fullName.length() >= WinFontNative.LF_FACESIZE) {
                return false;
            }
            boolean[] different = new boolean[1];
            enumeration.enumerate(screenDC, fullName, WinFontNative.DEFAULT_CHARSET, (font, fontType) -> {
                different[0] = !font.faceName().equals(family);
                return 0;
            });
            return different[0];
        }
    }

    /**
     * {@code populateFontFileNameFromRegistryKey}, L627-686: every {@code REG_SZ} value of the
     * {@code Fonts} key whose name ends in {@code " (TrueType)"} (stripped), or else whose data ends in
     * {@code .ttf} or {@code .otf}, registered under its name. Enumeration stops at the first value that
     * cannot be read.
     */
    static void populateFontFileNameFromRegistryKey(MemorySegment root, HashMap<String, String> fontToFileMap,
                                                            Locale locale) {
        /* Use the windows registry to map font names to files */
        MemorySegment fontsKey = WinFontNative.regOpenKeyRead(root, FONTS_KEY);
        if (fontsKey == null) {
            return;
        }
        try {
            WinFontNative.RegKeyInfo info = WinFontNative.regQueryInfoKey(fontsKey);
            if (info == null) {
                return;
            }
            int nameCapacityChars = info.maxValueNameChars() + 1; /* Account for NULL-terminator */
            int dataCapacityBytes = info.maxValueDataBytes();
            for (int index = 0; index < info.values(); index++) {
                WinFontNative.RegEnumValue value =
                        WinFontNative.regEnumValue(fontsKey, index, nameCapacityChars, dataCapacityBytes);
                if (value.status() != WinFontNative.ERROR_SUCCESS) {
                    break;
                }
                if (value.type() != WinFontNative.REG_SZ) { /* REG_SZ means a null-terminated string */
                    continue;
                }
                String data = cString(value.data());
                String name = registryToBaseTTName(value.name());
                if (name == null) {
                    /* If the filename ends with ".ttf" or ".otf" also accept it.
                     * Not expecting to need to do this for .ttc files.
                     */
                    if (!hasTrueTypeExtension(data)) {
                        continue; /* not a TT font... */
                    }
                    name = value.name();
                }
                registerFont(fontToFileMap, name, data, locale);
            }
        } finally {
            WinFontNative.regCloseKey(fontsKey);
        }
    }

    /**
     * {@code RegistryToBaseTTNameW}, L503-527: the name without its {@code " (TrueType)"} suffix, or
     * {@code null} when it does not carry exactly that suffix. {@code " (OpenType)"} is deliberately not
     * accepted ("REMIND : renable OpenType (.otf) some day").
     */
    static String registryToBaseTTName(String name) {
        int length = name.length();
        if (length == 0) {
            return null;
        }
        if (name.charAt(length - 1) != ')') {
            return null;
        }
        if (length <= TRUETYPE_SUFFIX.length()) {
            return null;
        }
        /* suffix length is the same for truetype and opentype fonts */
        int suffix = length - TRUETYPE_SUFFIX.length();
        if (name.startsWith(TRUETYPE_SUFFIX, suffix)) {
            return name.substring(0, suffix); /* truncate name */
        }
        return null;
    }

    /** L674-678: whether the file name's last {@code .}-suffix is {@code .ttf} or {@code .otf}, ASCII case aside. */
    static boolean hasTrueTypeExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) {
            return false;
        }
        String extension = fileName.substring(dot);
        return wcsicmpEqual(extension, ".ttf") || wcsicmpEqual(extension, ".otf");
    }

    /**
     * {@code registerFontW}, L529-625. A data string ending in {@code C} or {@code c} (a {@code .ttc}
     * collection, by MS file naming) whose name contains {@code " & "} is split into the names it
     * joins, registered from the last to the first; anything else is registered under the whole name.
     * Empty data is treated as no collection, where the C read one {@code WCHAR} before its buffer.
     */
    static void registerFont(HashMap<String, String> fontToFileMap, String name, String data, Locale locale) {
        /* TTC or ttc means it may be a collection. Need to parse out
         * multiple font face names separated by " & "
         * By only doing this for fonts which look like collections based on
         * file name we are adhering to MS recommendations for font file names
         * so it seems that we can be sure that this identifies precisely
         * the MS-supplied truetype collections.
         * This avoids any potential issues if a TTF file happens to have
         * a & in the font name (I can't find anything which prohibits this)
         * and also means we only parse the key in cases we know to be
         * worthwhile.
         */
        int dataLength = data.length();
        boolean collection = dataLength > 0
                && (data.charAt(dataLength - 1) == 'C' || data.charAt(dataLength - 1) == 'c');
        int ptr1 = collection ? name.indexOf(COLLECTION_SEPARATOR) : -1;
        if (ptr1 < 0) {
            fontToFileMap.put(name.toLowerCase(locale), data);
            return;
        }
        String remaining = name;
        ptr1 += COLLECTION_SEPARATOR.length();
        while (true) { /* L558: while (ptr1 >= name), which always holds */
            int ptr2;
            while ((ptr2 = remaining.indexOf(COLLECTION_SEPARATOR, ptr1)) >= 0) {
                ptr1 = ptr2 + COLLECTION_SEPARATOR.length();
            }
            fontToFileMap.put(remaining.substring(ptr1).toLowerCase(locale), data);
            if (ptr1 == 0) {
                break;
            }
            remaining = remaining.substring(0, ptr1 - COLLECTION_SEPARATOR.length()); /* L591: *(ptr1-3) = 0 */
            ptr1 = 0;
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * DWFactory.regReadFontLink (L797-842)
     * ------------------------------------------------------------------------------------------- */

    /**
     * The {@code FontLink\SystemLink} value named {@code fontName} under {@code HKEY_LOCAL_MACHINE},
     * as the {@code REG_MULTI_SZ} bytes read back: every {@code WCHAR} of them, NUL separators and
     * terminators included, the type unchecked. {@code null} when the key cannot be opened, the value
     * is absent or empty, or the read fails.
     */
    public static String regReadFontLink(String fontName) {
        MemorySegment key = WinFontNative.regOpenKeyRead(WinFontNative.HKEY_LOCAL_MACHINE, FONT_LINK_KEY);
        if (key == null) {
            return null;
        }
        try {
            //get the buffer size
            WinFontNative.RegValue size = WinFontNative.regQueryValue(key, fontName, -1);
            if (size.status() != WinFontNative.ERROR_SUCCESS || size.sizeBytes() == 0) {
                return null;
            }
            WinFontNative.RegValue value = WinFontNative.regQueryValue(key, fontName, size.sizeBytes());
            if (value.status() != WinFontNative.ERROR_SUCCESS) {
                return null;
            }
            return new String(value.data());
        } finally {
            WinFontNative.regCloseKey(key);
        }
    }

    /* ---------------------------------------------------------------------------------------------
     * DWFactory.getEUDCFontFile (L845-942)
     * ------------------------------------------------------------------------------------------- */

    /**
     * The end-user-defined-character font file configured for the system language: the
     * {@code SystemDefaultEUDCFont} value of the user's {@code EUDC\<code page>} key, with
     * {@code %SystemRoot%} expanded and a bare {@code EUDC.TTE} resolved into the Windows {@code FONTS}
     * directory. {@code null} for a language without EUDC support, a missing key or value, a value that
     * is not {@code REG_SZ} or does not fit, or an expansion that would exceed {@code MAX_PATH}.
     */
    public static String getEUDCFontFile() {
        String eudcKey = eudcKey(WinFontNative.systemDefaultLangID());
        if (eudcKey == null) {
            return null;
        }
        MemorySegment key = WinFontNative.regOpenKeyRead(WinFontNative.HKEY_CURRENT_USER, eudcKey);
        if (key == null) {
            return null;
        }
        WinFontNative.RegValue value;
        try {
            /* L869-903: the C passed its WCHAR count, MAX_PATH + 1, where the API takes a byte count */
            value = WinFontNative.regQueryValue(key, EUDC_VALUE, WinFontNative.MAX_PATH + 1);
        } finally {
            WinFontNative.regCloseKey(key);
        }
        int fontPathLength = value.sizeBytes() / 2;
        if (value.status() != WinFontNative.ERROR_SUCCESS || value.type() != WinFontNative.REG_SZ
                || fontPathLength > WinFontNative.MAX_PATH) {
            return null;
        }
        return eudcFontFile(value.data(), fontPathLength, System.getenv("SystemRoot"),
                () -> WinFontNative.windowsDirectory(WinFontNative.MAX_PATH));
    }

    /**
     * L875-892: the {@code EUDC} subkey for a system language ID, {@code null} for one without EUDC
     * support ("EUDC only supported in codepage 932, 936, 949, 950 (and unicode)").
     */
    static String eudcKey(int langID) {
        if (langID == LANGID_JA_JP) {
            return "EUDC\\932";
        } else if (langID == LANGID_ZH_CN || langID == LANGID_ZH_SG) {
            return "EUDC\\936";
        } else if (langID == LANGID_ZH_HK || langID == LANGID_ZH_TW || langID == LANGID_ZH_MO) {
            return "EUDC\\950";
        } else if (langID == LANGID_KO_KR) {
            return "EUDC\\949";
        } else if (langID == LANGID_US) {
            return "EUDC\\1252";
        }
        return null;
    }

    /**
     * L911-941 on the {@code fontPathLength} {@code WCHAR}s read from the registry, which include the
     * value's own NUL terminator. The C terminated the buffer after them and then used C string
     * functions, so the prefix test, the comparison and the concatenations see the text up to the first
     * NUL, while the fall-through return hands back all {@code fontPathLength} characters, that stored
     * NUL included.
     *
     * @param systemRoot the {@code SystemRoot} environment variable, {@code null} when unset
     * @param windowsDirectory {@code GetWindowsDirectoryW} into a {@code MAX_PATH} buffer, consulted only
     *        for a bare {@code EUDC.TTE}; {@code null} when it failed or did not fit
     */
    static String eudcFontFile(char[] fontPath, int fontPathLength, String systemRoot,
                               Supplier<String> windowsDirectory) {
        String stored = new String(fontPath, 0, fontPathLength);
        String text = cString(stored);
        if (text.startsWith(SYSTEM_ROOT_PREFIX)) {
            //if the fontPath includes %SystemRoot%
            // Subtract 12, being the length of "SystemRoot".
            if (systemRoot == null || fontPathLength - 12 + systemRoot.length() > WinFontNative.MAX_PATH) {
                return null;
            }
            return systemRoot + text.substring(SYSTEM_ROOT_PREFIX.length());
        } else if (text.equals(EUDC_TTE)) {
            //else to see if it only inludes "EUDC.TTE"
            String windows = windowsDirectory.get();
            if (windows == null || windows.length() + 16 > WinFontNative.MAX_PATH) {
                return null;
            }
            return windows + EUDC_TTE_UNDER_WINDOWS;
        }
        return stored;
    }

    /* ---------------------------------------------------------------------------------------------
     * PrismFontFactory.getLCDContrastWin32, getSystemFontSizeNative, getSystemFontNative,
     * getSystemLCID (L944-1028)
     * ------------------------------------------------------------------------------------------- */

    /** L976-984: {@code SPI_GETFONTSMOOTHINGCONTRAST}, or 1300 when the query fails. */
    public static int getLCDContrastWin32() {
        return WinFontNative.systemParametersInfoUInt(WinFontNative.SPI_GETFONTSMOOTHINGCONTRAST, LCD_CONTRAST_DEFAULT);
    }

    /**
     * L986-1001: the message font's height from {@code SPI_GETNONCLIENTMETRICS}, negated and scaled
     * from the desktop's vertical DPI to 96, or 12 when the query fails.
     */
    public static float getSystemFontSizeNative() {
        WinFontNative.NonClientMetrics metrics = WinFontNative.nonClientMetrics();
        if (metrics == null) {
            return SYSTEM_FONT_SIZE_DEFAULT;
        }
        MemorySegment desktop = WinFontNative.getDesktopWindow();
        MemorySegment hdc = WinFontNative.getDC(desktop);
        int dpiY = WinFontNative.getDeviceCaps(hdc, WinFontNative.LOGPIXELSY);
        WinFontNative.releaseDC(desktop, hdc);
        return (-metrics.messageFontHeight()) * ((float) WinFontNative.USER_DEFAULT_SCREEN_DPI) / dpiY;
    }

    /** L1003-1014: the message font's face name from {@code SPI_GETNONCLIENTMETRICS}, or {@code null}. */
    public static String getSystemFontNative() {
        WinFontNative.NonClientMetrics metrics = WinFontNative.nonClientMetrics();
        return metrics == null ? null : metrics.messageFontFaceName();
    }

    /** L1017-1028: {@code LOCALE_ILANGUAGE} of the system default locale, as the C's {@code (jshort)}. */
    public static short getSystemLCID() {
        return (short) WinFontNative.localeInfoNumber(WinFontNative.systemDefaultLCID(),
                WinFontNative.LOCALE_ILANGUAGE | WinFontNative.LOCALE_RETURN_NUMBER);
    }

    /* ---------------------------------------------------------------------------------------------
     * C string semantics
     * ------------------------------------------------------------------------------------------- */

    /** The NUL-terminated string in {@code chars}: up to the first NUL, or all of them when there is none. */
    static String cString(char[] chars) {
        int length = 0;
        while (length < chars.length && chars[length] != 0) {
            length++;
        }
        return new String(chars, 0, length);
    }

    /** {@code text} up to its first NUL. */
    static String cString(String text) {
        int nul = text.indexOf('\0');
        return nul < 0 ? text : text.substring(0, nul);
    }

    /**
     * Whether {@code _wcsnicmp(s1 + from1, s2, count)} is 0 in the CRT's "C" locale: up to
     * {@code count} characters compared with only {@code A-Z} folded, stopping early, as equal, when
     * both strings end. Each string is read as NUL-terminated at its end.
     */
    static boolean wcsnicmpEqual(String s1, int from1, String s2, int count) {
        for (int i = 0; i < count; i++) {
            char c1 = from1 + i < s1.length() ? s1.charAt(from1 + i) : 0;
            char c2 = i < s2.length() ? s2.charAt(i) : 0;
            if (asciiLower(c1) != asciiLower(c2)) {
                return false;
            }
            if (c1 == 0) {
                return true;
            }
        }
        return true;
    }

    /** Whether {@code _wcsicmp(s1, s2)} is 0 in the CRT's "C" locale. */
    static boolean wcsicmpEqual(String s1, String s2) {
        return wcsnicmpEqual(s1, 0, s2, Math.max(s1.length(), s2.length()) + 1);
    }

    private static char asciiLower(char c) {
        return c >= 'A' && c <= 'Z' ? (char) (c + ('a' - 'A')) : c;
    }
}
