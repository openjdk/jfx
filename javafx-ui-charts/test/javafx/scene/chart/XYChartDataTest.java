/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package javafx.scene.chart;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 */
public class XYChartDataTest {

    @Test public void creatingDataShouldSetValuesAndCurrentValues() {
        XYChart.Data<Number,Number> data = new XYChart.Data<Number,Number>(10, 20);
        assertEquals(10, data.getXValue().longValue());
        assertEquals(10, data.getCurrentX().longValue());
        assertEquals(20, data.getYValue().longValue());
        assertEquals(20, data.getCurrentY().longValue());
    }

    @Ignore("Waiting on fix for RT-13478")
    @Test public void updatingValuesBeforeAddingToASeriesShouldUpdateValuesAndCurrentValues() {
        XYChart.Data<Number,Number> data = new XYChart.Data<Number,Number>(10, 20);
        data.setXValue(100);
        data.setYValue(200);
        assertEquals(100, data.getXValue().longValue());
        assertEquals(100, data.getCurrentX().longValue());
        assertEquals(200, data.getYValue().longValue());
        assertEquals(200, data.getCurrentY().longValue());
    }
}
