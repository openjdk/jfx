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

import com.sun.javafx.css.*;
import com.sun.javafx.css.converters.StringConverter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.WritableValue;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;

import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.scene.control.Logging;

/**
 * Base class for all user interface controls. A "Control" is a node in the
 * scene graph which can be manipulated by the user. Controls provide
 * additional variables and behaviors beyond those of Node to support
 * common user interactions in a manner which is consistent and predictable
 * for the user.
 * <p>
 * Additionally, controls support explicit skinning to make it easy to
 * leverage the functionality of a control while customizing its appearance.
 * <p>
 * See specific Control subclasses for information on how to use individual
 * types of controls.
 * <p> Most controls have their focusTraversable property set to true by default, however
 * read-only controls such as {@link Label} and {@link ProgressIndicator}, and some
 * controls that are containers {@link ScrollPane} and {@link ToolBar} do not.
 * Consult individual control documentation for details.
 */
public abstract class Control extends Parent implements Skinnable {

    /**
     * Sentinel value which can be passed to a control's setMinWidth(), setMinHeight(),
     * setMaxWidth() or setMaxHeight() methods to indicate that the preferred dimension
     * should be used for that max and/or min constraint.
     */
    public static final double USE_PREF_SIZE = Double.NEGATIVE_INFINITY;

    /**
     * Sentinel value which can be passed to a control's setMinWidth(), setMinHeight(),
     * setPrefWidth(), setPrefHeight(), setMaxWidth(), setMaxHeight() methods
     * to reset the control's size constraint back to it's intrinsic size returned
     * by computeMinWidth(), computeMinHeight(), computePrefWidth(), computePrefHeight(),
     * computeMaxWidth(), or computeMaxHeight().
     */
    public static final double USE_COMPUTED_SIZE = -1;

    static {
        // Ensures that the caspian.css file is set as the user agent style sheet
        // when the first control is created.
        UAStylesheetLoader.doLoad();
    }
    
    
    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/    
    
