/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Node;

import com.sun.javafx.css.Styleable;
import com.sun.javafx.css.StyleManager;
import com.sun.javafx.css.StyleableProperty;
import javafx.beans.DefaultProperty;

/**
 * A Control that provides a scrolled, clipped viewport of its contents. It
 * allows the user to scroll the content around either directly (panning) or
 * by using scroll bars. The ScrollPane allows specification of the scroll
 * bar policy, which determines when scroll bars are displayed: always, never,
 * or only when they are needed. The scroll bar policy can be specified
 * independently for the horizontal and vertical scroll bars.
 * <p>
 * The ScrollPane allows the application to set the current, minimum, and
 * maximum values for positioning the contents in the horizontal and
 * vertical directions. These values are mapped proportionally onto the
 * {@link javafx.scene.Node#layoutBoundsProperty layoutBounds} of the contained node.
 * <p>
 * ScrollPane layout calculations are based on the layoutBounds rather than
 * the boundsInParent (visual bounds) of the scroll node.
 * If an application wants the scrolling to be based on the visual bounds
 * of the node (for scaled content etc.), they need to wrap the scroll
 * node in a Group.
 * <p>
 * ScrollPane sets focusTraversable to false.
 * </p>
 *
 * <p>
 * This example creates a ScrollPane, which contains a Rectangle :
 * <pre><code>
 * import javafx.scene.control.ScrollPane;
 * import javafx.scene.shape.Rectangle;
 * 
 * Rectangle rect = new Rectangle(200, 200, Color.RED);
 * ScrollPane s1 = new ScrollPane();
 * s1.setPrefSize(120, 120);
 * s1.setContent(rect);
 * </code></pre>
 *
 * Implementation of ScrollPane According to JavaFX UI Control API Specification
 */
@DefaultProperty("content")
public class ScrollPane extends Control {

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new ScrollPane.
     */
    public ScrollPane() {
        getStyleClass().setAll(DEFAULT_STYLE_CLASS);
        setFocusTraversable(false);
    }
    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/
    /**
     * Specifies the policy for showing the horizontal scroll bar.
     */
    @Styleable(property="-fx-hbar-policy", initial="as-needed")
    private ObjectProperty<ScrollBarPolicy> hbarPolicy;
    public final void setHbarPolicy(ScrollBarPolicy value) {
        hbarPolicyProperty().set(value);
    }

    public final ScrollBarPolicy getHbarPolicy() {
        return hbarPolicy == null ? ScrollBarPolicy.AS_NEEDED : hbarPolicy.get();
    }

    public final ObjectProperty<ScrollBarPolicy> hbarPolicyProperty() {
        if (hbarPolicy == null) {
            hbarPolicy = new ObjectPropertyBase<ScrollBarPolicy>(ScrollBarPolicy.AS_NEEDED) {

                @Override
                public void invalidated() {
                    impl_cssPropertyInvalidated(StyleableProperties.HBAR_POLICY);
                }

                @Override
                public Object getBean() {
                    return ScrollPane.this;
                }

                @Override
                public String getName() {
                    return "hbarPolicy";
                }
            };
        }
        return hbarPolicy;
    }
    /**
     * Specifies the policy for showing the vertical scroll bar.
     */
    @Styleable(property="-fx-vbar-policy", initial="as-needed")
    private ObjectProperty<ScrollBarPolicy> vbarPolicy;
    public final void setVbarPolicy(ScrollBarPolicy value) {
        vbarPolicyProperty().set(value);
    }

    public final ScrollBarPolicy getVbarPolicy() {
        return vbarPolicy == null ? ScrollBarPolicy.AS_NEEDED : vbarPolicy.get();
    }

    public final ObjectProperty<ScrollBarPolicy> vbarPolicyProperty() {
        if (vbarPolicy == null) {
            vbarPolicy = new ObjectPropertyBase<ScrollBarPolicy>(ScrollBarPolicy.AS_NEEDED) {

                @Override
                public void invalidated() {
                    impl_cssPropertyInvalidated(StyleableProperties.VBAR_POLICY);
                }

                @Override
                public Object getBean() {
                    return ScrollPane.this;
                }

                @Override
                public String getName() {
                    return "vbarPolicy";
                }
            };
        }
        return vbarPolicy;
    }
    /**
     * The node used as the content of this ScrollPane.
     */
    private ObjectProperty<Node> content;

    public final void setContent(Node value) {
        contentProperty().set(value);
    }

