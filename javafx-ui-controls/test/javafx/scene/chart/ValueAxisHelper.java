/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.chart;

import java.util.List;


import org.junit.Before;

/**
 * Helper class for ValueAxis. All functionality common to ValueAxis types goes here.
 * @author srikalyc
 */
public class ValueAxisHelper {
    private ValueAxis axis;//Empty string
    private ValueAxis twoValueAxis;//Empty string
    private NumberAxis threeValueAxis;//Empty string
    private NumberAxis fourValueAxis;//Empty string

    public ValueAxisHelper() {
    }
    
    public void setValueAxis(ValueAxis axis) {
        this.axis = axis;
    }
    
    public void setTwoValueAxis(ValueAxis axis) {
        this.twoValueAxis = axis;
    }
    
    public void setThreeValueAxis(NumberAxis axis) {
        this.threeValueAxis = axis;
    }
    
    public void setFourValueAxis(NumberAxis axis) {
        this.fourValueAxis = axis;
    }
    
    public ValueAxis getDummyValueAxis() {
        return new ValueAxis() {
                @Override
                protected List calculateMinorTickMarks() {return null;}
                @Override
                protected void setRange(Object o, boolean bln) {}
                @Override
                protected Object getRange() {return null;}
                @Override
                protected List calculateTickValues(double d, Object o) {return null;}
                @Override
                protected String getTickMarkLabel(Object t) {return null;}
            };
    }
    public ValueAxis getDummyTwoArgValueAxis() {
        return new ValueAxis(2.0, 100.0) {
                @Override
                protected List calculateMinorTickMarks() {return null;}
                @Override
                protected void setRange(Object o, boolean bln) {}
                @Override
                protected Object getRange() {return null;}
                @Override
                protected List calculateTickValues(double d, Object o) {return null;}
                @Override
                protected String getTickMarkLabel(Object t) {return null;}
            };
    }
    @Before public void setup() {
        if (axis == null) {
            axis = getDummyValueAxis();
        }
    }
    
    /*********************************************************************
     * Currently nothing                                                 *
     ********************************************************************/
    
    void defaultCalculateMinorTickValues() {}
    void defaultRange() {}
    void defaultCalculateTickValues() {}
    void defaultTickMarkLabel() {}
  
    
}
