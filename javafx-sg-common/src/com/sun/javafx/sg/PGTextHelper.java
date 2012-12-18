/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.sg;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;

import com.sun.javafx.sg.PGShape.StrokeLineCap;
import com.sun.javafx.sg.PGShape.StrokeLineJoin;
import com.sun.javafx.sg.PGShape.StrokeType;

public interface PGTextHelper {
    /* These are the methods that set state from the Text node.
     * They are all safe to call on the FX thread as they don't
     * update the peer PGText node except in the sync. They may
     * be called during that sync. or earlier if a query method
     * is called which requires that the helper report bounds etc.
     */
    public void setLocation(float x, float y);
    public void setText(String text);
    public void setFont(Object font);
    public void setTextBoundsType(int textBoundsType);
    public void setTextOrigin(int textOrigin);
    public void setWrappingWidth(float width);
    public void setUnderline(boolean underline);
    public void setStrikethrough(boolean strikethrough);
    public void setTextAlignment(int alignment);
    public void setFontSmoothingType(int fontSmoothingTyoe);

    // Rendering state info needed for bounds calculations
    public void setCumulativeTransform(BaseTransform tx);
    public void setMode(PGShape.Mode mode);
    public void setStroke(boolean doStroke);
    public void setStrokeParameters(StrokeType strokeType,
                                    float[] strokeDashArray,
                                    float strokeDashOffset,
                                    StrokeLineCap lineCap,
                                    StrokeLineJoin lineJoin,
                                    float strokeMiterLimit,
                                    float strokeWidth);

    public void setLogicalSelection(int start, int end);
    public void setSelectionPaint(Object strokePaint, Object fillPaint);

    /* These are the methods that report back to the Text node the
     * state of the node without affecting the peer.
     */
    public BaseBounds computeContentBounds(BaseBounds bounds,
                                           BaseTransform tx);
    public BaseBounds computeLayoutBounds(BaseBounds bounds);
    public Object getCaretShape(int charIndex, boolean isLeading);
    public Object getSelectionShape();
    public Object getRangeShape(int start, int end);
    public Object getUnderlineShape(int start, int end);
    public Object getShape();
    public Object getHitInfo(float localX, float localY);
    public boolean computeContains(float localX, float localY);
}
