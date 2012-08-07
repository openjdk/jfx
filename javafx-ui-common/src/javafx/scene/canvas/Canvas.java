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
package javafx.scene.canvas;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.jmx.MXNodeAlgorithm;
import com.sun.javafx.jmx.MXNodeAlgorithmContext;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.sg.GrowableDataBuffer;
import com.sun.javafx.sg.PGCanvas;
import com.sun.javafx.sg.PGNode;
import com.sun.javafx.tk.Toolkit;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.scene.Node;

/**
 * {@code Canvas} is an image that can be drawn on using a set of graphics 
 * commands provided by a {@code GraphicsContext}. 
 * 
 * <p>
 * A {@code Canvas} node is constructed with a width and height that specifies the size 
 * of the image into which the canvas drawing commands are rendered. All drawing 
 * operations are clipped to the bounds of that image.
 * 
 * <p>Example:</p>
 *
 * <p>
 * <pre>
import javafx.scene.*;
import javafx.scene.paint.*;
import javafx.scene.canvas.*;

Group root = new Group();
Scene s = new Scene(root, 300, 300, Color.BLACK);

final Canvas canvas = new Canvas(250,250);
GraphicsContext gc = canvas.getGraphicsContext2D();
 
gc.setFill(Color.BLUE);
gc.fillRect(75,75,100,100);
 
root.getChildren().add(canvas);
 * </pre>
 * </p>
 *
 * @since 2.2
 */
public class Canvas extends Node {
    private static final int DEFAULT_BUF_SIZE = 1024;

    private GrowableDataBuffer<Object> empty;
    private GrowableDataBuffer<Object> full;

    private GraphicsContext theContext;

    /**
     * Creates an empty instance of Canvas.
     */
    public Canvas() {
        this(0, 0);
    }

    /**
     * Creates a new instance of Canvas with the given size.
     * 
     * @param width width of the canvas
     * @param height height of the canvas
     */
    public Canvas(double width, double height) {
        setWidth(width);
        setHeight(height);
    }

    GrowableDataBuffer<Object> getBuffer() {
        impl_markDirty(DirtyBits.NODE_CONTENTS);
        if (empty == null) {
            empty = new GrowableDataBuffer<Object>(DEFAULT_BUF_SIZE);
        }
        return empty;
    }

    /**
     * returns the {@code GraphicsContext} associated with this {@code Canvas}.
     */
    public GraphicsContext getGraphicsContext2D() {
        if (theContext == null) {
            theContext = new GraphicsContext(this);
        }
        return theContext;
    }

    /**
     * Defines the width of the canvas.
     *
     * @profile common
     * @defaultvalue 0.0
     */
    private DoubleProperty width;

    public final void setWidth(double value) {
        widthProperty().set(value);
    }

    public final double getWidth() {
        return width == null ? 0.0 : width.get();
    }

    public final DoubleProperty widthProperty() {
        if (width == null) {
            width = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    impl_markDirty(DirtyBits.NODE_GEOMETRY);
                    impl_geomChanged();
                }

                @Override
                public Object getBean() {
                    return Canvas.this;
                }

                @Override
                public String getName() {
                    return "width";
                }
            };
        }
        return width;
    }

    /**
     * Defines the height of the canvas.
     *
     * @profile common
     * @defaultvalue 0.0
     */
    private DoubleProperty height;

    public final void setHeight(double value) {
        heightProperty().set(value);
    }

    public final double getHeight() {
        return height == null ? 0.0 : height.get();
    }

    public final DoubleProperty heightProperty() {
        if (height == null) {
            height = new DoublePropertyBase() {

                @Override
                public void invalidated() {
                    impl_markDirty(DirtyBits.NODE_GEOMETRY);
                    impl_geomChanged();
                }

                @Override
                public Object getBean() {
                    return Canvas.this;
                }

                @Override
                public String getName() {
                    return "height";
                }
            };
        }
        return height;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected PGNode impl_createPGNode() {
        return Toolkit.getToolkit().createPGCanvas();
    }

    PGCanvas getPGCanvas() {
        return (PGCanvas) impl_getPGNode();
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public void impl_updatePG() {
        super.impl_updatePG();
        if (impl_isDirty(DirtyBits.NODE_GEOMETRY)) {
            PGCanvas peer = getPGCanvas();
            peer.updateBounds((float)getWidth(),
                              (float)getHeight());
        }
        if (impl_isDirty(DirtyBits.NODE_CONTENTS)) {
            PGCanvas peer = getPGCanvas();
            if (empty != null && empty.position() > 0) {
                 peer.updateRendering(empty);
                 if (full != null) {
                    full.resetForWrite();
                 }
                 GrowableDataBuffer tmp = empty;
                 empty = full;
                 full = tmp;
            }
        }
    }
    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    protected boolean impl_computeContains(double localX, double localY) {
        double w = getWidth();
        double h = getHeight();
        return (w > 0 && h > 0 &&
                localX >= 0 && localY >= 0 &&
                localX <  w && localY <  h);
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public BaseBounds impl_computeGeomBounds(BaseBounds bounds, BaseTransform tx) {
        bounds = new RectBounds(0f, 0f, (float) getWidth(), (float) getHeight());  
        bounds = tx.transform(bounds, bounds);
        return bounds;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override
    public Object impl_processMXNode(MXNodeAlgorithm alg,
                                     MXNodeAlgorithmContext ctx) {
        return alg.processLeafNode(this, ctx);
    }
}
