/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