    // --- skin
    /**
     * Skin is responsible for rendering this {@code Control}. From the
     * perspective of the {@code Control}, the {@code Skin} is a black box.
     * It listens and responds to changes in state in a {@code Control}.
     * <p>
     * There is a one-to-one relationship between a {@code Control} and its
     * {@code Skin}. Every {@code Skin} maintains a back reference to the
     * {@code Control} via the {@link Skin#getSkinnable()} method.
     * <p>
     * A skin may be null.
     */
    @Override public final ObjectProperty<Skin<?>> skinProperty() { return skin; }
    @Override public final void setSkin(Skin<?> value) { skinProperty().setValue(value); }
    @Override public final Skin<?> getSkin() { return skinProperty().getValue(); }
    private ObjectProperty<Skin<?>> skin = new StyleableObjectProperty<Skin<?>>() {
        // We store a reference to the oldValue so that we can handle
        // changes in the skin properly in the case of binding. This is
        // only needed because invalidated() does not currently take
        // a reference to the old value.
        private Skin<?> oldValue;

        @Override protected void invalidated() {

            // Dispose of the old skin
            if (oldValue != null) oldValue.dispose();
            // Get the new value, and save it off as the new oldValue
            final Skin<?> skin = oldValue = getValue();
            // Collect the name of the currently installed skin class. We do this
            // so that subsequent updates from CSS to the same skin class will not
            // result in reinstalling the skin
            if (skin == null) setSkinClassName(null);
            else setSkinClassName(skin.getClass().getName());
            // Update the children list with the new skin node
            updateChildren();
            // DEBUG: Log that we've changed the skin
            final PlatformLogger logger = Logging.getControlsLogger();
            if (logger.isLoggable(PlatformLogger.FINEST)) {
                logger.finest("Stored skin[" + getValue() + "] on " + this);
            }
        }

        @Override 
        public StyleableProperty getStyleableProperty() {
            return StyleableProperties.SKIN;
        }
        
        @Override
        public Object getBean() {
            return Control.this;
        }

        @Override
        public String getName() {
            return "skin";
        }
    };

    
    // --- tooltip
    /**
     * The ToolTip for this control.
     */
    public final ObjectProperty<Tooltip> tooltipProperty() {
        if (tooltip == null) {
            tooltip = new ObjectPropertyBase<Tooltip>() {
                private Tooltip old = null;
                @Override protected void invalidated() {
                    Tooltip t = get();
                    // install / uninstall
                    if (t != old) {
                        if (old != null) {
                            Tooltip.uninstall(Control.this, old);
                        }
                        if (t != null) {
                            Tooltip.install(Control.this, t);
                        }
                        old = t;
                    }
                }

                @Override
                public Object getBean() {
                    return Control.this;
                }

                @Override
                public String getName() {
                    return "tooltip";
                }
            };
        }
        return tooltip;
    }
    private ObjectProperty<Tooltip> tooltip;
    public final void setTooltip(Tooltip value) { tooltipProperty().setValue(value); }
    public final Tooltip getTooltip() { return tooltip == null ? null : tooltip.getValue(); }

    
    // --- context menu
    /**
     * The ContextMenu to show for this control.
     */
    private ObjectProperty<ContextMenu> contextMenu = new SimpleObjectProperty<ContextMenu>(this, "contextMenu") {
        @Override protected void invalidated() {
            // set this flag so contextmenu show will be relative to parent window not anchor
            ContextMenu ctx = get();
            if (ctx != null) ctx.setImpl_showRelativeToWindow(true); //RT-15160
        }
        @Override
        public Object getBean() {
            return Control.this;
        }
        @Override
        public String getName() {
            return "contextMenu";
        }
    };
    public final ObjectProperty<ContextMenu> contextMenuProperty() { return contextMenu; }
    public final void setContextMenu(ContextMenu value) { contextMenu.setValue(value); }
    public final ContextMenu getContextMenu() { return contextMenu == null ? null : contextMenu.getValue(); }

    
    // --- width
    /**
     * The width of this control.
     */
    public final ReadOnlyDoubleProperty widthProperty() { return width.getReadOnlyProperty(); }
    protected final void setWidth(double value) { width.set(value); }
    public final double getWidth() { return width.get(); }
    private ReadOnlyDoubleWrapper width = new ReadOnlyDoubleWrapper() {
        @Override protected void invalidated() {
            impl_layoutBoundsChanged();
            requestLayout();
        }

        @Override
        public Object getBean() {
            return Control.this;
        }

        @Override
        public String getName() {
            return "width";
        }
    };

    
    // --- height
    /**
     * The height of this control.
     */
    public final ReadOnlyDoubleProperty heightProperty() { return height.getReadOnlyProperty(); }
    protected final void setHeight(double value) { height.set(value); }
    public final double getHeight() { return height.get(); }
    private ReadOnlyDoubleWrapper height = new ReadOnlyDoubleWrapper() {
        @Override protected void invalidated() {
            impl_layoutBoundsChanged();
            requestLayout();
        }

        @Override
        public Object getBean() {
            return Control.this;
        }

        @Override
        public String getName() {
            return "height";
        }
    };

    
    // --- min width
    /**
     * Property for overriding the control's computed minimum width.
     * This should only be set if the control's internally computed minimum width
     * doesn't meet the application's layout needs.
     * <p>
     * Defaults to the <code>USE_COMPUTED_SIZE</code> flag, which means that
     * <code>getMinWidth(forHeight)</code> will return the control's internally
     * computed minimum width.
     * <p>
     * Setting this value to the <code>USE_PREF_SIZE</code> flag will cause
     * <code>getMinWidth(forHeight)</code> to return the control's preferred width,
     * enabling applications to easily restrict the resizability of the control.
     *
     */
    public final DoubleProperty minWidthProperty() {
        if (minWidth == null) {
            minWidth = new DoublePropertyBase(USE_COMPUTED_SIZE) {
                @Override
                public void invalidated() {
                    if (getParent() != null) {
                        getParent().requestLayout();
                    }
                }

                @Override
                public Object getBean() {
                    return Control.this;
                }

                @Override
                public String getName() {
                    return "minWidth";
                }
            };
        }
        return minWidth;
    }
    private DoubleProperty minWidth;
    public final void setMinWidth(double value) { minWidthProperty().set(value); }
    public final double getMinWidth() { return minWidth == null ? USE_COMPUTED_SIZE : minWidth.get(); }


