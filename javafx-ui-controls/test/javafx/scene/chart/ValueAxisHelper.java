/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
