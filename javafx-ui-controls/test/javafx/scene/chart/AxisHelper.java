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
 * Helper class for Axis. All functionality common to ValueAxis,NumberAxis,CategoryAxis goes here.
 * @author srikalyc
 */
public class AxisHelper {
    private Axis axis;//Empty string

    public AxisHelper() {
    }
    
    public void setAxis(Axis axis) {
        this.axis = axis;
    }
    
    public Axis getDummyAxis() {
        return new Axis() {
                @Override
                protected void setRange(Object o, boolean bln) {}
                @Override
                protected Object getRange() {return null;}
                @Override
                protected List calculateTickValues(double d, Object o) {return null;}
                @Override
                protected String getTickMarkLabel(Object t) {return null;}
                @Override
                protected Object autoRange(double d) {return null;}
                @Override
                public double getZeroPosition() {return 0.0;}
                @Override
                public double getDisplayPosition(Object t) {return 0.0;}
                @Override
                public Object getValueForDisplay(double d) {return null;}
                @Override
                public boolean isValueOnAxis(Object t) {return false;}
                @Override
                public double toNumericValue(Object t) {return 0.0;}
                @Override
                public Object toRealValue(double d) {return 0.0;}

            };
    }
    @Before public void setup() {
        if (axis == null) {
            axis = getDummyAxis();
        }
    }
    
    /*********************************************************************
     * Currently nothing                                                 *
     ********************************************************************/
    
    void defaultRange() {}
    void defaultCalculateTickValues() {}
    void defaultTickMarkLabel() {}
    void defaultAutoRange() {}
    void defaultZeroPosition() {}
    void defaultDisplayPosition() {}
    void defaultValueForDisplay() {}
    void defaultValueOnAxis() {}
    void defaultNumericValue() {}
    void defaultRealValue() {}
   
    
}