    // --- min height
    /**
     * Property for overriding the control's computed minimum height.
     * This should only be set if the control's internally computed minimum height
     * doesn't meet the application's layout needs.
     * <p>
     * Defaults to the <code>USE_COMPUTED_SIZE</code> flag, which means that
     * <code>getMinHeight(forWidth)</code> will return the control's internally
     * computed minimum height.
     * <p>
     * Setting this value to the <code>USE_PREF_SIZE</code> flag will cause
     * <code>getMinHeight(forWidth)</code> to return the control's preferred height,
     * enabling applications to easily restrict the resizability of the control.
     *
     */
    public final DoubleProperty minHeightProperty() {
        if (minHeight == null) {
            minHeight = new DoublePropertyBase(USE_COMPUTED_SIZE) {
                @Override
                public void invalidated() {
                    if (getParent() != null) {
                        getParent().requestLayout();
                    }
                }

                @Override
                public Object getBean() {
                    return Control.this;
                }

                @Override
                public String getName() {
                    return "minHeight";
                }
            };
        }
        return minHeight;
    }
    private DoubleProperty minHeight;
    public final void setMinHeight(double value) { minHeightProperty().set(value); }
    public final double getMinHeight() { return minHeight == null ? USE_COMPUTED_SIZE : minHeight.get(); }

    /**
     * Convenience method for overriding the control's computed minimum width and height.
     * This should only be called if the control's internally computed minimum size
     * doesn't meet the application's layout needs.
     *
     * @see #setMinWidth(double)
     * @see #setMinHeight(double)
     * @param minWidth  the override value for minimum width
     * @param minHeight the override value for minimum height
     */
    public void setMinSize(double minWidth, double minHeight) {
        setMinWidth(minWidth);
        setMinHeight(minHeight);
    }

    /**
     * Property for overriding the control's computed preferred width.
     * This should only be set if the control's internally computed preferred width
     * doesn't meet the application's layout needs.
     * <p>
     * Defaults to the <code>USE_COMPUTED_SIZE</code> flag, which means that
     * <code>getPrefWidth(forHeight)</code> will return the control's internally
     * computed preferred width.
     *
     */
    public final DoubleProperty prefWidthProperty() {
        if (prefWidth == null) {
            prefWidth = new DoublePropertyBase(USE_COMPUTED_SIZE) {
                @Override
                public void invalidated() {
                    if (getParent() != null) {
                        getParent().requestLayout();
                    }
                }

                @Override
                public Object getBean() {
                    return Control.this;
                }

                @Override
                public String getName() {
                    return "prefWidth";
                }
            };
        }
        return prefWidth;
    }
    private DoubleProperty prefWidth;
    public final void setPrefWidth(double value) { prefWidthProperty().set(value); }
    public final double getPrefWidth() { return prefWidth == null ? USE_COMPUTED_SIZE : prefWidth.get(); }


    /**
     * Property for overriding the control's computed preferred height.
     * This should only be set if the control's internally computed preferred height
     * doesn't meet the application's layout needs.
     * <p>
     * Defaults to the <code>USE_COMPUTED_SIZE</code> flag, which means that
     * <code>getPrefHeight(forWidth)</code> will return the control's internally
     * computed preferred width.
     *
     */
    public final DoubleProperty prefHeightProperty() {
        if (prefHeight == null) {
            prefHeight = new DoublePropertyBase(USE_COMPUTED_SIZE) {
                @Override
                public void invalidated() {
                    if (getParent() != null) {
                        getParent().requestLayout();
                    }
                }

                @Override
                public Object getBean() {
                    return Control.this;
                }

                @Override
                public String getName() {
                    return "prefHeight";
                }
            };
        }
        return prefHeight;
    }
    private DoubleProperty prefHeight;
    public final void setPrefHeight(double value) { prefHeightProperty().set(value); }
    public final double getPrefHeight() { return prefHeight == null ? USE_COMPUTED_SIZE : prefHeight.get(); }


    /**
     * Convenience method for overriding the control's computed preferred width and height.
     * This should only be called if the control's internally computed preferred size
     * doesn't meet the application's layout needs.
     *
     * @see #setPrefWidth(double)
     * @see #setPrefHeight(double)
     * @param prefWidth the override value for preferred width
     * @param prefHeight the override value for preferred height
     */
    public void setPrefSize(double prefWidth, double prefHeight) {
        setPrefWidth(prefWidth);
        setPrefHeight(prefHeight);
    }

