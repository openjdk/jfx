/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.webkit.prism;

import com.sun.prism.BasicStroke;
import com.sun.prism.Graphics;
import com.sun.prism.paint.Color;
import com.sun.prism.paint.Paint;
import com.sun.webkit.graphics.WCStroke;

final class WCStrokeImpl extends WCStroke<Paint, BasicStroke> {

    private BasicStroke stroke;

    protected void invalidate() {
        this.stroke = null;
    }

    public BasicStroke getPlatformStroke() {
        if (this.stroke == null) {
            int style = getStyle();
            if (style != NO_STROKE) {
                float width = getThickness();
                float[] dash = getDashSizes();
                if (dash == null) {
                    switch (style) {
                        case DOTTED_STROKE:
                            dash = new float[] { width, width };
                            break;
                        case DASHED_STROKE:
                            dash = new float[] { 3 * width, 3 * width };
                            break;
                    }
                }
                this.stroke = new BasicStroke(width, getLineCap(), getLineJoin(), getMiterLimit(),
                                              dash, getDashOffset());
            }
        }
        return this.stroke;
    }

    boolean apply(Graphics graphics) {
        Paint paint = getPaint();
        if (paint == null) {
            return false;
        }
        BasicStroke stroke = getPlatformStroke();
        if (stroke == null) {
            return false;
        }
        graphics.setPaint(paint);
        graphics.setStroke(stroke);
        return true;
    }
}
