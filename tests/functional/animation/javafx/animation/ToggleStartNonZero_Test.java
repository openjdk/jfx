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

import javafx.util.Duration;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test toggle behavior of a non-autoReverse, repeating TL that begins playback
 * at time > 0.  Playback should toggle normally, and end when we reach the
 * beginning of the TL.
 *
 * Added for RT-9365.
 *
 */

public class ToggleStartNonZero_Test extends AnimationFunctionalTestBase {
    TestSet set = new TestSet(8);
    private Timeline tt;

    public ToggleStartNonZero_Test() {
        super("Test toggle behavior of a non-autoReverse TL starting from time > 0");
    }

    @Before public void setUp() {
        set.timeline.setCycleCount(100);

        tt = new SimpleTimeline(
                new SimpleKeyFrame(0) {
                    @Override protected void action() {
                        set.timeline.getCurrentTime(Duration.valueOf(500));
                        set.timeline.play();
                    }
                },
                new SimpleKeyFrame(2000) {
                    @Override protected void action() {
                        set.timeline.setRate(-set.timeline.getRate());
                    }
                },
                new SimpleKeyFrame(5000) {
                    @Override protected void action() {
                        set.check(5, 5, false, 0, 0);

                        set.resetCount();
                        set.timeline.setCycleCount(Timeline.INDEFINITE);
                        set.timeline.getCurrentTime(Duration.valueOf(500));
                        set.timeline.setRate(1);
                        set.timeline.play();
                    }
                },
                new SimpleKeyFrame(7000) {
                    @Override protected void action() {
                        set.timeline.setRate(-set.timeline.getRate());
                    }
                },
                new SimpleKeyFrame(10000) {
                    @Override protected void action() {
                        set.check(5, 5, false, 0, 0);
                    }
                }
        );
    }

    @Ignore
    @Test public void test() {
        tt.playFromStart();
        delayFor(tt);
    }

    @After public void cleanUp() {
        set.timeline.stop();
    }
}
