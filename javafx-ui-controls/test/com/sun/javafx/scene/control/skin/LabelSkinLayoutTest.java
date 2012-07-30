/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.scene.control.skin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collection;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.SkinBaseAccessor;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 */
@RunWith(Parameterized.class)
public class LabelSkinLayoutTest {
    private static final double GRAPHIC_WIDTH = 23;
    private static final double GRAPHIC_HEIGHT = 32;
    private static final double LABEL_WIDTH = 300;
    private static final double LABEL_HEIGHT = 300;
    
    @SuppressWarnings("rawtypes")
    @Parameters public static Collection implementations() {
        return Arrays.asList(new Object[][] {
                {-10},
                {0},
                {10}
//            { HPos.CENTER, VPos.BOTTOM, -10 },
//            { HPos.CENTER, VPos.BOTTOM, 0 },
//            { HPos.CENTER, VPos.BOTTOM, 10 },
//            { HPos.CENTER, VPos.CENTER, -10 },
//            { HPos.CENTER, VPos.CENTER, 0 },
//            { HPos.CENTER, VPos.CENTER, 10 },
//            { HPos.CENTER, VPos.TOP, -10 },
//            { HPos.CENTER, VPos.TOP, 0 },
//            { HPos.CENTER, VPos.TOP, 10 },
//            { HPos.LEFT, VPos.BOTTOM, -10 },
//            { HPos.LEFT, VPos.BOTTOM, 0 },
//            { HPos.LEFT, VPos.BOTTOM, 10 },
//            { HPos.LEFT, VPos.CENTER, -10 },
//            { HPos.LEFT, VPos.CENTER, 0 },
//            { HPos.LEFT, VPos.CENTER, 10 },
//            { HPos.LEFT, VPos.TOP, -10 },
//            { HPos.LEFT, VPos.TOP, 0 },
//            { HPos.LEFT, VPos.TOP, 10 },
//            { HPos.RIGHT, VPos.BOTTOM, -10 },
//            { HPos.RIGHT, VPos.BOTTOM, 0 },
//            { HPos.RIGHT, VPos.BOTTOM, 10 },
//            { HPos.RIGHT, VPos.CENTER, -10 },
//            { HPos.RIGHT, VPos.CENTER, 0 },
//            { HPos.RIGHT, VPos.CENTER, 10 },
//            { HPos.RIGHT, VPos.TOP, -10 },
//            { HPos.RIGHT, VPos.TOP, 0 },
//            { HPos.RIGHT, VPos.TOP, 10 },
        });
    }

    private int graphicTextGap = 0;
//    private VPos vpos;
//    private HPos hpos;
    private Label label;
    private LabelSkin skin;
    private Text text;
    
    public LabelSkinLayoutTest(int graphicTextGap) {
//    public LabelSkinLayoutTest(HPos hpos, VPos vpos, int graphicTextGap) {
        this.graphicTextGap = graphicTextGap;
//        this.hpos = hpos;
//        this.vpos = vpos;
    }

    // We will parameterize the hpos and vpos to use, but all of the other tests
    // are all done manually.
    
    @Before public void setup() {
        label = new Label();
        label.resize(LABEL_WIDTH, LABEL_HEIGHT);
        label.setGraphicTextGap(graphicTextGap);
//        label.setHpos(hpos);
//        label.setVpos(vpos);
        skin = new LabelSkin(label);
        label.setSkin(skin);
        text = skin.text;
    }

    private Bounds getContentBounds() {
        Bounds b = null;
        for (Node child : SkinBaseAccessor.getChildren(skin)) {
            if (child.isManaged()) {
                Bounds childBounds = child.getBoundsInParent();
                if (b == null) {
                    b = childBounds;
                } else {
                    final double minX = Math.min(b.getMinX(), childBounds.getMinX());
                    final double minY = Math.min(b.getMinY(), childBounds.getMinY());
                    final double maxX = Math.max(b.getMaxX(), childBounds.getMaxX());
                    final double maxY = Math.max(b.getMaxY(), childBounds.getMaxY());
                    b = new BoundingBox(minX, minY, maxX - minX, maxY - minY);
                }
            }
        }
        return b;
    }
    
    private Bounds getNormalizedBounds(Bounds contentBounds, Node graphic) {
        Bounds b = graphic.getBoundsInParent();
        return new BoundingBox(
                b.getMinX() - contentBounds.getMinX(),
                b.getMinY() - contentBounds.getMinY(),
                b.getWidth(),
                b.getHeight());
    }
    
