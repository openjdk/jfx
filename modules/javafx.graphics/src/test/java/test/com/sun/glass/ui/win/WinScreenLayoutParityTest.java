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
import com.sun.glass.ui.win.WinGlassNativeShim.AnchorArm;
import com.sun.glass.ui.win.WinGlassNativeShim.AnchorHits;
import com.sun.glass.ui.win.WinGlassNativeShim.TestMonitor;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code WinScreenLayout}, the Java port of {@code GlassScreen.cpp}'s monitor arrangement,
 * against what the C computed, bit for bit.
 * <p>
 * <b>The oracle is a golden, and only the golden.</b> {@value #GOLDEN_FILE} holds every layout's input and the
 * answer of {@code gwin_test_screen_anchor}, a test hook that ran the very statements {@code CreateJavaScreens}
 * ran over synthetic monitors. Those statements were first moved out of {@code CreateJavaScreens} into helpers
 * the hook could reach, and a C differential harness compared the moved code with the unmoved one over 25,030
 * layouts with zero differences, so what the hook answered is what the JNI enumeration in commit {@code 8492cb03b0}
 * computes. The golden
 * was captured from that hook once, while the C existed, and while the hook was exported this class also compared
 * {@code WinScreenLayout} with it live, on every layout of both corpora, with no difference. The hook has since
 * been deleted together with the rest of {@code GlassScreen.cpp}'s anchoring, so from then on the golden is
 * the only oracle for that arithmetic: every layout is run through {@code WinScreenLayout}, turned into the
 * {@code Screen} the JNI would construct, and compared with the golden field by field, floats by their raw bits.
 * The golden cannot be captured again - a capture now could only copy the code under test into its own
 * expectation - so its data lines are pinned by md5 ({@value #GOLDEN_DATA_MD5}), and changing them is a
 * behaviour change, never a test fix.
 * <p>
 * <b>The corpus.</b> Corpus A is named: the layouts the port was specified against - one monitor at each scale
 * and at {@code 1.33f}, the four taskbar edges, neighbours on every side at mixed scales, the three arms of
 * {@code originOffsetFromRanges}, an L, a row of four, a primary that has to be swapped into slot 0, a
 * primary chosen by flag, two islands, corner-only contact, negative coordinates, rounding on both sides of
 * one half - plus three the port added: a scale so small the division leaves the {@code int} range, a scale
 * of 0, and a work area outside its monitor (negative rounding inputs). Each is asserted to reach the branch
 * it is named for, counted by the probe {@code WinScreenLayout} reports to - a layout that silently stopped
 * reaching its arm would otherwise keep passing. The first thirty are the C harness's named layouts in its
 * order. Corpus B is {@value #CORPUS_B_SIZE} layouts from that harness's generator and seed (splitmix64,
 * {@code 0x51CE10A1D1FF}), so the totals printed for "harness subset" are comparable with its counters.
 * <p>
 * <b>What keeps it from being vacuous.</b> The golden comparison reads the inputs from the golden itself, not
 * from the generator, and first asserts that they are exactly the corpus - so neither can drift away from what was
 * captured. The arm counters keep a named layout from silently ceasing to reach its branch. And corpus B stays in
 * the golden because the named layouts alone are not enough: the negative control, the C harness's own
 * mutation ({@code originOffsetFromRanges}' midpoint plus {@code 0.25f}), passed every named layout - A19's offset
 * moved by 0.057 and still rounded the same - and failed the comparison at {@code B0017}, both against the live hook
 * and against the golden; the same mutation, run once more after the hook comparisons were removed, still fails
 * {@link #theJavaReproducesTheGolden()} at {@code B0017}.
 */
@EnabledOnOs(OS.WINDOWS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WinScreenLayoutParityTest {

    static final String GOLDEN_FILE = "screen-anchor-golden.txt";

    /**
     * The md5 of {@value #GOLDEN_FILE}'s data lines, each followed by one line feed: what
     * {@code gwin_test_screen_anchor} answered over the corpus when the golden was captured from it.
     */
    static final String GOLDEN_DATA_MD5 = "929f2a7981434d5138ce694ca1587f17";

    /** Where every run writes the Java's rendering of the corpus in the golden's data format, for a diff. */
    static final Path JAVA_RENDERING = Path.of("target", "screen-anchor-java.txt");

    static final long SEED = 0x51CE10A1D1FFL;

    static final int CORPUS_B_SIZE = 2_000;

    /** How many of the named layouts are the C harness's, in its order. */
    static final int HARNESS_NAMED = 30;

    /** {@code Screen}'s own override of the resolution ({@code Screen.java:42-43}). */
    static final String SCREEN_DPI_PROPERTY = "com.sun.javafx.screenDPI";

    /** A named layout and the branch it exists to reach. */
    record Layout(String id, List<TestMonitor> monitors, String arm, BiPredicate<AnchorHits, Screen[]> onArm) {
    }

    @BeforeAll
    static void requireNatives() {
        WinGlassNatives.require();
        // The four lazy holders, in the order WinGlassNativeTest's exact symbol list expects.
        WinGlassNativeShim.bindTimerSymbols();
        WinGlassNativeShim.bindCursorSymbols();
        WinGlassNativeShim.bindBrowserSymbols();
        WinGlassNativeShim.bindScreenSymbols();
        assertTrue(Integer.getInteger(SCREEN_DPI_PROPERTY, 0) <= 0, "-D" + SCREEN_DPI_PROPERTY
                + " replaces both resolutions in Screen's constructor, so the Java's own could not be compared");
    }

    // ---------------------------------------------------------------------------------------------
    // Corpus A and B: the branches they reach
    // ---------------------------------------------------------------------------------------------

    /**
     * Every named layout reaches the branch it is named for, and every screen's output scale is its platform
     * scale bit for bit, which is what the JNI passed. Until the hook was deleted this also compared each layout
     * with the live hook; {@link #theJavaReproducesTheGolden()} is that comparison now.
     */
    @Test
    @Order(1)
    public void everyNamedLayoutReachesItsArm() {
        List<String> offArm = new ArrayList<>();
        for (Layout layout : corpusA()) {
            AnchorHits hits = new AnchorHits();
            Screen[] java = WinGlassNativeShim.anchorInJava(layout.monitors(), hits);
            assertOutputScaleIsPlatformScale(layout.id(), java);
            boolean on = layout.onArm().test(hits, java);
            System.out.println("WinScreenLayoutParityTest: " + layout.id() + " n=" + layout.monitors().size()
                    + (on ? " ON ITS ARM (" : " MISSED ITS ARM (") + layout.arm() + ") " + describe(hits));
            if (!on) {
                offArm.add(layout.id() + ": " + layout.arm());
            }
        }
        assertEquals(List.of(), offArm, "named layouts that no longer reach the branch they are named for");
    }

    /**
     * Together the two corpora reach every arm the probe counts and every input class the C harness counted;
     * the totals over the harness's own subset are printed for comparison with its counters.
     */
    @Test
    @Order(2)
    public void theCorpusReachesEveryArm() {
        AnchorHits harnessSubset = new AnchorHits();
        List<Layout> named = corpusA();
        for (int i = 0; i < named.size(); i++) {
            AnchorHits hits = new AnchorHits();
            WinGlassNativeShim.anchorInJava(named.get(i).monitors(), hits);
            if (i < HARNESS_NAMED) {
                harnessSubset.add(hits);
            }
        }
        AnchorHits generated = new AnchorHits();
        Classes classes = new Classes();
        int slots = 0;
        for (Layout layout : corpusB()) {
            AnchorHits hits = new AnchorHits();
            Screen[] java = WinGlassNativeShim.anchorInJava(layout.monitors(), hits);
            assertOutputScaleIsPlatformScale(layout.id(), java);
            generated.add(hits);
            classes.add(layout.monitors(), java, hits);
            slots += java.length;
        }
        harnessSubset.add(generated);
        System.out.println("WinScreenLayoutParityTest: corpus B " + CORPUS_B_SIZE + " layouts, " + slots + " slots");
        System.out.println("WinScreenLayoutParityTest: harness subset (" + HARNESS_NAMED + " named + "
                + CORPUS_B_SIZE + " generated) " + describe(harnessSubset));
        System.out.println("WinScreenLayoutParityTest: corpus B classes " + classes);

        AnchorHits all = new AnchorHits();
        for (Layout layout : corpusA()) {
            AnchorHits hits = new AnchorHits();
            WinGlassNativeShim.anchorInJava(layout.monitors(), hits);
            all.add(hits);
        }
        all.add(generated);
        for (AnchorArm arm : AnchorArm.values()) {
            assertTrue(all.get(arm) > 0, "no layout of either corpus reached " + arm);
        }
        classes.assertEveryClassReached();
    }

    // ---------------------------------------------------------------------------------------------
    // The golden
    // ---------------------------------------------------------------------------------------------

    /**
     * The golden, four ways: its data lines are the capture unchanged (md5), its inputs are exactly the corpus (so
     * neither the generator nor a named layout drifted away from what was captured), {@code WinScreenLayout} over
     * the golden's own inputs reproduces every captured screen, and its rendering of the corpus equals the golden
     * line for line. The Java rendering is written to {@code target/} first, so a failure can be diffed.
     */
    @Test
    @Order(3)
    public void theJavaReproducesTheGolden() throws IOException {
        List<Layout> corpus = new ArrayList<>(corpusA());
        corpus.addAll(corpusB());
        String rendering = renderFromJava(corpus);
        Files.createDirectories(JAVA_RENDERING.getParent());
        Files.writeString(JAVA_RENDERING, rendering, StandardCharsets.US_ASCII);

        String goldenText = new String(goldenBytes(), StandardCharsets.US_ASCII);
        assertEquals(GOLDEN_DATA_MD5, md5(dataLines(goldenText)), GOLDEN_FILE + ": the data lines are not the ones"
                + " captured from the C before its test hook went; the golden cannot be captured again, so any"
                + " change to them is a behaviour change to review, not a test fix");
        Map<String, List<String>> golden = loadGolden(goldenText);
        assertEquals(corpus.size(), golden.size(), "layouts in the golden");
        int layoutIndex = 0;
        for (Map.Entry<String, List<String>> entry : golden.entrySet()) {
            Layout layout = corpus.get(layoutIndex++);
            assertEquals(layout.id(), entry.getKey(), "layout order in the golden");
            List<String> lines = entry.getValue();
            List<TestMonitor> inputs = new ArrayList<>();
            List<String> captured = new ArrayList<>();
            for (String line : lines) {
                String[] fields = line.split(" ");
                assertEquals(36, fields.length, "golden line: " + line);
                inputs.add(parseMonitor(fields));
                captured.add(String.join(" ", List.of(fields).subList(18, 36)));
            }
            assertEquals(inputRows(layout.monitors()), inputRows(inputs), layout.id() + ": the corpus drifted from"
                    + " the inputs the golden was captured for");
            assertEquals(captured, rows(WinGlassNativeShim.anchorInJava(inputs, new AnchorHits())),
                    layout.id() + ": the golden (expected, what the C computed) vs WinScreenLayout over"
                            + " its inputs");
        }
        assertEquals(dataLines(goldenText), dataLines(rendering),
                "the golden and WinScreenLayout's rendering of the corpus, line for line (" + JAVA_RENDERING + ")");
        System.out.println("WinScreenLayoutParityTest: " + golden.size() + " layouts, " + dataLines(rendering).size()
                + " screens: WinScreenLayout == " + GOLDEN_FILE + " on every field");
    }

    // ---------------------------------------------------------------------------------------------
    // GetUIScale and the float to int conversion
    // ---------------------------------------------------------------------------------------------

    /**
     * {@code GlassApplication::GetUIScale}: the override when it is positive, else {@code dpi / 96.0f} in
     * {@code float}. The expectations are literals: {@code 1.75f} at 168 DPI is exact, and every override
     * that is not positive - -1, 0, NaN - falls through to the DPI.
     */
    @Test
    public void uiScaleIsGetUIScale() {
        int[] dpis = {96, 120, 144, 168, 192, 240, 288};
        float[] byDpi = {1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f};
        for (int i = 0; i < dpis.length; i++) {
            for (float override : new float[] {-1.0f, 0.0f, Float.NaN}) {
                assertEquals(Float.floatToRawIntBits(byDpi[i]),
                        Float.floatToRawIntBits(WinGlassNativeShim.uiScale(dpis[i], override)),
                        "dpi " + dpis[i] + ", override " + override);
            }
            assertEquals(Float.floatToRawIntBits(1.75f),
                    Float.floatToRawIntBits(WinGlassNativeShim.uiScale(dpis[i], 1.75f)), "dpi " + dpis[i]);
        }
        // Not a DPI step: the division really is in float.
        assertEquals(Float.floatToRawIntBits(97 / 96.0f), Float.floatToRawIntBits(WinGlassNativeShim.uiScale(97, -1)));
        // UINT: a DPI with the top bit set is a large positive number, not a negative one.
        assertEquals(Float.floatToRawIntBits(4294967295.0f / 96.0f),
                Float.floatToRawIntBits(WinGlassNativeShim.uiScale(-1, -1)));
    }

    /**
     * {@code (jint) floorf(x)} on MSVC x64 is {@code cvttss2si}: in range it is the floor, and a NaN or any
     * value outside {@code [-2^31, 2^31)} is {@code 0x80000000}. The named layouts
     * {@code degenerate-scale-tiny} and {@code degenerate-scale-zero} proved the same against the hook, whose
     * {@code -2147483648} for both is in the golden.
     */
    @Test
    public void floorToIntIsMsvcsConversion() {
        assertEquals(0, WinGlassNativeShim.floorToInt(0.5f));
        assertEquals(0, WinGlassNativeShim.floorToInt(0.99999994f));
        assertEquals(-1, WinGlassNativeShim.floorToInt(-0.5f));
        assertEquals(1, WinGlassNativeShim.floorToInt(0.49999997f + 0.5f));
        assertEquals(2147483520, WinGlassNativeShim.floorToInt(2147483520.0f));
        assertEquals(Integer.MIN_VALUE, WinGlassNativeShim.floorToInt(2147483648.0f));
        assertEquals(Integer.MIN_VALUE, WinGlassNativeShim.floorToInt(-2147483648.0f));
        assertEquals(Integer.MIN_VALUE, WinGlassNativeShim.floorToInt(-2147483904.0f));
        assertEquals(Integer.MIN_VALUE, WinGlassNativeShim.floorToInt(Float.POSITIVE_INFINITY));
        assertEquals(Integer.MIN_VALUE, WinGlassNativeShim.floorToInt(Float.NEGATIVE_INFINITY));
        assertEquals(Integer.MIN_VALUE, WinGlassNativeShim.floorToInt(Float.NaN));
    }

    // ---------------------------------------------------------------------------------------------
    // Corpus A: the named layouts
    // ---------------------------------------------------------------------------------------------

    static List<Layout> corpusA() {
        List<Layout> v = new ArrayList<>();
        add(v, "single-1.0", "no division at scale 1.0", List.of(mon(0, 0, 1920, 1080, 1.0f, true, 1)),
                (h, o) -> h.get(AnchorArm.SCALE_X_DIVIDES) == 0 && o[0].getWidth() == 1920);
        float[] scales = {1.25f, 1.5f, 1.75f, 2.0f, 2.25f, 3.0f, 1.33f};
        String[] names = {"single-1.25", "single-1.5", "single-1.75", "single-2.0", "single-2.25", "single-3.0",
            "single-1.33f"};
        for (int i = 0; i < scales.length; i++) {
            add(v, names[i], "one division", List.of(mon(0, 0, 2880, 1800, scales[i], true, 1, 48)),
                    (h, o) -> h.get(AnchorArm.SCALE_X_DIVIDES) == 1 && o[0].getWidth() != 2880);
        }
        add(v, "taskbar-bottom", "work area short at the bottom", List.of(mon(0, 0, 2560, 1440, 1.5f, true, 1, 60)),
                (h, o) -> o[0].getVisibleY() == o[0].getY() && o[0].getVisibleHeight() < o[0].getHeight()
                        && o[0].getVisibleWidth() == o[0].getWidth());
        add(v, "taskbar-top", "work area short at the top", List.of(mon(0, 0, 2560, 1440, 1.5f, true, 2, 60)),
                (h, o) -> o[0].getVisibleY() > o[0].getY() && o[0].getVisibleHeight() < o[0].getHeight());
        add(v, "taskbar-left", "work area narrow at the left", List.of(mon(0, 0, 2560, 1440, 1.5f, true, 3, 81)),
                (h, o) -> o[0].getVisibleX() > o[0].getX() && o[0].getVisibleWidth() < o[0].getWidth());
        add(v, "taskbar-right", "work area narrow at the right", List.of(mon(0, 0, 2560, 1440, 1.5f, true, 4, 81)),
                (h, o) -> o[0].getVisibleX() == o[0].getX() && o[0].getVisibleWidth() < o[0].getWidth());
        add(v, "side-by-side-right-mixed", "touchesLeft(m2, m): anchorH after",
                List.of(mon(0, 0, 1920, 1080, 1.0f, true, 1), mon(1920, 0, 2560, 1440, 1.5f, false, 1)),
                (h, o) -> h.get(AnchorArm.TOUCHES_LEFT_TRUE) >= 1 && h.get(AnchorArm.ANCHOR_H_AFTER) >= 1);
        add(v, "side-by-side-left-mixed", "touchesLeft(m, m2): anchorH before",
                List.of(mon(0, 0, 2560, 1440, 1.5f, true, 1), mon(-1920, 0, 1920, 1080, 1.0f, false, 1)),
                (h, o) -> h.get(AnchorArm.ANCHOR_H_BEFORE) >= 1 && o[1].getX() < 0);
        add(v, "stacked-below", "touchesAbove(m2, m): anchorV after",
                List.of(mon(0, 0, 1920, 1080, 1.25f, true, 1), mon(0, 1080, 1920, 1200, 1.0f, false)),
                (h, o) -> h.get(AnchorArm.TOUCHES_ABOVE_TRUE) >= 1 && h.get(AnchorArm.ANCHOR_V_AFTER) >= 1);
        add(v, "stacked-above", "touchesAbove(m, m2): anchorV before",
                List.of(mon(0, 0, 1920, 1080, 1.0f, true, 1), mon(0, -1440, 2560, 1440, 1.5f, false)),
                (h, o) -> h.get(AnchorArm.ANCHOR_V_BEFORE) >= 1 && o[1].getY() < 0);
        add(v, "ooff-equal-start", "originOffsetFromRanges: equal first coordinates",
                List.of(mon(0, 0, 1920, 1080, 1.0f, true), mon(1920, 0, 1280, 1024, 1.25f, false)),
                (h, o) -> originOffsets(h, 1, 0, 0));
        add(v, "ooff-equal-end", "originOffsetFromRanges: equal last coordinates",
                List.of(mon(0, 0, 1920, 1080, 1.0f, true), mon(1920, -360, 2560, 1440, 1.5f, false)),
                (h, o) -> originOffsets(h, 0, 1, 0));
        add(v, "ooff-midpoint", "originOffsetFromRanges: middle of a partial overlap",
                List.of(mon(0, 0, 1920, 1080, 1.25f, true), mon(1920, 200, 1920, 1200, 1.75f, false)),
                (h, o) -> originOffsets(h, 0, 0, 1));
        add(v, "L-of-three", "propagation over three passes",
                List.of(mon(0, 0, 1920, 1080, 1.0f, true), mon(1920, 0, 1920, 1080, 1.5f, false),
                        mon(1920, 1080, 1920, 1080, 1.25f, false)),
                (h, o) -> h.get(AnchorArm.MAX_ANCHOR_PASS) == 3 && h.get(AnchorArm.ANCHOR_H_AFTER) >= 1
                        && h.get(AnchorArm.ANCHOR_V_AFTER) >= 1 && h.get(AnchorArm.ANCHOR) == 1);
        add(v, "row-of-four-alternating", "a monitor anchored in pass 4",
                List.of(mon(0, 0, 1920, 1080, 1.0f, true), mon(1920, 0, 1920, 1080, 2.0f, false),
                        mon(3840, 0, 1920, 1080, 1.0f, false), mon(5760, 0, 1920, 1080, 2.0f, false)),
                (h, o) -> h.get(AnchorArm.MAX_ANCHOR_PASS) == 4 && h.get(AnchorArm.ANCHOR) == 1);
        add(v, "primary-not-index-0", "the swap into slot 0",
                List.of(mon(1920, 0, 1920, 1080, 1.5f, false), mon(-1280, 0, 1280, 1024, 1.0f, false),
                        mon(0, 0, 1920, 1080, 1.25f, true)),
                (h, o) -> o[0].getNativeScreen() == 2 && o[1].getNativeScreen() == 1 && o[2].getNativeScreen() == 0);
        add(v, "no-origin-last-of-two-flagged", "no monitor holds the origin: the last flagged one",
                List.of(mon(100, 100, 1920, 1080, 1.0f, true), mon(2020, 100, 1920, 1080, 1.5f, false),
                        mon(-1820, 100, 1920, 1080, 1.25f, true)),
                (h, o) -> o[0].getNativeScreen() == 2 && o[2].getNativeScreen() == 0);
        add(v, "two-disjoint-islands", "anchor the first unanchored monitor in the same pass",
                List.of(mon(0, 0, 1920, 1080, 1.0f, true), mon(4000, 0, 1920, 1080, 1.5f, false)),
                (h, o) -> h.get(AnchorArm.ANCHOR) == 2 && o[1].getX() == 4000);
        add(v, "corner-only-contact", "corners do not touch",
                List.of(mon(0, 0, 1920, 1080, 1.0f, true), mon(1920, 1080, 1920, 1080, 1.5f, false)),
                (h, o) -> h.get(AnchorArm.TOUCHES_LEFT_TRUE) == 0 && h.get(AnchorArm.TOUCHES_ABOVE_TRUE) == 0
                        && h.get(AnchorArm.ANCHOR) == 2);
        add(v, "negative-coordinates", "neighbours and an island at negative coordinates",
                List.of(mon(0, 0, 1920, 1080, 1.5f, true), mon(-2560, -360, 2560, 1440, 1.0f, false),
                        mon(-6000, -3000, 1280, 1024, 1.25f, false)),
                (h, o) -> h.get(AnchorArm.ANCHOR_H_BEFORE) >= 1 && h.get(AnchorArm.ANCHOR) == 2 && o[2].getX() < 0
                        && o[2].getY() < 0);
        add(v, "odd-round-lt-half", "floor(q + 0.5f) with a fraction below one half",
                List.of(mon(0, 0, 1363, 767, 1.25f, true)),
                (h, o) -> h.get(AnchorArm.ROUND_BELOW_HALF) >= 1);
        add(v, "odd-round-eq-half", "floor(q + 0.5f) at exactly one half",
                List.of(mon(0, 0, 1921, 1081, 2.0f, true, 1, 41)),
                (h, o) -> h.get(AnchorArm.ROUND_AT_HALF) >= 3 && o[0].getWidth() == 961);
        add(v, "odd-round-gt-half", "floor(q + 0.5f) with fractions on both sides of one half",
                List.of(mon(0, 0, 1367, 769, 1.5f, true)),
                (h, o) -> h.get(AnchorArm.ROUND_ABOVE_HALF) >= 1 && h.get(AnchorArm.ROUND_BELOW_HALF) >= 1);
        add(v, "independent-x-y-scale", "x and y divided by different scales",
                List.of(mon(0, 0, 1920, 1200, 1.25f, 1.0f, true, 1, 40),
                        mon(1920, 0, 1600, 1200, 1.0f, 1.5f, false, 0, 40)),
                (h, o) -> h.get(AnchorArm.SCALE_X_DIVIDES) == 1 && h.get(AnchorArm.SCALE_Y_DIVIDES) == 1);
        assertEquals(HARNESS_NAMED, v.size(), "the C harness's named layouts come first");

        // Added by the port: the conversion of an out-of-range or NaN quotient, and negative rounding inputs.
        add(v, "degenerate-scale-tiny", "a quotient beyond the int range converts to 0x80000000",
                List.of(mon(0, 0, 1920, 1080, 1.0e-7f, true, 1)),
                (h, o) -> o[0].getWidth() == Integer.MIN_VALUE && o[0].getHeight() == Integer.MIN_VALUE);
        add(v, "degenerate-scale-zero", "a NaN or infinite quotient converts to 0x80000000",
                List.of(mon(0, 0, 1920, 1080, 0.0f, true, 1)),
                (h, o) -> o[0].getResolutionX() == Integer.MIN_VALUE && o[0].getWidth() == Integer.MIN_VALUE
                        && o[0].getVisibleX() == Integer.MIN_VALUE);
        add(v, "work-area-outside-monitor", "negative rounding inputs",
                List.of(workArea(mon(0, 0, 1921, 1081, 2.0f, true), -41, -41, 1921, 1081),
                        workArea(mon(1921, 0, 1280, 1024, 1.25f, false), 1921 - 33, -7, 3201, 1024)),
                (h, o) -> h.get(AnchorArm.ROUND_NEGATIVE) >= 4 && o[0].getVisibleX() < o[0].getX());
        return v;
    }

    private static void add(List<Layout> corpus, String name, String arm, List<TestMonitor> monitors,
                            BiPredicate<AnchorHits, Screen[]> onArm) {
        corpus.add(new Layout(String.format("A%02d-%s", corpus.size() + 1, name), monitors, arm, onArm));
    }

    private static boolean originOffsets(AnchorHits h, long equalStart, long equalEnd, long midpoint) {
        return h.get(AnchorArm.ORIGIN_OFFSET_EQUAL_START) == equalStart
                && h.get(AnchorArm.ORIGIN_OFFSET_EQUAL_END) == equalEnd
                && h.get(AnchorArm.ORIGIN_OFFSET_MIDPOINT) == midpoint;
    }

    private static TestMonitor mon(int l, int t, int w, int h, float scale, boolean primary) {
        return mon(l, t, w, h, scale, scale, primary, 0, 40);
    }

    private static TestMonitor mon(int l, int t, int w, int h, float scale, boolean primary, int taskbarEdge) {
        return mon(l, t, w, h, scale, scale, primary, taskbarEdge, 40);
    }

    private static TestMonitor mon(int l, int t, int w, int h, float scale, boolean primary, int taskbarEdge,
                                   int taskbar) {
        return mon(l, t, w, h, scale, scale, primary, taskbarEdge, taskbar);
    }

    /**
     * The harness's {@code Mon}: a monitor at {@code (l, t)} of {@code w x h}, its work area the whole
     * monitor minus a taskbar of {@code taskbar} pixels on edge 1 bottom, 2 top, 3 left or 4 right (0 none),
     * colour depth 32, and the DPI {@code GetUIScale} would have divided into that scale,
     * {@code floor(96 * scale + 0.5)}.
     */
    private static TestMonitor mon(int l, int t, int w, int h, float scaleX, float scaleY, boolean primary,
                                   int taskbarEdge, int taskbar) {
        int workLeft = l;
        int workTop = t;
        int workRight = l + w;
        int workBottom = t + h;
        switch (taskbarEdge) {
            case 1 -> workBottom -= taskbar;
            case 2 -> workTop += taskbar;
            case 3 -> workLeft += taskbar;
            case 4 -> workRight -= taskbar;
            default -> {
            }
        }
        return new TestMonitor(l, t, l + w, t + h, workLeft, workTop, workRight, workBottom, primary, 32,
                dpiFor(scaleX), dpiFor(scaleY), scaleX, scaleY);
    }

    private static int dpiFor(float scale) {
        return (int) Math.floor(96.0f * scale + 0.5f);
    }

    private static TestMonitor workArea(TestMonitor m, int left, int top, int right, int bottom) {
        return new TestMonitor(m.monitorLeft(), m.monitorTop(), m.monitorRight(), m.monitorBottom(), left, top, right,
                bottom, m.primary(), m.colorDepth(), m.dpiX(), m.dpiY(), m.uiScaleX(), m.uiScaleY());
    }

    // ---------------------------------------------------------------------------------------------
    // Corpus B: the C harness's generator
    // ---------------------------------------------------------------------------------------------

    static List<Layout> corpusB() {
        Generator generator = new Generator(SEED);
        List<Layout> v = new ArrayList<>(CORPUS_B_SIZE);
        for (int k = 0; k < CORPUS_B_SIZE; k++) {
            v.add(new Layout(String.format("B%04d", k), generator.layout(), "generated", (h, o) -> true));
        }
        return v;
    }

    /**
     * {@code RandomLayout()} of {@code slice10-anchor-diff/main.cpp}, call for call: splitmix64,
     * {@code R(n) = next() % n} unsigned, {@code RR(lo, hi) = lo + R(hi - lo + 1)}. 1 to 6 monitors from
     * seventeen resolutions and nine scales (10 % with an independent y scale); each monitor after the first
     * 85 % snapped to a side of an earlier one (25 % equal starts, 15 % equal ends, 5 % corner only, 3 % a
     * gap, the rest a partial overlap), 15 % placed freely; 25 % of layouts translated; 70 % of monitors with
     * a taskbar; primary flags: 80 % on the monitor holding the origin when there is one, else one, two or
     * none at random.
     */
    static final class Generator {

        private static final int[][] RESOLUTIONS = {{1024, 768}, {1280, 720}, {1280, 1024}, {1366, 768},
            {1440, 900}, {1600, 900}, {1920, 1080}, {1920, 1200}, {2560, 1440}, {2560, 1600}, {3440, 1440},
            {3840, 2160}, {1080, 1920}, {1365, 767}, {1367, 769}, {2880, 1800}, {1921, 1081}};
        private static final float[] SCALES = {1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f, 1.33f, 1.1f};
        private static final int[] TASKBARS = {30, 40, 41, 48, 60, 72, 81};

        private long state;

        Generator(long seed) {
            state = seed;
        }

        private long next() {
            long z = (state += 0x9E3779B97F4A7C15L);
            z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
            z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
            return z ^ (z >>> 31);
        }

        private int r(int n) {
            return (int) Long.remainderUnsigned(next(), n);
        }

        private int rr(int lo, int hi) {
            return lo + r(hi - lo + 1);
        }

        private float scale() {
            return SCALES[r(SCALES.length)];
        }

        /** The mutable struct the C filled in place. */
        private static final class M {
            int left;
            int top;
            int right;
            int bottom;
            int workLeft;
            int workTop;
            int workRight;
            int workBottom;
            boolean primary;
            int depth;
            final float scaleX;
            final float scaleY;

            M(int l, int t, int w, int h, float scaleX, float scaleY) {
                left = l;
                top = t;
                right = l + w;
                bottom = t + h;
                this.scaleX = scaleX;
                this.scaleY = scaleY;
            }
        }

        List<TestMonitor> layout() {
            int n = rr(1, 6);
            List<M> m = new ArrayList<>();
            {
                int[] res = RESOLUTIONS[r(RESOLUTIONS.length)];
                float s = scale();
                m.add(new M(0, 0, res[0], res[1], s, r(10) == 0 ? scale() : s));
            }
            for (int k = 1; k < n; k++) {
                int[] res = RESOLUTIONS[r(RESOLUTIONS.length)];
                int w = res[0];
                int h = res[1];
                float s = scale();
                float sy = r(10) == 0 ? scale() : s;
                int l;
                int t;
                if (r(100) < 85) {
                    M a = m.get(r(m.size()));
                    int aw = a.right - a.left;
                    int ah = a.bottom - a.top;
                    int side = r(4);
                    int mode = r(100);
                    switch (side) {
                        case 0 -> {
                            l = a.right;
                            t = a.top + along(mode, h, ah);
                        }
                        case 1 -> {
                            l = a.left - w;
                            t = a.top + along(mode, h, ah);
                        }
                        case 2 -> {
                            t = a.bottom;
                            l = a.left + along(mode, w, aw);
                        }
                        default -> {
                            t = a.top - h;
                            l = a.left + along(mode, w, aw);
                        }
                    }
                } else {
                    l = rr(-20000, 20000);
                    t = rr(-12000, 12000);
                }
                m.add(new M(l, t, w, h, s, sy));
            }
            if (r(100) < 25) {
                int dx = rr(-3000, 3000);
                int dy = rr(-3000, 3000);
                for (M x : m) {
                    x.left += dx;
                    x.right += dx;
                    x.top += dy;
                    x.bottom += dy;
                }
            }
            for (M x : m) {
                x.workLeft = x.left;
                x.workTop = x.top;
                x.workRight = x.right;
                x.workBottom = x.bottom;
                if (r(100) < 70) {
                    int tb = TASKBARS[r(TASKBARS.length)];
                    switch (r(4)) {
                        case 0 -> x.workBottom -= tb;
                        case 1 -> x.workTop += tb;
                        case 2 -> x.workLeft += tb;
                        default -> x.workRight -= tb;
                    }
                }
                x.depth = r(2) != 0 ? 32 : 24;
            }
            int originIdx = -1;
            for (int i = 0; i < m.size(); i++) {
                M x = m.get(i);
                if (x.left <= 0 && x.top <= 0 && x.right > 0 && x.bottom > 0) {
                    originIdx = i;
                    break;
                }
            }
            int p = r(100);
            if (originIdx >= 0 && p < 80) {
                m.get(originIdx).primary = true;
            } else if (p < 92) {
                m.get(r(m.size())).primary = true;
            } else if (p < 97) {
                m.get(r(m.size())).primary = true;
                m.get(r(m.size())).primary = true;
            }
            List<TestMonitor> monitors = new ArrayList<>(m.size());
            for (M x : m) {
                monitors.add(new TestMonitor(x.left, x.top, x.right, x.bottom, x.workLeft, x.workTop, x.workRight,
                        x.workBottom, x.primary, x.depth, dpiFor(x.scaleX), dpiFor(x.scaleY), x.scaleX, x.scaleY));
            }
            return monitors;
        }

        /** The offset of a new monitor along the shared edge. */
        private int along(int mode, int size, int asize) {
            if (mode < 25) {
                return 0;
            }
            if (mode < 40) {
                return asize - size;
            }
            if (mode < 45) {
                return r(2) != 0 ? -size : asize;
            }
            if (mode < 48) {
                return r(2) != 0 ? -size - rr(1, 400) : asize + rr(1, 400);
            }
            return rr(-size + 1, asize - 1);
        }
    }

    /** The input and output classes the harness counted for its generated layouts, each of which must occur. */
    private static final class Classes {
        private final Map<String, Long> counts = new LinkedHashMap<>();

        Classes() {
            for (String name : List.of("primary holds the origin", "primary by last flag", "last of two flagged",
                    "no primary, index 0 kept", "primary swapped into slot 0", "layout with an island",
                    "monitor anchored in pass >= 4", "corner-only contact pair", "negative coordinates",
                    "multi-monitor")) {
                counts.put(name, 0L);
            }
        }

        void add(List<TestMonitor> m, Screen[] out, AnchorHits hits) {
            int n = m.size();
            if (n > 1) {
                bump("multi-monitor");
            }
            int origin = -1;
            int flagged = 0;
            boolean negative = false;
            for (int i = 0; i < n; i++) {
                TestMonitor x = m.get(i);
                if (origin < 0 && x.monitorLeft() <= 0 && x.monitorTop() <= 0 && x.monitorRight() > 0
                        && x.monitorBottom() > 0) {
                    origin = i;
                }
                if (x.primary()) {
                    flagged++;
                }
                negative |= x.monitorLeft() < 0 || x.monitorTop() < 0;
            }
            if (origin >= 0) {
                bump("primary holds the origin");
            } else if (flagged > 0) {
                bump("primary by last flag");
                if (flagged >= 2) {
                    bump("last of two flagged");
                }
            } else {
                bump("no primary, index 0 kept");
            }
            if (out[0].getNativeScreen() != 0) {
                bump("primary swapped into slot 0");
            }
            if (hits.get(AnchorArm.ANCHOR) > 1) {
                bump("layout with an island");
            }
            if (hits.get(AnchorArm.MAX_ANCHOR_PASS) >= 4) {
                bump("monitor anchored in pass >= 4");
            }
            if (negative) {
                bump("negative coordinates");
            }
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (i == j) {
                        continue;
                    }
                    TestMonitor a = m.get(i);
                    TestMonitor b = m.get(j);
                    boolean leftCorner = a.monitorLeft() == b.monitorRight()
                            && (a.monitorTop() == b.monitorBottom() || a.monitorBottom() == b.monitorTop());
                    boolean aboveCorner = a.monitorTop() == b.monitorBottom()
                            && (a.monitorLeft() == b.monitorRight() || a.monitorRight() == b.monitorLeft());
                    if (leftCorner || aboveCorner) {
                        bump("corner-only contact pair");
                    }
                }
            }
        }

        private void bump(String name) {
            counts.merge(name, 1L, Long::sum);
        }

        void assertEveryClassReached() {
            counts.forEach((name, count) -> assertTrue(count > 0, "no generated layout has: " + name));
        }

        @Override
        public String toString() {
            return counts.toString();
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Rows, comparison and the golden file
    // ---------------------------------------------------------------------------------------------

    /**
     * The output scale equal to the platform scale bit for bit, which is what the JNI passed. The golden records
     * only the platform scale, so this is asserted on the Java directly.
     */
    private static void assertOutputScaleIsPlatformScale(String id, Screen[] java) {
        for (int i = 0; i < java.length; i++) {
            String slot = id + " slot " + i;
            assertEquals(bits(java[i].getPlatformScaleX()), bits(java[i].getRecommendedOutputScaleX()), slot);
            assertEquals(bits(java[i].getPlatformScaleY()), bits(java[i].getRecommendedOutputScaleY()), slot);
        }
    }

    static List<String> rows(Screen[] screens) {
        List<String> rows = new ArrayList<>(screens.length);
        for (Screen s : screens) {
            rows.add(s.getNativeScreen() + " " + s.getDepth() + " " + s.getX() + " " + s.getY() + " " + s.getWidth()
                    + " " + s.getHeight() + " " + s.getPlatformX() + " " + s.getPlatformY() + " "
                    + s.getPlatformWidth() + " " + s.getPlatformHeight() + " " + s.getVisibleX() + " "
                    + s.getVisibleY() + " " + s.getVisibleWidth() + " " + s.getVisibleHeight() + " "
                    + s.getResolutionX() + " " + s.getResolutionY() + " " + bits(s.getPlatformScaleX()) + " "
                    + bits(s.getPlatformScaleY()));
        }
        return rows;
    }

    private static List<String> inputRows(List<TestMonitor> monitors) {
        List<String> rows = new ArrayList<>(monitors.size());
        for (TestMonitor m : monitors) {
            rows.add(m.monitorLeft() + " " + m.monitorTop() + " " + m.monitorRight() + " " + m.monitorBottom() + " "
                    + m.workLeft() + " " + m.workTop() + " " + m.workRight() + " " + m.workBottom() + " "
                    + (m.primary() ? 1 : 0) + " " + m.colorDepth() + " " + m.dpiX() + " " + m.dpiY() + " "
                    + bits(m.uiScaleX()) + " " + bits(m.uiScaleY()));
        }
        return rows;
    }

    private static TestMonitor parseMonitor(String[] f) {
        assertEquals("monitor", f[2]);
        assertEquals("screen", f[17]);
        return new TestMonitor(Integer.parseInt(f[3]), Integer.parseInt(f[4]), Integer.parseInt(f[5]),
                Integer.parseInt(f[6]), Integer.parseInt(f[7]), Integer.parseInt(f[8]), Integer.parseInt(f[9]),
                Integer.parseInt(f[10]), Integer.parseInt(f[11]) != 0, Integer.parseInt(f[12]),
                Integer.parseInt(f[13]), Integer.parseInt(f[14]), fromBits(f[15]), fromBits(f[16]));
    }

    private static String bits(float value) {
        return String.format("0x%08x", Float.floatToRawIntBits(value));
    }

    private static float fromBits(String hex) {
        assertTrue(hex.startsWith("0x") && hex.length() == 10, "a float as 0x + eight hex digits: " + hex);
        return Float.intBitsToFloat(Integer.parseUnsignedInt(hex.substring(2), 16));
    }

    /**
     * {@code WinScreenLayout}'s answer over the corpus in the golden's data format - one line per screen, layout
     * id first - under a comment header saying that it is not a golden.
     */
    private static String renderFromJava(List<Layout> corpus) {
        StringBuilder text = new StringBuilder(1 << 21);
        text.append("# WinScreenLayout over WinScreenLayoutParityTest's corpus, in ").append(GOLDEN_FILE)
                .append("'s data format. Not a golden: the code under test.\n");
        for (Layout layout : corpus) {
            List<String> inputs = inputRows(layout.monitors());
            List<String> outputs = rows(WinGlassNativeShim.anchorInJava(layout.monitors(), new AnchorHits()));
            for (int i = 0; i < inputs.size(); i++) {
                text.append(layout.id()).append(' ').append(i).append(" monitor ").append(inputs.get(i))
                        .append(" screen ").append(outputs.get(i)).append('\n');
            }
        }
        return text.toString();
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

    private static byte[] goldenBytes() throws IOException {
        try (InputStream stream = WinScreenLayoutParityTest.class.getResourceAsStream(GOLDEN_FILE)) {
            if (stream == null) {
                throw new AssertionError("no " + GOLDEN_FILE + " on the classpath. It is the record of what"
                        + " GlassScreen.cpp's anchoring computed, captured from gwin_test_screen_anchor before the"
                        + " hook was deleted, and it cannot be captured again; an absent golden is never a"
                        + " reason to skip.");
            }
            return stream.readAllBytes();
        }
    }

    /** The golden's data lines grouped by layout id, in file order. */
    private static Map<String, List<String>> loadGolden(String goldenText) {
        Map<String, List<String>> layouts = new LinkedHashMap<>();
        for (String line : dataLines(goldenText)) {
            String id = line.substring(0, line.indexOf(' '));
            layouts.computeIfAbsent(id, k -> new ArrayList<>()).add(line);
        }
        return layouts;
    }

    /** The md5 of {@code lines}, each followed by one line feed: the data lines as the capture wrote them. */
    private static String md5(List<String> lines) {
        StringBuilder text = new StringBuilder(1 << 21);
        for (String line : lines) {
            text.append(line).append('\n');
        }
        try {
            byte[] bytes = text.toString().getBytes(StandardCharsets.US_ASCII);
            byte[] digest = MessageDigest.getInstance("MD5").digest(bytes);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    private static String describe(AnchorHits hits) {
        StringBuilder text = new StringBuilder();
        for (AnchorArm arm : AnchorArm.values()) {
            text.append(arm).append('=').append(hits.get(arm)).append(' ');
        }
        return text.toString().trim();
    }
}
