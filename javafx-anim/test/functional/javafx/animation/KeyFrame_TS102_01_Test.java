/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.TimeUnit;

public class KeyFrame_TS102_01_Test extends AnimationFunctionalTestBase {
//    private boolean ea = false;
    private boolean eb = false;
    private boolean ec = false;

    private long s1;
//    private long t0;
    private long t1;
    private long t2;
    private Timeline t;

    public KeyFrame_TS102_01_Test() {
        super("Should visit all KeyFrames in right order");
    }

    private long getDuration(long begin) {
        return TimeUnit.NANOSECONDS.toMillis((System.nanoTime()-begin));
    }


    @Before public void setUp() {
        t = new SimpleTimeline(
//                // Negative time - should never be visited
//                // COMMENTED OUT: this causes assertion error in Timeline
//                new SimpleKeyFrame(-1000) {
//                    @Override protected void action() {
//                        ea = true;
//                        t0 = getDuration(s1);
//                    }
//                },
                new SimpleKeyFrame(1000) {
                    @Override protected void action() {
                        ec = true;
                        t2 = getDuration(s1);
                    }
                },
                new SimpleKeyFrame(0) {
                    @Override protected void action() {
                        eb = true;
                        t1 = getDuration(s1);
                    }
                }
        );
    }

    @Test public void test() {
        s1 = System.nanoTime();
        t.playFromStart();
        delay(5000);

        if(/*t0 != 0 || t0 > t2 ||*/ t1 > t2) {
            fail("visited in wrong order");
        }

        if(/*ea ||*/ !eb || !ec) {
            fail("didn'timeline visit all KeyFrames");
        }
    }

    @After public void cleanUp() {
        t.stop();
    }
}