    public final Node getContent() {
        return content == null ? null : content.get();
    }

    public final ObjectProperty<Node> contentProperty() {
        if (content == null) {
            content = new SimpleObjectProperty<Node>(this, "content");
        }
        return content;
    }
    /**
     * The current horizontal scroll position of the ScrollPane. This value
     * may be set by the application to scroll the view programatically.
     * The ScrollPane will update this value whenever the viewport is
     * scrolled or panned by the user. This value must always be within
     * the range of {@link #hminProperty hmin} to {@link #hmaxProperty hmax}. When {@link #hvalueProperty hvalue}
     * equals {@link #hminProperty hmin}, the contained node is positioned so that
     * its layoutBounds {@link javafx.geometry.Bounds#getMinX minX} is visible. When {@link #hvalueProperty hvalue}
     * equals {@link #hmaxProperty hmax}, the contained node is positioned so that its
     * layoutBounds {@link javafx.geometry.Bounds#getMaxX maxX} is visible. When {@link #hvalueProperty hvalue} is between
     * {@link #hminProperty hmin} and {@link #hmaxProperty hmax}, the contained node is positioned
     * proportionally between layoutBounds {@link javafx.geometry.Bounds#getMinX minX} and
     * layoutBounds {@link javafx.geometry.Bounds#getMaxX maxX}.
     */
    private DoubleProperty hvalue;

    public final void setHvalue(double value) {
        hvalueProperty().set(value);
    }

    public final double getHvalue() {
        return hvalue == null ? 0.0 : hvalue.get();
    }

    public final DoubleProperty hvalueProperty() {
        if (hvalue == null) {
            hvalue = new SimpleDoubleProperty(this, "hvalue");
        }
        return hvalue;
    }
    /**
     * The current vertical scroll position of the ScrollPane. This value
     * may be set by the application to scroll the view programatically.
     * The ScrollPane will update this value whenever the viewport is
     * scrolled or panned by the user. This value must always be within
     * the range of {@link #vminProperty vmin} to {@link #vmaxProperty vmax}. When {@link #vvalueProperty vvalue}
     * equals {@link #vminProperty vmin}, the contained node is positioned so that
     * its layoutBounds {@link javafx.geometry.Bounds#getMinY minY} is visible. When {@link #vvalueProperty vvalue}
     * equals {@link #vmaxProperty vmax}, the contained node is positioned so that its
     * layoutBounds {@link javafx.geometry.Bounds#getMaxY maxY} is visible. When {@link #vvalueProperty vvalue} is between
     * {@link #vminProperty vmin} and {@link #vmaxProperty vmax}, the contained node is positioned
     * proportionally between layoutBounds {@link javafx.geometry.Bounds#getMinY minY} and
     * layoutBounds {@link javafx.geometry.Bounds#getMaxY maxY}.
     */
    private DoubleProperty vvalue;

    public final void setVvalue(double value) {
        vvalueProperty().set(value);
    }

    public final double getVvalue() {
        return vvalue == null ? 0.0 : vvalue.get();
    }

    public final DoubleProperty vvalueProperty() {
        if (vvalue == null) {
            vvalue = new SimpleDoubleProperty(this, "vvalue");
        }
        return vvalue;
    }
    /**
     * The minimum allowable {@link #hvalueProperty hvalue} for this ScrollPane.
     */
    private DoubleProperty hmin;

    public final void setHmin(double value) {
        hminProperty().set(value);
    }

    public final double getHmin() {
        return hmin == null ? 0.0F : hmin.get();
    }

    public final DoubleProperty hminProperty() {
        if (hmin == null) {
            hmin = new SimpleDoubleProperty(this, "hmin", 0.0);
        }
        return hmin;
    }
    /**
     * The minimum allowable {@link #hvalueProperty vvalue} for this ScrollPane.
     */
    private DoubleProperty vmin;

    public final void setVmin(double value) {
        vminProperty().set(value);
    }

    public final double getVmin() {
        return vmin == null ? 0.0F : vmin.get();
    }

    public final DoubleProperty vminProperty() {
        if (vmin == null) {
            vmin = new SimpleDoubleProperty(this, "vmin", 0.0);
        }
        return vmin;
    }
    /**
     * The maximum allowable {@link #hvalueProperty hvalue} for this ScrollPane.
     */
    private DoubleProperty hmax;

    public final void setHmax(double value) {
        hmaxProperty().set(value);
    }

