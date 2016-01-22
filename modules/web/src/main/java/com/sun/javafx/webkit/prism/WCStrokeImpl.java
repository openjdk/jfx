/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.webkit.prism;

import com.sun.prism.BasicStroke;
import com.sun.prism.Graphics;
import com.sun.prism.paint.Paint;
import com.sun.webkit.graphics.WCStroke;

final class WCStrokeImpl extends WCStroke<Paint, BasicStroke> {

    private BasicStroke stroke;

    public WCStrokeImpl() {
    }

    public WCStrokeImpl(float width, int cap, int join, float miterLimit,
                        float[] dash, float dashOffset)
    {
        setThickness(width);
        setLineCap(cap);
        setLineJoin(join);
        setMiterLimit(miterLimit);
        setDashSizes(dash);
        setDashOffset(dashOffset);
    }

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

    boolean isApplicable() {
        return getPaint() != null && getPlatformStroke() != null;
    }

    boolean apply(Graphics graphics) {
        if (isApplicable()) {
            Paint _paint = getPaint();
            BasicStroke _stroke = getPlatformStroke();
            graphics.setPaint(_paint);
            graphics.setStroke(_stroke);
            return true;
        }
        return false;
    }
}
