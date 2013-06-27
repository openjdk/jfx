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


public class Timeline_TS019_02_Test extends AnimationFunctionalTestBase {
    private Timeline t;
    private double dur = 500;

    public Timeline_TS019_02_Test() {
        super("Should stop at the correct position");
    }

    @Before public void setUp() {
        t = new SimpleTimeline(
                new SimpleKeyFrame(100),
                new SimpleKeyFrame(dur)
        );
    }

    @Test public void test() {
        t.play();
        delayFor(t);
        check(dur, "didn'timeline stay at the end after playing forward");

        t.setRate(-1);
        t.play();
        delayFor(t);
        check(0, "didn'timeline stay at 0 after playing backward");

        t.setRate(1);
        t.play();
        delayFor(t);
        t.stop();
        check(0, "didn'timeline stay at 0 after stop()");
    }

    private void check(double time, String message) {
        if (t.getCurrentTime().toMillis() != time) {
            fail(message + " expected: " + time + "ms, actual: " + t.getCurrentTime());
        }
    }

    @After public void cleanUp() {
        t.stop();
    }
}