    public final double getHmax() {
        return hmax == null ? 1.0F : hmax.get();
    }

    public final DoubleProperty hmaxProperty() {
        if (hmax == null) {
            hmax = new SimpleDoubleProperty(this, "hmax", 1.0);
        }
        return hmax;
    }
    /**
     * The maximum allowable {@link #hvalueProperty vvalue} for this ScrollPane.
     */
    private DoubleProperty vmax;

    public final void setVmax(double value) {
        vmaxProperty().set(value);
    }

    public final double getVmax() {
        return vmax == null ? 1.0F : vmax.get();
    }

    public final DoubleProperty vmaxProperty() {
        if (vmax == null) {
            vmax = new SimpleDoubleProperty(this, "vmax", 1.0);
        }
        return vmax;
    }
    /**
     * If true and if the contained node is a Resizable, then the node will be
     * kept resized to match the width of the ScrollPane's viewport. If the
     * contained node is not a Resizable, this value is ignored.
     */
    @Styleable(property="-fx-fit-to-width", initial="false")
    private BooleanProperty fitToWidth;
    public final void setFitToWidth(boolean value) {
        fitToWidthProperty().set(value);
    }
    public final boolean isFitToWidth() {
        return fitToWidth == null ? false : fitToWidth.get();
    }
    public final BooleanProperty fitToWidthProperty() {
        if (fitToWidth == null) {
            fitToWidth = new BooleanPropertyBase() {
                @Override public void invalidated() {
                    impl_cssPropertyInvalidated(StyleableProperties.FIT_TO_WIDTH);
                    impl_pseudoClassStateChanged(PSEUDO_CLASS_FIT_TO_WIDTH);
                }

                @Override
                public Object getBean() {
                    return ScrollPane.this;
                }

                @Override
                public String getName() {
                    return "fitToWidth";
                }
            };
        }
        return fitToWidth;
    }
    /**
     * If true and if the contained node is a Resizable, then the node will be
     * kept resized to match the height of the ScrollPane's viewport. If the
     * contained node is not a Resizable, this value is ignored.
     */
    @Styleable(property="-fx-fit-to-height", initial="false")
    private BooleanProperty fitToHeight;
    public final void setFitToHeight(boolean value) {
        fitToHeightProperty().set(value);
    }
    public final boolean isFitToHeight() {
        return fitToHeight == null ? false : fitToHeight.get();
    }
    public final BooleanProperty fitToHeightProperty() {
        if (fitToHeight == null) {
            fitToHeight = new BooleanPropertyBase() {
                @Override public void invalidated() {
                    impl_cssPropertyInvalidated(StyleableProperties.FIT_TO_HEIGHT);
                    impl_pseudoClassStateChanged(PSEUDO_CLASS_FIT_TO_HEIGHT);
                }

                @Override
                public Object getBean() {
                    return ScrollPane.this;
                }

                @Override
                public String getName() {
                    return "fitToHeight";
                }
            };
        }
        return fitToHeight;
    }
    /**
     * Specifies whether the user should be able to pan the viewport by using
     * the mouse. If mouse events reach the ScrollPane (that is, if mouse
     * events are not blocked by the contained node or one of its children)
     * then {@link #pannableProperty pannable} is consulted to determine if the events should be
     * used for panning.
     */
    @Styleable(property="-fx-pannable", initial="false")
    private BooleanProperty pannable;
    public final void setPannable(boolean value) {
        pannableProperty().set(value);
    }
    public final boolean isPannable() {
        return pannable == null ? false : pannable.get();
    }
    public final BooleanProperty pannableProperty() {
        if (pannable == null) {
            pannable = new BooleanPropertyBase() {
                @Override public void invalidated() {
                    impl_cssPropertyInvalidated(StyleableProperties.PANNABLE);
                    impl_pseudoClassStateChanged(PSEUDO_CLASS_PANNABLE);
                }

                @Override
                public Object getBean() {
                    return ScrollPane.this;
                }

                @Override
                public String getName() {
                    return "pannable";
                }
            };
        }
        return pannable;
    }


    /**
     * Specify the perferred width of the ScrollPane Viewport.
     * This is the width that will be available to the content node.
     * The overall width of the ScrollPane is the ViewportWidth + padding
     */
    private DoubleProperty prefViewportWidth;

    public final void setPrefViewportWidth(double value) {
        prefViewportWidthProperty().set(value);
    }

