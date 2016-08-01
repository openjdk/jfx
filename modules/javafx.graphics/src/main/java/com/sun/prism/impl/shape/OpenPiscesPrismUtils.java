/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.impl.shape;


import com.sun.javafx.geom.PathConsumer2D;
import com.sun.javafx.geom.PathIterator;
import com.sun.javafx.geom.Path2D;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.Shape;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.openpisces.Dasher;
import com.sun.openpisces.Renderer;
import com.sun.openpisces.Stroker;
import com.sun.openpisces.TransformingPathConsumer2D;
import com.sun.prism.BasicStroke;

public class OpenPiscesPrismUtils {
    private static final Renderer savedAARenderer = new Renderer(3, 3);
    private static final Renderer savedRenderer = new Renderer(0, 0);
    private static final Stroker savedStroker = new Stroker(savedRenderer);
    private static final Dasher savedDasher = new Dasher(savedStroker);

    private static TransformingPathConsumer2D.FilterSet transformer =
        new TransformingPathConsumer2D.FilterSet();

    private static PathConsumer2D initRenderer(BasicStroke stroke,
                                               BaseTransform tx,
                                               Rectangle clip,
                                               int pirule,
                                               Renderer renderer)
    {
        int oprule = (stroke == null && pirule == PathIterator.WIND_EVEN_ODD) ?
            Renderer.WIND_EVEN_ODD : Renderer.WIND_NON_ZERO;
        renderer.reset(clip.x, clip.y, clip.width, clip.height, oprule);
        PathConsumer2D ret = transformer.getConsumer(renderer, tx);
        if (stroke != null) {
            savedStroker.reset(stroke.getLineWidth(), stroke.getEndCap(),
                               stroke.getLineJoin(), stroke.getMiterLimit());
            savedStroker.setConsumer(ret);
            ret = savedStroker;
            float dashes[] = stroke.getDashArray();
            if (dashes != null) {
                savedDasher.reset(dashes, stroke.getDashPhase());
                ret = savedDasher;
            }
        }
        return ret;
    }

    public static void feedConsumer(PathIterator pi, PathConsumer2D pc) {
        float[] coords = new float[6];
        while (!pi.isDone()) {
            int type = pi.currentSegment(coords);
            switch (type) {
                case PathIterator.SEG_MOVETO:
                    pc.moveTo(coords[0], coords[1]);
                    break;
                case PathIterator.SEG_LINETO:
                    pc.lineTo(coords[0], coords[1]);
                    break;
                case PathIterator.SEG_QUADTO:
                    pc.quadTo(coords[0], coords[1],
                              coords[2], coords[3]);
                    break;
                case PathIterator.SEG_CUBICTO:
                    pc.curveTo(coords[0], coords[1],
                               coords[2], coords[3],
                               coords[4], coords[5]);
                    break;
                case PathIterator.SEG_CLOSE:
                    pc.closePath();
                    break;
            }
            pi.next();
        }
        pc.pathDone();
    }

    public static Renderer setupRenderer(Shape shape,
                                  BasicStroke stroke,
                                  BaseTransform xform,
                                  Rectangle rclip,
                                  boolean antialiasedShape)
    {
        PathIterator pi = shape.getPathIterator(null);
        Renderer r = antialiasedShape ? savedAARenderer : savedRenderer;
        feedConsumer(pi, initRenderer(stroke, xform, rclip, pi.getWindingRule(), r));
        return r;
    }

    public static Renderer setupRenderer(Path2D p2d,
                                  BasicStroke stroke,
                                  BaseTransform xform,
                                  Rectangle rclip,
                                  boolean antialiasedShape)
    {
        Renderer r = antialiasedShape ? savedAARenderer : savedRenderer;
        PathConsumer2D pc2d = initRenderer(stroke, xform, rclip, p2d.getWindingRule(), r);

        float coords[] = p2d.getFloatCoordsNoClone();
        byte types[] = p2d.getCommandsNoClone();
        int nsegs = p2d.getNumCommands();
        int coff = 0;
        for (int i = 0; i < nsegs; i++) {
            switch (types[i]) {
                case PathIterator.SEG_MOVETO:
                    pc2d.moveTo(coords[coff+0], coords[coff+1]);
                    coff += 2;
                    break;
                case PathIterator.SEG_LINETO:
                    pc2d.lineTo(coords[coff+0], coords[coff+1]);
                    coff += 2;
                    break;
                case PathIterator.SEG_QUADTO:
                    pc2d.quadTo(coords[coff+0], coords[coff+1],
                                coords[coff+2], coords[coff+3]);
                    coff += 4;
                    break;
                case PathIterator.SEG_CUBICTO:
                    pc2d.curveTo(coords[coff+0], coords[coff+1],
                                 coords[coff+2], coords[coff+3],
                                 coords[coff+4], coords[coff+5]);
                    coff += 6;
                    break;
                case PathIterator.SEG_CLOSE:
                    pc2d.closePath();
                    break;
            }
        }
        pc2d.pathDone();
        return r;
    }
}
