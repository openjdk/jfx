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

package com.sun.glass.ui.win;

import com.sun.glass.ui.Application;
import com.sun.glass.ui.win.WinGlassNative.HighContrast;
import com.sun.glass.ui.win.WinGlassNative.NetworkInfo;
import com.sun.glass.ui.win.WinGlassNative.UiSettings;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.scene.paint.Color;

/**
 * {@code PlatformSupport.cpp}'s preference map, built in Java: the eight {@code GetSysColor} colours
 * of {@code PlatformSupport::querySystemColors} ({@code native-glass/win/PlatformSupport.cpp}), the
 * three {@code SystemParametersInfoW} values of {@code querySystemParameters}, the eleven
 * {@code IUISettings} entries of {@code queryUISettings} and the one {@code INetworkInformation}
 * entry of {@code queryNetworkInformation} - and the collect / compare / notify cycle of
 * {@code updatePreferences} around them.
 * <p>
 * The C built all of that itself, with eleven upcalls in {@code PlatformSupport.cpp}:
 * {@code HashMap.<init>}, five {@code Map.put}s, two {@code Color.rgb}s, {@code Object.equals},
 * {@code Collections.unmodifiableMap} and {@code Application.notifyPreferencesChanged}, plus two
 * {@code Boolean.TRUE}/{@code FALSE} field reads. Ten of those were Java calling Java through C; they
 * became unreachable once {@link WinGlassNative#installPreferencesCallback} had run, and have been
 * deleted. The eleventh, the preferences-changed <em>event</em>, was not a data call and is
 * still an upcall, now the one slot of {@code GwinPrefsCallbacks}: only native code - the two WinRT
 * sinks and four {@code WndProc} arms - knows when a preference may have changed. The C's copies of
 * the key strings went with them: the collector spells them only here, and the declaration
 * in {@code WinApplication.getPlatformKeys} carries a comment pointing back to this class.
 * <p>
 * <b>Two sources, two directions.</b> The colours and the two {@code SPI} values are plain
 * {@code user32} calls this class makes itself. The twelve WinRT entries are not: {@code IUISettings}
 * and {@code INetworkInformation} need {@code RoActivateInstance}, three generations of a vtable and
 * two event sinks, so those objects stay in {@code PlatformSupport} and hand their results out as POD
 * through {@code gwin_prefs_query_ui_settings} / {@code gwin_prefs_query_network}
 * ({@link WinGlassNative#uiSettings}, {@link WinGlassNative#networkInfo}). This class is the only
 * place that turns either source into key/value pairs, so the key names exist once.
 * <p>
 * <b>Presence is observable, not only value.</b> {@code PlatformPreferences} distinguishes an absent
 * key from a key with a null value, so every presence rule of the C is reproduced exactly: a
 * {@code SystemParametersInfoW} that returns {@code FALSE} contributes <em>no</em> key (both calls in
 * {@code querySystemParameters} are guarded by their return value); a successful call with high
 * contrast off contributes {@code Windows.SPI.HighContrastColorScheme} with a <em>null</em> value
 * (the {@code else} arm of {@code querySystemParameters}); and an
 * {@code IUISettings} generation that could not be reached contributes no key rather than a
 * {@code false} one, and takes every later group with it. The destination map must therefore accept
 * null values - a {@code HashMap}, never {@code Map.of} or {@code Map.copyOf} - which is also what
 * the C handed to {@code Collections.unmodifiableMap}.
 * <p>
 * <b>The initial map is built here too.</b> {@code WinApplication.getPlatformPreferences} is
 * {@link #collect collect(GWIN_PT_ALL)}, returned unwrapped as the C's
 * {@code PlatformSupport::collectPreferences(PT_ALL)} returned its {@code HashMap}, so the map an
 * application reads at startup and every later update come from the same code. That closed the seam
 * this class used to document, where the startup map was still built in C: the two produced the same
 * 23 keys, values and value classes on a started toolkit ({@code tests/system}'s
 * {@code WinPreferencesParityTest}, run while both arms existed). The state machines still line up:
 * {@code collect} does not touch {@link #collected}, just as {@code collectPreferences} never touched
 * the C's former {@code preferences} field, so {@code collected} starts out as "nothing seen yet" and
 * the first update after startup counts as a change.
 * <p>
 * <b>Threading.</b> No lock, deliberately - {@code PlatformSupport} had none and a lock held across
 * {@code notifyPreferencesChanged} (application listener code) would be a deadlock the C never had.
 * The four {@code WndProc} arms reach {@link #update} on the Glass toolkit thread. The two WinRT sinks
 * need not: they are WRL {@code Callback<>} delegates, which are agile (they aggregate the
 * free-threaded marshaler), so they are not marshalled into the single-threaded apartment the toolkit
 * thread created and may run on a WinRT thread instead. A second thread is therefore expected, not
 * excluded: {@link #collected} is {@code volatile} and {@link #update} checks no thread. Two concurrent
 * updates can still both report a change, exactly as two concurrent {@code updatePreferences} calls
 * could.
 * <p>
 * Line numbers into {@code Utils.cpp} refer to it at commit {@code 8492cb03b0}
 * ({@code git show 8492cb03b0:modules/javafx.graphics/src/main/native-glass/win/Utils.cpp}).
 */
