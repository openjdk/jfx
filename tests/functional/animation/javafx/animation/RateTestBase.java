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

public abstract class RateTestBase extends AnimationFunctionalTestBase {
    protected TestSet set = new TestSet(8);

    public RateTestBase(String name) {
        super(name);
    }

    protected abstract void startTimeline();

    // to be called from @Test test()
    protected void doTest() {
        set.timeline.setCycleCount(4);
        
        set.timeline.setRate(0.5f);
        check(4500, 8500);

        set.timeline.setRate(2f);
        check(1250, 2500);

        set.timeline.setCycleCount(Timeline.INDEFINITE);

        set.timeline.setRate(0.5f);
        check(4500, 8500);

        set.timeline.setRate(2f);
        check(1250, 2500);

        set.timeline.setCycleCount(4);
        set.timeline.setRate(0.5f);
        check(4000, 4250, 5250);

        set.timeline.setCycleCount(Timeline.INDEFINITE);
        set.timeline.setRate(0.5f);
        check(4000, 4250, 5250);
    }

    protected void check(int halfTime, int doneTime) {
        check(-1, halfTime, doneTime);
    }

    protected final void check(final int rateChangeTime, final int halfTime, final int doneTime) {
        Timeline tt = new SimpleTimeline(
                new SimpleKeyFrame(0) {
                    @Override protected void action() {
                        set.resetCount();
                        startTimeline();
                    }
                },
                new SimpleKeyFrame(rateChangeTime) {
                    @Override protected void action() {
                        if (rateChangeTime > 0) {
                            set.timeline.setRate(set.timeline.getRate() * 4);
                        }
                    }
                },
                new SimpleKeyFrame(halfTime) {
                    @Override protected void action() {
                        checkHalf();
                    }
                },
                new SimpleKeyFrame(doneTime) {
                    @Override protected void action() {
                        checkDone(set.timeline.getRepeatCount() < 0);
                        set.timeline.stop();
                    }
                }
        );
        tt.play();
        delayFor(tt);
    }

    protected abstract void checkHalf();
    protected abstract void checkDone(boolean infinite);
}
