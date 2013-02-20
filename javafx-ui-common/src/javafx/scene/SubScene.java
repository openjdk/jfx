/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.jmx.MXNodeAlgorithm;
import com.sun.javafx.jmx.MXNodeAlgorithmContext;
import com.sun.javafx.sg.PGNode;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

/**
 * The {@code SubScene} class is the container for content in a scene graph.
 * 
 * @since JavaFX 8
 */
public class SubScene extends Node {

    /**
     * Creates a SubScene for a specific root Node with a specific size.
     *
     * @param root The root node of the scene graph
     * @param width The width of the scene
     * @param height The height of the scene
     *
     * @throws IllegalStateException if this constructor is called on a thread
     * other than the JavaFX Application Thread.
     * @throws NullPointerException if root is null
     */
    public SubScene(Parent root, double width, double height) {
        throw new UnsupportedOperationException("*** 3D Work is still in progress ... ***");
    }

    /**
     * Constructs a SubScene consisting of a root, with a dimension of width and
     * height, specifies whether a depth buffer is created for this scene and 
     * specifies whether scene anti-aliasing is requested.
     *
     * @param root The root node of the scene graph
     * @param width The width of the scene
     * @param height The height of the scene
     * @param depthBuffer The depth buffer flag
     * @param antiAliasing The sub-scene anti-aliasing flag
     * <p>
     * The depthBuffer and antiAliasing flags are conditional feature and the default 
     * value for both are false. See
     * {@link javafx.application.ConditionalFeature#SCENE3D ConditionalFeature.SCENE3D}
     * for more information.
     *
     * @throws IllegalStateException if this constructor is called on a thread
     * other than the JavaFX Application Thread.
     * @throws NullPointerException if root is null
     *
     * @see javafx.scene.Node#setDepthTest(DepthTest)
     */
    public SubScene(Parent root, double width, double height,
            boolean depthBuffer, boolean antiAliasing) {
        throw new UnsupportedOperationException("*** 3D Work is still in progress ... ***");
    }

    /**
     * Return true if this {@code SubScene} is anti-aliased otherwise false.
     */
    public boolean isAntiAliasing() {
        throw new UnsupportedOperationException("Unsupported --- *** isAntiAliasing method ***");        
    }

    /**
     * Defines the root {@code Node} of the SubScene scene graph.
     * If a {@code Group} is used as the root, the
     * contents of the scene graph will be clipped by the SubScene's width and height.
     * 
     * SubScene doesn't accept null root.
     * 
     */
    private ObjectProperty<Parent> root;

    public final void setRoot(Parent value) {
        rootProperty().set(value);
    }

    public final Parent getRoot() {
        return root == null ? null : root.get();
    }

    public final ObjectProperty<Parent> rootProperty() {
        if (root == null) {
            root = new ObjectPropertyBase<Parent>() {

                @Override
                protected void invalidated() {
                }

                @Override
                public Object getBean() {
                    return SubScene.this;
                }

                @Override
                public String getName() {
                    return "root";
                }
            };
        }
        return root;
    }

    /**
     * Specifies the type of camera use for rendering this {@code SubScene}.
     * If {@code camera} is null, a parallel camera is used for rendering.
     * <p>
     * Note: this is a conditional feature. See
     * {@link javafx.application.ConditionalFeature#SCENE3D ConditionalFeature.SCENE3D}
     * for more information.
     *
     * @defaultValue null
     */
    private ObjectProperty<Camera> camera;

    public final void setCamera(Camera value) {
        cameraProperty().set(value);
    }

    public final Camera getCamera() {
        return camera == null ? null : camera.get();
    }

    public final ObjectProperty<Camera> cameraProperty() {
        if (camera == null) {
            camera = new ObjectPropertyBase<Camera>() {

                @Override
                protected void invalidated() {
                }

                @Override
                public Object getBean() {
                    return SubScene.this;
                }

                @Override
                public String getName() {
                    return "camera";
                }
            };
        }
        return camera;
    }

    /**
     * Defines the width of this {@code SubScene}
     * 
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
                }

                @Override
                public Object getBean() {
                    return SubScene.this;
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
     * Defines the height of this {@code SubScene}
     *
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
                }

                @Override
                public Object getBean() {
                    return SubScene.this;
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
     * Defines the background fill of this {@code SubScene}. Both a {@code null}
     * value meaning paint no background and a {@link javafx.scene.paint.Paint}
     * with transparency are supported, but what is painted behind it will
     * depend on the platform.  The default value is COLOR.TRANSPARENT.
     *
     * @defaultValue WHITE
     */
    private ObjectProperty<Paint> fill;

    public final void setFill(Paint value) {
        fillProperty().set(value);
    }

    public final Paint getFill() {
        return fill == null ? Color.TRANSPARENT : fill.get();
    }

    public final ObjectProperty<Paint> fillProperty() {
        if (fill == null) {
            fill = new ObjectPropertyBase<Paint>(Color.TRANSPARENT) {

                @Override
                protected void invalidated() {
                }

                @Override
                public Object getBean() {
                    return SubScene.this;
                }

                @Override
                public String getName() {
                    return "fill";
                }
            };
        }
        return fill;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated    @Override
    protected PGNode impl_createPGNode() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated    @Override
    public BaseBounds impl_computeGeomBounds(BaseBounds bounds, BaseTransform tx) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated    @Override
    protected boolean impl_computeContains(double localX, double localY) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated    @Override
    public Object impl_processMXNode(MXNodeAlgorithm alg, MXNodeAlgorithmContext ctx) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
