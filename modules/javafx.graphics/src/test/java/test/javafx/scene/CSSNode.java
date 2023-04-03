/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene;

import javafx.css.converter.PaintConverter;
import javafx.css.converter.SizeConverter;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.sg.prism.NGGroup;
import com.sun.javafx.sg.prism.NGNode;
import javafx.beans.property.*;
import javafx.css.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.Node;
import test.com.sun.javafx.scene.CSSNodeHelper;

public  class CSSNode extends Node {
    static {
        CSSNodeHelper.setCSSNodeAccessor(new CSSNodeHelper.CSSNodeAccessor() {
            @Override
            public NGNode doCreatePeer(Node node) {
                return ((CSSNode) node).doCreatePeer();
            }

            @Override
            public BaseBounds doComputeGeomBounds(Node node,
                    BaseBounds bounds, BaseTransform tx) {
                return ((CSSNode) node).doComputeGeomBounds(bounds, tx);
            }

            @Override
            public boolean doComputeContains(Node node, double localX, double localY) {
                return ((CSSNode) node).doComputeContains(localX, localY);
            }
        });
    }

    {
        // To initialize the class helper at the begining each constructor of this class
        CSSNodeHelper.initHelper(this);
    }

    public CSSNode() {
        setContentSize(100);
    }

    /**
     * This variable can be set from CSS and represents the fill
     */
    private ObjectProperty<Paint> fill;
    public ObjectProperty<Paint> fillProperty() {
        if (fill == null) {
            fill = new StyleableObjectProperty<Paint>(Color.BLACK) {

                @Override
                public Object getBean() {
                    return CSSNode.this;
                }

                @Override
                public String getName() {
                    return "fill";
                }

                @Override
                public CssMetaData getCssMetaData() {
                    return StyleableProperties.FILL;
                }

            };
        }
        return fill;
    }
    public void setFill(Paint paint) {
        fillProperty().set(paint);
    }
    public Paint getFill() {
        return (fill == null ? Color.BLACK : fill.get());
    }

    /**
     * This variable can be set from CSS and represents the stroke fill
     */
    private ObjectProperty<Paint> stroke;
    public ObjectProperty<Paint> strokeProperty() {
        if (stroke == null) {
            stroke = new StyleableObjectProperty<Paint>(Color.BLACK) {

                @Override
                public Object getBean() {
                    return CSSNode.this;
                }

                @Override
                public String getName() {
                    return "stroke";
                }

                @Override
                public CssMetaData getCssMetaData() {
                    return StyleableProperties.STROKE;
                }

            };
        }
        return stroke;
    }
    public void setStroke(Paint paint) {
        strokeProperty().set(paint);
    }
    public Paint getStroke() {
        return (stroke == null ? Color.BLACK : stroke.get());
    }

    /**
     * This variable can be set from CSS and is a simple number. For testing,
     * this can be font-based, absolute, or percentage based. The CSSNode has
     * a contentSize:Number variable which is used when padding is based on
     * a percentage
     */
    private FloatProperty padding;

    public final void setPadding(float value) {
        paddingProperty().set(value);
    }

    public final float getPadding() {
        return padding == null ? 0.0F : padding.get();
    }

    public FloatProperty paddingProperty() {
        if (padding == null) {
            padding = new StyleableFloatProperty() {

                @Override
                protected void invalidated() {
                    NodeHelper.geomChanged(CSSNode.this);
                }

                @Override
                public Object getBean() {
                    return CSSNode.this;
                }

                @Override
                public String getName() {
                    return "padding";
                }

                @Override
                public CssMetaData getCssMetaData() {
                    return StyleableProperties.PADDING;
                }
            };
        }
        return padding;
    }

    /**
     * Used only when padding is specified as a percentage, as it is a
     * percentage of the content size.
     */
    private FloatProperty contentSize;

    public final void setContentSize(float value) {
        contentSizeProperty().set(value);
    }

    public final float getContentSize() {
        return contentSize == null ? 0.0F : contentSize.get();
    }

    public FloatProperty contentSizeProperty() {
        if (contentSize == null) {
            contentSize = new SimpleFloatProperty() {

                @Override
                protected void invalidated() {
                    NodeHelper.geomChanged(CSSNode.this);
                }
            };
        }
        return contentSize;
    }

    /**
     * A pseudoclass state for this Node. It cannot be styled, but can
     * be used as a pseudoclass
     */
    private PseudoClass SPECIAL_PSEUDO_CLASS = PseudoClass.getPseudoClass("special");
    private BooleanProperty special;
    public final void setSpecial(boolean value) {
        specialProperty().set(value);
    }

    public final boolean isSpecial() {
        return special == null ? false : special.get();
    }

    public BooleanProperty specialProperty() {
        if (special == null) {
            special = new SimpleBooleanProperty() {

                @Override
                protected void invalidated() {
                    pseudoClassStateChanged(SPECIAL_PSEUDO_CLASS, get());
                }
            };
        }
        return special;
    }

    /*
     * These vars are used solely for the sake of testing.
     */

    public boolean reapply = false;
    public boolean processCalled = false;
    public boolean applyCalled = false;

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private BaseBounds doComputeGeomBounds(BaseBounds bounds, BaseTransform tx) {
        if (bounds != null) {
            bounds = bounds.deriveWithNewBounds(0, 0, 0,
                    getContentSize() + getPadding() + getPadding(), getContentSize() + getPadding() + getPadding(), 0);
        }
        return bounds;
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private boolean doComputeContains(double localX, double localY) {
        // TODO: Missing code.
        return false;
    }

    private NGNode doCreatePeer() {
        return new NGGroup();
    }

    public static class StyleableProperties {

        public static final CssMetaData<CSSNode,Paint> FILL =
            new CssMetaData<>("fill", PaintConverter.getInstance()) {

            @Override
            public boolean isSettable(CSSNode n) {
                return n.fill == null || !n.fill.isBound();
            }

            @Override
            public StyleableProperty<Paint> getStyleableProperty(CSSNode n) {
                return (StyleableProperty<Paint>)n.fillProperty();
            }
        };

        public static final CssMetaData<CSSNode,Paint> STROKE =
            new CssMetaData<>("stroke", PaintConverter.getInstance()) {

            @Override
            public boolean isSettable(CSSNode n) {
                return n.stroke == null || !n.stroke.isBound();
            }

            @Override
            public StyleableProperty<Paint> getStyleableProperty(CSSNode n) {
                return (StyleableProperty<Paint>)n.strokeProperty();
            }
        };

        public static final CssMetaData<CSSNode,Number> PADDING =
            new CssMetaData<>("padding", SizeConverter.getInstance()) {

            @Override
            public boolean isSettable(CSSNode n) {
                return n.padding == null || !n.padding.isBound();
            }

            @Override
            public StyleableProperty<Number> getStyleableProperty(CSSNode n) {
                return (StyleableProperty<Number>)n.paddingProperty();
            }
        };

        private static List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables =
                new ArrayList<>(Node.getClassCssMetaData());
            styleables.add(FILL);
            styleables.add(STROKE);
            styleables.add(PADDING);

        }

    }

    /**
     * @return The CssMetaData associated with this class, which may include the
     * CssMetaData of its superclasses.
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * {@inheritDoc}
     *
     */


    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }
}