    // Note that in Label, we pixel align so that the text is crisp, so do so here
    private void assertCenteredHorizontally(Bounds totalSpace, Bounds b) {
        if (b.getWidth() != totalSpace.getWidth()) {
            final double expected = Math.round((totalSpace.getWidth() - b.getWidth()) / 2.0);
            assertEquals(expected, b.getMinX(), 0.001);
        }
    }

    // Note that in Label, we pixel align (snap to pixel)
    private void assertCenteredVertically(Bounds totalSpace, Bounds b) {
        if (b.getHeight() != totalSpace.getHeight()) {
            final double expected = Math.round((totalSpace.getHeight() - b.getHeight()) / 2.0);
            assertEquals(expected, b.getMinY(), 0.001);
        }
    }
    
    private void assertContentAreaPositionedCorrectly(Bounds contentBounds) {
//        switch (label.getVpos()) {
//            //case BASELINE:
//            case CENTER:
//                assertCenteredVertically(label.getBoundsInLocal(), contentBounds);
//                break;
//            case BOTTOM:
//                assertEquals(label.getBoundsInLocal().getHeight(), contentBounds.getMaxY(), 0.001);
//                break;
//            case TOP:
//                assertEquals(0, contentBounds.getMinY(), 0);
//                break;
//            default:
//                System.err.println("Unhandled vpos case for LabelSkinLayoutTest");
//        }
    }
    
    @Test public void graphic_nullText_TOP() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        label.setGraphic(graphic);
        label.setText(null);
        label.setContentDisplay(ContentDisplay.TOP);        
        label.layout();
        