final class WinPreferences {

    /** {@code Windows.SPI.HighContrast} ({@code PlatformSupport::querySystemParameters}). */
    static final String HIGH_CONTRAST = "Windows.SPI.HighContrast";

    /** {@code Windows.SPI.HighContrastColorScheme} ({@code PlatformSupport::querySystemParameters}). */
    static final String HIGH_CONTRAST_COLOR_SCHEME = "Windows.SPI.HighContrastColorScheme";

    /** {@code Windows.SPI.ClientAreaAnimation} ({@code PlatformSupport::querySystemParameters}). */
    static final String CLIENT_AREA_ANIMATION = "Windows.SPI.ClientAreaAnimation";

    /**
     * {@code Windows.UISettings.AdvancedEffectsEnabled} ({@code PlatformSupport::queryUISettings},
     * the {@code IUISettings4} block).
     */
    static final String ADVANCED_EFFECTS_ENABLED = "Windows.UISettings.AdvancedEffectsEnabled";

    /**
     * {@code Windows.UISettings.AutoHideScrollBars} ({@code PlatformSupport::queryUISettings}, the
     * {@code IUISettings5} block).
     */
    static final String AUTO_HIDE_SCROLL_BARS = "Windows.UISettings.AutoHideScrollBars";

    /** {@code Windows.NetworkInformation.InternetCostType} ({@code PlatformSupport::queryNetworkInformation}). */
    static final String INTERNET_COST_TYPE = "Windows.NetworkInformation.InternetCostType";

    /**
     * The eight {@code Windows.SysColor.*} keys and the {@code GetSysColor} index each one reports,
     * in the order {@code PlatformSupport::querySystemColors} queried them.
     * <p>
     * This is the single list: {@link #systemColors} iterates it and {@link #keys} derives the key
     * set from it, so a mistyped key cannot appear in one and not the other. The strings themselves
     * are the ones {@code WinApplication.getPlatformKeys} declares. The "kept in sync" comments that
     * asked for that in {@code PlatformSupport::querySystemParameters}, {@code querySystemColors} and
     * {@code queryUISettings} went with those functions; the comments above
     * {@code getPlatformKeys} / {@code getPlatformKeyMappings} still ask, and
     * {@code WinPreferencesNativeTest} checks.
     */
    private static final Map<String, Integer> SYSTEM_COLORS = systemColorIndices();

    /**
     * The nine {@code Windows.UIColor.*} keys, in the order of {@code GwinUiSettings.colors} - which
     * is the order {@code PlatformSupport::queryUISettings} asked {@code IUISettings3::GetColorValue}
     * for them. Index i of the struct is key i here, and
     * {@link #uiSettings} asserts nothing about that beyond the two lengths agreeing, so this list
     * and {@code GWIN_UI_COLOR_COUNT} have to move together.
     */
    private static final List<String> UI_COLOR_KEYS = List.of(
            "Windows.UIColor.Background",
            "Windows.UIColor.Foreground",
            "Windows.UIColor.AccentDark3",
            "Windows.UIColor.AccentDark2",
            "Windows.UIColor.AccentDark1",
            "Windows.UIColor.Accent",
            "Windows.UIColor.AccentLight1",
            "Windows.UIColor.AccentLight2",
            "Windows.UIColor.AccentLight3");