    /**
     * Property for overriding the control's computed maximum width.
     * This should only be set if the control's internally computed maximum width
     * doesn't meet the application's layout needs.
     * <p>
     * Defaults to the <code>USE_COMPUTED_SIZE</code> flag, which means that
     * <code>getMaxWidth(forHeight)</code> will return the control's internally
     * computed maximum width.
     * <p>
     * Setting this value to the <code>USE_PREF_SIZE</code> flag will cause
     * <code>getMaxWidth(forHeight)</code> to return the control's preferred width,
     * enabling applications to easily restrict the resizability of the control.
     *
     */
    public final DoubleProperty maxWidthProperty() {
        if (maxWidth == null) {
            maxWidth = new DoublePropertyBase(USE_COMPUTED_SIZE) {
                @Override
                public void invalidated() {
                    if (getParent() != null) {
                        getParent().requestLayout();
                    }
                }

                @Override
                public Object getBean() {
                    return Control.this;
                }

                @Override
                public String getName() {
                    return "maxWidth";
                }
            };
        }
        return maxWidth;
    }
    private DoubleProperty maxWidth;
    public final void setMaxWidth(double value) { maxWidthProperty().set(value); }
    public final double getMaxWidth() { return maxWidth == null ? USE_COMPUTED_SIZE : maxWidth.get(); }

    /**
     * Property for overriding the control's computed maximum height.
     * This should only be set if the control's internally computed maximum height
     * doesn't meet the application's layout needs.
     * <p>
     * Defaults to the <code>USE_COMPUTED_SIZE</code> flag, which means that
     * <code>getMaxHeight(forWidth)</code> will return the control's internally
     * computed maximum height.
     * <p>
     * Setting this value to the <code>USE_PREF_SIZE</code> flag will cause
     * <code>getMaxHeight(forWidth)</code> to return the control's preferred height,
     * enabling applications to easily restrict the resizability of the control.
     *
     */
    public final DoubleProperty maxHeightProperty() {
        if (maxHeight == null) {
            maxHeight = new DoublePropertyBase(USE_COMPUTED_SIZE) {
                @Override
                public void invalidated() {
                    if (getParent() != null) {
                        getParent().requestLayout();
                    }
                }

                @Override
                public Object getBean() {
                    return Control.this;
                }

                @Override
                public String getName() {
                    return "maxHeight";
                }
            };
        }
        return maxHeight;
    }
    private DoubleProperty maxHeight;
    public final void setMaxHeight(double value) { maxHeightProperty().set(value); }
    public final double getMaxHeight() { return maxHeight == null ? USE_COMPUTED_SIZE : maxHeight.get(); }

    
    
    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/
    
    /**
     *  Create a new Control.
     */
    protected Control() {
        setFocusTraversable(true);
    }
    
    
    
    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/    

    /**
     * Returns <code>true</code> since all Controls are resizable.
     * @return whether this node can be resized by its parent during layout
     */
    @Override public boolean isResizable() {
        return true;
    }

    /**
     * Invoked by the control's parent during layout to set the control's
     * width and height.  <b>Applications should not invoke this method directly</b>.
     * If an application needs to directly set the size of the control, it should
     * override its size constraints by calling <code>setMinSize()</code>,
     *  <code>setPrefSize()</code>, or <code>setMaxSize()</code> and it's parent
     * will honor those overrides during layout.
     *
     * @param width the target layout bounds width
     * @param height the target layout bounds height
     */
    @Override public void resize(double width, double height) {
        setWidth(width);
        setHeight(height);
        final PlatformLogger logger = com.sun.javafx.Logging.getLayoutLogger();
        if (logger.isLoggable(PlatformLogger.FINER)) {
            logger.finer(this.toString()+" resized to "+width+" x "+height);
        }
    }

    /**
     * Called during layout to determine the minimum width for this node.
     * Returns the value from <code>computeMinWidth(forHeight)</code> unless
     * the application overrode the minimum width by setting the minWidth property.
     *
     * @see #setMinWidth(double)
     * @return the minimum width that this node should be resized to during layout
     */
    @Override public final double minWidth(double height) {
        double override = getMinWidth();
        if (override == USE_COMPUTED_SIZE) {
            return computeMinWidth(height);
        } else if (override == USE_PREF_SIZE) {
            return prefWidth(height);
        }
        return override;
    }

