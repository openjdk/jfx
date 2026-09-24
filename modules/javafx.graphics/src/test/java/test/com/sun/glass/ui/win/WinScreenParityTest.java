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

import com.sun.glass.ui.Screen;
import com.sun.glass.ui.win.WinGlassNativeShim;
import com.sun.glass.ui.win.WinGlassNativeShim.MachineMonitors;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import test.com.sun.javafx.test.ParityGate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The screen oracle now that the JNI is gone: {@code WinApplication.staticScreen_getScreens()} -
 * {@code WinScreenLayout.arrange(WinGlassNative.collectMonitors(), WinApplication.overrideUIScale)} - against the
 * table the JNI built on the machine that owns the goldens, at that machine's own UI scale and at a forced
 * {@value #FORCED_SCALE}.
 * <p>
 * <b>Where the goldens come from.</b> {@value #GOLDEN_FILE} and {@value #SCALED_GOLDEN_FILE} are the two snapshots
 * that {@code WinScreenSnapshotTest} (merged into this class since) took of
 * {@code GlassScreen::CreateJavaScreens} as it is in commit {@code 8492cb03b0},
 * checked in byte for byte under a comment header - their data lines must still hash to the capture's md5
 * ({@link #theGoldensAreTheJniCapturesUnchanged()}). While the JNI existed this class compared the two arms in one
 * JVM - JNI, Java, JNI again - and the Java tables equalled both snapshots byte for byte; on a started toolkit
 * ({@code tests/system}) the startup list, the JNI called again and the Java agreed as well. That A/B cannot run
 * any more. The goldens are what it left behind, and they are never regenerated: a capture now could only copy the
 * code under test.
 * <p>
 * <b>The gate.</b> A screen table is a fact about one machine's monitors, so the comparison runs only where the
 * monitors are the golden's: the {@code machine.*} keys of its header - the number of monitors and, per monitor in
 * enumeration order, the primary flag, the monitor and work rectangles and the effective DPI - must equal what this
 * machine reports. <b>What it reports is never asked of the code under test.</b>
 * {@link WinGlassNativeShim#machineMonitors()} reads it through handles of its own:
 * {@code GetSystemMetrics(SM_CMONITORS)}; the primary monitor's {@code SM_CXSCREEN} x {@code SM_CYSCREEN} at the
 * origin; its work area from {@code SystemParametersInfoW(SPI_GETWORKAREA)}; and its effective DPI from
 * {@code GetDpiForMonitor} on {@code MonitorFromPoint((0, 0), MONITOR_DEFAULTTOPRIMARY)}. The gate used to describe
 * the machine with {@code collectMonitors()} itself, so an enumeration bug that moved a rectangle or a DPI moved the
 * description with it and the comparison was skipped - a green run - instead of failing: with
 * {@code rcWork.bottom + 1} forced into {@code GetMonitorSettings}' port, that gate skipped both goldens, and this one
 * fails both. Those queries see the primary monitor only, so only a one-monitor golden can ever match - which both
 * goldens are, and no later capture can add another. Elsewhere the comparison is skipped naming every difference;
 * under {@code -Djfx.parity.require=true}, on the machine that owns the goldens, it fails ({@link ParityGate}) - the
 * font goldens' rule for their inventory. Colour depth and raw DPI are not gated on; they are compared.
 * <p>
 * <b>The comparison.</b> All twenty values of every screen, floats by raw bits, except {@code nativeScreen}: an
 * {@code HMONITOR} is a session handle that a re-plug, a driver reset or a reboot renumbers. The forced variant
 * sets only {@code WinApplication.overrideUIScale} - its only reader since the C copy {@code initIDs} kept went -
 * and restores it in a {@code finally}; its anti-vacuity is that every scale of every screen is the forced one and
 * every size really was divided. A display change during an enumeration is a retry, detected by a second
 * enumeration that disagrees, at most {@value #MAX_ATTEMPTS} times; it is never a pass.
 * <p>
 * <b>The DPI guard.</b> Without a toolkit the first enumeration in this JVM runs the facade's
 * {@code loadDpiFuncs(PROCESS_PER_MONITOR_DPI_AWARE)}. Under {@code java.exe}, whose manifest pins PerMonitorV2,
 * that is refused, which is asserted rather than assumed: {@code GetProcessDpiAwareness} must succeed and read the
 * same before and after every enumeration.
 * <p>
 * Every Java table is also written to {@code target/} in the goldens' data format. The multi-monitor half -
 * enumeration order, the primary swap, anchoring between monitors - has no machine here;
 * {@code WinScreenLayoutParityTest} holds that arithmetic to the C's own golden everywhere.
 */
@EnabledOnOs(OS.WINDOWS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WinScreenParityTest {

    static final String GOLDEN_FILE = "screen-snapshot-golden.txt";

    static final String SCALED_GOLDEN_FILE = "screen-snapshot-golden-uiscale-1.75.txt";

    /** The md5 of {@value #GOLDEN_FILE}'s data lines: the JNI snapshot captured at the machine's own scale. */
    static final String GOLDEN_DATA_MD5 = "b4a57d1e24dd20ba27b0305cda3e8060";

    /** The md5 of {@value #SCALED_GOLDEN_FILE}'s data lines: the JNI snapshot captured at the forced scale. */
    static final String SCALED_GOLDEN_DATA_MD5 = "259cf8811be6ee60727066ca3def4beb";

    static final float FORCED_SCALE = 1.75f;

    static final int MAX_ATTEMPTS = 3;

    static final int S_OK = 0;

    static final String MACHINE_PREFIX = "machine.";

    static final Path JAVA_SNAPSHOT = Path.of("target", "screen-snapshot-java.txt");

    static final Path JAVA_SCALED_SNAPSHOT = Path.of("target", "screen-snapshot-java-uiscale-1.75.txt");

    /** {@code Screen}'s own override of the resolution ({@code Screen.java:42-43}). */
    static final String SCREEN_DPI_PROPERTY = "com.sun.javafx.screenDPI";

    private static final ParityGate.Ledger LEDGER = ParityGate.ledger(WinScreenParityTest.class);

    @BeforeAll
    static void requireNatives() {
        WinGlassNatives.require();
        // The four lazy holders, in the order WinGlassNativeTest's exact symbol list expects.
        WinGlassNativeShim.bindTimerSymbols();
        WinGlassNativeShim.bindCursorSymbols();
        WinGlassNativeShim.bindBrowserSymbols();
        WinGlassNativeShim.bindScreenSymbols();
        assertTrue(Integer.getInteger(SCREEN_DPI_PROPERTY, 0) <= 0, "-D" + SCREEN_DPI_PROPERTY
                + " replaces both resolutions in Screen's constructor, so the table would not be the enumeration's");
    }

    @AfterAll
    static void theOracleRan() {
        LEDGER.assertOracleRan();
    }

    /**
     * Runs on every machine: both goldens load, their data lines are the JNI captures byte for byte, and they describe
     * the same machine - so that neither a hand edit nor a regeneration can pass for the record of the JNI.
     */
    @Test
    @Order(1)
    public void theGoldensAreTheJniCapturesUnchanged() throws IOException {
        Golden golden = Golden.load(GOLDEN_FILE);
        Golden scaled = Golden.load(SCALED_GOLDEN_FILE);
        assertEquals(GOLDEN_DATA_MD5, md5(golden.dataText()), GOLDEN_FILE + ": the data lines are not the JNI capture");
        assertEquals(SCALED_GOLDEN_DATA_MD5, md5(scaled.dataText()),
                SCALED_GOLDEN_FILE + ": the data lines are not the JNI capture");
        assertFalse(golden.machine().isEmpty(), GOLDEN_FILE + " has no machine.* keys to gate on");
        assertEquals(golden.machine(), scaled.machine(), "the two goldens describe different machines");
    }

    @Test
    @Order(2)
    public void theScreenTableEqualsTheJniGoldenAtThisMachinesScale() throws IOException {
        Golden golden = Golden.load(GOLDEN_FILE);
        float override = WinGlassNativeShim.uiScaleOverride();
        assertFalse(override > 0.0f, "WinApplication.overrideUIScale is " + override + " (glass.win.uiScale, or"
                + " prism.allowhidpi=false): " + GOLDEN_FILE + " was captured without an override");
        int[] awareness = WinGlassNativeShim.processDpiAwareness();
        Map<String, String> machine = describeMachine();

        Table table = enumerateStably("unforced (override " + override + ")", awareness);
        write(JAVA_SNAPSHOT, table.text());

        requireSameMachine(golden, machine, GOLDEN_FILE);
        assertSameAsGolden(golden, table, GOLDEN_FILE);
    }

    @Test
    @Order(3)
    public void theScreenTableEqualsTheJniGoldenAtAForcedScale() throws IOException {
        Golden golden = Golden.load(SCALED_GOLDEN_FILE);
        float override = WinGlassNativeShim.uiScaleOverride();
        assertNotEquals(Float.floatToRawIntBits(FORCED_SCALE), Float.floatToRawIntBits(override),
                "glass.win.uiScale already forces " + FORCED_SCALE + ", so the forced table could not be told from"
                        + " the machine's own");
        int[] awareness = WinGlassNativeShim.processDpiAwareness();
        Map<String, String> machine = describeMachine();

        Table unforced = enumerateStably("before forcing the scale", awareness);
        Table scaled;
        try {
            WinGlassNativeShim.setJavaUiScaleOverride(FORCED_SCALE);
            scaled = enumerateStably("forced to " + FORCED_SCALE, awareness);
        } finally {
            WinGlassNativeShim.setJavaUiScaleOverride(override);
        }
        Table restored = enumerateStably("after restoring the scale", awareness);
        write(JAVA_SCALED_SNAPSHOT, scaled.text());

        assertEquals(Float.floatToRawIntBits(override), Float.floatToRawIntBits(WinGlassNativeShim.uiScaleOverride()),
                "the override was not restored");
        assertEquals(unforced.text(), restored.text(), "restoring the override did not restore the machine's table");
        assertForcedScale(scaled.screens());
        assertNotEquals(unforced.text(), scaled.text(), "forcing the scale changed nothing");

        requireSameMachine(golden, machine, SCALED_GOLDEN_FILE);
        assertSameAsGolden(golden, scaled, SCALED_GOLDEN_FILE);
    }

    /** One enumeration's screens and their text in the goldens' data format. */
    private record Table(Screen[] screens, String text) {
    }

    /**
     * Two enumerations that must agree, or the display changed under them and the pair is repeated. Every
     * enumeration sits between two {@code GetProcessDpiAwareness} readings that must succeed and equal
     * {@code awareness}; the table is printed before anything about it is asserted.
     */
    private static Table enumerateStably(String what, int[] awareness) {
        assertEquals(S_OK, awareness[0], "GetProcessDpiAwareness failed before " + what);
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String label = what + ", attempt " + attempt;
            Screen[] first = enumerate(label, awareness);
            Screen[] second = enumerate(label + ", again", awareness);
            String text = snapshot(first);
            if (text.equals(snapshot(second))) {
                assertValidScreens(label, first);
                return new Table(first, text);
            }
            System.out.println("WinScreenParityTest: the display changed during " + label + "; retrying");
        }
        return fail("the display changed during all " + MAX_ATTEMPTS + " attempts of " + what
                + "; nothing was compared");
    }

    private static Screen[] enumerate(String what, int[] awareness) {
        Screen[] screens = WinGlassNativeShim.screensThroughApplicationPeer();
        int[] after = WinGlassNativeShim.processDpiAwareness();
        System.out.print("WinScreenParityTest: " + what + ": GetProcessDpiAwareness {hresult, value} before="
                + describe(awareness) + " after=" + describe(after) + "\n"
                + (screens == null ? "null\n" : snapshot(screens)));
        assertEquals(S_OK, after[0], "GetProcessDpiAwareness failed after " + what);
        assertEquals(awareness[1], after[1], what + " changed this process's DPI awareness: the facade's"
                + " SetProcessDpiAwareness(PROCESS_PER_MONITOR_DPI_AWARE) was not refused, so the table describes a"
                + " different process and every later test in this JVM runs in it");
        assertNotNull(screens, what + " returned null: no monitor was enumerated");
        assertTrue(screens.length >= 1, what + " returned no screens");
        return screens;
    }

    /** At least one screen, distinct non-zero {@code HMONITOR}s, positive sizes and positive scales. */
    private static void assertValidScreens(String what, Screen[] screens) {
        Set<Long> monitors = new HashSet<>();
        for (int i = 0; i < screens.length; i++) {
            Screen screen = screens[i];
            String which = what + ": " + line(i, screen);
            assertNotEquals(0L, screen.getNativeScreen(), which + ": no HMONITOR");
            assertTrue(monitors.add(screen.getNativeScreen()), which + ": the HMONITOR of an earlier screen");
            assertTrue(screen.getWidth() > 0 && screen.getHeight() > 0, which + ": empty FX bounds");
            assertTrue(screen.getPlatformWidth() > 0 && screen.getPlatformHeight() > 0,
                    which + ": empty platform bounds");
            assertTrue(screen.getVisibleWidth() > 0 && screen.getVisibleHeight() > 0, which + ": empty work area");
            assertTrue(screen.getPlatformScaleX() > 0 && screen.getPlatformScaleY() > 0, which + ": platform scale");
            assertTrue(screen.getRecommendedOutputScaleX() > 0 && screen.getRecommendedOutputScaleY() > 0,
                    which + ": output scale");
        }
    }

    /** Every scale of every screen is the forced one, and every size was really divided by it. */
    private static void assertForcedScale(Screen[] screens) {
        int forcedBits = Float.floatToRawIntBits(FORCED_SCALE);
        for (int i = 0; i < screens.length; i++) {
            Screen screen = screens[i];
            String which = "forced: " + line(i, screen);
            assertEquals(forcedBits, Float.floatToRawIntBits(screen.getPlatformScaleX()), which);
            assertEquals(forcedBits, Float.floatToRawIntBits(screen.getPlatformScaleY()), which);
            assertEquals(forcedBits, Float.floatToRawIntBits(screen.getRecommendedOutputScaleX()), which);
            assertEquals(forcedBits, Float.floatToRawIntBits(screen.getRecommendedOutputScaleY()), which);
            // floor(w / 1.75 + 0.5) < w for every w >= 2, so equal sizes mean the division never ran.
            assertTrue(screen.getWidth() < screen.getPlatformWidth(), which + ": the width was not scaled");
            assertTrue(screen.getHeight() < screen.getPlatformHeight(), which + ": the height was not scaled");
        }
    }

    /**
     * This machine's {@code machine.*} keys, in the goldens' header format, from
     * {@link WinGlassNativeShim#machineMonitors()} - never from the enumeration under test. {@code monitor.0} is
     * described only when there is exactly one monitor, which is then the primary; a value a query did not write
     * is described as {@code unread}, which matches no golden. {@code collectMonitors()} is printed next to it for
     * the log and gated on nowhere.
     */
    private static Map<String, String> describeMachine() {
        MachineMonitors facts = WinGlassNativeShim.machineMonitors();
        Map<String, String> machine = new LinkedHashMap<>();
        machine.put(MACHINE_PREFIX + "monitors", Integer.toString(facts.monitors()));
        if (facts.monitors() == 1) {
            String work = facts.workAreaRead()
                    ? facts.workLeft() + " " + facts.workTop() + " " + facts.workRight() + " " + facts.workBottom()
                    : "unread";
            String dpi = facts.dpiResult() == S_OK
                    ? facts.dpiX() + " " + facts.dpiY()
                    : "unread (" + String.format("0x%08x", facts.dpiResult()) + ")";
            machine.put(MACHINE_PREFIX + "monitor.0", "primary true rect 0 0 " + facts.width() + " " + facts.height()
                    + " work " + work + " effectiveDpi " + dpi);
        }
        System.out.println("WinScreenParityTest: this machine, from Win32 queries independent of the enumeration: "
                + machine + " " + facts);
        System.out.println("WinScreenParityTest: collectMonitors (under test, not gated on): "
                + WinGlassNativeShim.collectedMonitors());
        return machine;
    }

    /**
     * The gate: every {@code machine.*} key of the golden equals this machine's, and this machine has none the
     * golden lacks. Skips (or, under {@code -Djfx.parity.require=true}, fails) naming every difference.
     */
    private static void requireSameMachine(Golden golden, Map<String, String> machine, String fileName) {
        List<String> differences = new ArrayList<>();
        Set<String> keys = new TreeSet<>(golden.machine().keySet());
        keys.addAll(machine.keySet());
        for (String key : keys) {
            String captured = golden.machine().get(key);
            String now = machine.get(key);
            if (captured == null || !captured.equals(now)) {
                differences.add(key + ": golden=" + captured + " this machine=" + now);
            }
        }
        LEDGER.requireOracle(differences.isEmpty(), () -> fileName + " was captured on other monitors, so it is"
                + " not an oracle for this machine and the comparison is skipped. Differences: " + differences
                + ". It is never re-captured: the JNI that produced it is gone.");
    }

    /**
     * Field for field, screen for screen, {@code nativeScreen} excepted. A table equal but for its handles is
     * reported as a renumbered {@code HMONITOR}, which is not a regression.
     */
    private static void assertSameAsGolden(Golden golden, Table table, String fileName) {
        List<String> expected = golden.data();
        List<String> actual = dataLines(table.text());
        assertEquals(expected.size(), actual.size(), fileName + ": number of screens");
        for (int i = 0; i < expected.size(); i++) {
            String[] expectedFields = expected.get(i).split(" ");
            String[] actualFields = actual.get(i).split(" ");
            assertEquals(expectedFields.length, actualFields.length, fileName + ": screen " + i + " field count");
            for (int f = 0; f < expectedFields.length; f++) {
                if (expectedFields[f].startsWith("nativeScreen=")) {
                    assertTrue(actualFields[f].startsWith("nativeScreen="), fileName + ": screen " + i
                            + " field " + f + " is " + actualFields[f] + ", not nativeScreen");
                    continue;
                }
                assertEquals(expectedFields[f], actualFields[f], fileName + " (the JNI, expected) vs"
                        + " staticScreen_getScreens (actual): screen " + i + " field " + f);
            }
        }
        LEDGER.compared(expected.size());
        System.out.println("WinScreenParityTest: " + expected.size() + " screen(s) equal " + fileName
                + (expected.equals(actual) ? " byte for byte"
                        : " in every value but nativeScreen: the HMONITOR was renumbered (a session handle)"));
    }

    private static void write(Path file, String text) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, text, StandardCharsets.US_ASCII);
        System.out.println("WinScreenParityTest: wrote " + file.toAbsolutePath());
    }

    /** One line per screen, the twenty constructor arguments by getter: the goldens' data format. */
    private static String snapshot(Screen[] screens) {
        StringBuilder text = new StringBuilder(256 * screens.length);
        for (int i = 0; i < screens.length; i++) {
            text.append(line(i, screens[i])).append('\n');
        }
        return text.toString();
    }

    private static String line(int index, Screen screen) {
        return "screen " + index
                + " nativeScreen=" + screen.getNativeScreen()
                + " depth=" + screen.getDepth()
                + " x=" + screen.getX()
                + " y=" + screen.getY()
                + " width=" + screen.getWidth()
                + " height=" + screen.getHeight()
                + " platformX=" + screen.getPlatformX()
                + " platformY=" + screen.getPlatformY()
                + " platformWidth=" + screen.getPlatformWidth()
                + " platformHeight=" + screen.getPlatformHeight()
                + " visibleX=" + screen.getVisibleX()
                + " visibleY=" + screen.getVisibleY()
                + " visibleWidth=" + screen.getVisibleWidth()
                + " visibleHeight=" + screen.getVisibleHeight()
                + " resolutionX=" + screen.getResolutionX()
                + " resolutionY=" + screen.getResolutionY()
                + " platformScaleX=" + bits(screen.getPlatformScaleX())
                + " platformScaleY=" + bits(screen.getPlatformScaleY())
                + " outputScaleX=" + bits(screen.getRecommendedOutputScaleX())
                + " outputScaleY=" + bits(screen.getRecommendedOutputScaleY());
    }

    private static String bits(float value) {
        return String.format("0x%08x", Float.floatToRawIntBits(value));
    }

    private static String describe(int[] hresultAndValue) {
        return String.format("{0x%08x, %d}", hresultAndValue[0], hresultAndValue[1]);
    }

    private static List<String> dataLines(String text) {
        List<String> lines = new ArrayList<>();
        for (String line : text.split("\n")) {
            String trimmed = line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
            if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                lines.add(trimmed);
            }
        }
        return lines;
    }

    private static String md5(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("MD5").digest(text.getBytes(StandardCharsets.US_ASCII));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    /** A screen golden: its {@code # machine.key=value} header lines and its data lines, in file order. */
    private record Golden(Map<String, String> machine, List<String> data) {

        static Golden load(String fileName) throws IOException {
            String text;
            try (InputStream stream = WinScreenParityTest.class.getResourceAsStream(fileName)) {
                if (stream == null) {
                    throw new AssertionError("no " + fileName + " on the classpath. It is the record of what"
                            + " GlassScreen::CreateJavaScreens built at 8492cb03b0, and cannot be captured again;"
                            + " an absent golden is never a reason to skip.");
                }
                text = new String(stream.readAllBytes(), StandardCharsets.US_ASCII);
            }
            Map<String, String> machine = new LinkedHashMap<>();
            String header = "# " + MACHINE_PREFIX;
            for (String line : text.split("\n")) {
                String trimmed = line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
                if (trimmed.startsWith(header)) {
                    int equals = trimmed.indexOf('=');
                    machine.put(trimmed.substring(2, equals), trimmed.substring(equals + 1));
                }
            }
            return new Golden(machine, dataLines(text));
        }

        /** The data lines as the capture wrote them: each followed by one line feed. */
        String dataText() {
            StringBuilder text = new StringBuilder();
            for (String line : data) {
                text.append(line).append('\n');
            }
            return text.toString();
        }
    }
}