    /**
     * The map the last notification carried. It replaces {@code PlatformSupport::preferences}
     * ({@code PlatformSupport.h}), the member the JNI {@code updatePreferences} compared against; that
     * member has been deleted with the comparison, and the C {@code updatePreferences} now
     * compares nothing - it calls the table slot.
     * <p>
     * {@code null}, not {@code Map.of()}: {@code HashMap.equals(null)} is false, so the first
     * collection always counts as a change even when it is empty, which is what the C's null
     * {@code jobject} did in {@code updatePreferences}' {@code Object.equals} upcall. Note that the
     * comparison is against whatever the <em>last update</em> collected, not against the whole map - a
     * {@code WM_SETTINGCHANGE(SPI_SETCLIENTAREAANIMATION)} collects three keys, compares those three
     * against the previous map and then becomes the previous map. That is the C's behaviour verbatim,
     * and it is harmless because the FX side merges each notification into the full set.
     */
    private static volatile Map<String, Object> collected;

    private WinPreferences() {
    }

    /**
     * {@code PlatformSupport::updatePreferences}, which is the body of the one surviving upcall.
     * Collects the requested groups, compares them with the last collection and notifies the
     * application when they differ.
     * <p>
     * The C's exact mapping, which {@code GlassApplication::WindowProc} turns into {@code return 0}
     * versus {@code DefWindowProc}, is reproduced including its oddity: <em>a notify that throws still
     * counts as a change</em> (its {@code CheckAndClearException} swallowed the exception and fell through to
     * {@code return true}). The one arm with no Java counterpart is the C's
     * "{@code Collections.unmodifiableMap} threw", which cannot happen to a constructor call.
     * <p>
     * Called on the Glass toolkit thread from {@code WndProc}, and from the two WinRT sinks, which may
     * run on a WinRT thread - see the class documentation on why this tolerates that.
     *
     * @param types a bitmask of {@code GwinPreferenceType}
     * @return true if the preferences changed
     */
    static boolean update(int types) {
        Map<String, Object> updated = collect(types);
        if (updated.equals(collected)) {
            return false;
        }
        collected = updated;
        try {
            WinApplication.firePreferencesChanged(Collections.unmodifiableMap(updated));
        } catch (Throwable t) {
            // The C reported and still returned true (updatePreferences, via CheckAndClearException).
            reportFailure(t);
        }
        return true;
    }

    /**
     * {@code PlatformSupport::collectPreferences} without the upcalls: the four groups, in the C's
     * order, into a map that accepts null values.
     *
     * @param types a bitmask of {@code GwinPreferenceType}
     */
    static Map<String, Object> collect(int types) {
        Map<String, Object> preferences = new HashMap<>();
        if ((types & WinGlassNative.GWIN_PT_SYSTEM_COLORS) != 0) {
            systemColors(preferences);
        }
        if ((types & WinGlassNative.GWIN_PT_SYSTEM_PARAMS) != 0) {
            systemParameters(preferences);
        }
        if ((types & WinGlassNative.GWIN_PT_UI_SETTINGS) != 0) {
            uiSettings(preferences);
        }
        if ((types & WinGlassNative.GWIN_PT_NETWORK_INFORMATION) != 0) {
            network(preferences);
        }
        return preferences;
    }

    /**
     * {@code PlatformSupport::querySystemColors}: eight {@code GetSysColor} calls, each turned into a
     * {@code Color} with a hard-coded alpha of 1.0 (the {@code int} overload of {@code putColor}).
     * <p>
     * The one thing that has to be right and is invisible when it is wrong: a {@code COLORREF} is
     * {@code 0x00BBGGRR}, so red is the <em>low</em> byte. The C used {@code GetRValue}/
     * {@code GetGValue}/{@code GetBValue}; this uses their Java copies in {@code WinGlassNative}.
     *
     * @param preferences the map to add to; must accept null values
     */
    static void systemColors(Map<String, Object> preferences) {
        for (Map.Entry<String, Integer> entry : SYSTEM_COLORS.entrySet()) {
            int colorRef = WinGlassNative.sysColor(entry.getValue());
            preferences.put(entry.getKey(), Color.rgb(WinGlassNative.sysColorRed(colorRef),
                    WinGlassNative.sysColorGreen(colorRef), WinGlassNative.sysColorBlue(colorRef), 1.0));
        }
    }

