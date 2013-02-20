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

public class KeyValue_TS200_02_Test extends AnimationFunctionalTestBase {
    private SimpleKeyValueTarget s = new SimpleKeyValueTarget("abc");
    private SimpleKeyValueTarget i = new SimpleKeyValueTarget((Integer)0);
    private SimpleKeyValueTarget b = new SimpleKeyValueTarget(true);
    private SimpleKeyValueTarget f = new SimpleKeyValueTarget((Float)1.0F);
    private SimpleKeyValueTarget d = new SimpleKeyValueTarget(Duration.valueOf(1000));

    private Timeline t;

    private final String newS = "def";
    private final Integer newI = 10;
    private final Boolean newB = false;
    private final Float newF = 10.0F;
    private final Duration newD = Duration.valueOf(10000);

    public KeyValue_TS200_02_Test() {
        super("Should change key values with different interpolators");
    }

    @Before public void setUp() {
        t = new SimpleTimeline(
                new SimpleKeyFrame(1000, false,
                        new KeyValue(s, newS, Interpolator.DISCRETE),
                        new KeyValue(i, newI, Interpolator.EASE_BOTH),
                        new KeyValue(b, newB, Interpolator.EASE_IN),
                        new KeyValue(f, newF, Interpolator.EASE_OUT),
                        new KeyValue(d, newD, Interpolator.LINEAR)
                )
        );
    }

    @Test public void test() {
        t.play();
        delayFor(t);

        if(!newS.equals(s.getValue())) {
            fail("String " + s.getValue());
        }
        if(!newI.equals(i.getValue())) {
            fail("Integer " + i.getValue());
        }
        if(!newB.equals(b.getValue())) {
            fail("Boolean " + b.getValue());
        }
        if(!newF.equals(f.getValue())) {
            fail("Number " + f.getValue());
        }
        if(!newD.equals(d.getValue())) {
            fail("Duration " + d.getValue());
        }
    }


    @After public void cleanUp() {
        t.stop();
    }
}
