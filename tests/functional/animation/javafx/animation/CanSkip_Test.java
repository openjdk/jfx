/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.animation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * Test behavior of canSkip (though the pass/fail of the test only depends on
 * the non-canSkip KeyFrame being executed the correct number of times).
 *
 * Ideally, test should be run with a few different animation timer values,
 * by setting -Dcom.sun.scenario.animation=
 * 5, 60 & 500
 * One must also run using the Swing toolkit, at least until RT-5647 gets
 * fixed.
 * Best is to compare behavior to 1.3.1.  The 2ms KeyFrame should execute
 * roughly the same number of times.
 *
 */

public class CanSkip_Test extends AnimationFunctionalTestBase {
    public static final int N = 100;
    private Timeline t;
    private int count2ms = 0;
    private int count4ms = 0;

    public CanSkip_Test() {
        super("Should not skip KeyFrames");
    }

    @Before public void setUp() {
        t = new SimpleTimeline(N,
                new SimpleKeyFrame(2, true) {
                    @Override protected void action() {
                        count2ms++;
                    }
                },
                new SimpleKeyFrame(4) {
                    @Override protected void action() {
                        count4ms++;
                    }
                }
        );
    }

    @Test public void test() {
        t.play();
        delayFor(t);
        if (count4ms != N) {
            fail("visited only " + count4ms + " out of " + N);
        }
    }


    @After public void cleanUp() {
        t.stop();
    }
}