    /**
     * {@code PlatformSupport::querySystemParameters}: one
     * {@code SPI_GETHIGHCONTRAST} call that contributes two keys or none, and one
     * {@code SPI_GETCLIENTAREAANIMATION} call that contributes one key or none.
     * <p>
     * When high contrast is off the scheme key is still put, with a null value - see the class
     * documentation.
     *
     * @param preferences the map to add to; must accept null values
     */
    static void systemParameters(Map<String, Object> preferences) {
        HighContrast highContrast = WinGlassNative.highContrast();
        if (highContrast != null) {
            preferences.put(HIGH_CONTRAST, highContrast.on());
            preferences.put(HIGH_CONTRAST_COLOR_SCHEME, highContrast.scheme());
        }

        Boolean clientAreaAnimation = WinGlassNative.clientAreaAnimation();
        if (clientAreaAnimation != null) {
            preferences.put(CLIENT_AREA_ANIMATION, clientAreaAnimation);
        }
    }

    /**
     * {@code PlatformSupport::queryUISettings}, whose WinRT half
     * runs in {@code PlatformSupport::collectUISettings} and reaches Java as a POD struct.
     * <p>
     * Each of the three groups appears only when its own flag says the C got that far: no
     * {@code UISettings} at all means no keys, a failed {@code IUISettings3} means no colours
     * <em>and</em> nothing after them, and so on. The flags are monotone, so the early returns of the
     * C need no reproducing here beyond honouring each flag. The colours are {@code 0xAARRGGBB} - the
     * layout of {@code ABI::Windows::UI::Color}, not a {@code COLORREF} - and the alpha is real, where
     * {@link #systemColors} hard-codes 1.0 ({@code putColor}'s {@code Color} overload against its {@code int} one).
     *
     * @param preferences the map to add to; must accept null values
     */
    static void uiSettings(Map<String, Object> preferences) {
        UiSettings settings = WinGlassNative.uiSettings();
        if (!settings.available()) {
            return;
        }
        if (settings.colorsValid()) {
            int[] colors = settings.colors();
            for (int i = 0; i < UI_COLOR_KEYS.size(); i++) {
                preferences.put(UI_COLOR_KEYS.get(i), uiColor(colors[i]));
            }
        }
        if (settings.advancedEffectsValid()) {
            preferences.put(ADVANCED_EFFECTS_ENABLED, settings.advancedEffectsEnabled());
        }
        if (settings.autoHideValid()) {
            preferences.put(AUTO_HIDE_SCROLL_BARS, settings.autoHideScrollBars());
        }
    }

    /**
     * {@code PlatformSupport::queryNetworkInformation}, whose
     * WinRT half runs in {@code PlatformSupport::collectNetworkInfo}.
     * <p>
     * Either there is no key at all - no activation factory, or an {@code RoException} before the C
     * reached its {@code putString} - or there is exactly one, and {@code "Unknown"} is a value it can
     * legitimately have: no internet connection profile, or a cost the C's switch did not name.
     *
     * @param preferences the map to add to; must accept null values
     */
    static void network(Map<String, Object> preferences) {
        NetworkInfo info = WinGlassNative.networkInfo();
        if (info.available()) {
            preferences.put(INTERNET_COST_TYPE, costTypeName(info.costType()));
        }
    }

    /**
     * Every key {@link #collect} can emit - the union, not the intersection: no single collection
     * produces all of them, because both {@code SPI} calls, all three {@code IUISettings} generations
     * and the network factory can each contribute nothing.
     * <p>
     * These are exactly the keys {@code WinApplication.getPlatformKeys} declares, and a test asserts
     * that rather than trusting the two hand-maintained lists to agree.
     */
    static Set<String> keys() {
        Set<String> keys = new LinkedHashSet<>(SYSTEM_COLORS.keySet());
        keys.add(HIGH_CONTRAST);
        keys.add(HIGH_CONTRAST_COLOR_SCHEME);
        keys.add(CLIENT_AREA_ANIMATION);
        keys.addAll(UI_COLOR_KEYS);
        keys.add(ADVANCED_EFFECTS_ENABLED);
        keys.add(AUTO_HIDE_SCROLL_BARS);
        keys.add(INTERNET_COST_TYPE);
        return Collections.unmodifiableSet(keys);
    }