    /**
     * Called during layout to determine the minimum height for this node.
     * Returns the value from <code>computeMinHeight(forWidth)</code> unless
     * the application overrode the minimum height by setting the minHeight property.
     *
     * @see #setMinHeight(double)
     * @return the minimum height that this node should be resized to during layout
     */
    @Override public final double minHeight(double width) {
        double override = getMinHeight();
        if (override == USE_COMPUTED_SIZE) {
            return computeMinHeight(width);
        } else if (override == USE_PREF_SIZE) {
            return prefHeight(width);
        }
        return override;
    }

    /**
     * Called during layout to determine the preferred width for this node.
     * Returns the value from <code>computePrefWidth(forHeight)</code> unless
     * the application overrode the preferred width by setting the prefWidth property.
     *
     * @see #setPrefWidth(double)
     * @return the preferred width that this node should be resized to during layout
     */
    @Override public final double prefWidth(double height) {
        double override = getPrefWidth();
        if (override == USE_COMPUTED_SIZE) {
            return super.prefWidth(height);
        } 
        return override;
    }

    /**
     * Called during layout to determine the preferred height for this node.
     * Returns the value from <code>computePrefHeight(forWidth)</code> unless
     * the application overrode the preferred height by setting the prefHeight property.
     *
     * @see #setPrefHeight(double)
     * @return the preferred height that this node should be resized to during layout
     */
    @Override public final double prefHeight(double width) {
        double override = getPrefHeight();
        if (override == USE_COMPUTED_SIZE) {
            return super.prefHeight(width);
        } 
        return override;
    }
    /**
     * Called during layout to determine the maximum width for this node.
     * Returns the value from <code>computeMaxWidth(forHeight)</code> unless
     * the application overrode the maximum width by setting the maxWidth property.
     *
     * @see #setMaxWidth(double)
     * @return the maximum width that this node should be resized to during layout
     */
    @Override public final double maxWidth(double height) {
        double override = getMaxWidth();
        if (override == USE_COMPUTED_SIZE) {
            return computeMaxWidth(height);
        } else if (override == USE_PREF_SIZE) {
            return prefWidth(height);
        }
        return override;
    }

    /**
     * Called during layout to determine the maximum height for this node.
     * Returns the value from <code>computeMaxHeight(forWidth)</code> unless
     * the application overrode the maximum height by setting the maxHeight property.
     *
     * @see #setMaxHeight(double)
     * @return the maximum height that this node should be resized to during layout
     */
    @Override public final double maxHeight(double width) {
        double override = getMaxHeight();
        if (override == USE_COMPUTED_SIZE) {
            return computeMaxHeight(width);
        } else if (override == USE_PREF_SIZE) {
            return prefHeight(width);
        }
        return override;
    }
    
    /**
     * Convenience method for overriding the control's computed maximum width and height.
     * This should only be called if the control's internally computed maximum size
     * doesn't meet the application's layout needs.
     *
     * @see #setMaxWidth(double)
     * @see #setMaxHeight(double)
     * @param maxWidth  the override value for maximum width
     * @param maxHeight the override value for maximum height
     */
    public void setMaxSize(double maxWidth, double maxHeight) {
        setMaxWidth(maxWidth);
        setMaxHeight(maxHeight);
    }

