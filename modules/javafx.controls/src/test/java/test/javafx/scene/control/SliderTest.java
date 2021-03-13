/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.control;


import javafx.css.ParsedValue;
import javafx.css.CssMetaData;
import javafx.css.CssParserShim;
import test.com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.Toolkit;
import javafx.css.StyleableProperty;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Slider;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author smarks
 */
public class SliderTest {


    private Slider slider;
    private Toolkit tk;
    private Scene scene;
    private Stage stage;

    @Before public void setup() {
        tk = (StubToolkit)Toolkit.getToolkit();//This step is not needed (Just to make sure StubToolkit is loaded into VM)
        slider = new Slider();
    }

    protected void startApp() {
        scene = new Scene(new StackPane(slider), 800, 600);
        stage = new Stage();
        stage.setScene(scene);
        stage.show();
        tk.firePulse();
    }

    @Test public void testSettingMinorTickCountViaCSS() {
        startApp();
        ParsedValue pv = new CssParserShim().parseExpr("-fx-minor-tick-count","2");
        Object val = pv.convert(null);
        try {
            ((StyleableProperty)slider.minorTickCountProperty()).applyStyle(null, val);
            assertEquals(2, slider.getMinorTickCount(), 0.);
        } catch (Exception e) {
            Assert.fail(e.toString());
        }
    }
    @Test public void testSettingTickLabelFormatter() {
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setLabelFormatter(new StringConverter<Double>() {
            @Override public String toString(Double t) {
                return "Ok.";
            }
            @Override public Double fromString(String string) {
                return 10.0;
            }
        });
        startApp();
        assertEquals("Ok.", slider.getLabelFormatter().toString(10.0));
    }
    @Test
    public void testSnapToTicks() {
        startApp();
        slider.setValue(5);
        slider.setSnapToTicks(true);
        assertEquals(6.25, slider.getValue(), 0);
    }
    @Test
    public void testSliderHasHorizontalPseudoclassByDefault() {
        Slider slider = new Slider();
        assertTrue(slider.getPseudoClassStates().stream().anyMatch(c -> c.getPseudoClassName().equals("horizontal")));
        assertFalse(slider.getPseudoClassStates().stream().anyMatch(c -> c.getPseudoClassName().equals("vertical")));
    }
//    Slider slider;
//
//    /**
//     * Creates a slider.
//     *
//     * Assumes min == 0, value == 0, max == 100, horizontal
//     * (i.e., vertical == false).
//     */
//    @Override protected Node createNodeToTest() {
//        slider = new Slider();
//        return slider;
//    }
//
//    // TESTS
//
//    @Test
//    public void ensureSkinExists() {
//        assertNotNull(slider.getSkin());
//    }
//
//    @Test
//    public void ensureSkinNodesExist() {
//        assertNotNull(findNodeByStyleClass("thumb"));
//        assertNotNull(findNodeByStyleClass("track"));
//    }
//
//    @Test
//    public void thumbIsPositionedAtLeftWhenValueIsMinimum() {
//        Node thumb = findNodeByStyleClass("thumb");
//        Node track = findNodeByStyleClass("track");
//        Bounds thumbBounds = thumb.localToScene(thumb.getBoundsInLocal());
//        Bounds trackBounds = track.localToScene(track.getBoundsInLocal());
//        double trackLeftX = trackBounds.getMinX();
//        assertTrue(thumbBounds.getMinX() < trackLeftX);
//        assertTrue(thumbBounds.getMaxX() > trackLeftX);
//    }
//
//    @Test
//    public void thumbIsPositionedAtHcenterWhenValueIsMiddle() {
//        slider.setValue(50);
//        awaitQuiescent();
//        Node thumb = findNodeByStyleClass("thumb");
//        Node track = findNodeByStyleClass("track");
//        Bounds thumbBounds = thumb.localToScene(thumb.getBoundsInLocal());
//        Bounds trackBounds = track.localToScene(track.getBoundsInLocal());
//        double trackCenterX = trackBounds.getMinX() + trackBounds.getWidth() / 2.0;
//        assertTrue(thumbBounds.getMinX() < trackCenterX);
//        assertTrue(thumbBounds.getMaxX() > trackCenterX);
//    }
//
//    @Test
//    public void thumbIsPositionedAtRightWhenValueIsMaximum() {
//        slider.setValue(100);
//        awaitQuiescent();
//        Node thumb = findNodeByStyleClass("thumb");
//        Node track = findNodeByStyleClass("track");
//        Bounds thumbBounds = thumb.localToScene(thumb.getBoundsInLocal());
//        Bounds trackBounds = track.localToScene(track.getBoundsInLocal());
//        double trackRightX = trackBounds.getMaxX();
//        assertTrue(thumbBounds.getMinX() < trackRightX);
//        assertTrue(thumbBounds.getMaxX() > trackRightX);
//    }
//
//    @Test
//    public void thumbIsPositionedAtTopWhenValueIsMinimum() {
//        slider.setVertical(true);
//        awaitQuiescent();
//        Node track = findNodeByStyleClass("track");
//        Node thumb = findNodeByStyleClass("thumb");
//        Bounds thumbBounds = thumb.localToScene(thumb.getBoundsInLocal());
//        Bounds trackBounds = track.localToScene(track.getBoundsInLocal());
//        double trackTopY = trackBounds.getMinY();
//        assertTrue(thumbBounds.getMinY() < trackTopY);
//        assertTrue(thumbBounds.getMaxY() > trackTopY);
//    }
//
//    @Test
//    public void thumbIsPositionedAtVcenterWhenValueIsMiddle() {
//        slider.setVertical(true);
//        slider.setValue(50);
//        awaitQuiescent();
//        Node track = findNodeByStyleClass("track");
//        Node thumb = findNodeByStyleClass("thumb");
//        Bounds thumbBounds = thumb.localToScene(thumb.getBoundsInLocal());
//        Bounds trackBounds = track.localToScene(track.getBoundsInLocal());
//        double trackCenterY = trackBounds.getMinY() + trackBounds.getHeight() / 2.0;
//        assertTrue(thumbBounds.getMinY() < trackCenterY);
//        assertTrue(thumbBounds.getMaxY() > trackCenterY);
//    }
//
//    @Test
//    public void thumbIsPositionedAtBottomWhenValueIsMaximum() {
//        slider.setVertical(true);
//        slider.setValue(100);
//        awaitQuiescent();
//        Node track = findNodeByStyleClass("track");
//        Node thumb = findNodeByStyleClass("thumb");
//        Bounds thumbBounds = thumb.localToScene(thumb.getBoundsInLocal());
//        Bounds trackBounds = track.localToScene(track.getBoundsInLocal());
//        double trackBottomY = trackBounds.getMaxY();
//        assertTrue(thumbBounds.getMinY() < trackBottomY);
//        assertTrue(thumbBounds.getMaxY() > trackBottomY);
//    }
//
//    @Test
//    public void movingThumbShouldChangeValue() {
//        double originalValue = slider.getValue();
//        Node thumb = findNodeByStyleClass("thumb");
//        mouse().positionAtCenterOf(thumb);
//        Point2D center = centerOf(thumb);
//        mouse().leftPressDragRelease(50, center.getY());
//        assertTrue(slider.getValue() > originalValue);
//    }

}
