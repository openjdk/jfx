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

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The normalisation rules of the GTK glass boundary traces, on the goldens themselves, without a display: every rule
 * is idempotent on the golden it was captured with, and {@link GtkDnDTraceTest#ENTER_AFTER_THE_DROP} makes the
 * in-process golden equal to the trace the gate recorded under load - the pointer {@code ENTER} of each drop
 * dispatched after the drag source's loop instead of inside the drop target's reads - while an {@code ENTER} that is
 * missing, or another line that moved, still differs.
 */
public class GtkTraceRulesTest {

    private static final String IN_PROCESS_GOLDEN = GtkTraceGolden.fileName(GtkDnDTraceTest.IN_PROCESS, "1");

    private static List<String> golden(String file) {
        GtkTraceGolden.Golden golden = GtkTraceGolden.load(file);
        assertNotNull(golden, file);
        return golden.trace();
    }

    /** Every golden is a fixed point of the rules of its test class. */
    @Test
    public void theRulesChangeNoGoldenTheyWereCapturedWith() {
        for (String scenario : List.of(GtkDnDTraceTest.BETWEEN_PROCESSES, GtkDnDTraceTest.THROWING_TARGET)) {
            List<String> trace = golden(GtkTraceGolden.fileName(scenario, "1"));
            assertEquals(trace, GtkTraceGolden.normalise(trace, GtkDnDTraceTest.RULES), scenario);
        }
        List<String> trace = golden(IN_PROCESS_GOLDEN);
        assertEquals(trace, GtkTraceGolden.normalise(trace, GtkDnDTraceTest.RULES));
        for (String monitors : List.of("1", "2")) {
            List<String> events = golden(GtkTraceGolden.fileName(GtkEventTraceTest.SCENARIO, monitors));
            assertEquals(events, GtkTraceGolden.normalise(events, GtkEventTraceTest.RULES), monitors);
        }
    }

    /** The in-process rules, applied twice, change nothing the first application did not. */
    @Test
    public void theInProcessRulesAreIdempotent() {
        List<String> once = GtkTraceGolden.normalise(golden(IN_PROCESS_GOLDEN), GtkDnDTraceTest.IN_PROCESS_RULES);
        assertEquals(once, GtkTraceGolden.normalise(once, GtkDnDTraceTest.IN_PROCESS_RULES));
    }

    /**
     * The trace of the gate's loaded runs: in both drop steps, the {@code ENTER} and the {@code isEnabled} of its
     * {@code EnterNotify} came after {@code # source flush returned}, unmarked, instead of inside the nested reads.
     */
    @Test
    public void anEnterAfterTheDragLoopNormalisesLikeTheGolden() {
        List<String> golden = golden(IN_PROCESS_GOLDEN);
        List<String> late = enterAfterTheLoop(golden, "left to right: release over the target", "2");
        late = enterAfterTheLoop(late, "right to left: release over the target", "1");
        assertNotEquals(GtkTraceGolden.normalise(golden, GtkDnDTraceTest.RULES),
                GtkTraceGolden.normalise(late, GtkDnDTraceTest.RULES));
        assertEquals(GtkTraceGolden.normalise(golden, GtkDnDTraceTest.IN_PROCESS_RULES),
                GtkTraceGolden.normalise(late, GtkDnDTraceTest.IN_PROCESS_RULES));
    }

    /** A drop step without its {@code ENTER} still differs from the golden. */
    @Test
    public void aMissingEnterStillDiffers() {
        List<String> golden = golden(IN_PROCESS_GOLDEN);
        List<String> missing = new ArrayList<>(golden);
        assertTrue(missing.removeIf(line -> line.startsWith("~ v2.mouse ENTER ")));
        assertNotEquals(GtkTraceGolden.normalise(golden, GtkDnDTraceTest.IN_PROCESS_RULES),
                GtkTraceGolden.normalise(missing, GtkDnDTraceTest.IN_PROCESS_RULES));
    }

    /** A drop step whose {@code dragEnd} fell into the next step still differs from the golden. */
    @Test
    public void aDragEndInTheNextStepStillDiffers() {
        List<String> golden = golden(IN_PROCESS_GOLDEN);
        List<String> moved = new ArrayList<>(golden);
        int dragEnd = moved.indexOf("v1.dragEnd COPY");
        String line = moved.remove(dragEnd);
        int next = moved.indexOf("== right to left: press in the source");
        moved.add(next + 1, line);
        assertNotEquals(GtkTraceGolden.normalise(golden, GtkDnDTraceTest.IN_PROCESS_RULES),
                GtkTraceGolden.normalise(moved, GtkDnDTraceTest.IN_PROCESS_RULES));
    }

    /**
     * {@code trace} with the nested {@code ~ v<n>.mouse ENTER} of {@code step} and one nested
     * {@code ~ w<n>.isEnabled=true} taken out of the nested reads and put, unmarked, right after the step's
     * {@code # source flush returned}.
     */
    private static List<String> enterAfterTheLoop(List<String> trace, String step, String n) {
        List<String> lines = new ArrayList<>(trace);
        int start = lines.indexOf("== " + step);
        assertTrue(start >= 0, step);
        int enter = -1;
        int isEnabled = -1;
        int returned = -1;
        for (int i = start + 1; i < lines.size() && !lines.get(i).startsWith("== "); i++) {
            String line = lines.get(i);
            if (line.startsWith("~ v" + n + ".mouse ENTER ")) {
                enter = i;
            } else if (line.equals("~ w" + n + ".isEnabled=true")) {
                isEnabled = i;
            } else if (line.equals("# source flush returned")) {
                returned = i;
            }
        }
        assertTrue(enter > start && isEnabled > start && returned > Math.max(enter, isEnabled), step);
        String enterLine = lines.get(enter).substring(GtkEventTrace.NESTED_MARK.length());
        lines.remove(Math.max(enter, isEnabled));
        lines.remove(Math.min(enter, isEnabled));
        returned -= 2;
        lines.add(returned + 1, "w" + n + ".isEnabled=true");
        lines.add(returned + 2, enterLine);
        return lines;
    }
}
