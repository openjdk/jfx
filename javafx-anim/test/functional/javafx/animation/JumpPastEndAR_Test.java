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
import org.junit.Test;


public class JumpPastEndAR_Test extends AnimationFunctionalTestBase {
    private TestSet set = new TestSet();
    private Timeline tt;

    public JumpPastEndAR_Test() {
        super("Test of behavior when jumping beyond Timeline.cycleDuration with autoReverse=true");
    }

    @Before public void setUp() {
        set.timeline.setCycleCount(4);
        set.timeline.setAutoReverse(true);

        tt = new SimpleTimeline(
                new SimpleKeyFrame(2100) {
                    @Override protected void action() {
                        set.timeline.getCurrentTime(Duration.valueOf(2000));
                    }
                },
                new SimpleKeyFrame(2200) {
                    @Override protected void action() {
                        set.checkVisitCount(0, 2);
                        if (!set.timeline.isRunning()) {
                            fail("Should be running");
                        }
                        if (((Integer)set.target.getValue()) < set.TARGET_VALUE * 0.85) {
                            fail("Did not skip to beginning " + set.target.getValue());
                        }
                        if (set.timeline.getCurrentTime().toMillis() < set.timeline.getCycleDuration().toMillis() * 0.85) {
                            fail("Time should be under 850ms while it's actually " + set.timeline.getCurrentTime());
                        }
                    }
                },
                new SimpleKeyFrame(3000) {
                    @Override protected void action() {
                        if (!set.timeline.isRunning()) {
                            throw new AssertionError("Should still be running");
                        }
                    }
                },
                new SimpleKeyFrame(3150) {
                    @Override protected void action() {
                        if (set.timeline.isRunning()) {
                            throw new AssertionError("Should not be running");
                        }
                    }
                }
        );
    }

    @Test public void test() {
        set.timeline.play();
        tt.play();
        delayFor(tt);
    }


    @After public void cleanUp() {
        set.timeline.stop();
    }
}
