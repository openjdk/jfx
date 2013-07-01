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



public class JumpPastEnd_Test extends AnimationFunctionalTestBase {
    private TestSet set = new TestSet();
    private Timeline tt;

    public JumpPastEnd_Test() {
        super("Test of behavior when jumping beyond Timeline.cycleDuration");
    }

    @Before public void setUp() {
        set.timeline.setCycleCount(4);
        
        tt = new SimpleTimeline(
                new SimpleKeyFrame(2100) {
                    @Override protected void action() {
                        set.timeline.getCurrentTime(Duration.valueOf(2000));
                    }
                },
                new SimpleKeyFrame(2200) {
                    @Override protected void action() {
                        set.checkVisitCount(0, 4);
                        if (!set.timeline.isRunning()) {
                            fail("Should be running");
                        }
                        if (((Integer)set.target.getValue()) > set.TARGET_VALUE * 0.15) {
                            fail("Did not skip to beginning");
                        }
                        if (set.timeline.getCurrentTime().toMillis() > set.timeline.getCycleDuration().toMillis() * 0.15) {
                            fail("Time should be under 100ms while it's actually " + set.timeline.getCurrentTime());
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
                },
                new SimpleKeyFrame(3500) {
                    @Override protected void action() {
                        // testing jump past end from final cycle
                        set.resetCount();
                        set.timeline.playFromStart();
                    }
                },
                new SimpleKeyFrame(7000) {
                    @Override protected void action() {
                        set.timeline.getCurrentTime(Duration.valueOf(2000));
                    }
                },
                new SimpleKeyFrame(7100) {
                    @Override protected void action() {
                        set.checkVisitCount(0, 4);
                        if (set.timeline.isRunning()) {
                            fail("Should not be running");
                        }
                        set.checkTarget(set.TARGET_VALUE);
                        set.checkTime(set.timeline.getCycleDuration().toMillis());
                    }
                },
                new SimpleKeyFrame(7500) {
                    @Override protected void action() {
                        // test jumping to end of Timeline with repeatCount=1
                        set.resetCount();
                        set.timeline.setCycleCount(1);
                        set.timeline.playFromStart();
                    }
                },
                new SimpleKeyFrame(7800) {
                    @Override protected void action() {
                        set.timeline.getCurrentTime(set.TIME_DURATION);
                    }
                },
                new SimpleKeyFrame(8300) {
                    @Override protected void action() {
                        set.checkVisitCount(0, 1);
                        if (set.timeline.isRunning()) {
                            fail("Should not be running");
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
