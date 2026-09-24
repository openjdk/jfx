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

import com.sun.glass.ui.Application;
import com.sun.javafx.PlatformUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;

import test.util.Util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The platform preference map on a started Windows toolkit: {@code WinApplication.getPlatformPreferences()} against
 * {@code WinPreferences.collect(GWIN_PT_ALL)}, and the startup map that reached the public
 * {@link Platform#getPreferences()} through {@code QuantumToolkit.runToolkit}.
 * <p>
 * <b>History.</b> This class was written as an A/B of two arms: the JNI {@code getPlatformPreferences()}
 * ({@code PlatformSupport::collectPreferences(PT_ALL)}, a {@code java.util.HashMap} built in C) against
 * {@code collect(GWIN_PT_ALL)}, the Java replacement that already owned every later update. That evidence was
 * captured on 2026-09-13, on a started toolkit and before the {@code native} was deleted: 23 keys,
 * both arms a plain {@code HashMap}, 0 entry differences, stable over three runs. The C paths on which the arms could
 * legitimately have differed - a value read from uninitialised memory after an ignored WinRT failure, or a
 * high-contrast scheme name with an unpaired surrogate - were named in its failure messages and were not hit.
 * <p>
 * <b>What it checks now.</b> The JNI arm is gone, so the same assertions check the wiring: that
 * {@code getPlatformPreferences} is no longer {@code native} in the code under test, that it returns a fresh, plain,
 * unwrapped {@code HashMap} as the JNI did, and that its entries are exactly those of {@code collect(GWIN_PT_ALL)} -
 * a wrong type mask, an {@code unmodifiableMap} wrapper or a cached instance fails here. The anti-vacuity checks are
 * unchanged. The public map is checked separately: {@code PlatformPreferences.update} drops null values, so it must
 * hold every non-null entry of the startup map and nothing else.
 * <p>
 * Every read runs on the JavaFX application thread, which on Windows is the Glass toolkit thread: the WinRT objects
 * {@code collect} reads live in that thread's single-threaded apartment. A system setting change between two reads is
 * retried: the {@code collect} read is bracketed by two {@code getPlatformPreferences} reads, and only a bracket whose
 * two outer reads agree is compared.
 */
public class WinPreferencesParityTest {

    /** Eight {@code Windows.SysColor.*} keys and three {@code Windows.SPI.*} keys, present whenever user32 answers. */
    private static final int USER32_KEY_COUNT = 11;

    /**
     * {@link #USER32_KEY_COUNT} less {@code Windows.SPI.HighContrastColorScheme}, which is present with a null value
     * while high contrast is off and so never reaches the public map.
     */
    private static final int USER32_NON_NULL_KEY_COUNT = USER32_KEY_COUNT - 1;

    /** {@link #USER32_KEY_COUNT} plus the nine {@code Windows.UIColor.*} keys of {@code IUISettings3}. */
    private static final int WITH_UI_COLORS_KEY_COUNT = 20;

    private static final int MAX_BRACKETS = 5;

    private static final long SETTLE_MILLIS = 500;

    private static final CountDownLatch startupLatch = new CountDownLatch(1);

    /**
     * One stable bracket: the two {@code getPlatformPreferences()} reads around the {@code collect} read, and a copy of
     * the public map taken in the same toolkit-thread runnable.
     */
    private record Snapshot(Map<String, Object> peer, Map<String, Object> peerAgain, Map<String, Object> collected,
            Map<String, Object> publicPreferences, int brackets) {
    }

    @BeforeAll
    static void initFX() {
        assumeTrue(PlatformUtil.isWindows());
        Util.startup(startupLatch, startupLatch::countDown);
        AtomicReference<String> provenance = new AtomicReference<>();
        Util.runAndWait(() -> provenance.set(WinToolkitProbe.provenance()));
        System.out.println("WinPreferencesParityTest provenance\n" + provenance.get());
    }

    @AfterAll
    static void shutdown() {
        assumeTrue(PlatformUtil.isWindows());
        Util.shutdown();
    }

    @Test
    public void getPlatformPreferencesIsNoLongerNative() {
        assertFalse(WinToolkitProbe.isWinApplicationNative("getPlatformPreferences"),
                "WinApplication.getPlatformPreferences is still native in the code under test");
    }

    @Test
    public void bothReturnAFreshPlainHashMap() {
        Snapshot snapshot = capture();
        assertSame(HashMap.class, snapshot.peer().getClass(), "getPlatformPreferences() map class");
        assertSame(HashMap.class, snapshot.collected().getClass(), "collect(GWIN_PT_ALL) map class");
        assertNotSame(snapshot.peer(), snapshot.peerAgain(), "getPlatformPreferences() answered a cached instance");
    }

    @Test
    public void bothReportTheSameKeys() {
        Snapshot snapshot = capture();
        Set<String> peerOnly = new TreeSet<>(snapshot.peer().keySet());
        peerOnly.removeAll(snapshot.collected().keySet());
        Set<String> collectOnly = new TreeSet<>(snapshot.collected().keySet());
        collectOnly.removeAll(snapshot.peer().keySet());
        assertEquals(snapshot.peer().keySet(), snapshot.collected().keySet(),
                "keys only in getPlatformPreferences() " + peerOnly + ", keys only in collect " + collectOnly);
    }

    @Test
    public void everyKeyHasTheSameValueOfTheSameClass() {
        Snapshot snapshot = capture();
        Set<String> keys = new TreeSet<>(snapshot.peer().keySet());
        keys.addAll(snapshot.collected().keySet());

        List<String> differences = new ArrayList<>();
        StringBuilder record = new StringBuilder();
        for (String key : keys) {
            Object peerValue = snapshot.peer().get(key);
            Object collectedValue = snapshot.collected().get(key);
            record.append("  ").append(key).append(" = ").append(presence(snapshot.peer(), key)).append('\n');
            boolean samePresence = snapshot.peer().containsKey(key) == snapshot.collected().containsKey(key);
            if (!samePresence || !Objects.equals(peerValue, collectedValue)
                    || classOf(peerValue) != classOf(collectedValue)) {
                differences.add(key + ": getPlatformPreferences() " + presence(snapshot.peer(), key) + ", collect "
                        + presence(snapshot.collected(), key));
            }
        }
        System.out.println("WinPreferencesParityTest " + keys.size() + " keys (brackets "
                + snapshot.brackets() + ")\n" + record);

        assertTrue(keys.size() >= USER32_KEY_COUNT, "compared only " + keys.size() + " keys: " + keys);
        assertTrue(differences.isEmpty(), differences.size() + " differing entries:\n  "
                + String.join("\n  ", differences));
    }

    @Test
    public void theMapsAreEqual() {
        Snapshot snapshot = capture();
        assertEquals(snapshot.peer(), snapshot.collected());
    }

    @Test
    public void theComparisonIsNotVacuous() {
        Snapshot snapshot = capture();
        assertTrue(snapshot.peer().size() >= USER32_KEY_COUNT,
                "getPlatformPreferences() has " + snapshot.peer().size() + " keys, fewer than the "
                        + USER32_KEY_COUNT + " user32 always contributes: " + snapshot.peer().keySet());

        AtomicReference<Boolean> uiSettings = new AtomicReference<>();
        Util.runAndWait(() -> uiSettings.set(WinToolkitProbe.uiSettingsAvailable()));
        WinToolkitProbe.requireOracle(WinPreferencesParityTest.class, uiSettings.get(),
                () -> "gwin_prefs_query_ui_settings reports no WinRT UISettings object on the started toolkit"
                        + " (RoActivateInstance(Windows.UI.ViewManagement.UISettings) failed or RoInitialize"
                        + " did not run), so the nine Windows.UIColor.* keys cannot be compared; keys: "
                        + snapshot.peer().keySet());
        assertTrue(snapshot.peer().size() >= WITH_UI_COLORS_KEY_COUNT,
                "UISettings is available but getPlatformPreferences() has only " + snapshot.peer().size()
                        + " keys: " + snapshot.peer().keySet());
    }

    /**
     * {@code Platform.getPreferences()} holds what {@code QuantumToolkit.runToolkit} handed
     * {@code PlatformPreferences.update}, plus every later update - all of which now come from {@code collect} too.
     * A setting change whose notification has not been dispatched yet makes the two disagree for one pump of the
     * message loop, so a disagreement is re-read, from a new runnable, before it fails.
     */
    @Test
    public void thePublicPreferencesHoldEveryNonNullEntry() {
        Snapshot snapshot = null;
        Map<String, Object> expected = null;
        for (int attempt = 1; attempt <= MAX_BRACKETS; attempt++) {
            snapshot = capture();
            expected = new HashMap<>(snapshot.peer());
            expected.values().removeIf(Objects::isNull);
            if (expected.equals(snapshot.publicPreferences())) {
                break;
            }
            Util.sleep(SETTLE_MILLIS);
        }

        Set<String> nullValued = new TreeSet<>(snapshot.peer().keySet());
        nullValued.removeAll(expected.keySet());
        StringBuilder record = new StringBuilder();
        for (String key : new TreeSet<>(snapshot.publicPreferences().keySet())) {
            record.append("  ").append(key).append(" = ").append(presence(snapshot.publicPreferences(), key))
                    .append('\n');
        }
        System.out.println("WinPreferencesParityTest Platform.getPreferences() " + snapshot.publicPreferences().size()
                + " keys; getPlatformPreferences() " + snapshot.peer().size() + " keys, null-valued (dropped by"
                + " PlatformPreferences.update) " + nullValued + "\n" + record);

        assertTrue(snapshot.publicPreferences().size() >= USER32_NON_NULL_KEY_COUNT,
                "Platform.getPreferences() has only " + snapshot.publicPreferences().size() + " keys: "
                        + snapshot.publicPreferences().keySet());
        assertEquals(expected, snapshot.publicPreferences(),
                "Platform.getPreferences() against the non-null entries of getPlatformPreferences()");
    }

    /** One stable bracket, taken on the JavaFX application thread; see the class documentation. */
    private static Snapshot capture() {
        AtomicReference<Snapshot> snapshot = new AtomicReference<>();
        Util.runAndWait(() -> snapshot.set(captureOnToolkitThread()));
        assertNotNull(snapshot.get().peer(), "getPlatformPreferences() answered null");
        assertNotNull(snapshot.get().collected(), "collect(GWIN_PT_ALL) answered null");
        return snapshot.get();
    }

    private static Snapshot captureOnToolkitThread() {
        assertTrue(Platform.isFxApplicationThread() && Application.isEventThread(),
                "the preference reads must run on the Glass toolkit thread, not " + Thread.currentThread());
        int all = WinToolkitProbe.preferenceTypeAll();
        Map<String, Object> before = Application.GetApplication().getPlatformPreferences();
        for (int bracket = 1; bracket <= MAX_BRACKETS; bracket++) {
            Map<String, Object> collected = WinToolkitProbe.collectPreferences(all);
            Map<String, Object> after = Application.GetApplication().getPlatformPreferences();
            if (Objects.equals(before, after)) {
                return new Snapshot(before, after, collected, new HashMap<>(Platform.getPreferences()), bracket);
            }
            before = after;
        }
        return fail("the preference map changed between every pair of reads in " + MAX_BRACKETS
                + " brackets; the system settings did not settle");
    }

    private static Class<?> classOf(Object value) {
        return value == null ? null : value.getClass();
    }

    private static String describe(Object value) {
        return value == null ? "null" : value + " (" + value.getClass().getName() + ")";
    }

    private static String presence(Map<String, Object> map, String key) {
        return map.containsKey(key) ? describe(map.get(key)) : "absent";
    }
}
