/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import javafx.scene.text.Font;
import javafx.scene.text.LayoutInfo;
import javafx.scene.text.TabStop;
import javafx.scene.text.TabStopPolicy;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.junit.jupiter.api.Test;

/**
 * Tests TabStopPolicy APIs.
 */
public class TabStopPolicyTest {

    private static final double EPSILON = 0.0001;

    @Test
    public void testPolicyInEffect() {
        TextFlow f = new TextFlow(text(12, "1\t2\t3\n"), text(24, "4\t5\t6"));

        // no tab stop policy defined: tab stop position is determined by the first text node font (at 96)
        verifyPositions(11, f, 0, 12, 96, 108, 192, 204, 0, 24, 96, 120, 192, 216);

        TabStopPolicy p = new TabStopPolicy();
        p.tabStops().add(new TabStop(200));
        f.setTabStopPolicy(p);

        // tab stop policy defined: tab stop at 200, no intervals
        verifyPositions(11, f, 0, 12, 200, 212, 224, 236, 0, 24, 200, 224, 236, 260);

        // add default intervals
        p.setDefaultInterval(500);
        verifyPositions(11, f, 0, 12, 200, 212, 500, 512, 0, 24, 200, 224, 500, 524);

        // tabSize ignored when policy is in effect
        f.setTabSize(100);
        verifyPositions(11, f, 0, 12, 200, 212, 500, 512, 0, 24, 200, 224, 500, 524);

        // removing tab stop policy brings back tabSize
        f.setTabStopPolicy(null);
        verifyPositions(11, f, 0, 12, 1200, 1212, 2400, 2412, 0, 24, 1200, 1224, 2400, 2424);
    }

    @Test
    public void testTabStopOrderHasNoEffect() {
        TextFlow f = new TextFlow(text(12, "1\t2\t3\n"), text(24, "4\t5\t6"));
        TabStopPolicy p = new TabStopPolicy();
        p.tabStops().setAll(new TabStop(200), new TabStop(500));
        f.setTabStopPolicy(p);

        verifyPositions(11, f, 0, 12, 200, 212, 500, 512, 0, 24, 200, 224, 500, 524);

        // order of tabs is irrelevant
        p.tabStops().setAll(new TabStop(500), new TabStop(200));
        verifyPositions(11, f, 0, 12, 200, 212, 500, 512, 0, 24, 200, 224, 500, 524);
    }

    @Test
    public void testNoTabs() {
        TextFlow f = new TextFlow(text(12, "1\t2\t3\n"), text(24, "4\t5\t6"));
        TabStopPolicy p = new TabStopPolicy();
        f.setTabStopPolicy(p);

        // no tab stops whatsoever
        verifyPositions(11, f, 0, 12, 24, 36, 48, 60, 0, 24, 36, 60, 72, 96);
    }

    private static Text text(double size, String text) {
        Text t = new Text(text);
        t.setFont(Font.font("System", size));
        return t;
    }

    private static double diff(double a, double b) {
        return Math.abs(a - b);
    }

    private static void verifyPositions(int count, TextFlow f, double... expected) {
        LayoutInfo la = f.getLayoutInfo();
        for (int i = 0; i <= count; i++) {
            double x = la.caretInfoAt(i, true).getSegmentAt(0).getMinX();
            assertEquals(expected[i], x, EPSILON, "index=" + i);
        }
    }
}