    public final double getPrefViewportWidth() {
        return prefViewportWidth == null ? 0.0F : prefViewportWidth.get();
    }

    public final DoubleProperty prefViewportWidthProperty() {
        if (prefViewportWidth == null) {
            prefViewportWidth = new SimpleDoubleProperty(this, "prefViewportWidth");
        }
        return prefViewportWidth;
    }

    /**
     * Specify the preferred height of the ScrollPane Viewport.
     * This is the height that will be available to the content node.
     * The overall height of the ScrollPane is the ViewportHeight + padding
     */
    private DoubleProperty prefViewportHeight;

    public final void setPrefViewportHeight(double value) {
        prefViewportHeightProperty().set(value);
    }

    public final double getPrefViewportHeight() {
        return prefViewportHeight == null ? 0.0F : prefViewportHeight.get();
    }

    public final DoubleProperty prefViewportHeightProperty() {
        if (prefViewportHeight == null) {
            prefViewportHeight = new SimpleDoubleProperty(this, "prefViewportHeight");
        }
        return prefViewportHeight;
    }

    /**
     * The actual Bounds of the ScrollPane Viewport.
     * This is the Bounds of the content node.
     */
    private ObjectProperty<Bounds> viewportBounds;

    public final void setViewportBounds(Bounds value) {
        viewportBoundsProperty().set(value);
    }

    public final Bounds getViewportBounds() {
        return viewportBounds == null ? new BoundingBox(0,0,0,0) : viewportBounds.get();
    }

    public final ObjectProperty<Bounds> viewportBoundsProperty() {
        if (viewportBounds == null) {
            viewportBounds = new SimpleObjectProperty<Bounds>(this, "viewportBounds");
        }
        return viewportBounds;
    }


    /***************************************************************************
     *                                                                         *
     * Methods                                                                 *
     *                                                                         *
     **************************************************************************/

    /*
     * TODO The unit increment and block increment variables have been
     * removed from the public API. These are intended to be mapped to
     * the corresponding variables of the scrollbars. However, the problem
     * is that they are specified in terms of the logical corrdinate space
     * of the ScrollPane (that is, [hmin..hmax] by [vmin..vmax]. This is
     * incorrect. Scrolling is a user action and should properly be based
     * on how much of the content is visible, not on some abstract
     * coordinate space. At some later date we may add a finer-grained
     * API to allow applications to control this. Meanwhile, the skin should
     * set unit and block increments for the scroll bars to do something
     * reasonable based on the viewport size, e.g. the block increment
     * should scroll 90% of the pixel size of the viewport, and the unit
     * increment should scroll 10% of the pixel size of the viewport.
     */

    /**
     * Defines the horizontal unit increment amount. Typically this is used when clicking on the
     * increment or decrement arrow buttons of the horizontal scroll bar.
     */
    // public var hunitIncrement:Number = 20.0;

    /**
     * Defines the vertical unit increment amount. Typically this is used when clicking on the
     * increment or decrement arrow buttons of the vertical scroll bar.
     */
    // public var vunitIncrement:Number = 20.0;

    /**
     * Defines the horizontal block increment amount. Typically this is used when clicking on the
     * track of the scroll bar.
     */
    // public var hblockIncrement:Number = -1;

    /**
     * Defines the vertical block increment amount. Typically this is used when clicking on the
     * track of the scroll bar.
     */
    // public var vblockIncrement:Number = -1;

    /***************************************************************************
     *                                                                         *
     *                         Stylesheet Handling                             *
     *                                                                         *
     **************************************************************************/

    /**
     * Initialize the style class to 'scroll-view'.
     *
     * This is the selector class from which CSS can be used to style
     * this control.
     */
    private static final String DEFAULT_STYLE_CLASS = "scroll-pane";

    /**
     * Pseudoclass indicating the fitToWidth property is {@code true}
     */
    private static final String PSEUDO_CLASS_FIT_TO_WIDTH = "fitToWidth";

    /**
     * Pseudoclass indicating the fitToHeight property is {@code true}
     */
    private static final String PSEUDO_CLASS_FIT_TO_HEIGHT = "fitToHeight";

    /**
     * Pseudoclass indicating the pannable property is {@code true}
     */
    private static final String PSEUDO_CLASS_PANNABLE = "pannable";

