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

package test.com.sun.glass.ui.win;

import com.sun.glass.events.KeyEvent;
import com.sun.glass.ui.win.WinGlassNativeShim;
import com.sun.glass.ui.win.WinGlassNativeShim.HighContrastInfo;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import test.com.sun.javafx.test.ParityGate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The preferences part of the Windows Glass migration (ABI 2): the whole of {@code PlatformSupport.cpp}'s
 * preference map, now built in Java, plus the one {@code KeyTable.cpp} JNI body that was a two-case switch over
 * {@code GetKeyState} ({@code _isKeyLocked}, L421-439).
 * <p>
 * Two of the four sources of that map are plain {@code user32} calls Java makes itself
 * ({@code PlatformSupport::querySystemColors}, {@code querySystemParameters}); the other two are
 * WinRT and reach Java as POD through {@code gwin_prefs_query_ui_settings} /
 * {@code gwin_prefs_query_network}. Around them is {@code updatePreferences}, which is now
 * the {@code GwinPrefsCallbacks} upcall. The initial map behind
 * {@code WinApplication.getPlatformPreferences} was the last thing built in C; it is now
 * {@code WinPreferences.collect(GWIN_PT_ALL)} as well, and that this seam is closed is asserted here
 * rather than described.
 * <p>
 * For the two {@code user32} groups the C added nothing to the OS call, so there is no
 * {@code gwin_*} export for them and no C left to compare against once they are flipped. The
 * parity evidence therefore comes from three independent places, and the interesting one is the
 * third:
 * <ul>
 * <li>the peer, {@code WinApplication._isKeyLocked}, which was the JNI body when this file was
 *     written and is the same Java afterwards ({@link #bothWaysToAskAboutALockAgree()});
 * <li>invariants of the Win32 types themselves - a {@code COLORREF} has a zero high byte, an
 *     absent {@code SystemParametersInfoW} answer contributes no key at all;
 * <li><b>the AWT Windows toolkit</b>, which calls exactly the same {@code GetSysColor} and
 *     {@code SPI_GETHIGHCONTRAST} from its own C and publishes the results as desktop properties
 *     ({@code sun.awt.windows.WDesktopProperties}). That is the only oracle in this JVM that can
 *     tell {@code 0x00BBGGRR} from {@code 0x00RRGGBB} on a live theme, which is the one mistake in
 *     the preferences code that would produce plausible colours and no failure anywhere else.
 * </ul>
 * The AWT half is skipped rather than failed when the toolkit cannot be reached (a headless fork, a
 * session without a window station, a property this Windows version does not publish): it is an
 * oracle, not the subject.
 * <p>
 * Line numbers into the {@code native-glass/win} C++ sources, bare {@code LN} forms included, refer to those files
 * at commit {@code 8492cb03b0} ({@code git show 8492cb03b0:modules/javafx.graphics/src/main/native-glass/win/<file>}).
 */
@EnabledOnOs(OS.WINDOWS)
public class WinPreferencesNativeTest {

    /** The eight {@code Windows.SysColor.*} keys, in the order {@code querySystemColors} queries. */
    private static final List<String> SYSTEM_COLOR_KEYS = List.of(
            "Windows.SysColor.COLOR_3DFACE", "Windows.SysColor.COLOR_BTNTEXT",
            "Windows.SysColor.COLOR_GRAYTEXT", "Windows.SysColor.COLOR_HIGHLIGHT",
            "Windows.SysColor.COLOR_HIGHLIGHTTEXT", "Windows.SysColor.COLOR_HOTLIGHT",
            "Windows.SysColor.COLOR_WINDOW", "Windows.SysColor.COLOR_WINDOWTEXT");

    private static final String HIGH_CONTRAST = "Windows.SPI.HighContrast";
    private static final String HIGH_CONTRAST_COLOR_SCHEME = "Windows.SPI.HighContrastColorScheme";
    private static final String CLIENT_AREA_ANIMATION = "Windows.SPI.ClientAreaAnimation";

    /**
     * The nine {@code Windows.UIColor.*} keys, in the order {@code PlatformSupport::queryUISettings}
     * asked {@code IUISettings3::GetColorValue} for them, which is the order
     * {@code GwinUiSettings.colors} is in. Spelled out here rather than read from the collector, so
     * that a missing, extra or misspelt key fails instead of agreeing with itself. The order is
     * documentation only: every use of this list below is a set comparison, so reordering it passes,
     * and so does reordering {@code WinPreferences.UI_COLOR_KEYS} or the C's {@code colorTypes} - which
     * would put colours under the wrong keys. Nothing in this class catches that; the colour-to-key
     * mapping was checked by the JNI A/B while both arms existed.
     */
    private static final List<String> UI_COLOR_KEYS = List.of(
            "Windows.UIColor.Background", "Windows.UIColor.Foreground", "Windows.UIColor.AccentDark3",
            "Windows.UIColor.AccentDark2", "Windows.UIColor.AccentDark1", "Windows.UIColor.Accent",
            "Windows.UIColor.AccentLight1", "Windows.UIColor.AccentLight2",
            "Windows.UIColor.AccentLight3");

    private static final String ADVANCED_EFFECTS_ENABLED = "Windows.UISettings.AdvancedEffectsEnabled";
    private static final String AUTO_HIDE_SCROLL_BARS = "Windows.UISettings.AutoHideScrollBars";
    private static final String INTERNET_COST_TYPE = "Windows.NetworkInformation.InternetCostType";

    /**
     * {@code GetSysColor} index to the AWT desktop property that reports the same colour.
     * {@code sun.awt.windows.WDesktopProperties} builds each of these with
     * {@code GetRValue}/{@code GetGValue}/{@code GetBValue} on the same {@code COLORREF}, so equality
     * is a real cross-check of the byte order and not a tautology. {@code COLOR_GRAYTEXT} and
     * {@code COLOR_HOTLIGHT} are not in the list because this Windows does not publish a property for
     * them.
     */
    private static final Map<String, String> COLOUR_ORACLE = colourOracle();

    private static final ParityGate.Ledger LEDGER = ParityGate.ledger(WinPreferencesNativeTest.class);

    @BeforeAll
    static void requireNatives() {
        WinGlassNatives.require();
    }

    /** The AWT oracle answered at least once, so at least one value was compared against it. */
    @AfterAll
    static void theOracleRan() {
        LEDGER.assertOracleRan();
    }

    private static int constant(String name) {
        return WinGlassNativeShim.constant(name);
    }

    // ---------------------------------------------------------------------------------------------
    // _isKeyLocked: KeyTable.cpp:421-439, now user32!GetKeyState in Java
    // ---------------------------------------------------------------------------------------------

    /**
     * The whole of the C body: {@code GetKeyState} of the matching virtual key, then bit 0 - the
     * toggle bit, not the high "is down" bit (L437-438). The expectation is computed here from the
     * raw {@code GetKeyState} the facade also binds, so a swapped mapping or a wrong bit fails.
     */
    @Test
    public void keyLockStateIsBitZeroOfGetKeyState() {
        assertEquals(expectedLockState(constant("VK_CAPITAL")),
                WinGlassNativeShim.keyLockState(KeyEvent.VK_CAPS_LOCK));
        assertEquals(expectedLockState(constant("VK_NUMLOCK")),
                WinGlassNativeShim.keyLockState(KeyEvent.VK_NUM_LOCK));
    }

    /**
     * Every other code returns {@code KEY_LOCK_UNKNOWN} without asking the OS at all (L434-436) -
     * including {@code VK_SCROLL_LOCK}, which Windows does have a toggle state for and the C
     * deliberately never asked about.
     */
    @Test
    public void everyOtherKeyCodeIsUnknownWithoutAskingWindows() {
        for (int code : new int[] {KeyEvent.VK_SCROLL_LOCK, KeyEvent.VK_A, KeyEvent.VK_UNDEFINED, -1,
                Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            assertEquals(KeyEvent.KEY_LOCK_UNKNOWN, WinGlassNativeShim.keyLockState(code),
                    "key code " + code);
        }
    }

    /**
     * The A/B of the key lock state: the facade against {@code WinApplication._isKeyLocked}, which is what
     * {@code Application.isKeyLocked} calls. While the JNI body existed this compared the two
     * implementations; after the flip it compares the same Java through one more frame and proves the
     * peer still routes to the facade. The run that mattered could only be made while the JNI body existed.
     */
    @Test
    public void bothWaysToAskAboutALockAgree() {
        for (int code : new int[] {KeyEvent.VK_CAPS_LOCK, KeyEvent.VK_NUM_LOCK, KeyEvent.VK_SCROLL_LOCK,
                KeyEvent.VK_A, KeyEvent.VK_UNDEFINED, -1}) {
            assertEquals(WinGlassNativeShim.keyLockState(code),
                    WinGlassNativeShim.isKeyLockedThroughPeer(code), "key code " + code);
        }
    }

    /**
     * The peer has lost the two natives ABI 2 flipped. What stayed was pinned here as well, because the
     * boundary of a change set is worth pinning: {@code getPlatformPreferences} - the seam ABI 2 left, with
     * {@link WinGlassNativeShim#collect} already able to build the same map - until ABI 5 closed it after
     * an A/B against a started toolkit ({@code tests/system}'s {@code WinPreferencesParityTest}), and
     * {@code staticScreen_getScreens}, the JNI monitor enumeration, until ABI 5 made it Java too. Both are
     * now asserted gone; {@code WinApplicationNativeTest} pins that no native is left at all.
     */
    @Test
    public void winApplicationHasLostTheTwoNativesSliceFourFlipped() {
        List<String> natives = WinGlassNativeShim.nativeMethodsOf("WinApplication");
        assertFalse(natives.contains("_isKeyLocked"), natives.toString());
        assertFalse(natives.contains("_getKeyCodeForChar"), natives.toString());
        assertFalse(natives.contains("getPlatformPreferences"), natives.toString());
        assertFalse(natives.contains("staticScreen_getScreens"), natives.toString());
    }

    private static int expectedLockState(int virtualKey) {
        return (WinGlassNativeShim.getKeyState(virtualKey) & 0x1) != 0
                ? KeyEvent.KEY_LOCK_ON : KeyEvent.KEY_LOCK_OFF;
    }

    // ---------------------------------------------------------------------------------------------
    // PlatformSupport::querySystemColors, now user32!GetSysColor in Java
    // ---------------------------------------------------------------------------------------------

    /**
     * {@code GetRValue}/{@code GetGValue}/{@code GetBValue} on a synthetic {@code COLORREF}: red is
     * the low byte and blue the high one. Every other assertion about colour in this file would pass
     * with the order reversed; this one and {@link #systemColoursMatchWhatAwtReadsFromTheSameApi()}
     * are the two that would not.
     */
    @Test
    public void colorRefChannelsAreBlueGreenRed() {
        assertArrayEquals(new int[] {0x56, 0x34, 0x12}, WinGlassNativeShim.sysColorChannels(0x00123456));
        assertArrayEquals(new int[] {0xFF, 0, 0}, WinGlassNativeShim.sysColorChannels(0x000000FF));
        assertArrayEquals(new int[] {0, 0, 0xFF}, WinGlassNativeShim.sysColorChannels(0x00FF0000));
        assertArrayEquals(new int[] {0, 0, 0}, WinGlassNativeShim.sysColorChannels(0xFF000000));
    }

    /** A {@code COLORREF} is {@code 0x00BBGGRR}: the top byte is always zero, for all eight indices. */
    @Test
    public void everySystemColourComesBackAsAColorRef() {
        for (String key : SYSTEM_COLOR_KEYS) {
            int colorRef = WinGlassNativeShim.sysColor(indexOf(key));
            assertEquals(0, colorRef & 0xFF000000, key + " = 0x" + Integer.toHexString(colorRef));
        }
    }

    /**
     * The byte-order oracle. {@code sun.awt.windows.WDesktopProperties} calls the same
     * {@code GetSysColor} indices from its own C and publishes {@code java.awt.Color}s; if this
     * facade decoded {@code 0x00BBGGRR} as {@code 0x00RRGGBB} the two would disagree on any theme
     * whose accent colour is not grey. Skipped when AWT is unreachable, except under
     * {@code -Djfx.parity.require=true}, where a machine that owns the oracle must produce it ({@link ParityGate}).
     */
    @Test
    public void systemColoursMatchWhatAwtReadsFromTheSameApi() {
        Map<String, Object> preferences = WinGlassNativeShim.collectSystemColors();
        int compared = 0;
        for (Map.Entry<String, String> entry : COLOUR_ORACLE.entrySet()) {
            int[] awt = awtColour(entry.getValue());
            if (awt == null) {
                continue;
            }
            Color ours = (Color) preferences.get(entry.getKey());
            assertNotNull(ours, entry.getKey());
            assertEquals(awt[0], (int) Math.round(ours.getRed() * 255.0), entry.getKey() + " red");
            assertEquals(awt[1], (int) Math.round(ours.getGreen() * 255.0), entry.getKey() + " green");
            assertEquals(awt[2], (int) Math.round(ours.getBlue() * 255.0), entry.getKey() + " blue");
            compared++;
        }
        int resolved = compared;
        LEDGER.compared(resolved);
        LEDGER.requireOracle(resolved > 0, () -> "no AWT colour property resolved: Toolkit.getDesktopProperty"
                + " answered null for every win.* colour key, so the GetSysColor byte-order oracle had nothing"
                + " to compare against.");
    }

    /**
     * {@code querySystemColors} puts exactly eight keys, always, and each value is the
     * {@code Color.rgb(r, g, b, 1.0)} the C's {@code putColor(jobject, const char*, int)} built.
     */
    @Test
    public void systemColoursAreEightOpaqueColours() {
        Map<String, Object> preferences = WinGlassNativeShim.collectSystemColors();
        assertEquals(Set.copyOf(SYSTEM_COLOR_KEYS), preferences.keySet());
        for (String key : SYSTEM_COLOR_KEYS) {
            Color colour = assertInstanceOf(Color.class, preferences.get(key), key);
            assertEquals(1.0, colour.getOpacity(), key + " alpha is hard-coded to 1.0 in putColor");
            int[] channels = WinGlassNativeShim.sysColorChannels(WinGlassNativeShim.sysColor(indexOf(key)));
            assertEquals(Color.rgb(channels[0], channels[1], channels[2], 1.0), colour, key);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // PlatformSupport::querySystemParameters, now user32!SystemParametersInfoW in Java
    // ---------------------------------------------------------------------------------------------

    /**
     * {@code SPI_GETHIGHCONTRAST} needs its structure size in {@code cbSize} <em>and</em> in
     * {@code uiParam} ({@code PlatformSupport::querySystemParameters}); passing 0 for the second is the mistake
     * that makes the call return {@code FALSE} and both high-contrast keys silently vanish. On any
     * Windows that supports the query at all it therefore has to answer.
     */
    @Test
    public void highContrastIsAnsweredWhichMeansTheSizeWasPassedTwice() {
        assertNotNull(WinGlassNativeShim.highContrast(),
                "SystemParametersInfoW(SPI_GETHIGHCONTRAST, sizeof(HIGHCONTRAST), &hc, 0) returned"
                + " FALSE: cbSize or uiParam is wrong, and the C put both keys whenever it returned"
                + " TRUE");
    }

    /** The second AWT oracle: {@code win.highContrast.on} is the same {@code HCF_HIGHCONTRASTON} bit. */
    @Test
    public void highContrastAgreesWithAwt() {
        Object property = desktopProperty("win.highContrast.on");
        LEDGER.requireOracle(property instanceof Boolean, () -> "the AWT Windows toolkit published no"
                + " win.highContrast.on here (it answered " + property + "), so the HCF_HIGHCONTRASTON bit has"
                + " no oracle.");
        HighContrastInfo highContrast = WinGlassNativeShim.highContrast();
        assertNotNull(highContrast);
        assertEquals(property, highContrast.on());
        LEDGER.compared();
    }

    /**
     * The scheme name is only read in the high-contrast-on branch; with high contrast off the C put
     * the key with an explicit null value (the {@code else} arm of {@code PlatformSupport::querySystemParameters}).
     */
    @Test
    public void theSchemeIsNullWheneverHighContrastIsOff() {
        HighContrastInfo highContrast = WinGlassNativeShim.highContrast();
        LEDGER.requireOracle(highContrast != null, () -> "SystemParametersInfoW(SPI_GETHIGHCONTRAST) returned"
                + " FALSE, so there is no scheme to inspect.");
        LEDGER.compared();
        if (!highContrast.on()) {
            assertNull(highContrast.scheme());
        }
    }

    /**
     * {@code SPI_GETCLIENTAREAANIMATION} contributes one key or none; the value is the {@code BOOL}
     * read as "any nonzero is true", which is what {@code putBoolean(const bool)} did.
     */
    @Test
    public void clientAreaAnimationIsABooleanOrAbsent() {
        Boolean animation = WinGlassNativeShim.clientAreaAnimation();
        Map<String, Object> preferences = WinGlassNativeShim.collectSystemParameters();
        if (animation == null) {
            assertFalse(preferences.containsKey(CLIENT_AREA_ANIMATION));
        } else {
            assertEquals(animation, preferences.get(CLIENT_AREA_ANIMATION));
        }
    }

    /**
     * The high-contrast scheme name is the one preference value that crosses as a string, and it
     * only exists while high contrast is on - which no unit test can arrange. The reader is therefore
     * driven over a string the test allocated in the same encoding Windows uses: NUL-terminated
     * UTF-16 little-endian, which is what {@code env->NewString((jchar*)value, wcslen(value))} read
     * (the {@code wchar_t*} overload of {@code PlatformSupport::putString}). A {@code NULL} pointer -
     * what a zero-filled struct leaves where the C had stack garbage - reads as "no scheme".
     */
    @Test
    public void theSchemeNameIsReadAsNulTerminatedUtf16() {
        assertEquals("High Contrast Black", WinGlassNativeShim.schemeNameOf("High Contrast Black"));
        assertEquals("", WinGlassNativeShim.schemeNameOf(""));
        // Escaped, not literal, so that the assertion does not depend on the build's source encoding.
        String beyondAscii = "Kontrast \u00fcber Alles \u30c6\u30b9\u30c8";
        assertEquals(beyondAscii, WinGlassNativeShim.schemeNameOf(beyondAscii));
        assertNull(WinGlassNativeShim.schemeNameOfNull());
    }

    /**
     * The scheme name crosses as UTF-16 <em>code units</em>, not as decoded text: the JNI's
     * {@code NewString((jchar*)value, wcslen(value))} copied every {@code WCHAR} as a {@code char}, so an
     * unpaired surrogate - which a Windows name may contain, since nothing makes it well-formed UTF-16 -
     * reached Java as that same {@code char}. A charset decoder would have turned each one into
     * {@code U+FFFD}. The buffer is written unit for unit (a {@code String} encoded through a charset
     * could not even carry these), and everything after the first {@code U+0000} - including a second
     * lone surrogate - must stay unread.
     */
    @Test
    public void theSchemeNameKeepsEveryCodeUnitUpToTheFirstNul() {
        char[] name = {
            '\uDC00',           // a lone low surrogate, first
            'H', 'C',
            '\uD83D', '\uDE00', // a valid pair, U+1F600
            '\uD800', 'x',      // a lone high surrogate followed by a non-surrogate
            '\uDBFF'            // a lone high surrogate, last before the terminator
        };
        char[] buffer = new char[name.length + 4];
        System.arraycopy(name, 0, buffer, 0, name.length);
        buffer[name.length] = '\0';
        buffer[name.length + 1] = '\uDFFF';
        buffer[name.length + 2] = 'y';
        buffer[name.length + 3] = '\0';

        String read = WinGlassNativeShim.schemeNameOfCodeUnits(buffer);

        assertArrayEquals(name, read.toCharArray());
        assertEquals(new String(name), read);
        assertEquals(8, read.length());
        assertEquals("", WinGlassNativeShim.schemeNameOfCodeUnits(new char[] {'\0', '\uD800', '\0'}));
    }

    /**
     * Presence, which {@code PlatformPreferences} distinguishes from a null value: the two
     * high-contrast keys arrive together or not at all, and the scheme key is present with a
     * <em>null</em> value when high contrast is off. That null is why the map has to be a
     * {@code HashMap}: {@code Map.copyOf} - and the {@code Map.of} that a reviewer would reach for -
     * throws on it, which this asserts rather than leaves as a comment.
     */
    @Test
    public void systemParametersFollowThePresenceRulesOfTheC() {
        Map<String, Object> preferences = WinGlassNativeShim.collectSystemParameters();
        assertTrue(Set.of(HIGH_CONTRAST, HIGH_CONTRAST_COLOR_SCHEME, CLIENT_AREA_ANIMATION)
                .containsAll(preferences.keySet()), preferences.keySet().toString());
        assertEquals(preferences.containsKey(HIGH_CONTRAST),
                preferences.containsKey(HIGH_CONTRAST_COLOR_SCHEME),
                "the two high-contrast keys are put together or not at all");

        if (Boolean.FALSE.equals(preferences.get(HIGH_CONTRAST))) {
            assertTrue(preferences.containsKey(HIGH_CONTRAST_COLOR_SCHEME));
            assertNull(preferences.get(HIGH_CONTRAST_COLOR_SCHEME));
            assertThrows(NullPointerException.class, () -> Map.copyOf(preferences),
                    "an unmodifiable copy that rejects nulls would drop a key the C put");
        }
    }

    // ---------------------------------------------------------------------------------------------
    // The key list, which two hand-maintained lists have to agree on
    // ---------------------------------------------------------------------------------------------

    /**
     * The union of everything the four collectors can emit - not what one collection produces,
     * because either {@code SystemParametersInfoW}, all three {@code IUISettings} generations and the
     * network factory can each contribute nothing - against the keys
     * {@code WinApplication.getPlatformKeys()} declares. That is the hand-maintained pair the comments
     * in {@code WinApplication.java} ask to be kept in sync (as the C comments in {@code PlatformSupport}'s
     * three query methods did until those methods were deleted), and since ABI 2 it is an
     * <em>equality</em>: the Java collectors now cover
     * all 23 keys, WinRT included, which is exactly what made wiring
     * {@code PlatformSupport::updatePreferences} to Java a migration rather than a behaviour change.
     */
    @Test
    public void everyKeyTheCollectorsCanEmitIsDeclaredByWinApplication() {
        Set<String> expected = new LinkedHashSet<>(SYSTEM_COLOR_KEYS);
        expected.add(HIGH_CONTRAST);
        expected.add(HIGH_CONTRAST_COLOR_SCHEME);
        expected.add(CLIENT_AREA_ANIMATION);
        expected.addAll(UI_COLOR_KEYS);
        expected.add(ADVANCED_EFFECTS_ENABLED);
        expected.add(AUTO_HIDE_SCROLL_BARS);
        expected.add(INTERNET_COST_TYPE);
        assertEquals(23, expected.size());
        assertEquals(expected, WinGlassNativeShim.preferenceKeys());
        assertEquals(expected, WinGlassNativeShim.platformKeys());
    }

    /** Both {@code user32} collectors together produce only keys of the declared set, and no duplicates. */
    @Test
    public void collectingBothGroupsProducesOnlyDeclaredKeys() {
        Map<String, Object> preferences = new LinkedHashMap<>(WinGlassNativeShim.collectSystemColors());
        preferences.putAll(WinGlassNativeShim.collectSystemParameters());
        assertTrue(WinGlassNativeShim.preferenceKeys().containsAll(preferences.keySet()),
                preferences.keySet().toString());
        assertTrue(preferences.size() >= SYSTEM_COLOR_KEYS.size());
    }

    // ---------------------------------------------------------------------------------------------
    // The WinRT half: gwin_prefs_query_ui_settings / gwin_prefs_query_network, and the one upcall
    // ---------------------------------------------------------------------------------------------

    /**
     * The two structures {@link WinGlassNativeShim} declares against the two {@code sizeof}s the C
     * compiler computed. This is the whole reason {@code gwin_sizeof_ui_settings} and
     * {@code gwin_sizeof_network_info} exist: {@code GwinUiSettings} is fifteen {@code int32_t} with
     * a nine-element array in the middle, and a {@code MemoryLayout} that drifted from it would read
     * the flags out of the colours and never fail anywhere else.
     */
    @Test
    public void theTwoPreferenceStructuresHaveTheSizeTheCCompilerGaveThem() {
        assertEquals(60, WinGlassNativeShim.layoutByteSize("GwinUiSettings"));
        assertEquals(WinGlassNativeShim.sizeOfUiSettings(),
                WinGlassNativeShim.layoutByteSize("GwinUiSettings"));
        assertEquals(8, WinGlassNativeShim.layoutByteSize("GwinNetworkInfo"));
        assertEquals(WinGlassNativeShim.sizeOfNetworkInfo(),
                WinGlassNativeShim.layoutByteSize("GwinNetworkInfo"));
        assertEquals(8, WinGlassNativeShim.layoutByteSize("GwinPrefsCallbacks"));
    }

    /** Every field of {@code GwinUiSettings} and {@code GwinNetworkInfo}, in declaration order, 4-aligned. */
    @Test
    public void thePreferenceStructureFieldsSitWhereTheCPutThem() {
        assertEquals(0, WinGlassNativeShim.offset("GwinUiSettings", "available"));
        assertEquals(4, WinGlassNativeShim.offset("GwinUiSettings", "colors_valid"));
        assertEquals(8, WinGlassNativeShim.offset("GwinUiSettings", "colors"));
        assertEquals(9, constant("GWIN_UI_COLOR_COUNT"));
        // 8 + 9 * 4 = 44: the array is in the struct, not behind a pointer.
        assertEquals(44, WinGlassNativeShim.offset("GwinUiSettings", "advanced_effects_valid"));
        assertEquals(48, WinGlassNativeShim.offset("GwinUiSettings", "advanced_effects_enabled"));
        assertEquals(52, WinGlassNativeShim.offset("GwinUiSettings", "auto_hide_valid"));
        assertEquals(56, WinGlassNativeShim.offset("GwinUiSettings", "auto_hide_scroll_bars"));
        assertEquals(0, WinGlassNativeShim.offset("GwinNetworkInfo", "available"));
        assertEquals(4, WinGlassNativeShim.offset("GwinNetworkInfo", "cost_type"));
    }

    /** The bitmask and the cost enum, which Java pins rather than deriving from WinRT's numbering. */
    @Test
    public void thePreferenceEnumsMatchGlassWinApiHeader() {
        assertEquals(1, constant("GWIN_PT_SYSTEM_COLORS"));
        assertEquals(2, constant("GWIN_PT_SYSTEM_PARAMS"));
        assertEquals(4, constant("GWIN_PT_UI_SETTINGS"));
        assertEquals(8, constant("GWIN_PT_NETWORK_INFORMATION"));
        assertEquals(15, constant("GWIN_PT_ALL"));
        // NOT the WinRT NetworkCostType, where Fixed is 2 and Variable is 3.
        assertEquals(0, constant("GWIN_NET_COST_UNKNOWN"));
        assertEquals(1, constant("GWIN_NET_COST_UNRESTRICTED"));
        assertEquals(2, constant("GWIN_NET_COST_VARIABLE"));
        assertEquals(3, constant("GWIN_NET_COST_FIXED"));
    }

    /**
     * Both queries answer without a toolkit, and answer "no keys": {@code GetPlatformSupport()} is
     * null before {@code Application.run} creates the toolkit window, which is the same state the JNI
     * answered with a {@code NULL} map. A unit test cannot reach any other state - activating
     * {@code IUISettings} needs the toolkit thread's apartment - so what is asserted is the contract
     * that surrounds the values: nine colours whatever happens, no flag set without
     * {@code available}, and the monotonicity the C's early returns produce.
     */
    @Test
    public void theWinRtQueriesAnswerNoKeysWithoutAToolkit() {
        WinGlassNativeShim.UiSettingsInfo settings = WinGlassNativeShim.uiSettings();
        assertEquals(constant("GWIN_UI_COLOR_COUNT"), settings.colors().length);
        if (!settings.available()) {
            assertFalse(settings.colorsValid());
            assertFalse(settings.advancedEffectsValid());
            assertFalse(settings.autoHideValid());
            assertFalse(settings.advancedEffectsEnabled());
            assertFalse(settings.autoHideScrollBars());
            assertArrayEquals(new int[constant("GWIN_UI_COLOR_COUNT")], settings.colors(),
                    "gwin_prefs_query_ui_settings must zero-fill before it does anything else");
        }
        // Monotone: the C returned out of queryUISettings on the first failure, dropping later keys.
        assertTrue(settings.colorsValid() || !settings.advancedEffectsValid());
        assertTrue(settings.advancedEffectsValid() || !settings.autoHideValid());

        WinGlassNativeShim.NetworkCostInfo network = WinGlassNativeShim.networkInfo();
        assertTrue(network.costType() >= constant("GWIN_NET_COST_UNKNOWN")
                && network.costType() <= constant("GWIN_NET_COST_FIXED"), "cost " + network.costType());
        if (!network.available()) {
            assertEquals(constant("GWIN_NET_COST_UNKNOWN"), network.costType());
        }
    }

    /**
     * {@code collectPreferences(types)}: the bitmask selects groups, and nothing else appears. The two
     * WinRT groups contribute nothing in this JVM (no toolkit), which is exactly why this asserts
     * subset relations rather than counts for them - and why it asserts that the two {@code user32}
     * groups are unaffected by whether the WinRT bits are set.
     */
    @Test
    public void collectTakesExactlyTheGroupsTheBitmaskNames() {
        Map<String, Object> colours = WinGlassNativeShim.collect(constant("GWIN_PT_SYSTEM_COLORS"));
        assertEquals(new LinkedHashSet<>(SYSTEM_COLOR_KEYS), new LinkedHashSet<>(colours.keySet()));

        Map<String, Object> parameters = WinGlassNativeShim.collect(constant("GWIN_PT_SYSTEM_PARAMS"));
        assertTrue(Set.of(HIGH_CONTRAST, HIGH_CONTRAST_COLOR_SCHEME, CLIENT_AREA_ANIMATION)
                .containsAll(parameters.keySet()), parameters.keySet().toString());

        Map<String, Object> uiSettings = WinGlassNativeShim.collect(constant("GWIN_PT_UI_SETTINGS"));
        Set<String> uiKeys = new LinkedHashSet<>(UI_COLOR_KEYS);
        uiKeys.add(ADVANCED_EFFECTS_ENABLED);
        uiKeys.add(AUTO_HIDE_SCROLL_BARS);
        assertTrue(uiKeys.containsAll(uiSettings.keySet()), uiSettings.keySet().toString());

        Map<String, Object> network = WinGlassNativeShim.collect(constant("GWIN_PT_NETWORK_INFORMATION"));
        assertTrue(Set.of(INTERNET_COST_TYPE).containsAll(network.keySet()), network.keySet().toString());

        assertEquals(Map.of(), WinGlassNativeShim.collect(0));

        Map<String, Object> all = WinGlassNativeShim.collect(constant("GWIN_PT_ALL"));
        assertTrue(WinGlassNativeShim.preferenceKeys().containsAll(all.keySet()), all.keySet().toString());
        assertEquals(colours.size() + parameters.size() + uiSettings.size() + network.size(), all.size(),
                "the four groups are disjoint and PT_ALL is their union");
    }

    /**
     * The collected map has to accept null values and carry the declared types, because
     * {@code PlatformPreferences} reads it with {@code getPlatformKeys()} in hand and a
     * {@code ClassCastException} there is a startup failure. High contrast off puts a <em>null</em>
     * scheme, so a {@code Map.of} anywhere in the chain would throw.
     */
    @Test
    public void everyCollectedValueHasTheTypeWinApplicationDeclares() {
        Map<String, Object> preferences = WinGlassNativeShim.collect(constant("GWIN_PT_ALL"));
        Map<String, Class<?>> declared = WinGlassNativeShim.platformKeyTypes();
        for (Map.Entry<String, Object> entry : preferences.entrySet()) {
            Class<?> type = declared.get(entry.getKey());
            assertNotNull(type, entry.getKey() + " is not declared by getPlatformKeys()");
            if (entry.getValue() != null) {
                assertTrue(type.isInstance(entry.getValue()),
                        entry.getKey() + " = " + entry.getValue() + ", declared " + type.getName());
            }
        }
    }

    /**
     * {@code PlatformSupport::updatePreferences} in Java: the
     * first collection always counts as a change, because the C compared against a null {@code jobject}
     * and {@code HashMap.equals(null)} is false; an identical second collection does not. Nothing is
     * notified here - {@code Application.GetApplication()} is null in a test JVM, which is the case
     * {@code WinApplication.firePreferencesChanged} tolerates and the C could not reach;
     * {@link #aThrowingNotifyTargetIsReportedAndStillCountsAsAChange()} installs one.
     */
    @Test
    public void updateReportsAChangeExactlyWhenTheCollectionDiffers() {
        WinGlassNativeShim.forgetCollectedPreferences();
        assertNull(WinGlassNativeShim.lastCollectedPreferences());

        assertTrue(WinGlassNativeShim.updatePreferences(constant("GWIN_PT_SYSTEM_COLORS")),
                "the first collection is always a change");
        Map<String, Object> after = WinGlassNativeShim.lastCollectedPreferences();
        assertEquals(new LinkedHashSet<>(SYSTEM_COLOR_KEYS), new LinkedHashSet<>(after.keySet()));

        assertFalse(WinGlassNativeShim.updatePreferences(constant("GWIN_PT_SYSTEM_COLORS")),
                "nothing changed on the desktop between the two calls");
        assertEquals(after, WinGlassNativeShim.lastCollectedPreferences());

        // The C compared the PARTIAL collection against the whole previous map and then replaced it,
        // so a different group is a change even when no preference moved. Reproduced verbatim; the FX
        // side merges each notification into the full set, which is what makes it harmless.
        assertTrue(WinGlassNativeShim.updatePreferences(constant("GWIN_PT_SYSTEM_PARAMS")));
        assertFalse(WinGlassNativeShim.lastCollectedPreferences().containsKey(SYSTEM_COLOR_KEYS.get(0)));

        WinGlassNativeShim.forgetCollectedPreferences();
    }

    /**
     * An empty collection is still a change the first time - {@code update(0)} collects nothing, and
     * an empty {@code HashMap} does not equal null. That is the C's {@code Object.equals} upcall, and it is
     * the reason the field starts as null rather than as an empty map.
     */
    @Test
    public void anEmptyFirstCollectionIsStillAChange() {
        WinGlassNativeShim.forgetCollectedPreferences();
        assertTrue(WinGlassNativeShim.updatePreferences(0));
        assertEquals(Map.of(), WinGlassNativeShim.lastCollectedPreferences());
        assertFalse(WinGlassNativeShim.updatePreferences(0));
        WinGlassNativeShim.forgetCollectedPreferences();
    }

    /**
     * The {@code preferences_changed} upcall target never lets a {@code Throwable} out, and reports
     * it: an exception escaping an upcall stub terminates the JVM, and the contract it replaces is
     * {@code CheckAndClearException}'s (the former {@code Utils.cpp:50-71}) - report through
     * {@code Application.reportException}, which routes to the current thread's uncaught-exception
     * handler, and carry on. The C's own oddity comes with it: {@code PlatformSupport::updatePreferences}
     * had already stored the new map and cleared the exception when it fell through to
     * {@code return true}, so a notify that throws still counts as a change and the target answers 1;
     * an identical second collection is then no change at all, and nothing is notified.
     * <p>
     * A test JVM has no {@code Application} - {@code Application.run} never ran - so the shim installs
     * its {@code WinApplication} peer and a throwing {@code EventHandler} for the duration of the
     * call, which is the only way to reach the notify arm from here. The other arm of the target's
     * own catch, {@code collect} throwing, cannot be reached without a hook in the product, and there
     * is none.
     */
    @Test
    public void aThrowingNotifyTargetIsReportedAndStillCountsAsAChange() {
        RuntimeException deliberate = new RuntimeException("WinPreferencesNativeTest: deliberate");
        AtomicReference<Throwable> reported = new AtomicReference<>();
        AtomicInteger notified = new AtomicInteger();
        Thread current = Thread.currentThread();
        Thread.UncaughtExceptionHandler previous = current.getUncaughtExceptionHandler();
        current.setUncaughtExceptionHandler((thread, throwable) -> reported.compareAndSet(null, throwable));
        WinGlassNativeShim.forgetCollectedPreferences();
        try {
            int types = constant("GWIN_PT_SYSTEM_COLORS");
            assertEquals(1, WinGlassNativeShim.firePreferencesChangedInto(preferences -> {
                notified.incrementAndGet();
                throw deliberate;
            }, types), "a notify that throws still counted as a change: the C had stored the map and"
                    + " fell through to return true");
            assertEquals(1, notified.get(), "the handler ran exactly once");
            assertSame(deliberate, reported.get(),
                    "the throwable must reach Application.reportException on the calling thread");
            assertNotNull(WinGlassNativeShim.lastCollectedPreferences(),
                    "the collection was stored before the notify, as the C's preferences field was");

            reported.set(null);
            assertEquals(0, WinGlassNativeShim.firePreferencesChangedInto(preferences -> {
                notified.incrementAndGet();
                throw deliberate;
            }, types), "nothing changed on the desktop between the two calls");
            assertEquals(1, notified.get(), "no change, no notify");
            assertNull(reported.get(), "and nothing to report");
        } finally {
            current.setUncaughtExceptionHandler(previous);
            WinGlassNativeShim.forgetCollectedPreferences();
        }
    }

    /**
     * When the report itself throws - here an uncaught-exception handler that does - still nothing
     * escapes: {@code CheckAndClearException} re-checked {@code ExceptionOccurred()} after its own
     * {@code reportException} call for exactly this ({@code Utils.cpp:64-67}), and the Java guards
     * its report the same way. The target still answers 1, because the map was stored before either
     * failure.
     */
    @Test
    public void aReportThatItselfThrowsIsSwallowedToo() {
        Thread current = Thread.currentThread();
        Thread.UncaughtExceptionHandler previous = current.getUncaughtExceptionHandler();
        current.setUncaughtExceptionHandler((thread, throwable) -> {
            throw new IllegalStateException("WinPreferencesNativeTest: the report throws too");
        });
        WinGlassNativeShim.forgetCollectedPreferences();
        try {
            assertEquals(1, WinGlassNativeShim.firePreferencesChangedInto(preferences -> {
                throw new RuntimeException("WinPreferencesNativeTest: deliberate");
            }, constant("GWIN_PT_SYSTEM_COLORS")));
        } finally {
            current.setUncaughtExceptionHandler(previous);
            WinGlassNativeShim.forgetCollectedPreferences();
        }
    }

    /**
     * The callback table is installed, and it was installed by {@code WinApplication}'s static
     * initializer rather than by anything here: touching the peer at all is enough, and the peer is
     * what {@code Application.run} builds. Installing again is a no-op, which is what makes the
     * assertion safe to make from a test.
     */
    @Test
    public void theCallbackTableIsInstalledByWinApplicationsStaticInitializer() {
        WinGlassNativeShim.platformKeys();   // forces the peer, and so WinApplication's initializer
        assertTrue(WinGlassNativeShim.preferencesCallbackInstalled());
        WinGlassNativeShim.installPreferencesCallback();
        assertTrue(WinGlassNativeShim.preferencesCallbackInstalled());
    }

    // ---------------------------------------------------------------------------------------------
    // The AWT oracle, kept at arm's length
    // ---------------------------------------------------------------------------------------------

    private static Map<String, String> colourOracle() {
        Map<String, String> oracle = new LinkedHashMap<>();
        oracle.put("Windows.SysColor.COLOR_3DFACE", "win.3d.backgroundColor");
        oracle.put("Windows.SysColor.COLOR_BTNTEXT", "win.button.textColor");
        oracle.put("Windows.SysColor.COLOR_HIGHLIGHT", "win.item.highlightColor");
        oracle.put("Windows.SysColor.COLOR_HIGHLIGHTTEXT", "win.item.highlightTextColor");
        oracle.put("Windows.SysColor.COLOR_WINDOW", "win.frame.backgroundColor");
        oracle.put("Windows.SysColor.COLOR_WINDOWTEXT", "win.frame.textColor");
        return oracle;
    }

    /** The red, green and blue of an AWT desktop property, or {@code null} when it is unavailable. */
    private static int[] awtColour(String property) {
        Object value = desktopProperty(property);
        if (!(value instanceof java.awt.Color colour)) {
            return null;
        }
        return new int[] {colour.getRed(), colour.getGreen(), colour.getBlue()};
    }

    /**
     * Reads one AWT desktop property, or returns {@code null} for any reason at all. Loading the AWT
     * Windows toolkit is a side effect this test accepts and no other test may depend on: it is here
     * only because it is the one independent implementation of {@code GetSysColor} in this JVM.
     */
    private static Object desktopProperty(String property) {
        try {
            return java.awt.Toolkit.getDefaultToolkit().getDesktopProperty(property);
        } catch (Throwable headlessOrUnsupported) {
            return null;
        }
    }

    private static int indexOf(String systemColorKey) {
        return constant(systemColorKey.substring("Windows.SysColor.".length()));
    }
}