    // Implementation of the Resizable interface.
    // Because only the skin can know the min, pref, and max sizes, these
    // functions are implemented to delegate to skin. If there is no skin then
    // we simply return 0 for all the values since a Control without a Skin
    // doesn't render
    /**
     * Computes the minimum allowable width of the Control, based on the provided
     * height. The minimum width is not calculated within the Control, instead
     * the calculation is delegated to the {@link Node#minWidth(double)} method 
     * of the {@link Skin}. If the Skin is null, the returned value is 0.
     * 
     * @param height The height of the Control, in case this value might dictate
     *      the minimum width.
     * @return A double representing the minimum width of this control.
     */
    protected double computeMinWidth(double height) {
        return getSkinNode() == null ? 0 : getSkinNode().minWidth(height);
    }
    /**
     * Computes the minimum allowable height of the Control, based on the provided
     * width. The minimum height is not calculated within the Control, instead
     * the calculation is delegated to the {@link Node#minHeight(double)} method 
     * of the {@link Skin}. If the Skin is null, the returned value is 0.
     * 
     * @param width The width of the Control, in case this value might dictate
     *      the minimum height.
     * @return A double representing the minimum height of this control.
     */
    protected double computeMinHeight(double width) {
        return getSkinNode() == null ? 0 : getSkinNode().minHeight(width);
    }
    /**
     * Computes the maximum allowable width of the Control, based on the provided
     * height. The maximum width is not calculated within the Control, instead
     * the calculation is delegated to the {@link Node#maxWidth(double)} method 
     * of the {@link Skin}. If the Skin is null, the returned value is 0.
     * 
     * @param height The height of the Control, in case this value might dictate
     *      the maximum width.
     * @return A double representing the maximum width of this control.
     */
    protected double computeMaxWidth(double height) {
        return getSkinNode() == null ? 0 : getSkinNode().maxWidth(height);
    }
    /**
     * Computes the maximum allowable height of the Control, based on the provided
     * width. The maximum height is not calculated within the Control, instead
     * the calculation is delegated to the {@link Node#maxHeight(double)} method 
     * of the {@link Skin}. If the Skin is null, the returned value is 0.
     * 
     * @param width The width of the Control, in case this value might dictate
     *      the maximum height.
     * @return A double representing the maximum height of this control.
     */
    protected double computeMaxHeight(double width) {
        return getSkinNode() == null ? 0 : getSkinNode().maxHeight(width);
    }
    /** {@inheritDoc} */
    @Override protected double computePrefWidth(double height) {
        return getSkinNode() == null? 0 : getSkinNode().prefWidth(height);
    }
    /** {@inheritDoc} */
    @Override protected double computePrefHeight(double width) {
        return getSkinNode() == null? 0 : getSkinNode().prefHeight(width);
    }
    /** {@inheritDoc} */
    @Override public double getBaselineOffset() { return getSkinNode() == null? 0 : getSkinNode().getBaselineOffset(); }

    /***************************************************************************
     * Implementation of layout bounds for the Control. We want to preserve    *
     * the lazy semantics of layout bounds. So whenever the width/height       *
     * changes on the node, we end up invalidating layout bounds. We then      *
     * recompute it on demand.                                                 *
     **************************************************************************/

    /**
     * @treatasprivate
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected void impl_notifyLayoutBoundsChanged() {
        // override Node's default behavior of having a geometric bounds change
        // trigger a change in layoutBounds. For Resizable nodes, layoutBounds
        // is unrelated to geometric bounds.
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected Bounds impl_computeLayoutBounds() {
        return new BoundingBox(0, 0, getWidth(), getHeight());
    }

    /** {@inheritDoc} */
    @Override protected void layoutChildren() {
        Node n = getSkinNode();
        if (n != null) {
            n.resizeRelocate(0, 0, getWidth(), getHeight());
        }
    }

    /***************************************************************************
     * Forward the following to the skin                                       *
     **************************************************************************/

    // overridden to base containment on the Skin rather than on the bounds
    // of the Skin, allowing the Skin to dictate containment.
    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected boolean impl_computeContains(double localX, double localY) {
        Node n = getSkinNode();
        return n == null ? false : n.contains(localX, localY);
    }

    // overridden to base intersects on the Skin rather than on the bounds
    // of the Skin, allowing the Skin to dictate intersection
    /** {@inheritDoc} */
    @Override public boolean intersects(double localX, double localY, double localWidth, double localHeight) {
        Node n = getSkinNode();
        return n == null ? false : n.intersects(localX, localY, localWidth, localHeight);
    }
    
    
    
    /***************************************************************************
     *                                                                         *
     * Private implementation                                                  *
     *                                                                         *
     **************************************************************************/    

    /**
     * Gets the Skin's node, or returns null if there is no Skin.
     * Convenience method for getting the node of the skin. This is null-safe,
     * meaning if skin is null then it will return null instead of throwing
     * a NullPointerException.
     *
     * @return The Skin's node, or null.
     */
    private Node getSkinNode() {
        return getSkin() == null ? null : getSkin().getNode();
    }

    /**
     * Called from several places whenever the children of the Control
     * may need to be updated (principally, when the tool tip changes,
     * the skin changes, or the skin.node changes).
     */
    private void updateChildren() {
        final Node n = getSkinNode();
        if (n != null) getChildren().setAll(n);
        else getChildren().clear();
    }

