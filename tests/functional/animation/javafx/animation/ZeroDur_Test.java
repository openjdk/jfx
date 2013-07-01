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


public class ZeroDur_Test extends AnimationFunctionalTestBase {
    private Timeline t;
    protected SimpleKeyValueTarget target = new SimpleKeyValueTarget(0d);
    int count = 0;


    public ZeroDur_Test() {
        super("Test various properties of running a 0-duration Timeline");
    }

    @Before public void setUp() {
        t = new SimpleTimeline(1,
                new SimpleKeyFrame(0, false, new KeyValue(target, 1d)) {
                    @Override protected void action() {
                        count ++;
                    }
                }
        );
    }

    @Ignore // TODO: Activate once this was fixed
    @Test public void test() {
        t.play();
        delayFor(t);
        if (count != 1) {
            fail("count=" + count + ", expected 1");
        }
        if (!target.getValue().equals(1d)) {
            fail("target=" + target.getValue() + ", expected 1.0d");
        }

        count = 0;
        t.setCycleCount(5);
        t.play();
        delayFor(t);
        if (count != 5) {
            fail("count=" + count + ", expected 5");
        }
        if (!target.getValue().equals(1d)) {
            fail("target=" + target.getValue() + ", expected 1.0d");
        }
    }


    @After public void cleanUp() {
        t.stop();
    }
}
