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

import com.sun.javafx.font.PrismFontFactoryShim;
import com.sun.javafx.font.WinFontPathShim;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The pure string logic {@code com.sun.javafx.font.WinFontPath} took over from {@code fontpath.c},
 * checked on every platform against the C algorithms traced by hand: the font-path assembly with the
 * {@code _wcsnicmp} prefix quirks, the registry name and file-name filters, the right-to-left
 * collection split of {@code registerFontW}, the EUDC key table and the EUDC path resolution with its
 * retained NUL terminator; and, on Windows only, the user-hive-then-machine-hive order of the two registry
 * passes, which the golden cannot see. The machine's actual values are pinned by
 * {@link FontEnumerationGoldenTest}.
 * <p>
 * Line numbers into {@code fontpath.c}, bare {@code LN} forms included, refer to it at commit {@code 8492cb03b0}
 * ({@code git show 8492cb03b0:modules/javafx.graphics/src/main/native-font/fontpath.c}).
 */
public class WinFontPathTest {

    private static final int MAX_PATH = 260;
    private static final String NUL = String.valueOf((char) 0);

    private static final Supplier<String> NO_WINDOWS_DIRECTORY = () -> {
        throw new AssertionError("GetWindowsDirectoryW must not be consulted on this path");
    };

    // ---------------------------------------------------------------------------------------------
    // getFontPath
    // ---------------------------------------------------------------------------------------------

    @Test
    public void fontPathReplacesASystemOrSystem32TailWithFonts() {
        assertEquals("C:\\WINDOWS\\Fonts", fontPath("C:\\WINDOWS\\system32", "C:\\WINDOWS"));
        assertEquals("C:\\WINDOWS\\Fonts", fontPath("C:\\WINDOWS\\System", "C:\\WINDOWS"));
        assertEquals("c:\\windows\\Fonts", fontPath("c:\\windows\\SYSTEM32", "C:\\WINDOWS"),
                "ASCII case folded in both comparisons, the system directory's own spelling kept");
    }

    @Test
    public void fontPathJoinsTheWindowsFontsDirectoryWhenTheyDiffer() {
        assertEquals("D:\\Shared\\Fonts;C:\\WINDOWS\\Fonts", fontPath("D:\\Shared\\system32", "C:\\WINDOWS"));
        assertEquals("C:\\WINDOWS\\system32x;C:\\WINDOWS\\Fonts", fontPath("C:\\WINDOWS\\system32x", "C:\\WINDOWS"),
                "a longer tail is not stripped and the system directory is returned as it is, without Fonts");
        assertEquals("system32;C:\\WINDOWS\\Fonts", fontPath("system32", "C:\\WINDOWS"),
                "no backslash: nothing stripped");
    }

    @Test
    public void fontPathKeepsTheWcsnicmpQuirksOfTheC() {
        // _wcsnicmp(end, "\\System32", endLen) is a prefix test: any prefix of the literal matches.
        assertEquals("C:\\WINDOWS\\Fonts", fontPath("C:\\WINDOWS\\Sys", "C:\\WINDOWS"));
        assertEquals("C:\\WINDOWS\\Fonts", fontPath("C:\\WINDOWS\\", "C:\\WINDOWS"));
        // The join compares only over the shorter path, so a prefix counts as the same directory.
        assertEquals("C:\\WINDOWS", fontPath("C:\\WINDOWS", "C:\\WINDOWS"));
        assertEquals("C:\\WIN", fontPath("C:\\WIN", "C:\\WINDOWS"));
    }

    @Test
    public void wcsnicmpFoldsAsciiOnlyAndStopsWhereBothStringsEnd() {
        String eAcute = String.valueOf((char) 0xE9);
        String eAcuteUpper = String.valueOf((char) 0xC9);
        assertTrue(WinFontPathShim.wcsnicmpEqual("abc", 0, "ABC", 3));
        assertTrue(WinFontPathShim.wcsnicmpEqual("xxabc", 2, "ABCDEF", 3));
        assertFalse(WinFontPathShim.wcsnicmpEqual("abd", 0, "ABC", 3));
        assertTrue(WinFontPathShim.wcsnicmpEqual("ab", 0, "AB", 10), "both end before count: equal");
        assertFalse(WinFontPathShim.wcsnicmpEqual("ab", 0, "ABC", 3), "one ends first: different");
        assertFalse(WinFontPathShim.wcsnicmpEqual(eAcute, 0, eAcuteUpper, 1), "no Unicode folding in the C locale");
        assertTrue(WinFontPathShim.wcsnicmpEqual(eAcute, 0, eAcute, 1));
        assertTrue(WinFontPathShim.wcsicmpEqual(".TTF", ".ttf"));
        assertFalse(WinFontPathShim.wcsicmpEqual(".ttf", ".ttfx"));
        assertFalse(WinFontPathShim.wcsicmpEqual("", "a"));
        assertTrue(WinFontPathShim.wcsicmpEqual("", ""));
    }

