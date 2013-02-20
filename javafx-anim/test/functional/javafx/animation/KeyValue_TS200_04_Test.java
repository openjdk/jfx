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

import javafx.beans.AbstractBean;
import javafx.beans.PropertyReference;
import javafx.util.Duration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class KeyValue_TS200_04_Test extends AnimationFunctionalTestBase {
    public static class XYZ extends AbstractBean {
        public XYZ() {}

        private String s= "abc";
        private Integer i = 0;
        private Boolean b = true;
        private Float f = 1.0F;
        private Duration d = Duration.valueOf(1000);

        public String getS() {return s;}
        public void setS(String s) {this.s = s;}
        public Integer getI() {return i;}
        public void setI(Integer i) {this.i = i;}
        public Boolean getB() {return b;}
        public void setB(Boolean b) {this.b = b;}
        public Float getF() {return f;}
        public void setF(Float f) {this.f = f;}
        public Duration getD() {return d;}
        public void setD(Duration d) {this.d = d;}
    }

    private Timeline t;

    private final XYZ xyz = new XYZ();
    private final String newS = "def";
    private final Integer newI = 10;
    private final Boolean newB = false;
    private final Float newF = 10.0F;
    private final Duration newD = Duration.valueOf(10000);

    public KeyValue_TS200_04_Test() {
        super("Should change key values of bean properties with different interpolators");
    }

    @Before public void setUp() {
        t = new SimpleTimeline(
                new SimpleKeyFrame(1000, false,
                        new KeyValue(xyz, new PropertyReference<String>(XYZ.class, "s"), newS, Interpolator.DISCRETE),
                        new KeyValue(xyz, new PropertyReference<Integer>(XYZ.class, "i"), newI, Interpolator.EASE_BOTH),
                        new KeyValue(xyz, new PropertyReference<Boolean>(XYZ.class, "b"), newB, Interpolator.EASE_IN),
                        new KeyValue(xyz, new PropertyReference<Float>(XYZ.class, "f"), newF, Interpolator.EASE_OUT),
                        new KeyValue(xyz, new PropertyReference<Duration>(XYZ.class, "d"), newD, Interpolator.LINEAR)
                )
        );
    }

    @Test public void test() {
        t.play();
        delayFor(t);

        if(!newS.equals(xyz.getS())) {
            fail("String " + xyz.getS());
        }
        if(!newI.equals(xyz.getI())) {
            fail("Integer " + xyz.getI());
        }
        if(!newB.equals(xyz.getB())) {
            fail("Boolean " + xyz.getB());
        }
        if(!newF.equals(xyz.getF())) {
            fail("Number " + xyz.getF());
        }
        if(!newD.equals(xyz.getD())) {
            fail("Duration " + xyz.getD());
        }
    }


    @After public void cleanUp() {
        t.stop();
    }
}