    /**
     * Keeps a reference to the name of the class currently acting as the skin.
     */
    private StringProperty skinClassName;
    protected StringProperty skinClassNameProperty() {
        if (skinClassName == null) {
            skinClassName = new StyleableStringProperty() {

               @Override
                public void invalidated() {
                    final Skin currentSkin = skinProperty().get();
                    if (currentSkin == null || !currentSkin.getClass().getName().equals(get()) ) {
                        loadSkinClass();
                    }
                }
                
                @Override
                public Object getBean() {
                    return Control.this;
                }

                @Override
                public String getName() {
                    return "skinClassName";
                }

                @Override
                public StyleableProperty getStyleableProperty() {
                    return StyleableProperties.SKIN;
                }
                
            };
        }
        return skinClassName;
    }
    
    protected void setSkinClassName(String skinClassName) {
        skinClassNameProperty().set(skinClassName);        
    }
    
    private void loadSkinClass() {
        
        if (skinClassName == null 
                || skinClassName.get() == null 
                || skinClassName.get().isEmpty()) {
            Logging.getControlsLogger().severe("Empty -fx-skin property specified for control " + this);
            return;
        }
        
        try {
            Class<?> skinClass;
            // RT-17525 : Use context class loader only if Class.forName fails.
            try {
                skinClass = Class.forName(skinClassName.get());
            } catch (ClassNotFoundException clne) {
                if (Thread.currentThread().getContextClassLoader() != null) {
                    skinClass = Thread.currentThread().getContextClassLoader().loadClass(skinClassName.get());
                } else {
                    throw clne;
                }
            }
            
            Constructor<?>[] constructors = skinClass.getConstructors();
            Constructor<?> skinConstructor = null;
            for (Constructor<?> c : constructors) {
                Class<?>[] parameterTypes = c.getParameterTypes();
                if (parameterTypes.length == 1 && Control.class.isAssignableFrom(parameterTypes[0])) {
                    skinConstructor = c;
                    break;
                }
            }

            if (skinConstructor == null) {
                Logging.getControlsLogger().severe(
                "No valid constructor defined in '" + skinClassName + "' for control " + this +
                         ".\r\nYou must provide a constructor that accepts a single "
                         + "Control parameter in " + skinClassName + ".",
                new NullPointerException());
            } else {
                Skin<?> skinInstance = (Skin<?>) skinConstructor.newInstance(this);
                setSkin(skinInstance);
            }
        } catch (InvocationTargetException e) {
            Logging.getControlsLogger().severe(
                "Failed to load skin '" + skinClassName + "' for control " + this,
                e.getCause());
        } catch (Exception e) {
            Logging.getControlsLogger().severe(
                "Failed to load skin '" + skinClassName + "' for control " + this, e);
            Node parent = this;
            int n = 0;
            while (parent != null) {
                for(int j=0; j<n*2; j++) {
                    System.err.print(' ');
                }
                n+=1;
                System.err.println(parent);
                parent = parent.getParent();
            }
            
        }
        return;
    }

    
    
    /***************************************************************************
     *                                                                         *
     * StyleSheet Handling                                                     *
     *                                                                         *
     **************************************************************************/

    /**
     * Implementors may specify their own user-agent stylesheet. The return
     * value is a string URL. A relative URL is resolved using the class
     * loader of the implementing Control.
     * @return A string URL
     */
    protected String getUserAgentStylesheet() {
        return null;
    }

    private static class StyleableProperties {
        private static final StyleableProperty<Control,String> SKIN = 
            new StyleableProperty<Control,String>("-fx-skin",
                StringConverter.getInstance()) {

            @Override
            public boolean isSettable(Control n) {
                return n.skin == null || !n.skin.isBound();
            }

            @Override
            public WritableValue<String> getWritableValue(Control n) {
                return n.skinClassNameProperty();
            }
        };

        private static final List<StyleableProperty> STYLEABLES;
        static {
            final List<StyleableProperty> styleables =
                new ArrayList<StyleableProperty>(Parent.impl_CSS_STYLEABLES());
            Collections.addAll(styleables,
                SKIN
            );
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static List<StyleableProperty> impl_CSS_STYLEABLES() {
        return Control.StyleableProperties.STYLEABLES;
    }

    /**
     * @treatasprivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override public void impl_processCSS(boolean reapply) {
        if (reapply && getUserAgentStylesheet() != null) {
            StyleManager.getInstance().addUserAgentStylesheet(getUserAgentStylesheet());
        }

        super.impl_processCSS(reapply);

        if (getSkin() == null) {
            Logging.getControlsLogger().severe(
            "The -fx-skin property has not been defined in CSS for " + this);
        }
    }
}