    // ---------------------------------------------------------------------------------------------
    // Registry Fonts key filters
    // ---------------------------------------------------------------------------------------------

    @Test
    public void registryToBaseTTNameStripsExactlyTheTrueTypeSuffix() {
        assertEquals("Arial", WinFontPathShim.registryToBaseTTName("Arial (TrueType)"));
        assertEquals("X", WinFontPathShim.registryToBaseTTName("X (TrueType)"));
        assertEquals("Foo & Bar", WinFontPathShim.registryToBaseTTName("Foo & Bar (TrueType)"));
        assertNull(WinFontPathShim.registryToBaseTTName("Arial (OpenType)"), "OpenType stays disabled");
        assertNull(WinFontPathShim.registryToBaseTTName("Arial (truetype)"), "case-sensitive");
        assertNull(WinFontPathShim.registryToBaseTTName("Arial"));
        assertNull(WinFontPathShim.registryToBaseTTName("Arial (TrueType) "));
        assertNull(WinFontPathShim.registryToBaseTTName(" (TrueType)"), "nothing before the suffix");
        assertNull(WinFontPathShim.registryToBaseTTName(""));
        assertNull(WinFontPathShim.registryToBaseTTName(")"));
    }

    @Test
    public void fileNamesAreAcceptedByTtfOrOtfExtensionOnly() {
        assertTrue(WinFontPathShim.hasTrueTypeExtension("arial.ttf"));
        assertTrue(WinFontPathShim.hasTrueTypeExtension("ARIAL.TTF"));
        assertTrue(WinFontPathShim.hasTrueTypeExtension(
                "C:\\Users\\x\\AppData\\Local\\Microsoft\\Windows\\Fonts\\F[wght].otf"));
        assertTrue(WinFontPathShim.hasTrueTypeExtension("x.OtF"));
        assertFalse(WinFontPathShim.hasTrueTypeExtension("msgothic.ttc"));
        assertFalse(WinFontPathShim.hasTrueTypeExtension("x.ttf.bak"));
        assertFalse(WinFontPathShim.hasTrueTypeExtension("noextension"));
        assertFalse(WinFontPathShim.hasTrueTypeExtension(""));
        assertFalse(WinFontPathShim.hasTrueTypeExtension("x.ttfx"));
    }

    // ---------------------------------------------------------------------------------------------
    // registerFontW
    // ---------------------------------------------------------------------------------------------

    @Test
    public void registerFontSplitsCollectionsFromTheRightAndLowerCasesEveryName() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        registerFont(map, "MS Gothic & MS PGothic & MS UI Gothic", "msgothic.ttc");
        assertEquals(List.of("ms ui gothic", "ms pgothic", "ms gothic"), List.copyOf(map.keySet()),
                "right to left, as the C walked ptr1");
        assertEquals("msgothic.ttc", map.get("ms gothic"));
        assertEquals("msgothic.ttc", map.get("ms pgothic"));
        assertEquals("msgothic.ttc", map.get("ms ui gothic"));

        map.clear();
        registerFont(map, "Cambria & Cambria Math", "CAMBRIA.TTC");
        assertEquals(List.of("cambria math", "cambria"), List.copyOf(map.keySet()), "an upper-case C too");

        map.clear();
        registerFont(map, "Foo & Bar", "foobar.ttf");
        assertEquals(List.of("foo & bar"), List.copyOf(map.keySet()), "only data ending in C/c is split");

        map.clear();
        registerFont(map, "Solo", "solo.ttc");
        assertEquals(List.of("solo"), List.copyOf(map.keySet()));

