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


public class Timeline_TS008_01_Test extends AnimationFunctionalTestBase {
    private Timeline t;
    private final String valid = "0123";
    private String actual = "";

    public Timeline_TS008_01_Test() {
        super("Should visit KeyFrames in right order");
    }

    @Before public void setUp() {
        t = new SimpleTimeline(
                new SimpleKeyFrame(500) {
                    @Override protected void action() {
                        actual += "1";
                    }
                },
                new SimpleKeyFrame(1500) {
                    @Override protected void action() {
                        actual += "3";
                    }
                },
                new SimpleKeyFrame(1000) {
                    @Override protected void action() {
                        actual += "2";
                    }
                },
                new SimpleKeyFrame(0) {
                    @Override protected void action() {
                        actual += "0";
                    }
                }
        );
    }

    @Test public void test() {
        t.play();
        delayFor(t);
        if (!valid.equals(actual)) {
            throw new AssertionError("visited in wrong order: " + actual);
        }
    }

    @After public void cleanUp() {
        t.stop();
    }
}
