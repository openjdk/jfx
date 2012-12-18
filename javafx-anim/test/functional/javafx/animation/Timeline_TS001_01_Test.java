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


public class Timeline_TS001_01_Test extends AnimationFunctionalTestBase {
    private static int N = 15;
    private static final int INTERVAL = 100; // ms between KeyFrames
    private Timeline t;
    private int frameIndex;

    public Timeline_TS001_01_Test() {
        super("Should play forward and backward (autoReverse=true)");
    }

    @Before public void setUp() {
        KeyFrame frames[] = new KeyFrame[N];
        frameIndex = 0;
        for (int i = 0; i < N; i++) {
            final int j = i;
            frames[i] = new SimpleKeyFrame(i * INTERVAL) {
                @Override protected void action() {
                    int expected = frameIndex < N ? frameIndex : 2 * N - frameIndex - 2;
                    if (j != expected) {
                        fail("visited " + j + " while expected " + expected);
                    }
                    if (frameIndex > N && expected == 0) {
                        frameIndex = 0; // reverse cycle done
                    } else {
                        frameIndex++;
                    }
                }
            };
        }

        t = new SimpleTimeline(2, frames);
        t.setAutoReverse(true);
    }

    @Test public void test() {
        t.play();
        delayFor(t);
    }


    @After public void cleanUp() {
        t.stop();
    }
}