        map.clear();
        registerFont(map, "Foo & Bar", "");
        assertEquals(List.of("foo & bar"), List.copyOf(map.keySet()), "empty data: no collection");
    }

    @Test
    public void registerFontKeepsTheSeparatorEdgeCasesOfThePointerWalk() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        registerFont(map, " & Foo", "x.ttc");
        assertEquals(List.of("foo", ""), List.copyOf(map.keySet()), "a leading separator leaves an empty first name");

        map.clear();
        registerFont(map, "Foo & ", "x.ttc");
        assertEquals(List.of("", "foo"), List.copyOf(map.keySet()), "a trailing separator leaves an empty last name");

        map.clear();
        registerFont(map, "A & B & C & D", "x.TTC");
        assertEquals(List.of("d", "c", "b", "a"), List.copyOf(map.keySet()));

        map.clear();
        registerFont(map, "A &  & B", "x.ttc");
        assertEquals(List.of("b", "", "a"), List.copyOf(map.keySet()),
                "adjacent separators: after the truncation to \"A & \" the last piece is empty");
    }

    @Test
    public void registerFontLowerCasesInTheGivenLocale() {
        Locale turkish = Locale.forLanguageTag("tr");
        String title = "TITLE".toLowerCase(turkish);
        assertNotEquals("title", title, "the locale is observable: dotless i");
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        WinFontPathShim.registerFont(map, "TITLE", "t.ttf", turkish);
        assertEquals(List.of(title), List.copyOf(map.keySet()));
        map.clear();
        WinFontPathShim.registerFont(map, "TITLE & SUB", "t.ttc", turkish);
        assertEquals(List.of("SUB".toLowerCase(turkish), title), List.copyOf(map.keySet()));
    }

    // ---------------------------------------------------------------------------------------------
    // getEUDCFontFile
    // ---------------------------------------------------------------------------------------------

    @Test
    public void eudcKeyFollowsTheSystemLanguage() {
        assertEquals("EUDC\\932", WinFontPathShim.eudcKey(0x411));
        assertEquals("EUDC\\936", WinFontPathShim.eudcKey(0x0804));
        assertEquals("EUDC\\936", WinFontPathShim.eudcKey(0x1004));
        assertEquals("EUDC\\950", WinFontPathShim.eudcKey(0x0404));
        assertEquals("EUDC\\950", WinFontPathShim.eudcKey(0x0c04));
        assertEquals("EUDC\\950", WinFontPathShim.eudcKey(0x1404));
        assertEquals("EUDC\\949", WinFontPathShim.eudcKey(0x0412));
        assertEquals("EUDC\\1252", WinFontPathShim.eudcKey(0x409));
        assertNull(WinFontPathShim.eudcKey(0x407), "German: no EUDC");
        assertNull(WinFontPathShim.eudcKey(0x0809), "en-GB is not LANGID_US");
        assertNull(WinFontPathShim.eudcKey(0));
    }

    @Test
    public void eudcFontFileExpandsSystemRootWithinMaxPath() {
        char[] value = ("%SystemRoot%\\Fonts\\EUDC.TTE" + NUL).toCharArray();
        int length = value.length;
        assertEquals("C:\\WINDOWS\\Fonts\\EUDC.TTE", eudcFontFile(value, length, "C:\\WINDOWS"),
                "expanded from the text up to the NUL, the stored NUL dropped by wcscat");
        assertNull(eudcFontFile(value, length, null), "no SystemRoot in the environment");
        // L917: fontPathLen - 12 + wcslen(systemRoot) > MAX_PATH, with fontPathLen counting the stored NUL
        String justFits = "x".repeat(MAX_PATH - (length - 12));
        assertEquals(justFits + "\\Fonts\\EUDC.TTE", eudcFontFile(value, length, justFits));
        assertNull(eudcFontFile(value, length, justFits + "x"));
        // The prefix test is wcsstr: case-sensitive. Anything else falls through and is returned raw.
        char[] lowerCase = ("%systemroot%\\x" + NUL).toCharArray();
        assertEquals("%systemroot%\\x" + NUL, eudcFontFile(lowerCase, lowerCase.length, "C:\\WINDOWS"));
    }

    @Test
    public void eudcFontFileResolvesABareEudcTteUnderTheWindowsFontsDirectory() {
        char[] value = ("EUDC.TTE" + NUL).toCharArray();
        int length = value.length;
        assertEquals("C:\\WINDOWS\\FONTS\\EUDC.TTE",
                WinFontPathShim.eudcFontFile(value, length, "ignored", () -> "C:\\WINDOWS"));
        assertNull(WinFontPathShim.eudcFontFile(value, length, null, () -> null), "GetWindowsDirectoryW failed");
        // L930: ret + 16 > MAX_PATH
        assertEquals("x".repeat(244) + "\\FONTS\\EUDC.TTE",
                WinFontPathShim.eudcFontFile(value, length, null, () -> "x".repeat(244)));
        assertNull(WinFontPathShim.eudcFontFile(value, length, null, () -> "x".repeat(245)));
        char[] lowerCase = ("eudc.tte" + NUL).toCharArray();
        assertEquals("eudc.tte" + NUL, eudcFontFile(lowerCase, lowerCase.length, null), "wcscmp is case-sensitive");
    }

    @Test
    public void eudcFontFileReturnsOtherValuesRawIncludingTheStoredTerminator() {
        char[] value = ("D:\\Fonts\\Custom.tte" + NUL).toCharArray();
        assertEquals("D:\\Fonts\\Custom.tte" + NUL, eudcFontFile(value, value.length, "C:\\WINDOWS"),
                "NewString(fontPath, fontPathLen) handed back the terminator the registry stored");
        assertEquals("D:\\Fonts", eudcFontFile(value, 8, "C:\\WINDOWS"), "only fontPathLen characters");
        assertEquals("", eudcFontFile(value, 0, "C:\\WINDOWS"));
    }

    @Test
    public void cStringStopsAtTheFirstNulOrTheEndOfTheBuffer() {
        assertEquals("abc", WinFontPathShim.cString(new char[] {'a', 'b', 'c', 0, 'd'}));
        assertEquals("abc", WinFontPathShim.cString(new char[] {'a', 'b', 'c'}), "no NUL: the whole buffer");
        assertEquals("", WinFontPathShim.cString(new char[] {0}));
        assertEquals("", WinFontPathShim.cString(new char[0]));
    }

    // ---------------------------------------------------------------------------------------------
    // populateFontFileNameMap: HKEY_CURRENT_USER first, HKEY_LOCAL_MACHINE second (fontpath.c L786-791)
    // ---------------------------------------------------------------------------------------------

    /**
     * The registry half of {@code populateFontFileNameMap} reads the user hive, then the machine hive, into
     * one map through plain puts, so the combined map is the user map with the machine map merged over it,
     * name by name. The golden cannot see the order: the capture machine's user hive holds one font that
     * the machine hive does not, so either order yields the same map.
     */
    @Test
    @EnabledOnOs(OS.WINDOWS)
    public void registryFontsAreMergedUserHiveFirstThenMachineHive() {
        HashMap<String, String> user = new HashMap<>();
        WinFontPathShim.populateFontFileNamesFromCurrentUser(user, Locale.ENGLISH);
        HashMap<String, String> machine = new HashMap<>();
        WinFontPathShim.populateFontFileNamesFromLocalMachine(machine, Locale.ENGLISH);
        assertTrue(machine.size() > 10, "HKLM Fonts yielded " + machine.size() + " entries");

        HashMap<String, String> expected = new HashMap<>(user);
        expected.putAll(machine);
        HashMap<String, String> combined = new HashMap<>();
        PrismFontFactoryShim.populateFontFileNameMap(combined, new HashMap<>(), new HashMap<>(), Locale.ENGLISH);
        assertEquals(expected.keySet(), combined.keySet(), "every name of either hive, and nothing else");
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            assertEquals(entry.getValue(), combined.get(entry.getKey()), entry.getKey());
        }
    }

    /**
     * The precedence itself, with the collision this machine's registry may not contain synthesised: a map
     * already holding a name the machine hive also holds - what the user pass leaves behind - is run through
     * the machine pass, and the machine hive's file wins; a name the machine hive does not hold survives
     * that pass untouched. This is the shape JDK-8281327 / JDK-8311124 left at {@code fontpath.c} L790-791
     * and {@code WinFontPath.populateFontFileNameMap} keeps.
     */
    @Test
    @EnabledOnOs(OS.WINDOWS)
    public void aNameInBothHivesTakesTheMachineHiveFileAndAUserOnlyNameSurvives() {
        HashMap<String, String> machine = new HashMap<>();
        WinFontPathShim.populateFontFileNamesFromLocalMachine(machine, Locale.ENGLISH);
        assertFalse(machine.isEmpty(), "HKLM Fonts is empty");
        String shared = machine.containsKey("arial") ? "arial" : machine.keySet().iterator().next();
        String userFile = "C:\\Users\\jfx\\AppData\\Local\\Microsoft\\Windows\\Fonts\\synthetic-user.ttf";
        String userOnly = "synthetic user-only font (jfx precedence test)";

        HashMap<String, String> map = new HashMap<>();
        map.put(shared, userFile);
        map.put(userOnly, userFile);
        WinFontPathShim.populateFontFileNamesFromLocalMachine(map, Locale.ENGLISH);
        assertEquals(machine.get(shared), map.get(shared), "the machine pass, second, overwrites " + shared);
        assertNotEquals(userFile, map.get(shared));
        assertEquals(userFile, map.get(userOnly), "a name only the user pass registered is kept");
        assertEquals(machine.size() + 1, map.size());
    }

    // ---------------------------------------------------------------------------------------------

    private static String fontPath(String systemDirectory, String windowsDirectory) {
        return WinFontPathShim.fontPath(systemDirectory, windowsDirectory);
    }

    private static void registerFont(LinkedHashMap<String, String> map, String name, String data) {
        WinFontPathShim.registerFont(map, name, data, Locale.ENGLISH);
    }

    private static String eudcFontFile(char[] value, int length, String systemRoot) {
        return WinFontPathShim.eudcFontFile(value, length, systemRoot, NO_WINDOWS_DIRECTORY);
    }
}
