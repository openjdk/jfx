/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
 */

package javafx.scene.control;


import com.sun.javafx.css.ParsedValue;
import com.sun.javafx.css.CssMetaData;
import com.sun.javafx.css.parser.CSSParser;
import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.Toolkit;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import static org.junit.Assert.assertEquals;
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
    
    protected void startApp(Parent root) {
        scene = new Scene(root,800,600);
        stage = new Stage();
        stage.setScene(scene);
        stage.show();
        tk.firePulse();
    }
    @Test public void testSettingMinorTickCountViaCSS() {
        StackPane pane = new StackPane();
        pane.getChildren().add(slider);
        startApp(pane);
        
        ParsedValue pv = CSSParser.getInstance().parseExpr("-fx-minor-tick-count","2");
        Object val = pv.convert(null);        
        CssMetaData prop = CssMetaData.getCssMetaData(slider.minorTickCountProperty());
        try {
            prop.set(slider, val, null);
            assertEquals(2, slider.getMinorTickCount(), 0.);
        } catch (Exception e) {
            Assert.fail(e.toString());
        }
    }
    
    @Test public void testSettingTickLabelFormatter() {
        StackPane pane = new StackPane();
        pane.getChildren().add(slider);
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
        startApp(pane);
        assertEquals("Ok.", slider.getLabelFormatter().toString(10.0));
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