    /**
     * @treatasprivate
     */
    private static class StyleableProperties {
        private static final StyleableProperty HBAR_POLICY =
            new StyleableProperty(ScrollPane.class, "hbarPolicy");
        private static final StyleableProperty VBAR_POLICY =
            new StyleableProperty(ScrollPane.class, "vbarPolicy");
        private static final StyleableProperty FIT_TO_WIDTH =
            new StyleableProperty(ScrollPane.class, "fitToWidth");
        private static final StyleableProperty FIT_TO_HEIGHT =
            new StyleableProperty(ScrollPane.class, "fitToHeight");
        private static final StyleableProperty PANNABLE =
            new StyleableProperty(ScrollPane.class, "pannable");

        private static final List<StyleableProperty> STYLEABLES;
        private static final int[] bitIndices;
        static {
            final List<StyleableProperty> styleables = 
                new ArrayList<StyleableProperty>(Control.impl_CSS_STYLEABLES());
            Collections.addAll(styleables,
                HBAR_POLICY,
                VBAR_POLICY,
                FIT_TO_WIDTH,
                FIT_TO_HEIGHT,
                PANNABLE
            );
            STYLEABLES = Collections.unmodifiableList(styleables);

            bitIndices = new int[StyleableProperty.getMaxIndex()];
            java.util.Arrays.fill(bitIndices, -1);
            for(int bitIndex=0; bitIndex<STYLEABLES.size(); bitIndex++) {
                bitIndices[STYLEABLES.get(bitIndex).getIndex()] = bitIndex;
            }
        }
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected int[] impl_cssStyleablePropertyBitIndices() {
        return ScrollPane.StyleableProperties.bitIndices;
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static List<StyleableProperty> impl_CSS_STYLEABLES() {
        return ScrollPane.StyleableProperties.STYLEABLES;
    }

    private static final long PANNABLE_PSEUDOCLASS_STATE =
            StyleManager.getInstance().getPseudoclassMask("pannable");
    private static final long FIT_TO_WIDTH_PSEUDOCLASS_STATE =
            StyleManager.getInstance().getPseudoclassMask("fitToWidth");
    private static final long FIT_TO_HEIGHT_PSEUDOCLASS_STATE =
            StyleManager.getInstance().getPseudoclassMask("fitToHeight");

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated @Override public long impl_getPseudoClassState() {
        long mask = super.impl_getPseudoClassState();
        if (isPannable()) {
            mask |= PANNABLE_PSEUDOCLASS_STATE;
        }
        if (isFitToWidth()) {
            mask |= FIT_TO_WIDTH_PSEUDOCLASS_STATE;
        }
        if (isFitToHeight()) {
            mask |= FIT_TO_HEIGHT_PSEUDOCLASS_STATE;
        }
        return mask;
    }
    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected boolean impl_cssSet(String property, Object value) {
        if ("-fx-hbar-policy".equals(property)) {
            setHbarPolicy((ScrollBarPolicy) value);
        } else if ("-fx-vbar-policy".equals(property)) {
            setVbarPolicy((ScrollBarPolicy) value);
        } else if ("-fx-fit-to-width".equals(property)) {
            setFitToWidth((Boolean) value);
        } else if ("-fx-fit-to-height".equals(property)) {
            setFitToHeight((Boolean) value);
        } else if ("-fx-pannable".equals(property)) {
            setPannable((Boolean) value);
        }
        return super.impl_cssSet(property, value);
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected boolean impl_cssSettable(String property) {
        if ("-fx-hbar-policy".equals(property)) {
            return hbarPolicy == null || !hbarPolicy.isBound();
        } else if ("-fx-vbar-policy".equals(property)) {
            return vbarPolicy == null || !vbarPolicy.isBound();
        } else if ("-fx-fit-to-width".equals(property)) {
            return fitToWidth == null || !fitToWidth.isBound();
        } else if ("-fx-fit-to-height".equals(property)) {
            return fitToHeight == null || !fitToHeight.isBound();
        } else if ("-fx-pannable".equals(property)) {
            return pannable == null || !pannable.isBound();
        } else {
            return super.impl_cssSettable(property);
        }
    }

    
    
    /**
     * An enumeration denoting the policy to be used by a scrollable
     * Control in deciding whether to show a scroll bar.
     */
    public static enum ScrollBarPolicy {
        /**
         * Indicates that a scroll bar should never be shown.
         */
        NEVER,
        /**
         * Indicates that a scroll bar should always be shown.
         */
        ALWAYS,
        /**
         * Indicates that a scroll bar should be shown when required.
         */
        AS_NEEDED
    }
}
