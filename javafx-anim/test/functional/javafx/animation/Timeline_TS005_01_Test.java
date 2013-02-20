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


public class Timeline_TS005_01_Test extends AnimationFunctionalTestBase {
    private Timeline t;
    private int count = 0;

    public Timeline_TS005_01_Test() {
        super("Should play forever after set RepeatCount=INDEFINITE");
    }

    @Before public void setUp() {
        t = new SimpleTimeline(2,
                new SimpleKeyFrame(100) {
                    @Override protected void action() {
                        count++;
                        if(count == 1) {
                            t.setCycleCount(Timeline.INDEFINITE);
                        }
                    }
                });
    }

    @Test public void test() {
        t.play();
        delay(3000);
        if (!t.isRunning()) {
            fail("stopped unexpectedly");
        }
    }

    @After public void cleanUp() {
        t.stop();
    }
}
