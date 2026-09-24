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

package test.com.sun.glass.ui.gtk;

import com.sun.glass.ui.gtk.GtkGlassShim;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Platform;
import javafx.collections.MapChangeListener;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code GtkApplication.getPlatformPreferences} and the GtkSettings change notification on a running toolkit, as
 * {@code PlatformSupport::collectPreferences} and {@code ::updatePreferences} of {@code PlatformSupport.cpp}
 * answered them at commit {@code 033187ad90}, pinned so that the {@code java.lang.foreign} replacement can be
 * held to it.
 * <ul>
 * <li>the map holds only the keys {@code GtkApplication.getPlatformKeys} declares, each with the declared type;
 * every colour it holds is opaque and its channels are whole multiples of 1/255;</li>
 * <li>every declared colour key carries the colour {@code gtk_style_lookup_color} answers for its name, with each
 * 16-bit channel truncated to a byte the way {@code putColor} truncated it - measured once against the theme's own
 * palette and once against a colour redefined through a CSS provider, which is off the 8-bit grid and so tells
 * truncation from rounding;</li>
 * <li>{@code GTK.theme_name}, {@code GTK.enable_animations} and {@code GTK.overlay_scrolling} are exactly the
 * GtkSettings properties of those names, and follow them when they change;</li>
 * <li>a change to one of the three observed GtkSettings properties reaches
 * {@code Application.notifyPreferencesChanged}, which is what {@link Platform#getPreferences} shows, and a second
 * change to the same value notifies nothing;</li>
 * <li>the map the peer answers is a plain modifiable map, not the unmodifiable view handed to the
 * notification.</li>
 * </ul>
 * It runs in {@link GtkGlassChild} on the X11 display of {@code DISPLAY}.
 */
@EnabledOnOs(OS.LINUX)
@Timeout(300)
public class GtkPreferencesNativeTest {

    /** What the scenario sets {@code gtk-theme-name} to; no such theme need exist for GtkSettings to keep it. */
    static final String OTHER_THEME = "JfxTestTheme";

    /** The theme colour the scenario redefines through a CSS provider, to reach a value off the 8-bit grid. */
    static final String OVERRIDDEN_COLOR = "GTK.theme_fg_color";

    /**
     * A colour no 8-bit palette can produce: {@code mix()} interpolates the two ends in floating point, so the
     * 16-bit channels of the {@code GdkColor} are not whole multiples of 257 and truncating the scaled channel
     * gives a different byte from rounding it.
     */
    static final String OVERRIDE_CSS = "mix(#000000, #ffffff, 0.333)";

    /** The keys of {@code PlatformSupport::collectPreferences} that are not one of the 18 theme colours. */
    static final List<String> NON_COLOUR_KEYS = List.of("GTK.theme_name", "GTK.enable_animations",
            "GTK.overlay_scrolling", "GTK.network_metered");

    private static GtkGlassChildJvm.Run run;

    @BeforeAll
    @Timeout(GtkGlassChildJvm.BEFORE_ALL_SECONDS)
    static void runScenario() {
        GtkGlassChildJvm.requireDisplay();
        run = GtkGlassChildJvm.run(GtkPreferencesNativeTest.class, "preferencesScenario", List.of());
    }

    private static String value(String key) {
        String value = run.values().get(key);
        if (value == null) {
            throw new AssertionError("the child recorded no " + key + ": " + run.describe());
        }
        return value;
    }

    /** Every key of the map is declared by {@code getPlatformKeys}, with the class declared there. */
    @Test
    public void everyKeyIsADeclaredPlatformKey() {
        Map<String, String> types = entries("prefs.type.");
        assertFalse(types.isEmpty(), "the peer answered no preferences");
        Map<String, String> declared = entries("declared.");
        for (Map.Entry<String, String> entry : types.entrySet()) {
            assertEquals(declared.get(entry.getKey()), entry.getValue(),
                    entry.getKey() + " is not a declared platform key of its type");
        }
    }

    /** The theme name, animation and metered keys are always there; the colours are whatever the theme defines. */
    @Test
    public void theNonColourKeysAreAlwaysPresent() {
        Map<String, String> types = entries("prefs.type.");
        for (String key : NON_COLOUR_KEYS) {
            if (key.equals("GTK.overlay_scrolling")) {
                continue;
            }
            assertTrue(types.containsKey(key), key + " missing from " + types.keySet());
        }
        assertEquals(value("gtk.overlayScrollingProperty").equals("true"),
                types.containsKey("GTK.overlay_scrolling"));
    }

    /**
     * Every colour key the peer declares is the colour {@code gtk_style_lookup_color} answers for its name, with
     * each 16-bit channel scaled by {@code (int) (c / 65535.0 * 255.0)} - truncated, as {@code putColor} did - and
     * a colour the style does not define is left out of the map altogether.
     */
    @Test
    public void everyColourIsTheStyleColourOfItsName() {
        Map<String, String> values = entries("prefs.value.");
        Map<String, String> style = entries("style.");
        int compared = 0;
        for (Map.Entry<String, String> entry : entries("declared.").entrySet()) {
            if (!entry.getValue().equals(Color.class.getName())) {
                continue;
            }
            String key = entry.getKey();
            int[] lookup = channels(value("style." + key));
            if (lookup[0] == 0) {
                assertFalse(values.containsKey(key), key + " is not defined by the style but was published");
                continue;
            }
            assertEquals(expectedColour(lookup).toString(), values.get(key), key + " from " + style.get(key));
            compared++;
        }
        assertEquals(entries("prefs.type.").values().stream().filter(Color.class.getName()::equals).count(),
                compared, "a published colour is not a declared colour key");
        assertTrue(compared > 0, "the theme defined no colour at all");
    }

    /**
     * A theme colour redefined through a CSS provider reaches the peer with the same truncating scale. The
     * redefined colour is off the 1/257 grid of an 8-bit palette, which is the only way to tell the C's
     * {@code (int) (CLAMP(c / 65535.0, 0, 1) * 255.0)} from a rounding one on a theme like this machine's.
     */
    @Test
    public void aColourOffTheEightBitGridIsTruncated() {
        assertEquals("true", value("override.loaded"), "the CSS provider did not parse " + OVERRIDE_CSS);
        int[] lookup = channels(value("override.style." + OVERRIDDEN_COLOR));
        assertEquals(1, lookup[0], OVERRIDDEN_COLOR + " is not defined after the override");
        assertTrue(lookup[1] % 257 != 0 || lookup[2] % 257 != 0 || lookup[3] % 257 != 0,
                "the override produced an 8-bit grid colour, which cannot tell truncation from rounding: "
                        + value("override.style." + OVERRIDDEN_COLOR));
        assertEquals(expectedColour(lookup).toString(), value("override.value." + OVERRIDDEN_COLOR));
    }

    /** {@code Color.rgb(channel(red), channel(green), channel(blue), 1.0)} of {@code putColor}. */
    private static Color expectedColour(int[] lookup) {
        return Color.rgb(channel(lookup[1]), channel(lookup[2]), channel(lookup[3]), 1.0);
    }

    /** {@code (int) (CLAMP((double) c / 65535.0, 0.0, 1.0) * 255.0)}, written out here as the C wrote it. */
    private static int channel(int value) {
        return (int) (Math.max(0.0, Math.min(1.0, value / 65535.0)) * 255.0);
    }

    /** {@code found,red,green,blue} as {@link GtkGlassShim#gtkStyleColor} recorded it. */
    private static int[] channels(String recorded) {
        String[] parts = recorded.split(",");
        int[] values = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            values[i] = Integer.parseInt(parts[i]);
        }
        return values;
    }

    /** Every colour is opaque and every channel is a whole 1/255 step, as {@code Color.rgb(r, g, b, 1.0)} is. */
    @Test
    public void everyColourIsAnOpaqueEightBitColour() {
        Map<String, String> values = entries("prefs.value.");
        int colours = 0;
        for (Map.Entry<String, String> entry : entries("prefs.type.").entrySet()) {
            if (!entry.getValue().equals(Color.class.getName())) {
                continue;
            }
            colours++;
            Color colour = Color.valueOf(values.get(entry.getKey()));
            assertEquals(1.0, colour.getOpacity(), 0.0, entry.getKey());
            for (double channel : new double[] {colour.getRed(), colour.getGreen(), colour.getBlue()}) {
                assertEquals(Math.rint(channel * 255.0), channel * 255.0, 1e-4, entry.getKey());
            }
        }
        assertTrue(colours > 0, "the theme defined no colour at all");
    }

    /** The three GtkSettings-backed keys are the settings themselves. */
    @Test
    public void theSettingsKeysAreTheGtkSettings() {
        assertEquals(value("gtk.themeName"), value("prefs.value.GTK.theme_name"));
        assertEquals(value("gtk.enableAnimations"), value("prefs.value.GTK.enable_animations"));
        if (value("gtk.overlayScrollingProperty").equals("true")) {
            assertEquals(value("gtk.overlayScrolling"), value("prefs.value.GTK.overlay_scrolling"));
        }
    }

    /** With the theme name and the animation flag changed, the peer answers the new values. */
    @Test
    public void thePeerFollowsAChangedSetting() {
        assertEquals(OTHER_THEME, value("changed.value.GTK.theme_name"));
        assertEquals(Boolean.toString(!Boolean.parseBoolean(value("gtk.enableAnimations"))),
                value("changed.value.GTK.enable_animations"));
    }

    /**
     * The {@code notify::} handlers of the observed settings reach {@code Application.notifyPreferencesChanged}:
     * the platform preferences the toolkit publishes carry the new values without anyone asking the peer.
     */
    @Test
    public void aChangedSettingNotifiesTheApplication() {
        assertEquals(OTHER_THEME, value("notified.GTK.theme_name"));
        assertEquals(Boolean.toString(!Boolean.parseBoolean(value("gtk.enableAnimations"))),
                value("notified.GTK.enable_animations"));
        assertEquals(value("gtk.themeName"), value("restored.GTK.theme_name"));
    }

    /** Setting the same value again changes no preference, so nothing is notified. */
    @Test
    public void anUnchangedSettingNotifiesNothing() {
        int afterChange = Integer.parseInt(value("notifications.afterChange"));
        assertTrue(afterChange > Integer.parseInt(value("notifications.atStart")),
                "the setting change published no preference change at all");
        assertEquals(afterChange, Integer.parseInt(value("notifications.afterRepeat")));
    }

    /** The map {@code getPlatformPreferences} answers is the peer's own, modifiable and newly built each call. */
    @Test
    public void thePeerAnswersAFreshModifiableMap() {
        assertEquals("false", value("prefs.sameInstance"));
        assertEquals("true", value("prefs.equalContents"));
        assertEquals("true", value("prefs.modifiable"));
    }

    private static Map<String, String> entries(String prefix) {
        Map<String, String> result = new TreeMap<>();
        for (Map.Entry<String, String> entry : run.values().entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                result.put(entry.getKey().substring(prefix.length()), entry.getValue());
            }
        }
        return result;
    }

    /** Every change {@link Platform#getPreferences} published since the scenario installed its listener. */
    private static final AtomicInteger NOTIFICATIONS = new AtomicInteger();

    /** Runs in {@link GtkGlassChild}. */
    static void preferencesScenario(Map<String, String> out) throws Exception {
        GtkGlassChild.recordEnvironment(out);
        String[] original = GtkGlassChild.onFx(() -> {
            Platform.getPreferences().addListener(
                    (MapChangeListener<String, Object>) change -> NOTIFICATIONS.incrementAndGet());
            out.put("notifications.atStart", Integer.toString(NOTIFICATIONS.get()));
            for (Map.Entry<String, Class<?>> entry : GtkGlassShim.platformKeys().entrySet()) {
                out.put("declared." + entry.getKey(), entry.getValue().getName());
            }
            Map<String, Object> preferences = GtkGlassShim.platformPreferences();
            record(out, "prefs", preferences);
            recordStyleColours(out, "style");
            Map<String, Object> again = GtkGlassShim.platformPreferences();
            out.put("prefs.sameInstance", Boolean.toString(preferences == again));
            out.put("prefs.equalContents", Boolean.toString(preferences.equals(again)));
            again.put("GTK.jfx_test_key", "written");
            out.put("prefs.modifiable", Boolean.toString(again.containsKey("GTK.jfx_test_key")));

            out.put("gtk.themeName", String.valueOf(GtkGlassShim.gtkSettingString("gtk-theme-name")));
            out.put("gtk.enableAnimations",
                    Boolean.toString(GtkGlassShim.gtkSettingInt("gtk-enable-animations") != 0));
            boolean overlay = GtkGlassShim.gtkSettingProperty("gtk-overlay-scrolling");
            out.put("gtk.overlayScrollingProperty", Boolean.toString(overlay));
            if (overlay) {
                out.put("gtk.overlayScrolling",
                        Boolean.toString(GtkGlassShim.gtkSettingInt("gtk-overlay-scrolling") != 0));
            }
            return new String[] {GtkGlassShim.gtkSettingString("gtk-theme-name"),
                Integer.toString(GtkGlassShim.gtkSettingInt("gtk-enable-animations"))};
        });

        boolean animations = !original[1].equals("0");
        try {
            GtkGlassChild.onFx(() -> {
                GtkGlassShim.setGtkSettingString("gtk-theme-name", OTHER_THEME);
                GtkGlassShim.setGtkSettingInt("gtk-enable-animations", animations ? 0 : 1);
                return null;
            });
            awaitPreference(out, "GTK.theme_name", OTHER_THEME);
            GtkGlassChild.onFx(() -> {
                record(out, "changed", GtkGlassShim.platformPreferences());
                out.put("notified.GTK.theme_name", String.valueOf(Platform.getPreferences().get("GTK.theme_name")));
                out.put("notified.GTK.enable_animations",
                        String.valueOf(Platform.getPreferences().get("GTK.enable_animations")));
                out.put("notifications.afterChange", Integer.toString(NOTIFICATIONS.get()));
                return null;
            });
            // setting the same values again changes no preference, so nothing is published
            GtkGlassChild.onFx(() -> {
                GtkGlassShim.setGtkSettingString("gtk-theme-name", OTHER_THEME);
                GtkGlassShim.setGtkSettingInt("gtk-enable-animations", animations ? 0 : 1);
                return null;
            });
            Thread.sleep(1000);
            GtkGlassChild.onFx(() -> {
                out.put("notifications.afterRepeat", Integer.toString(NOTIFICATIONS.get()));
                return null;
            });
        } finally {
            GtkGlassChild.onFx(() -> {
                GtkGlassShim.setGtkSettingString("gtk-theme-name", original[0]);
                GtkGlassShim.setGtkSettingInt("gtk-enable-animations", Integer.parseInt(original[1]));
                return null;
            });
            awaitPreference(out, "GTK.theme_name", original[0]);
            GtkGlassChild.onFx(() -> {
                out.put("restored.GTK.theme_name", String.valueOf(Platform.getPreferences().get("GTK.theme_name")));
                return null;
            });
        }

        // last, because it changes a colour for the rest of the process
        GtkGlassChild.onFx(() -> {
            out.put("override.loaded",
                    Boolean.toString(GtkGlassShim.defineThemeColor(lookupName(OVERRIDDEN_COLOR), OVERRIDE_CSS)));
            recordStyleColours(out, "override.style");
            record(out, "override", GtkGlassShim.platformPreferences());
            return null;
        });
    }

    /** {@code found,red,green,blue} of {@code gtk_style_lookup_color} for every declared colour key. */
    private static void recordStyleColours(Map<String, String> out, String prefix) {
        for (Map.Entry<String, Class<?>> entry : GtkGlassShim.platformKeys().entrySet()) {
            if (entry.getValue() != Color.class) {
                continue;
            }
            int[] lookup = GtkGlassShim.gtkStyleColor(lookupName(entry.getKey()));
            out.put(prefix + "." + entry.getKey(),
                    lookup[0] + "," + lookup[1] + "," + lookup[2] + "," + lookup[3]);
        }
    }

    /** The {@code gtk_style_lookup_color} name of a preference key: the key without its {@code GTK.} prefix. */
    private static String lookupName(String key) {
        return key.substring("GTK.".length());
    }

    /** Waits for the published platform preferences to carry {@code expected} under {@code key}. */
    private static void awaitPreference(Map<String, String> out, String key, Object expected) throws Exception {
        boolean arrived = GtkGlassChild.waitFor(15_000, () -> {
            try {
                return expected.equals(GtkGlassChild.onFx(() -> Platform.getPreferences().get(key)));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
        out.put("awaited." + key + "." + expected, Boolean.toString(arrived));
    }

    private static void record(Map<String, String> out, String prefix, Map<String, Object> preferences) {
        for (Map.Entry<String, Object> entry : new TreeMap<>(preferences).entrySet()) {
            Object value = entry.getValue();
            out.put(prefix + ".type." + entry.getKey(), value.getClass().getName());
            out.put(prefix + ".value." + entry.getKey(),
                    value instanceof Color colour ? colour.toString() : value.toString());
        }
    }
}
