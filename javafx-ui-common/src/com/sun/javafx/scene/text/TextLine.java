/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.scene.text;

import com.sun.javafx.geom.RectBounds;

public interface TextLine {
    /**
     * Returns the list of GlyphList in the line. The list is visually orderded.
     */
    public GlyphList[] getRuns();

    /**
     * Returns metrics information about the line as follow:
     * 
     * bounds().getWidth() - the width of the line.
     * The width for the line is sum of all run's width in the line, it is not 
     * affect by any wrapping width but it will include any changes caused by
     * justification.
     * 
     * bounds().getHeight() - the height of the line.
     * The height of the line is sum of the max ascent, max descent, and 
     * max line gap of all the fonts in the line.
     * 
     * bounds.().getMinY() - the ascent of the line (negative).
     * The ascent of the line is the max ascent of all fonts in the line.
     * 
     * bounds().getMinX() - the x origin of the line (relative to the layout).
     * The x origin is defined by TextAlignment of the text layout, always zero 
     * for left-aligned text.
     */
    public RectBounds getBounds();

    /**
     * Returns the left side bearing of the line (negative).
     */
    public float getLeftSideBearing();

    /**
     * Returns the right side bearing of the line (positive).
     */
    public float getRightSideBearing();
}
