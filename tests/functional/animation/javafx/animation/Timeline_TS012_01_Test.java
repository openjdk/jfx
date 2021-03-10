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


public class Timeline_TS012_01_Test extends AnimationFunctionalTestBase {
    private Timeline t;
    private SimpleKeyValueTarget target = new SimpleKeyValueTarget(1);
    private Integer newVal = 3;

    public Timeline_TS012_01_Test() {
        super("Timeline with repeatCount=0 should never start");
    }

    @Before public void setUp() {
        t = new SimpleTimeline(0,
                new SimpleKeyFrame(100, false, new KeyValue(target, newVal)) {
                    @Override protected void action() {
                        fail("KeyFrame visited");
                    }
                }
        );
    }

    @Ignore // TODO: Activate once this was fixed
    @Test public void test() {
        t.play();
        delay(200);

        if(!newVal.equals(target.getValue())) {
            throw new AssertionError("key value changed");
        }
    }

    @After public void cleanUp() {
        t.stop();
    }
}
