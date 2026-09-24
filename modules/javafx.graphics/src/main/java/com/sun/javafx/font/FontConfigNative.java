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

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * The Linux fontconfig enumeration of {@link FontConfigManager}, bound from Java to {@code libfontconfig.so.1} and
 * libc with {@code java.lang.foreign}: what {@code Java_com_sun_javafx_font_FontConfigManager_getFontConfig} and
 * {@code Java_com_sun_javafx_font_FontConfigManager_populateMapsNative} of {@code fontpath_linux.c} did in
 * {@code libjavafx_font.so} at commit {@code 7b43255b30}. Line numbers below, bare {@code :N} forms included, refer
 * to that file at that commit; the file, its library and the two {@code native} declarations of
 * {@code FontConfigManager} were removed once this class had replaced them.
 *
 * <h2>Library lifetime</h2>
 * The C {@code dlopen}ed {@code libfontconfig.so.1} (then {@code libfontconfig.so}) at the start of every call and
 * {@code dlclose}d it at the end (:66-100, :104-123): with nothing else holding the library, fontconfig is unloaded
 * after each call and rebuilds its configuration on the next. Each call here opens the library with
 * {@link SymbolLookup#libraryLookup(String, Arena)} in a confined arena that closes when the call returns, which is
 * {@code dlopen}/{@code dlclose} with the same reference counting (measured: five {@code /proc/self/maps} lines
 * while the arena is open, none after it is closed). A library that cannot be opened, or a symbol the C looked up
 * that is missing, makes the call return {@code false}; nothing here throws for an absent fontconfig. The
 * downcall handles are address-less ({@link Linker#downcallHandle(FunctionDescriptor, Linker.Option...)}) and
 * linked once; the symbol addresses are resolved per call from that call's library.
 *
 * <h2>{@code HOME}</h2>
 * After a successful {@code dlopen} and before anything else, the C called libc {@code getenv("HOME")} and, when it
 * was unset, {@code putenv("HOME=")} with a static string (:87-97), leaving {@code HOME} empty in the C environment
 * for the rest of the process. Both calls go to libc through {@link Linker#defaultLookup()}, never through
 * {@link System#getenv}, whose snapshot would neither see nor cause the change; the {@code HOME=} string lives in
 * {@link Arena#global()} because the environment keeps the pointer. {@code PRISM_FONTCONFIG_DEBUG} is read the same
 * way, before that point, as the C read it (:573).
 *
 * <h2>Strings</h2>
 * The locale and the twelve logical names go to fontconfig as modified UTF-8, as {@code GetStringUTFChars} gave
 * them; every {@code FcChar8 *} fontconfig reports (family, style, full name, file; the file after {@code realpath})
 * becomes a {@code String} the way {@code NewStringUTF} made it, both through {@link JniStringCodec}. Format and
 * language tags are compared as bytes, as {@code strcmp} compared them. {@code realpath} runs on the raw bytes into
 * a {@code PATH_MAX + 1} buffer (:709-716), never through {@code java.nio.file}.
 *
 * <h2>What is reproduced and what differs</h2>
 * The call sequence, the argument values ({@code NULL} config, {@code FcMatchPattern}, {@code trim = FcTrue}), the
 * selection rules ({@link FcSelection}), the strict format filter of the enumeration against the lenient one of
 * the sort, the name preference (the first name, then every later {@code en} one, :718-748), the lower-casing with
 * the passed {@link Locale}, the map operations and their order, the partial fills left behind by an early
 * {@code false}, the debug output on {@code stderr}, and what is destroyed when. The C destroyed neither the
 * {@code FcCharSetUnion} results nor the enumeration's pattern and object set; neither does this class. Differences:
 * the enumeration builds its pattern and object set with {@code FcPatternCreate} + {@code FcPatternAddBool} and
 * {@code FcObjectSetCreate} + {@code FcObjectSetAdd}, which is what the variadic {@code FcPatternBuild} and
 * {@code FcObjectSetBuild} do internally (measured on fontconfig 2.17.1: {@code FcPatternEqual} patterns, the same
 * {@code FcFontList} result in the same order), so those symbols are the ones whose absence returns {@code false};
 * and where the C would have dereferenced a null pointer (a {@code null} locale, array element or name, a missing
 * {@code FcFontSort} symbol, a {@code NULL} {@code FcFontList} result) this class throws
 * {@link NullPointerException} or returns {@code false} instead of crashing the process.
 */
public final class FontConfigNative {

    /** The names {@code openFontConfig} tried with {@code dlopen}, in order (:79-81). */
    static final List<String> LIBRARIES = List.of("libfontconfig.so.1", "libfontconfig.so");

    /** {@code PATH_MAX} of {@code linux/limits.h}; the C's {@code realpath} buffer is {@code PATH_MAX + 1} bytes. */
    static final int PATH_MAX = 4096;

    private static final int FC_TRUE = 1;

    private static final int FC_RESULT_MATCH = 0;

    private static final int FC_MATCH_PATTERN = 0;

    private static final byte[] EN = {'e', 'n'};

    private static final byte[] TRUE_TYPE = {'T', 'r', 'u', 'e', 'T', 'y', 'p', 'e'};

    private static final byte[] CFF = {'C', 'F', 'F'};

    private FontConfigNative() {
    }

    /**
     * {@code Java_com_sun_javafx_font_FontConfigManager_getFontConfig} (:174-546): for each entry of {@code fonts}, in
     * array order, parses its {@code fcName}, adds {@code locale} as {@code FC_LANG}, runs {@code FcConfigSubstitute},
     * {@code FcDefaultSubstitute} and {@code FcFontSort}, and fills the entry's {@code firstFont} and (with fallbacks)
     * {@code allFonts} as {@link FcSelection} describes.
     *
     * @return {@code false} when fontconfig cannot be opened or a name cannot be parsed, sorted or read; the entries
     *         before the failing one are then already filled and the rest untouched
     */
    public static boolean getLogicalFonts(String locale, FontConfigManager.FcCompFont[] fonts,
                                          boolean includeFallbacks) {
        return sortLogicalFonts(LIBRARIES, locale, fonts, includeFallbacks);
    }

    /**
     * {@code Java_com_sun_javafx_font_FontConfigManager_populateMapsNative} (:549-853): lists every outline font
     * fontconfig knows, keeps the TrueType and CFF ones whose file {@code realpath} resolves and that have a family
     * and a full name, and puts them into the three maps under their names lower-cased with {@code locale}.
     *
     * @return {@code false} for a {@code null} argument, or when fontconfig cannot be opened or lacks a symbol
     */
    public static boolean populateFontMaps(HashMap<String, String> fontToFileMap,
                                           HashMap<String, String> fontToFamilyNameMap,
                                           HashMap<String, ArrayList<String>> familyToFontListMap, Locale locale) {
        return listFonts(LIBRARIES, fontToFileMap, fontToFamilyNameMap, familyToFontListMap, locale);
    }

    /** {@link #getLogicalFonts} over the given library names; the entry point of the library-absence tests. */
    static boolean sortLogicalFonts(List<String> libraries, String locale, FontConfigManager.FcCompFont[] fonts,
                                    boolean includeFallbacks) {
        if (fonts == null) {
            return false;
        }
        try (Arena call = Arena.ofConfined()) {
            SymbolLookup fontconfig = openFontConfig(call, libraries);
            if (fontconfig == null) {
                return false;
            }
            MemorySegment[] fc = resolve(fontconfig, SORT_SYMBOLS);
            if (fc == null) {
                return false;
            }
            Session session = new Session(call, fc[SORT_PATTERN_GET_STRING]);
            MemorySegment localeChars = JniStringCodec.allocateModifiedUtf8(call, locale);
            for (FontConfigManager.FcCompFont entry : fonts) {
                MemorySegment fcName = JniStringCodec.allocateModifiedUtf8(call, entry.fcName);
                MemorySegment pattern = (MemorySegment) Fc.NAME_PARSE.invokeExact(fc[SORT_NAME_PARSE], fcName);
                if (pattern.address() == 0) {
                    return false;
                }
                int added = (int) Fc.PATTERN_ADD_STRING.invokeExact(fc[SORT_PATTERN_ADD_STRING], pattern, Fc.LANG,
                                                                    localeChars);
                int substituted = (int) Fc.CONFIG_SUBSTITUTE.invokeExact(fc[SORT_CONFIG_SUBSTITUTE],
                                                                        MemorySegment.NULL, pattern,
                                                                        FC_MATCH_PATTERN);
                Fc.DEFAULT_SUBSTITUTE.invokeExact(fc[SORT_DEFAULT_SUBSTITUTE], pattern);
                MemorySegment fontSet = (MemorySegment) Fc.FONT_SORT.invokeExact(fc[SORT_FONT_SORT],
                                                                                MemorySegment.NULL, pattern, FC_TRUE,
                                                                                MemorySegment.NULL,
                                                                                session.resultOut);
                if (fontSet.address() == 0) {
                    Fc.PATTERN_DESTROY.invokeExact(fc[SORT_PATTERN_DESTROY], pattern);
                    return false;
                }
                List<SortedPattern> sorted = session.patterns(fontSet, fc[SORT_PATTERN_GET_CHAR_SET]);
                FcCharSets charSets = new FcCharSets(fc[SORT_CHAR_SET_SUBTRACT_COUNT], fc[SORT_CHAR_SET_UNION]);
                if (!FcSelection.select(sorted, charSets, includeFallbacks, entry)) {
                    Fc.PATTERN_DESTROY.invokeExact(fc[SORT_PATTERN_DESTROY], pattern);
                    Fc.FONT_SET_DESTROY.invokeExact(fc[SORT_FONT_SET_DESTROY], fontSet);
                    return false;
                }
                Fc.FONT_SET_DESTROY.invokeExact(fc[SORT_FONT_SET_DESTROY], fontSet);
                Fc.PATTERN_DESTROY.invokeExact(fc[SORT_PATTERN_DESTROY], pattern);
            }
            return true;
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /** {@link #populateFontMaps} over the given library names; the entry point of the library-absence tests. */
    static boolean listFonts(List<String> libraries, HashMap<String, String> fontToFileMap,
                             HashMap<String, String> fontToFamilyNameMap,
                             HashMap<String, ArrayList<String>> familyToFontListMap, Locale locale) {
        try (Arena call = Arena.ofConfined()) {
            MemorySegment debugVariable = (MemorySegment) Libc.GETENV.invokeExact(Libc.PRISM_FONTCONFIG_DEBUG);
            boolean debug = debugVariable.address() != 0;
            if (fontToFileMap == null || fontToFamilyNameMap == null || familyToFontListMap == null
                    || locale == null) {
                if (debug) {
                    debug(call, "Null arg to native fontconfig lookup");
                }
                return false;
            }
            SymbolLookup fontconfig = openFontConfig(call, libraries);
            if (fontconfig == null) {
                if (debug) {
                    debug(call, "Could not open libfontconfig\n");
                }
                return false;
            }
            MemorySegment[] fc = resolve(fontconfig, LIST_SYMBOLS);
            if (fc == null) {
                if (debug) {
                    debug(call, "Could not find symbols in libfontconfig\n");
                }
                return false;
            }
            Session session = new Session(call, fc[LIST_PATTERN_GET_STRING]);
            MemorySegment pattern = (MemorySegment) Fc.PATTERN_CREATE.invokeExact(fc[LIST_PATTERN_CREATE]);
            int outline = (int) Fc.PATTERN_ADD_BOOL.invokeExact(fc[LIST_PATTERN_ADD_BOOL], pattern, Fc.OUTLINE,
                                                                FC_TRUE);
            MemorySegment objectSet = (MemorySegment) Fc.OBJECT_SET_CREATE.invokeExact(fc[LIST_OBJECT_SET_CREATE]);
            for (MemorySegment object : Fc.LIST_OBJECTS) {
                int added = (int) Fc.OBJECT_SET_ADD.invokeExact(fc[LIST_OBJECT_SET_ADD], objectSet, object);
            }
            MemorySegment fontSet = (MemorySegment) Fc.FONT_LIST.invokeExact(fc[LIST_FONT_LIST], MemorySegment.NULL,
                                                                            pattern, objectSet);
            if (fontSet.address() == 0) {
                return false;
            }
            int nfont = session.fontCount(fontSet);
            if (debug) {
                debug(call, "Fontconfig found " + nfont + " fonts\n");
            }
            MemorySegment pathname = call.allocate(PATH_MAX + 1);
            for (int f = 0; f < nfont; f++) {
                MemorySegment fontPattern = session.pattern(fontSet, f);
                MemorySegment format = session.string(fontPattern, Fc.FONTFORMAT, 0);
                if (format == null) {
                    continue;
                }
                if (format.address() == 0) {
                    continue;
                }
                byte[] formatBytes = cString(format);
                if (!Arrays.equals(formatBytes, TRUE_TYPE) && !Arrays.equals(formatBytes, CFF)) {
                    continue;
                }
                MemorySegment file = session.string(fontPattern, Fc.FILE, 0);
                if (file == null) {
                    continue;
                }
                MemorySegment path = (MemorySegment) Libc.REALPATH.invokeExact(file, pathname);
                if (path.address() == 0) {
                    continue;
                }
                byte[] fileBytes = cString(pathname);
                byte[] familyEN = null;
                byte[] fullNameEN = null;
                int n = 0;
                while (true) {
                    MemorySegment family = session.string(fontPattern, Fc.FAMILY, n);
                    if (family != null) {
                        MemorySegment familyLang = session.string(fontPattern, Fc.FAMILYLANG, n);
                        if (familyLang != null && family.address() != 0 && familyLang.address() != 0
                                && (familyEN == null || Arrays.equals(cString(familyLang), EN))) {
                            familyEN = cString(family);
                        }
                    }
                    MemorySegment fullName = session.string(fontPattern, Fc.FULLNAME, n);
                    if (fullName != null) {
                        MemorySegment fullNameLang = session.string(fontPattern, Fc.FULLNAMELANG, n);
                        if (fullNameLang != null && fullName.address() != 0 && fullNameLang.address() != 0
                                && (fullNameEN == null || Arrays.equals(cString(fullNameLang), EN))) {
                            fullNameEN = cString(fullName);
                        }
                    }
                    if ((family == null || family.address() == 0) && (fullName == null || fullName.address() == 0)) {
                        break;
                    }
                    n++;
                }
                if (debug) {
                    debug(call, ascii("Read FC font family="), familyEN == null ? ascii("null") : familyEN,
                          ascii(" fullname="), fullNameEN == null ? ascii("null") : fullNameEN, ascii(" file="),
                          fileBytes, ascii("\n"));
                    flushStderr();
                }
                if (familyEN == null || fullNameEN == null) {
                    if (debug) {
                        debug(call, "FC: Skipping on error for above font\n");
                        flushStderr();
                    }
                    continue;
                }
                String fileName = JniStringCodec.fromNewStringUtf(fileBytes);
                String familyName = JniStringCodec.fromNewStringUtf(familyEN);
                String fullNameText = JniStringCodec.fromNewStringUtf(fullNameEN);
                String familyLC = familyName.toLowerCase(locale);
                String fullNameLC = fullNameText.toLowerCase(locale);
                fontToFileMap.put(fullNameLC, fileName);
                fontToFamilyNameMap.put(fullNameLC, familyName);
                ArrayList<String> list = familyToFontListMap.get(familyLC);
                if (list == null) {
                    list = new ArrayList<>(4);
                    familyToFontListMap.put(familyLC, list);
                }
                list.add(fullNameText);
            }
            if (debug) {
                debug(call, "Done enumerating fontconfig fonts\n");
                flushStderr();
            }
            Fc.FONT_SET_DESTROY.invokeExact(fc[LIST_FONT_SET_DESTROY], fontSet);
            return true;
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    /**
     * {@code openFontConfig} (:66-100): the first of {@code libraries} that loads, bound to {@code call}; then
     * {@code putenv("HOME=")} when {@code getenv("HOME")} is {@code NULL}. {@code null} when no name loads, in which
     * case the environment is not touched.
     */
    @SuppressWarnings("restricted")
    private static SymbolLookup openFontConfig(Arena call, List<String> libraries) throws Throwable {
        SymbolLookup library = null;
        for (String name : libraries) {
            try {
                library = SymbolLookup.libraryLookup(name, call);
                break;
            } catch (IllegalArgumentException e) {
                // dlopen failed: the C tried the next name
            }
        }
        if (library == null) {
            return null;
        }
        MemorySegment home = (MemorySegment) Libc.GETENV.invokeExact(Libc.HOME);
        if (home.address() == 0) {
            int rc = (int) Libc.PUTENV.invokeExact(Libc.HOME_ASSIGNMENT);
        }
        return library;
    }

    /** The addresses of {@code names} in {@code library}, or {@code null} when one is missing (the dlsym checks). */
    private static MemorySegment[] resolve(SymbolLookup library, String[] names) {
        MemorySegment[] symbols = new MemorySegment[names.length];
        for (int i = 0; i < names.length; i++) {
            symbols[i] = library.find(names[i]).orElse(null);
            if (symbols[i] == null) {
                return null;
            }
        }
        return symbols;
    }

    /* The symbols getFontConfig resolved (:286-309); FcFontMatch was looked up and checked but never called. */
    private static final String[] SORT_SYMBOLS = {
        "FcNameParse", "FcPatternAddString", "FcConfigSubstitute", "FcDefaultSubstitute", "FcFontMatch",
        "FcPatternGetString", "FcPatternDestroy", "FcPatternGetCharSet", "FcFontSort", "FcFontSetDestroy",
        "FcCharSetUnion", "FcCharSetSubtractCount"
    };
    private static final int SORT_NAME_PARSE = 0;
    private static final int SORT_PATTERN_ADD_STRING = 1;
    private static final int SORT_CONFIG_SUBSTITUTE = 2;
    private static final int SORT_DEFAULT_SUBSTITUTE = 3;
    private static final int SORT_PATTERN_GET_STRING = 5;
    private static final int SORT_PATTERN_DESTROY = 6;
    private static final int SORT_PATTERN_GET_CHAR_SET = 7;
    private static final int SORT_FONT_SORT = 8;
    private static final int SORT_FONT_SET_DESTROY = 9;
    private static final int SORT_CHAR_SET_UNION = 10;
    private static final int SORT_CHAR_SET_SUBTRACT_COUNT = 11;

    /* The symbols populateMapsNative resolved (:592-601), with the builders replaced as the class comment says. */
    private static final String[] LIST_SYMBOLS = {
        "FcPatternCreate", "FcPatternAddBool", "FcObjectSetCreate", "FcObjectSetAdd", "FcFontList",
        "FcPatternGetString", "FcFontSetDestroy"
    };
    private static final int LIST_PATTERN_CREATE = 0;
    private static final int LIST_PATTERN_ADD_BOOL = 1;
    private static final int LIST_OBJECT_SET_CREATE = 2;
    private static final int LIST_OBJECT_SET_ADD = 3;
    private static final int LIST_FONT_LIST = 4;
    private static final int LIST_PATTERN_GET_STRING = 5;
    private static final int LIST_FONT_SET_DESTROY = 6;

    /**
     * The state of one call: its arena, its {@code FcPatternGetString} address and the out-parameter scratch the
     * C kept on its stack. Confined to the calling thread like the arena.
     */
    private static final class Session {

        final Arena arena;
        final MemorySegment patternGetString;
        final MemorySegment stringOut;
        final MemorySegment charSetOut;
        final MemorySegment resultOut;

        Session(Arena arena, MemorySegment patternGetString) {
            this.arena = arena;
            this.patternGetString = patternGetString;
            this.stringOut = arena.allocate(ADDRESS);
            this.charSetOut = arena.allocate(ADDRESS);
            this.resultOut = arena.allocate(JAVA_INT);
        }

        /** {@code fontSet->nfont}: the {@code int} at offset 0 of {@code FcFontSet} (sizeof 16, nfont 0, fonts 8). */
        @SuppressWarnings("restricted")
        int fontCount(MemorySegment fontSet) {
            return fontSet.reinterpret(16).get(JAVA_INT, 0);
        }

        /** {@code fontSet->fonts[index]}. */
        @SuppressWarnings("restricted")
        MemorySegment pattern(MemorySegment fontSet, int index) {
            MemorySegment fonts = fontSet.reinterpret(16).get(ADDRESS, 8);
            return fonts.reinterpret(ADDRESS.byteSize() * (index + 1)).getAtIndex(ADDRESS, index);
        }

        /** Lazy views over {@code fontSet->fonts[0 .. nfont)}, for {@link FcSelection}. */
        List<SortedPattern> patterns(MemorySegment fontSet, MemorySegment patternGetCharSet) {
            int nfont = fontCount(fontSet);
            List<SortedPattern> patterns = new ArrayList<>(nfont);
            for (int j = 0; j < nfont; j++) {
                patterns.add(new SortedPattern(this, pattern(fontSet, j), patternGetCharSet));
            }
            return patterns;
        }

        /**
         * {@code FcPatternGetString(pattern, object, n, &s)}: the {@code FcChar8 *} when the result is
         * {@code FcResultMatch}, else {@code null} (the C's out slot stays as it was).
         */
        MemorySegment string(MemorySegment pattern, MemorySegment object, int n) throws Throwable {
            stringOut.set(ADDRESS, 0, MemorySegment.NULL);
            int result = (int) Fc.PATTERN_GET_STRING.invokeExact(patternGetString, pattern, object, n, stringOut);
            return result == FC_RESULT_MATCH ? stringOut.get(ADDRESS, 0) : null;
        }

        /** {@link #string} decoded as {@code NewStringUTF} decoded it; {@code null} for no match or a {@code NULL}. */
        String javaString(MemorySegment pattern, MemorySegment object, int n) throws Throwable {
            MemorySegment s = string(pattern, object, n);
            if (s == null || s.address() == 0) {
                return null;
            }
            return JniStringCodec.fromNewStringUtf(cString(s));
        }
    }

    /** One pattern of an {@code FcFontSort} result, read on demand (:411-460). */
    private static final class SortedPattern implements FcSelection.SortedFont<MemorySegment> {

        private final Session session;
        private final MemorySegment pattern;
        private final MemorySegment patternGetCharSet;

        SortedPattern(Session session, MemorySegment pattern, MemorySegment patternGetCharSet) {
            this.session = session;
            this.pattern = pattern;
            this.patternGetCharSet = patternGetCharSet;
        }

        @Override
        public byte[] fontFormat() {
            try {
                MemorySegment format = session.string(pattern, Fc.FONTFORMAT, 0);
                return format == null || format.address() == 0 ? null : cString(format);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }

        @Override
        public MemorySegment charSet() {
            try {
                int result = (int) Fc.PATTERN_GET_CHAR_SET.invokeExact(patternGetCharSet, pattern, Fc.CHARSET, 0,
                                                                       session.charSetOut);
                return result == FC_RESULT_MATCH ? session.charSetOut.get(ADDRESS, 0) : null;
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }

        @Override
        public String file() {
            return read(Fc.FILE);
        }

        @Override
        public String family() {
            return read(Fc.FAMILY);
        }

        @Override
        public String style() {
            return read(Fc.STYLE);
        }

        @Override
        public String fullName() {
            return read(Fc.FULLNAME);
        }

        private String read(MemorySegment object) {
            try {
                return session.javaString(pattern, object, 0);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
    }

    /** {@code FcCharSetSubtractCount} and {@code FcCharSetUnion} of one call (:448-450). */
    private static final class FcCharSets implements FcSelection.CharSets<MemorySegment> {

        private final MemorySegment subtractCount;
        private final MemorySegment union;

        FcCharSets(MemorySegment subtractCount, MemorySegment union) {
            this.subtractCount = subtractCount;
            this.union = union;
        }

        @Override
        public int subtractCount(MemorySegment a, MemorySegment b) {
            try {
                return (int) Fc.CHAR_SET_SUBTRACT_COUNT.invokeExact(subtractCount, a, b);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }

        @Override
        public MemorySegment union(MemorySegment a, MemorySegment b) {
            try {
                return (MemorySegment) Fc.CHAR_SET_UNION.invokeExact(union, a, b);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
    }

    /** The bytes of the C string at {@code pointer} (not {@code NULL}) before its terminator, bounded by strlen. */
    @SuppressWarnings("restricted")
    private static byte[] cString(MemorySegment pointer) throws Throwable {
        long length = (long) Libc.STRLEN.invokeExact(pointer);
        return pointer.reinterpret(length).toArray(JAVA_BYTE);
    }

    private static byte[] ascii(String text) {
        return text.getBytes(StandardCharsets.US_ASCII);
    }

    /** {@code fprintf(stderr, text)} for a message without conversions: {@code fputs} on libc's {@code stderr}. */
    private static void debug(Arena call, String text) throws Throwable {
        debug(call, ascii(text));
    }

    /** {@code fprintf(stderr, ...)} with the {@code %s} bytes already substituted: one {@code fputs} of the whole. */
    private static void debug(Arena call, byte[]... parts) throws Throwable {
        int length = 0;
        for (byte[] part : parts) {
            length += part.length;
        }
        byte[] text = new byte[length + 1];
        int p = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, text, p, part.length);
            p += part.length;
        }
        int rc = (int) Libc.FPUTS.invokeExact(call.allocateFrom(JAVA_BYTE, text), Libc.stderr());
    }

    /** {@code fflush(stderr)}. */
    private static void flushStderr() throws Throwable {
        int rc = (int) Libc.FFLUSH.invokeExact(Libc.stderr());
    }

    private static RuntimeException rethrow(Throwable t) {
        if (t instanceof RuntimeException runtime) {
            return runtime;
        }
        if (t instanceof Error error) {
            throw error;
        }
        return new IllegalStateException(t);
    }

    /** libc, through the linker's default lookup; bound on first use only. */
    @SuppressWarnings("restricted")
    private static final class Libc {

        static final Linker LINKER = Linker.nativeLinker();

        /** {@code char *getenv(const char *)}. */
        static final MethodHandle GETENV = downcall("getenv", FunctionDescriptor.of(ADDRESS, ADDRESS));

        /** {@code int putenv(char *)}: the string becomes part of the environment. */
        static final MethodHandle PUTENV = downcall("putenv", FunctionDescriptor.of(JAVA_INT, ADDRESS));

        /** {@code char *realpath(const char *, char *)}. */
        static final MethodHandle REALPATH = downcall("realpath", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));

        /** {@code size_t strlen(const char *)}. */
        static final MethodHandle STRLEN = downcall("strlen", FunctionDescriptor.of(JAVA_LONG, ADDRESS));

        /** {@code int fputs(const char *, FILE *)}. */
        static final MethodHandle FPUTS = downcall("fputs", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));

        /** {@code int fflush(FILE *)}. */
        static final MethodHandle FFLUSH = downcall("fflush", FunctionDescriptor.of(JAVA_INT, ADDRESS));

        /** The {@code FILE *stderr} variable; read at each use, as {@code fprintf(stderr, ...)} reads it. */
        static final MemorySegment STDERR_VARIABLE = LINKER.defaultLookup().find("stderr")
                .orElseThrow(() -> new UnsatisfiedLinkError("libc exports no stderr"))
                .reinterpret(ADDRESS.byteSize());

        static final MemorySegment HOME = Arena.global().allocateFrom("HOME");

        /** {@code static char *homeEnvStr = "HOME="} (:69): the environment keeps the pointer; never freed. */
        static final MemorySegment HOME_ASSIGNMENT = Arena.global().allocateFrom("HOME=");

        static final MemorySegment PRISM_FONTCONFIG_DEBUG = Arena.global().allocateFrom("PRISM_FONTCONFIG_DEBUG");

        static MemorySegment stderr() {
            return STDERR_VARIABLE.get(ADDRESS, 0);
        }

        private static MethodHandle downcall(String name, FunctionDescriptor descriptor) {
            MemorySegment symbol = LINKER.defaultLookup().find(name)
                    .orElseThrow(() -> new UnsatisfiedLinkError("libc exports no " + name));
            return LINKER.downcallHandle(symbol, descriptor);
        }
    }

    /**
     * fontconfig: address-less downcall handles (the address comes from the call's own library lookup) and the
     * object names of {@code fontconfig.h}; bound on first use only.
     */
    @SuppressWarnings("restricted")
    private static final class Fc {

        /** {@code FcPattern *FcNameParse(const FcChar8 *)}. */
        static final MethodHandle NAME_PARSE = downcall(FunctionDescriptor.of(ADDRESS, ADDRESS));

        /** {@code FcBool FcPatternAddString(FcPattern *, const char *, const FcChar8 *)}. */
        static final MethodHandle PATTERN_ADD_STRING = downcall(FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS,
                                                                                      ADDRESS));

        /** {@code FcBool FcConfigSubstitute(FcConfig *, FcPattern *, FcMatchKind)}. */
        static final MethodHandle CONFIG_SUBSTITUTE = downcall(FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS,
                                                                                     JAVA_INT));

        /** {@code void FcDefaultSubstitute(FcPattern *)}. */
        static final MethodHandle DEFAULT_SUBSTITUTE = downcall(FunctionDescriptor.ofVoid(ADDRESS));

        /** {@code FcFontSet *FcFontSort(FcConfig *, FcPattern *, FcBool, FcCharSet **, FcResult *)}. */
        static final MethodHandle FONT_SORT = downcall(FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, JAVA_INT,
                                                                             ADDRESS, ADDRESS));

        /** {@code FcResult FcPatternGetString(const FcPattern *, const char *, int, FcChar8 **)}. */
        static final MethodHandle PATTERN_GET_STRING = downcall(FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS,
                                                                                      JAVA_INT, ADDRESS));

        /** {@code FcResult FcPatternGetCharSet(const FcPattern *, const char *, int, FcCharSet **)}. */
        static final MethodHandle PATTERN_GET_CHAR_SET = downcall(FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS,
                                                                                        JAVA_INT, ADDRESS));

        /** {@code FcChar32 FcCharSetSubtractCount(const FcCharSet *, const FcCharSet *)}. */
        static final MethodHandle CHAR_SET_SUBTRACT_COUNT = downcall(FunctionDescriptor.of(JAVA_INT, ADDRESS,
                                                                                           ADDRESS));

        /** {@code FcCharSet *FcCharSetUnion(const FcCharSet *, const FcCharSet *)}. */
        static final MethodHandle CHAR_SET_UNION = downcall(FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));

        /** {@code void FcFontSetDestroy(FcFontSet *)}. */
        static final MethodHandle FONT_SET_DESTROY = downcall(FunctionDescriptor.ofVoid(ADDRESS));

        /** {@code void FcPatternDestroy(FcPattern *)}. */
        static final MethodHandle PATTERN_DESTROY = downcall(FunctionDescriptor.ofVoid(ADDRESS));

        /** {@code FcPattern *FcPatternCreate(void)}. */
        static final MethodHandle PATTERN_CREATE = downcall(FunctionDescriptor.of(ADDRESS));

        /** {@code FcBool FcPatternAddBool(FcPattern *, const char *, FcBool)}. */
        static final MethodHandle PATTERN_ADD_BOOL = downcall(FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS,
                                                                                    JAVA_INT));

        /** {@code FcObjectSet *FcObjectSetCreate(void)}. */
        static final MethodHandle OBJECT_SET_CREATE = downcall(FunctionDescriptor.of(ADDRESS));

        /** {@code FcBool FcObjectSetAdd(FcObjectSet *, const char *)}. */
        static final MethodHandle OBJECT_SET_ADD = downcall(FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));

        /** {@code FcFontSet *FcFontList(FcConfig *, FcPattern *, FcObjectSet *)}. */
        static final MethodHandle FONT_LIST = downcall(FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS));

        static final MemorySegment LANG = Arena.global().allocateFrom("lang");
        static final MemorySegment FONTFORMAT = Arena.global().allocateFrom("fontformat");
        static final MemorySegment CHARSET = Arena.global().allocateFrom("charset");
        static final MemorySegment FILE = Arena.global().allocateFrom("file");
        static final MemorySegment FAMILY = Arena.global().allocateFrom("family");
        static final MemorySegment STYLE = Arena.global().allocateFrom("style");
        static final MemorySegment FULLNAME = Arena.global().allocateFrom("fullname");
        static final MemorySegment OUTLINE = Arena.global().allocateFrom("outline");
        static final MemorySegment FAMILYLANG = Arena.global().allocateFrom("familylang");
        static final MemorySegment FULLNAMELANG = Arena.global().allocateFrom("fullnamelang");

        /** The object set of the enumeration, in the order {@code FcObjectSetBuild} received it (:668-670). */
        static final MemorySegment[] LIST_OBJECTS = {FAMILY, FAMILYLANG, FULLNAME, FULLNAMELANG, FILE, FONTFORMAT};

        private static MethodHandle downcall(FunctionDescriptor descriptor) {
            return Libc.LINKER.downcallHandle(descriptor);
        }
    }
}