        Bounds contentBounds = getContentBounds();
        Bounds graphicBounds = getNormalizedBounds(contentBounds, graphic);
        assertContentAreaPositionedCorrectly(contentBounds);
        // Graphic alone makes up the content bounds, so contentBounds and
        // graphic bounds should have the same width and height
        assertEquals(contentBounds.getWidth(), graphicBounds.getWidth(), 0);
        assertEquals(contentBounds.getHeight(), graphicBounds.getHeight(), 0);
    }

    @Test public void graphic_emptyText_TOP() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        label.setGraphic(graphic);
        label.setText("");
        label.setContentDisplay(ContentDisplay.TOP);
        label.layout();
        
        Bounds contentBounds = getContentBounds();
        Bounds graphicBounds = getNormalizedBounds(contentBounds, graphic);
        assertContentAreaPositionedCorrectly(contentBounds);
        // Graphic alone makes up the content bounds, so contentBounds and
        // graphic bounds should have the same width and height
        assertEquals(contentBounds.getWidth(), graphicBounds.getWidth(), 0);
        assertEquals(contentBounds.getHeight(), graphicBounds.getHeight(), 0);
    }

    @Test public void graphic_Text_TOP() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        label.setGraphic(graphic);
        label.setText("Falcon");
        label.setFont(new Font("Amble Condensed", 12));
        label.setContentDisplay(ContentDisplay.TOP);
        label.layout();
        
        // There is both a graphic & text node in this case. So we need to
        // compare their positions. Since this is TOP, the graphic should
        // be above the text by the amount specified in graphic-text-gap
        Bounds contentBounds = getContentBounds();
        Bounds graphicBounds = getNormalizedBounds(contentBounds, graphic);
        Bounds textBounds = getNormalizedBounds(contentBounds, text);
        assertContentAreaPositionedCorrectly(contentBounds);
        final double expected = Math.round(textBounds.getMinY() - (GRAPHIC_HEIGHT + label.getGraphicTextGap()));
        assertEquals(expected, graphicBounds.getMinY(), 0);
        // And they should both be centered horizontally
        assertCenteredHorizontally(contentBounds, graphicBounds);
        assertCenteredHorizontally(contentBounds, textBounds);
    }

    @Test public void unmanagedGraphic_nullText_TOP() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        graphic.setManaged(false);
        label.setGraphic(graphic);
        label.setText(null);
        label.setContentDisplay(ContentDisplay.TOP);
        label.layout();
        
        assertNull(getContentBounds());
    }

    @Test public void unmanagedGraphic_emptyText_TOP() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        graphic.setManaged(false);
        label.setGraphic(graphic);
        label.setText("");
        label.setContentDisplay(ContentDisplay.TOP);
        label.layout();
        
        assertNull(getContentBounds());
    }

    @Test public void unmanagedGraphic_Text_TOP() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        graphic.setManaged(false);
        label.setGraphic(graphic);
        label.setText("Falcon");
        label.setContentDisplay(ContentDisplay.TOP);
        label.layout();
        
        Bounds contentBounds = getContentBounds();
        Bounds textBounds = getNormalizedBounds(contentBounds, text);
        assertContentAreaPositionedCorrectly(contentBounds);
        // Text alone makes up the content bounds, so contentBounds and
        // text bounds should have the same width and height
        assertEquals(contentBounds.getWidth(), textBounds.getWidth(), 0);
        assertEquals(contentBounds.getHeight(), textBounds.getHeight(), 0);
    }

    @Test public void noGraphic_nullText_TOP() {
        label.setText(null);
        label.setContentDisplay(ContentDisplay.TOP);
        label.layout();
        
        assertNull(getContentBounds());
    }

    @Test public void noGraphic_emptyText_TOP() {
        label.setText("");
        label.setContentDisplay(ContentDisplay.TOP);
        label.layout();
        
        assertNull(getContentBounds());
    }

    @Test public void noGraphic_Text_TOP() {
        label.setText("Falcon");
        label.setContentDisplay(ContentDisplay.TOP);
        label.layout();
        
        Bounds contentBounds = getContentBounds();
        Bounds textBounds = getNormalizedBounds(contentBounds, text);
        assertContentAreaPositionedCorrectly(contentBounds);
        // Text alone makes up the content bounds, so contentBounds and
        // text bounds should have the same width and height
        assertEquals(contentBounds.getWidth(), textBounds.getWidth(), 0);
        assertEquals(contentBounds.getHeight(), textBounds.getHeight(), 0);
    }

    /** */
    @Test public void graphic_nullText_RIGHT() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        label.setGraphic(graphic);
        label.setText(null);
        label.setContentDisplay(ContentDisplay.RIGHT);
        label.layout();
        
        Bounds contentBounds = getContentBounds();
        Bounds graphicBounds = getNormalizedBounds(contentBounds, graphic);
        assertContentAreaPositionedCorrectly(contentBounds);
        // Graphic alone makes up the content bounds, so contentBounds and
        // graphic bounds should have the same width and height
        assertEquals(contentBounds.getWidth(), graphicBounds.getWidth(), 0);
        assertEquals(contentBounds.getHeight(), graphicBounds.getHeight(), 0);
    }

    @Test public void graphic_emptyText_RIGHT() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        label.setGraphic(graphic);
        label.setText("");
        label.setContentDisplay(ContentDisplay.RIGHT);
        label.layout();
        
        Bounds contentBounds = getContentBounds();
        Bounds graphicBounds = getNormalizedBounds(contentBounds, graphic);
        assertContentAreaPositionedCorrectly(contentBounds);
        // Graphic alone makes up the content bounds, so contentBounds and
        // graphic bounds should have the same width and height
        assertEquals(contentBounds.getWidth(), graphicBounds.getWidth(), 0);
        assertEquals(contentBounds.getHeight(), graphicBounds.getHeight(), 0);
    }

    @Test public void graphic_Text_RIGHT() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        label.setGraphic(graphic);
        label.setText("Falcon");
        label.setContentDisplay(ContentDisplay.RIGHT);
        label.layout();
        
        // There is both a graphic & text node in this case. So we need to
        // compare their positions. Since this is RIGHT, the graphic should
        // be right of the text by the amount specified in graphic-text-gap
        Bounds contentBounds = getContentBounds();
        Bounds graphicBounds = getNormalizedBounds(contentBounds, graphic);
        Bounds textBounds = getNormalizedBounds(contentBounds, text);
        assertContentAreaPositionedCorrectly(contentBounds);
        final double expected = Math.round(textBounds.getMaxX() + label.getGraphicTextGap());
        assertEquals(expected, graphicBounds.getMinX(), 0.001);
        // And they should both be centered vertically
        assertCenteredVertically(contentBounds, graphicBounds);
        assertCenteredVertically(contentBounds, textBounds);
    }

    @Test public void unmanagedGraphic_nullText_RIGHT() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        graphic.setManaged(false);
        label.setGraphic(graphic);
        label.setText(null);
        label.setContentDisplay(ContentDisplay.RIGHT);
        label.layout();
        
        assertNull(getContentBounds());
    }

    @Test public void unmanagedGraphic_emptyText_RIGHT() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        graphic.setManaged(false);
        label.setGraphic(graphic);
        label.setText("");
        label.setContentDisplay(ContentDisplay.RIGHT);
        label.layout();
        
        assertNull(getContentBounds());
    }

    @Test public void unmanagedGraphic_Text_RIGHT() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        graphic.setManaged(false);
        label.setGraphic(graphic);
        label.setText("Falcon");
        label.setContentDisplay(ContentDisplay.RIGHT);
        label.layout();
        
        Bounds contentBounds = getContentBounds();
        Bounds textBounds = getNormalizedBounds(contentBounds, text);
        assertContentAreaPositionedCorrectly(contentBounds);
        // Text alone makes up the content bounds, so contentBounds and
        // text bounds should have the same width and height
        assertEquals(contentBounds.getWidth(), textBounds.getWidth(), 0);
        assertEquals(contentBounds.getHeight(), textBounds.getHeight(), 0);
    }

    @Test public void noGraphic_nullText_RIGHT() {
        label.setText(null);
        label.setContentDisplay(ContentDisplay.RIGHT);
        label.layout();
        
        assertNull(getContentBounds());
    }

    @Test public void noGraphic_emptyText_RIGHT() {
        label.setText("");
        label.setContentDisplay(ContentDisplay.RIGHT);
        label.layout();
        
        assertNull(getContentBounds());
    }

    @Test public void noGraphic_Text_RIGHT() {
        label.setText("Falcon");
        label.setContentDisplay(ContentDisplay.RIGHT);
        label.layout();
        
        Bounds contentBounds = getContentBounds();
        Bounds textBounds = getNormalizedBounds(contentBounds, text);
        assertContentAreaPositionedCorrectly(contentBounds);
        // Text alone makes up the content bounds, so contentBounds and
        // text bounds should have the same width and height
        assertEquals(contentBounds.getWidth(), textBounds.getWidth(), 0);
        assertEquals(contentBounds.getHeight(), textBounds.getHeight(), 0);
    }

    /** */
    @Test public void graphic_nullText_BOTTOM() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        label.setGraphic(graphic);
        label.setText(null);
        label.setContentDisplay(ContentDisplay.BOTTOM);
        label.layout();
        
        Bounds contentBounds = getContentBounds();
        Bounds graphicBounds = getNormalizedBounds(contentBounds, graphic);
        assertContentAreaPositionedCorrectly(contentBounds);
        // Graphic alone makes up the content bounds, so contentBounds and
        // graphic bounds should have the same width and height
        assertEquals(contentBounds.getWidth(), graphicBounds.getWidth(), 0);
        assertEquals(contentBounds.getHeight(), graphicBounds.getHeight(), 0);
    }

    @Test public void graphic_emptyText_BOTTOM() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        label.setGraphic(graphic);
        label.setText("");
        label.setContentDisplay(ContentDisplay.BOTTOM);
        label.layout();
        
        Bounds contentBounds = getContentBounds();
        Bounds graphicBounds = getNormalizedBounds(contentBounds, graphic);
        assertContentAreaPositionedCorrectly(contentBounds);
        // Graphic alone makes up the content bounds, so contentBounds and
        // graphic bounds should have the same width and height
        assertEquals(contentBounds.getWidth(), graphicBounds.getWidth(), 0);
        assertEquals(contentBounds.getHeight(), graphicBounds.getHeight(), 0);
    }

    @Test public void graphic_Text_BOTTOM() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        label.setGraphic(graphic);
        label.setText("Falcon");
        label.setFont(new Font("Amble Condensed", 12));
        label.setContentDisplay(ContentDisplay.BOTTOM);
        label.layout();
        
        // There is both a graphic & text node in this case. So we need to
        // compare their positions. Since this is BOTTOM, the graphic should
        // be below the text by the amount specified in graphic-text-gap
        Bounds contentBounds = getContentBounds();
        Bounds graphicBounds = getNormalizedBounds(contentBounds, graphic);
        Bounds textBounds = getNormalizedBounds(contentBounds, text);
        assertContentAreaPositionedCorrectly(contentBounds);
        final double expected = Math.round(textBounds.getMaxY() + label.getGraphicTextGap());
        assertEquals(expected, graphicBounds.getMinY(), 0);
        // And they should both be centered horizontally
        assertCenteredHorizontally(contentBounds, graphicBounds);
        assertCenteredHorizontally(contentBounds, textBounds);
    }

    @Test public void unmanagedGraphic_nullText_BOTTOM() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        graphic.setManaged(false);
        label.setGraphic(graphic);
        label.setText(null);
        label.setContentDisplay(ContentDisplay.BOTTOM);
        label.layout();
        
        assertNull(getContentBounds());
    }

    @Test public void unmanagedGraphic_emptyText_BOTTOM() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        graphic.setManaged(false);
        label.setGraphic(graphic);
        label.setText("");
        label.setContentDisplay(ContentDisplay.BOTTOM);
        label.layout();
        
        assertNull(getContentBounds());
    }

    @Test public void unmanagedGraphic_Text_BOTTOM() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        graphic.setManaged(false);
        label.setGraphic(graphic);
        label.setText("Falcon");
        label.setContentDisplay(ContentDisplay.BOTTOM);
        label.layout();
        
        Bounds contentBounds = getContentBounds();
        Bounds textBounds = getNormalizedBounds(contentBounds, text);
        assertContentAreaPositionedCorrectly(contentBounds);
        // Text alone makes up the content bounds, so contentBounds and
        // text bounds should have the same width and height
        assertEquals(contentBounds.getWidth(), textBounds.getWidth(), 0);
        assertEquals(contentBounds.getHeight(), textBounds.getHeight(), 0);
    }

    @Test public void noGraphic_nullText_BOTTOM() {
        label.setText(null);
        label.setContentDisplay(ContentDisplay.BOTTOM);
        label.layout();
        
        assertNull(getContentBounds());
    }

    @Test public void noGraphic_emptyText_BOTTOM() {
        label.setText("");
        label.setContentDisplay(ContentDisplay.BOTTOM);
        label.layout();
        
        assertNull(getContentBounds());
    }

    @Test public void noGraphic_Text_BOTTOM() {
        label.setText("Falcon");
        label.setContentDisplay(ContentDisplay.BOTTOM);
        label.layout();
        
        Bounds contentBounds = getContentBounds();
        Bounds textBounds = getNormalizedBounds(contentBounds, text);
        assertContentAreaPositionedCorrectly(contentBounds);
        // Text alone makes up the content bounds, so contentBounds and
        // text bounds should have the same width and height
        assertEquals(contentBounds.getWidth(), textBounds.getWidth(), 0);
        assertEquals(contentBounds.getHeight(), textBounds.getHeight(), 0);
    }

    /** */
    @Test public void graphic_nullText_LEFT() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        label.setGraphic(graphic);
        label.setText(null);
        label.setContentDisplay(ContentDisplay.LEFT);
        label.layout();
        
        Bounds contentBounds = getContentBounds();
        Bounds graphicBounds = getNormalizedBounds(contentBounds, graphic);
        assertContentAreaPositionedCorrectly(contentBounds);
        // Graphic alone makes up the content bounds, so contentBounds and
        // graphic bounds should have the same width and height
        assertEquals(contentBounds.getWidth(), graphicBounds.getWidth(), 0);
        assertEquals(contentBounds.getHeight(), graphicBounds.getHeight(), 0);
    }

    @Test public void graphic_emptyText_LEFT() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        label.setGraphic(graphic);
        label.setText("");
        label.setContentDisplay(ContentDisplay.LEFT);
        label.layout();
        
        Bounds contentBounds = getContentBounds();
        Bounds graphicBounds = getNormalizedBounds(contentBounds, graphic);
        assertContentAreaPositionedCorrectly(contentBounds);
        // Graphic alone makes up the content bounds, so contentBounds and
        // graphic bounds should have the same width and height
        assertEquals(contentBounds.getWidth(), graphicBounds.getWidth(), 0);
        assertEquals(contentBounds.getHeight(), graphicBounds.getHeight(), 0);
    }

    @Test public void graphic_Text_LEFT() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        label.setGraphic(graphic);
        label.setText("Falcon");
        label.setContentDisplay(ContentDisplay.LEFT);
        label.layout();
        
        // There is both a graphic & text node in this case. So we need to
        // compare their positions. Since this is LEFT, the graphic should
        // be left of the text by the amount specified in graphic-text-gap
        Bounds contentBounds = getContentBounds();
        Bounds graphicBounds = getNormalizedBounds(contentBounds, graphic);
        Bounds textBounds = getNormalizedBounds(contentBounds, text);
        assertContentAreaPositionedCorrectly(contentBounds);
        final double expected = Math.round(graphicBounds.getMaxX() + label.getGraphicTextGap());
        assertEquals(expected, textBounds.getMinX(), 0.001);
        // And they should both be centered vertically
        assertCenteredVertically(contentBounds, graphicBounds);
        assertCenteredVertically(contentBounds, textBounds);
    }

    @Test public void unmanagedGraphic_nullText_LEFT() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        graphic.setManaged(false);
        label.setGraphic(graphic);
        label.setText(null);
        label.setContentDisplay(ContentDisplay.LEFT);
        label.layout();
        
        assertNull(getContentBounds());
    }

    @Test public void unmanagedGraphic_emptyText_LEFT() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        graphic.setManaged(false);
        label.setGraphic(graphic);
        label.setText("");
        label.setContentDisplay(ContentDisplay.LEFT);
        label.layout();
        
        assertNull(getContentBounds());
    }

    @Test public void unmanagedGraphic_Text_LEFT() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        graphic.setManaged(false);
        label.setGraphic(graphic);
        label.setText("Falcon");
        label.setContentDisplay(ContentDisplay.LEFT);
        label.layout();
        
        Bounds contentBounds = getContentBounds();
        Bounds textBounds = getNormalizedBounds(contentBounds, text);
        assertContentAreaPositionedCorrectly(contentBounds);
        // Text alone makes up the content bounds, so contentBounds and
        // text bounds should have the same width and height
        assertEquals(contentBounds.getWidth(), textBounds.getWidth(), 0);
        assertEquals(contentBounds.getHeight(), textBounds.getHeight(), 0);
    }

    @Test public void noGraphic_nullText_LEFT() {
        label.setText(null);
        label.setContentDisplay(ContentDisplay.LEFT);
        label.layout();
        
        assertNull(getContentBounds());
    }

    @Test public void noGraphic_emptyText_LEFT() {
        label.setText("");
        label.setContentDisplay(ContentDisplay.LEFT);
        label.layout();
        
        assertNull(getContentBounds());
    }

    @Test public void noGraphic_Text_LEFT() {
        label.setText("Falcon");
        label.setContentDisplay(ContentDisplay.LEFT);
        label.layout();
        
        Bounds contentBounds = getContentBounds();
        Bounds textBounds = getNormalizedBounds(contentBounds, text);
        assertContentAreaPositionedCorrectly(contentBounds);
        // Text alone makes up the content bounds, so contentBounds and
        // text bounds should have the same width and height
        assertEquals(contentBounds.getWidth(), textBounds.getWidth(), 0);
        assertEquals(contentBounds.getHeight(), textBounds.getHeight(), 0);
    }

    /** */
    @Test public void graphic_nullText_CENTER() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        label.setGraphic(graphic);
        label.setText(null);
        label.setContentDisplay(ContentDisplay.CENTER);
        label.layout();
        
        Bounds contentBounds = getContentBounds();
        Bounds graphicBounds = getNormalizedBounds(contentBounds, graphic);
        assertContentAreaPositionedCorrectly(contentBounds);
        // Graphic alone makes up the content bounds, so contentBounds and
        // graphic bounds should have the same width and height
        assertEquals(contentBounds.getWidth(), graphicBounds.getWidth(), 0);
        assertEquals(contentBounds.getHeight(), graphicBounds.getHeight(), 0);
    }

    @Test public void graphic_emptyText_CENTER() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        label.setGraphic(graphic);
        label.setText("");
        label.setContentDisplay(ContentDisplay.CENTER);
        label.layout();
        
        Bounds contentBounds = getContentBounds();
        Bounds graphicBounds = getNormalizedBounds(contentBounds, graphic);
        assertContentAreaPositionedCorrectly(contentBounds);
        // Graphic alone makes up the content bounds, so contentBounds and
        // graphic bounds should have the same width and height
        assertEquals(contentBounds.getWidth(), graphicBounds.getWidth(), 0);
        assertEquals(contentBounds.getHeight(), graphicBounds.getHeight(), 0);
    }

    @Test public void graphic_Text_CENTER() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        label.setGraphic(graphic);
        label.setText("Falcon");
        label.setFont(new Font("Amble Condensed", 12));
        label.setContentDisplay(ContentDisplay.CENTER);
        label.layout();
        
        // There is both a graphic & text node in this case. So we need to
        // compare their positions. Since this is CENTER, the graphic and
        // text should overlap each other and be directly centered
        Bounds contentBounds = getContentBounds();
        Bounds graphicBounds = getNormalizedBounds(contentBounds, graphic);
        Bounds textBounds = getNormalizedBounds(contentBounds, text);
        assertContentAreaPositionedCorrectly(contentBounds);
        // And they should both be centered vertically
        assertCenteredVertically(contentBounds, graphicBounds);
        assertCenteredVertically(contentBounds, textBounds);
        assertCenteredHorizontally(contentBounds, graphicBounds);
        assertCenteredHorizontally(contentBounds, textBounds);
    }

    @Test public void unmanagedGraphic_nullText_CENTER() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        graphic.setManaged(false);
        label.setGraphic(graphic);
        label.setText(null);
        label.setContentDisplay(ContentDisplay.CENTER);
        label.layout();
        
        assertNull(getContentBounds());
    }

    @Test public void unmanagedGraphic_emptyText_CENTER() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        graphic.setManaged(false);
        label.setGraphic(graphic);
        label.setText("");
        label.setContentDisplay(ContentDisplay.CENTER);
        label.layout();
        
        assertNull(getContentBounds());
    }

    @Test public void unmanagedGraphic_Text_CENTER() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        graphic.setManaged(false);
        label.setGraphic(graphic);
        label.setText("Falcon");
        label.setContentDisplay(ContentDisplay.CENTER);
        label.layout();
        
        Bounds contentBounds = getContentBounds();
        Bounds textBounds = getNormalizedBounds(contentBounds, text);
        assertContentAreaPositionedCorrectly(contentBounds);
        // Text alone makes up the content bounds, so contentBounds and
        // text bounds should have the same width and height
        assertEquals(contentBounds.getWidth(), textBounds.getWidth(), 0);
        assertEquals(contentBounds.getHeight(), textBounds.getHeight(), 0);
    }

    @Test public void noGraphic_nullText_CENTER() {
        label.setText(null);
        label.setContentDisplay(ContentDisplay.CENTER);
        label.layout();
        
        assertNull(getContentBounds());
    }

    @Test public void noGraphic_emptyText_CENTER() {
        label.setText("");
        label.setContentDisplay(ContentDisplay.CENTER);
        label.layout();
        
        assertNull(getContentBounds());
    }

    @Test public void noGraphic_Text_CENTER() {
        label.setText("Falcon");
        label.setContentDisplay(ContentDisplay.CENTER);
        label.layout();
        
        Bounds contentBounds = getContentBounds();
        Bounds textBounds = getNormalizedBounds(contentBounds, text);
        assertContentAreaPositionedCorrectly(contentBounds);
        // Text alone makes up the content bounds, so contentBounds and
        // text bounds should have the same width and height
        assertEquals(contentBounds.getWidth(), textBounds.getWidth(), 0);
        assertEquals(contentBounds.getHeight(), textBounds.getHeight(), 0);
    }

    /** */
    @Test public void graphic_nullText_GRAPHIC_ONLY() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        label.setGraphic(graphic);
        label.setText(null);
        label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        label.layout();
        
        Bounds contentBounds = getContentBounds();
        Bounds graphicBounds = getNormalizedBounds(contentBounds, graphic);
        assertContentAreaPositionedCorrectly(contentBounds);
        // Graphic alone makes up the content bounds, so contentBounds and
        // graphic bounds should have the same width and height
        assertEquals(contentBounds.getWidth(), graphicBounds.getWidth(), 0);
        assertEquals(contentBounds.getHeight(), graphicBounds.getHeight(), 0);
    }

    @Test public void graphic_emptyText_GRAPHIC_ONLY() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        label.setGraphic(graphic);
        label.setText("");
        label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        label.layout();
        
        Bounds contentBounds = getContentBounds();
        Bounds graphicBounds = getNormalizedBounds(contentBounds, graphic);
        assertContentAreaPositionedCorrectly(contentBounds);
        // Graphic alone makes up the content bounds, so contentBounds and
        // graphic bounds should have the same width and height
        assertEquals(contentBounds.getWidth(), graphicBounds.getWidth(), 0);
        assertEquals(contentBounds.getHeight(), graphicBounds.getHeight(), 0);
    }

    @Test public void graphic_Text_GRAPHIC_ONLY() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        label.setGraphic(graphic);
        label.setText("Falcon");
        label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        label.layout();
        
        Bounds contentBounds = getContentBounds();
        Bounds graphicBounds = getNormalizedBounds(contentBounds, graphic);
        assertContentAreaPositionedCorrectly(contentBounds);
        // Graphic alone makes up the content bounds, so contentBounds and
        // graphic bounds should have the same width and height
        assertEquals(contentBounds.getWidth(), graphicBounds.getWidth(), 0);
        assertEquals(contentBounds.getHeight(), graphicBounds.getHeight(), 0);
    }

    @Test public void unmanagedGraphic_nullText_GRAPHIC_ONLY() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        graphic.setManaged(false);
        label.setGraphic(graphic);
        label.setText(null);
        label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        label.layout();
        
        assertNull(getContentBounds());
    }

    @Test public void unmanagedGraphic_emptyText_GRAPHIC_ONLY() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        graphic.setManaged(false);
        label.setGraphic(graphic);
        label.setText("");
        label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        label.layout();
        
        assertNull(getContentBounds());
    }

    @Test public void unmanagedGraphic_Text_GRAPHIC_ONLY() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        graphic.setManaged(false);
        label.setGraphic(graphic);
        label.setText("Falcon");
        label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        label.layout();
        
        assertNull(getContentBounds());
    }

    @Test public void noGraphic_nullText_GRAPHIC_ONLY() {
        label.setText(null);
        label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        label.layout();
        
        assertNull(getContentBounds());
    }

    @Test public void noGraphic_emptyText_GRAPHIC_ONLY() {
        label.setText("");
        label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        label.layout();
        
        assertNull(getContentBounds());
    }

    @Test public void noGraphic_Text_GRAPHIC_ONLY() {
        label.setText("Falcon");
        label.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        label.layout();
        
        assertNull(getContentBounds());
    }

    /** */
    @Test public void graphic_nullText_TEXT_ONLY() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        label.setGraphic(graphic);
        label.setText(null);
        label.setContentDisplay(ContentDisplay.TEXT_ONLY);
        label.layout();
        
        assertNull(getContentBounds());
    }

    @Test public void graphic_emptyText_TEXT_ONLY() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        label.setGraphic(graphic);
        label.setText("");
        label.setContentDisplay(ContentDisplay.TEXT_ONLY);
        label.layout();
        
        assertNull(getContentBounds());
    }

    @Test public void graphic_Text_TEXT_ONLY() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        label.setGraphic(graphic);
        label.setText("Falcon");
        label.setContentDisplay(ContentDisplay.TEXT_ONLY);
        label.layout();
        
        Bounds contentBounds = getContentBounds();
        Bounds textBounds = getNormalizedBounds(contentBounds, text);
        assertContentAreaPositionedCorrectly(contentBounds);
        // Text alone makes up the content bounds, so contentBounds and
        // text bounds should have the same width and height
        assertEquals(contentBounds.getWidth(), textBounds.getWidth(), 0);
        assertEquals(contentBounds.getHeight(), textBounds.getHeight(), 0);
    }

    @Test public void unmanagedGraphic_nullText_TEXT_ONLY() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        graphic.setManaged(false);
        label.setGraphic(graphic);
        label.setText(null);
        label.setContentDisplay(ContentDisplay.TEXT_ONLY);
        label.layout();
        
        assertNull(getContentBounds());
    }

    @Test public void unmanagedGraphic_emptyText_TEXT_ONLY() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        graphic.setManaged(false);
        label.setGraphic(graphic);
        label.setText("");
        label.setContentDisplay(ContentDisplay.TEXT_ONLY);
        label.layout();
        
        assertNull(getContentBounds());
    }

    @Test public void unmanagedGraphic_Text_TEXT_ONLY() {
        Rectangle graphic = new Rectangle(GRAPHIC_WIDTH, GRAPHIC_HEIGHT);
        graphic.setManaged(false);
        label.setGraphic(graphic);
        label.setText("Falcon");
        label.setContentDisplay(ContentDisplay.TEXT_ONLY);
        label.layout();
        
        Bounds contentBounds = getContentBounds();
        Bounds textBounds = getNormalizedBounds(contentBounds, text);
        assertContentAreaPositionedCorrectly(contentBounds);
        // Text alone makes up the content bounds, so contentBounds and
        // text bounds should have the same width and height
        assertEquals(contentBounds.getWidth(), textBounds.getWidth(), 0);
        assertEquals(contentBounds.getHeight(), textBounds.getHeight(), 0);
    }

    @Test public void noGraphic_nullText_TEXT_ONLY() {
        label.setText(null);
        label.setContentDisplay(ContentDisplay.TEXT_ONLY);
        label.layout();
        
        assertNull(getContentBounds());
    }

    @Test public void noGraphic_emptyText_TEXT_ONLY() {
        label.setText("");
        label.setContentDisplay(ContentDisplay.TEXT_ONLY);
        label.layout();
        
        assertNull(getContentBounds());
    }

    @Test public void noGraphic_Text_TEXT_ONLY() {
        label.setText("Falcon");
        label.setContentDisplay(ContentDisplay.TEXT_ONLY);
        label.layout();
        
        Bounds contentBounds = getContentBounds();
        Bounds textBounds = getNormalizedBounds(contentBounds, text);
        assertContentAreaPositionedCorrectly(contentBounds);
        // Text alone makes up the content bounds, so contentBounds and
        // text bounds should have the same width and height
        assertEquals(contentBounds.getWidth(), textBounds.getWidth(), 0);
        assertEquals(contentBounds.getHeight(), textBounds.getHeight(), 0);
    }
}
