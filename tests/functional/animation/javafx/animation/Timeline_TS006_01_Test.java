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
import org.junit.Ignore;
import org.junit.Test;


public class Timeline_TS006_01_Test extends AnimationFunctionalTestBase {
    private Timeline t1;
    private Timeline t2;
    private int count = 0;

    public Timeline_TS006_01_Test() {
        super("Should create Timelines in two different ways");
    }

    @Before public void setUp() {
        KeyFrame frames[] = {
                new SimpleKeyFrame(0) {
                    @Override protected void action() {
                        count += 1;
                    }
                },
                new SimpleKeyFrame(100) {
                    @Override protected void action() {
                        count += 2;
                    }
                },
                new SimpleKeyFrame(200) {
                    @Override protected void action() {
                        count += 4;
                    }
                }
        };

        t1 = new SimpleTimeline(frames[0], frames[1], frames[2]);
        t2 = new SimpleTimeline(frames);
    }

    @Ignore // TODO: Activate once this was fixed
    @Test public void test() {
        t1.play();
        delayFor(t1);
        if (count != 7) {
            fail("created using a variable argument list : " + count);
        }
        count = 0;
        t2.play();
        delayFor(t2);
        if (count != 7) {
            throw new AssertionError("created using an array: " + count);
        }
    }

    @After public void cleanUp() {
        t1.stop();
        t2.stop();
    }
}