    /**
     * The map the last {@link #update} collected, or null before the first one. The C kept the same
     * thing in a {@code jobject} that nothing outside {@code updatePreferences} read.
     */
    static Map<String, Object> collected() {
        return collected;
    }

    /**
     * Forgets that state, so that the next {@link #update} reports a change whatever it finds.
     * <p>
     * Nothing in the product calls this: the C's equivalent lived and died with the toolkit's
     * {@code PlatformSupport}. It exists so that the tests of {@link #update} are independent of the
     * order they run in inside a shared surefire JVM, which is the same reason
     * {@code WinGlassNative.schemeName} is package-private.
     */
    static void forgetCollected() {
        collected = null;
    }

    /**
     * {@code GwinNetworkCost} to the string {@code PlatformSupport::queryNetworkInformation}'s
     * {@code NetworkCostType} switch chose and its {@code putString} put in the map. The four names
     * are the contract {@code WinApplication.getPlatformKeyMappings} switches on, so they are
     * literals here and not derived from an enum name.
     */
    private static String costTypeName(int costType) {
        return switch (costType) {
            case WinGlassNative.GWIN_NET_COST_UNRESTRICTED -> "Unrestricted";
            case WinGlassNative.GWIN_NET_COST_VARIABLE -> "Variable";
            case WinGlassNative.GWIN_NET_COST_FIXED -> "Fixed";
            default -> "Unknown";
        };
    }

    /**
     * {@code PlatformSupport::putColor(jobject, const char*, Color)}: an
     * {@code ABI::Windows::UI::Color} is four bytes A, R, G, B, which
     * {@code PlatformSupport::collectUISettings} packs as {@code 0xAARRGGBB}, and the alpha reaches
     * {@code Color.rgb} as {@code A / 255.0} in double.
     */
    private static Color uiColor(int argb) {
        return Color.rgb((argb >>> 16) & 0xFF, (argb >>> 8) & 0xFF, argb & 0xFF,
                ((argb >>> 24) & 0xFF) / 255.0);
    }

    /**
     * {@code CheckAndClearException} (the former {@code Utils.cpp:50-71}): report, and swallow anything the
     * reporting itself raises, because this runs under an upcall stub and an exception that escapes
     * one terminates the JVM ({@code Application.reportException} runs an application-supplied
     * uncaught-exception handler, {@code Application.java:448-453}).
     */
    private static void reportFailure(Throwable t) {
        try {
            Application.reportException(t);
        } catch (Throwable ignored) {
            // Nothing left to try; the C re-checked ExceptionOccurred() after its own report for this.
        }
    }

    private static Map<String, Integer> systemColorIndices() {
        Map<String, Integer> indices = new LinkedHashMap<>();
        indices.put("Windows.SysColor.COLOR_3DFACE", WinGlassNative.COLOR_3DFACE);
        indices.put("Windows.SysColor.COLOR_BTNTEXT", WinGlassNative.COLOR_BTNTEXT);
        indices.put("Windows.SysColor.COLOR_GRAYTEXT", WinGlassNative.COLOR_GRAYTEXT);
        indices.put("Windows.SysColor.COLOR_HIGHLIGHT", WinGlassNative.COLOR_HIGHLIGHT);
        indices.put("Windows.SysColor.COLOR_HIGHLIGHTTEXT", WinGlassNative.COLOR_HIGHLIGHTTEXT);
        indices.put("Windows.SysColor.COLOR_HOTLIGHT", WinGlassNative.COLOR_HOTLIGHT);
        indices.put("Windows.SysColor.COLOR_WINDOW", WinGlassNative.COLOR_WINDOW);
        indices.put("Windows.SysColor.COLOR_WINDOWTEXT", WinGlassNative.COLOR_WINDOWTEXT);
        return Collections.unmodifiableMap(indices);
    }
}
