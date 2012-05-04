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

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.WritableValue;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.stage.PopupWindow;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sun.javafx.collections.TrackableObservableList;
import com.sun.javafx.css.StyleManager;
import com.sun.javafx.css.Styleable;
import com.sun.javafx.css.StyleableProperty;
import com.sun.javafx.css.StyleableStringProperty;
import com.sun.javafx.css.converters.StringConverter;
import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.scene.control.Logging;

/**
 * An extension of PopupWindow that allows for CSS styling.
 */
public class PopupControl extends PopupWindow implements Skinnable {

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
    
    // Ensures that the caspian.css file is set as the user agent style sheet
    // when the first popupcontrol is created.
    static {
        UAStylesheetLoader.doLoad();
    }

    /**
     * We need a special root node, except we can't replace the special
     * root node already in the PopupControl. So we'll set our own
     * special almost-root node that is a child of the root.
     *
     * This special root node is responsible for mapping the id, styleClass,
     * and style defined on the PopupControl such that CSS will read the
     * values from the PopupControl, and then apply CSS state to that
     * special node. The node will then be able to pass impl_cssSet calls
     * along, such that any subclass of PopupControl will be able to
     * use the Styleable properties  and we'll be able to style it from
     * CSS, in such a way that it participates and applies to the skin,
     * exactly the way that normal Skin's work for normal Controls.
     */
    protected CSSBridge bridge;
    
    /**
     * Create a new empty PopupControl.
     */
    public PopupControl() {
        super();
        this.bridge = new CSSBridge();

        // Bind up these two properties. Note that the third, styleClass, is
        // handled in the onChange listener for that list.
        bridge.idProperty().bind(idProperty());
        bridge.styleProperty().bind(styleProperty());

        getContent().add(bridge);

        // TODO the fact that PopupWindow uses a group for auto-moving things
        // around means that the scene resize semantics don't work if the
        // child is a resizable. I will need to replicate those semantics
        // here sometime, such that if the Skin provides a resizable, it is
        // given to match the popup window's width & height.
    }

    // TODO we should have some interface which has id, styleClass, style, etc
    // which is in common between PopupControl and Node.
    
    // TODO we should have some interface for minWidth, maxWidth, etc which are
    // both here and on Node.
    
    /**
     * The id of this {@code PopupControl}. This simple string identifier is useful for
     * finding a specific Node within the scene graph. While the id of a Node
     * should be unique within the scene graph, this uniqueness is not enforced.
     * This is analogous to the "id" attribute on an HTML element 
     * (<a href="http://www.w3.org/TR/CSS21/syndata.html#value-def-identifier">CSS ID Specification</a>).
     *
     * @defaultValue null
     */
    private final StringProperty id = new SimpleStringProperty(this, "id");
    public final StringProperty idProperty() { return id; }
    
    /**
     * Sets the id of this {@code PopupControl}. This simple string identifier is useful for
     * finding a specific Node within the scene graph. While the id of a Node
     * should be unique within the scene graph, this uniqueness is not enforced.
     * This is analogous to the "id" attribute on an HTML element 
     * (<a href="http://www.w3.org/TR/CSS21/syndata.html#value-def-identifier">CSS ID Specification</a>).
     *
     * @param value  the id assigned to this {@code PopupControl} using the {@code setId} 
     *         method or {@code null}, if no id has been assigned.
     * @defaultValue null
     */
    public final void setId(String value) { id.set(value); }
    
    /**
     * The id of this {@code PopupControl}. This simple string identifier is useful for
     * finding a specific Node within the scene graph. While the id of a Node
     * should be unique within the scene graph, this uniqueness is not enforced.
     * This is analogous to the "id" attribute on an HTML element 
     * (<a href="http://www.w3.org/TR/CSS21/syndata.html#value-def-identifier">CSS ID Specification</a>).
     *
     * @return the id assigned to this {@code PopupControl} using the {@code setId} 
     *         method or {@code null}, if no id has been assigned.
     * @defaultValue null
     */
    public final String getId() { return id.get(); }

    /**
     * A list of String identifiers which can be used to logically group
     * Nodes, specifically for an external style engine. This variable is
     * analogous to the "class" attribute on an HTML element and, as such,
     * each element of the list is a style class to which this Node belongs.
     *
     * @see <a href="http://www.w3.org/TR/css3-selectors/#class-html">CSS3 class selectors</a>
     * @defaultValue null
     */
    private final ObservableList<String> styleClass = new TrackableObservableList<String>() {
        // TODO we can save one class if we factor this into a StyleClassList, and reuse it between
        // here and in Node.
        @Override protected void onChanged(Change<String> c) {
            // Push the change along to the bridge group
            bridge.getStyleClass().setAll(styleClass);
        }

        @Override public String toString() {
            if (size() == 0) {
                return "";
            } else if (size() == 1) {
                return get(0);
            } else {
                StringBuilder buf = new StringBuilder();
                for (int i = 0; i < size(); i++) {
                    buf.append(get(i));
                    if (i + 1 < size()) {
                        buf.append(' ');
                    }
                }
                return buf.toString();
            }
        }
    };

    /**
     * Returns the list of String identifiers that make up the styleClass
     * for this PopupControl. 
     */
    public final ObservableList<String> getStyleClass() { return styleClass; }

    /**
     * A string representation of the CSS style associated with this
     * specific PopupControl. This is analogous to the "style" attribute of an
     * HTML element. Note that, like the HTML style attribute, this
     * variable contains style properties and values and not the
     * selector portion of a style rule.
     * <p>
     * Parsing this style might not be supported on some limited
     * platforms. It is recommended to use a standalone CSS file instead.
     *
     * @defaultValue empty string
     */
    private final StringProperty style = new SimpleStringProperty(this, "style");
    
    /**
     * A string representation of the CSS style associated with this
     * specific {@code PopupControl}. This is analogous to the "style" attribute of an
     * HTML element. Note that, like the HTML style attribute, this
     * variable contains style properties and values and not the
     * selector portion of a style rule.
     * @param value The inline CSS style to use for this {@code PopupControl}.
     *         {@code null} is implicitly converted to an empty String. 
     * @defaultValue empty string
     */
    public final void setStyle(String value) { style.set(value); }
    
    // TODO: javadoc copied from property for the sole purpose of providing a return tag
    /**
     * A string representation of the CSS style associated with this
     * specific {@code PopupControl}. This is analogous to the "style" attribute of an
     * HTML element. Note that, like the HTML style attribute, this
     * variable contains style properties and values and not the
     * selector portion of a style rule.
     * @defaultValue empty string
     * @return The inline CSS style associated with this {@code PopupControl}.
     *         If this {@code PopupControl} does not have an inline style,
     *         an empty String is returned.
     */
    public final String getStyle() { return style.get(); }
    public final StringProperty styleProperty() { return style; }

    @Override public final ObjectProperty<Skin<?>> skinProperty() { 
        return bridge.skin; }
    @Override public final void setSkin(Skin<?> value) { 
        skinProperty().set(value); 
    }
    @Override public final Skin<?> getSkin() {
        return skinProperty().getValue();
    }

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
     */
    private DoubleProperty minWidth;
    
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
     */
    public final void setMinWidth(double value) { minWidthProperty().set(value); }
    
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
     */
    public final double getMinWidth() { return minWidth == null ? USE_COMPUTED_SIZE : minWidth.get(); }
    public final DoubleProperty minWidthProperty() {
        if (minWidth == null) {
            minWidth = new DoublePropertyBase(USE_COMPUTED_SIZE) {
                @Override public void invalidated() {
                    if (isShowing()) bridge.requestLayout();
                }

                @Override
                public Object getBean() {
                    return PopupControl.this;
                }

                @Override
                public String getName() {
                    return "minWidth";
                }
            };
        }
        return minWidth;
    }


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
    private DoubleProperty minHeight;
    
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
    public final void setMinHeight(double value) { minHeightProperty().set(value); }
    
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
    public final double getMinHeight() { return minHeight == null ? USE_COMPUTED_SIZE : minHeight.get(); }
    public final DoubleProperty minHeightProperty() {
        if (minHeight == null) {
            minHeight = new DoublePropertyBase(USE_COMPUTED_SIZE) {
                @Override public void invalidated() {
                    if (isShowing()) bridge.requestLayout();
                }

                @Override
                public Object getBean() {
                    return PopupControl.this;
                }

                @Override
                public String getName() {
                    return "minHeight";
                }
            };
        }
        return minHeight;
    }

    /**
     * Convenience method for overriding the control's computed minimum width and height.
     * This should only be called if the control's internally computed minimum size
     * doesn't meet the application's layout needs.
     *
     * @see #setMinWidth
     * @see #setMinHeight
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
     */
    private DoubleProperty prefWidth;
    
    /**
     * Property for overriding the control's computed preferred width.
     * This should only be set if the control's internally computed preferred width
     * doesn't meet the application's layout needs.
     * <p>
     * Defaults to the <code>USE_COMPUTED_SIZE</code> flag, which means that
     * <code>getPrefWidth(forHeight)</code> will return the control's internally
     * computed preferred width.
     */
    public final void setPrefWidth(double value) { prefWidthProperty().set(value); }
    
    /**
     * Property for overriding the control's computed preferred width.
     * This should only be set if the control's internally computed preferred width
     * doesn't meet the application's layout needs.
     * <p>
     * Defaults to the <code>USE_COMPUTED_SIZE</code> flag, which means that
     * <code>getPrefWidth(forHeight)</code> will return the control's internally
     * computed preferred width.
     */
    public final double getPrefWidth() { return prefWidth == null ? USE_COMPUTED_SIZE : prefWidth.get(); }
    public final DoubleProperty prefWidthProperty() {
        if (prefWidth == null) {
            prefWidth = new DoublePropertyBase(USE_COMPUTED_SIZE) {
                @Override public void invalidated() {
                    if (isShowing()) bridge.requestLayout();
                }

                @Override
                public Object getBean() {
                    return PopupControl.this;
                }

                @Override
                public String getName() {
                    return "prefWidth";
                }
            };
        }
        return prefWidth;
    }

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
    private DoubleProperty prefHeight;
    
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
    public final void setPrefHeight(double value) { prefHeightProperty().set(value); }
    
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
    public final double getPrefHeight() { return prefHeight == null ? USE_COMPUTED_SIZE : prefHeight.get(); }
    public final DoubleProperty prefHeightProperty() {
        if (prefHeight == null) {
            prefHeight = new DoublePropertyBase(USE_COMPUTED_SIZE) {
                @Override public void invalidated() {
                    if (isShowing()) bridge.requestLayout();
                }

                @Override
                public Object getBean() {
                    return PopupControl.this;
                }

                @Override
                public String getName() {
                    return "prefHeight";
                }
            };
        }
        return prefHeight;
    }

    /**
     * Convenience method for overriding the control's computed preferred width and height.
     * This should only be called if the control's internally computed preferred size
     * doesn't meet the application's layout needs.
     *
     * @see #setPrefWidth
     * @see #setPrefHeight
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
     */
    private DoubleProperty maxWidth;
    
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
     */
    public final void setMaxWidth(double value) { maxWidthProperty().set(value); }
    
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
     */
    public final double getMaxWidth() { return maxWidth == null ? USE_COMPUTED_SIZE : maxWidth.get(); }
    public final DoubleProperty maxWidthProperty() {
        if (maxWidth == null) {
            maxWidth = new DoublePropertyBase(USE_COMPUTED_SIZE) {
                @Override public void invalidated() {
                    if (isShowing()) bridge.requestLayout();
                }

                @Override
                public Object getBean() {
                    return PopupControl.this;
                }

                @Override
                public String getName() {
                    return "maxWidth";
                }
            };
        }
        return maxWidth;
    }

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
    private DoubleProperty maxHeight;
    
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
    public final void setMaxHeight(double value) { maxHeightProperty().set(value); }
    
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
    public final double getMaxHeight() { return maxHeight == null ? USE_COMPUTED_SIZE : maxHeight.get(); }
    public final DoubleProperty maxHeightProperty() {
        if (maxHeight == null) {
            maxHeight = new DoublePropertyBase(USE_COMPUTED_SIZE) {
                @Override public void invalidated() {
                    if (isShowing()) bridge.requestLayout();
                }

                @Override
                public Object getBean() {
                    return PopupControl.this;
                }

                @Override
                public String getName() {
                    return "maxHeight";
                }
            };
        }
        return maxHeight;
    }

    /**
     * Convenience method for overriding the control's computed maximum width and height.
     * This should only be called if the control's internally computed maximum size
     * doesn't meet the application's layout needs.
     *
     * @see #setMaxWidth
     * @see #setMaxHeight
     * @param maxWidth  the override value for maximum width
     * @param maxHeight the override value for maximum height
     */
    public void setMaxSize(double maxWidth, double maxHeight) {
        setMaxWidth(maxWidth);
        setMaxHeight(maxHeight);
    }

    /**
     * Cached prefWidth, prefHeight, minWidth, minHeight. These
     * results are repeatedly sought during the layout pass,
     * and caching the results leads to a significant decrease
     * in overhead.
     */
    private double prefWidthCache = -1;
    private double prefHeightCache = -1;
    private double minWidthCache = -1;
    private double minHeightCache = -1;
    private double maxWidthCache = -1;
    private double maxHeightCache = -1;
    private boolean skinSizeComputed = false;
    
    /**
     * Called during layout to determine the minimum width for this node.
     * Returns the value from <code>minWidth(forHeight)</code> unless
     * the application overrode the minimum width by setting the minWidth property.
     *
     * @param height the height
     * @see #setMinWidth
     * @return the minimum width that this node should be resized to during layout
     */
    public final double minWidth(double height) {
        double override = getMinWidth();
        if (override == USE_COMPUTED_SIZE) {
            if (minWidthCache == -1) minWidthCache = recalculateMinWidth(height);
            return minWidthCache;
        } else if (override == USE_PREF_SIZE) {
            return prefWidth(height);
        }
        return override;
    }

    /**
     * Called during layout to determine the minimum height for this node.
     * Returns the value from <code>minHeight(forWidth)</code> unless
     * the application overrode the minimum height by setting the minHeight property.
     *
     * @param width The width
     * @see #setMinHeight
     * @return the minimum height that this node should be resized to during layout
     */
    public final double minHeight(double width) {
        double override = getMinHeight();
        if (override == USE_COMPUTED_SIZE) {
            if (minHeightCache == -1) minHeightCache = recalculateMinHeight(width);
            return minHeightCache;
        } else if (override == USE_PREF_SIZE) {
            return prefHeight(width);
        }
        return override;
    }


    /**
     * Called during layout to determine the preferred width for this node.
     * Returns the value from <code>prefWidth(forHeight)</code> unless
     * the application overrode the preferred width by setting the prefWidth property.
     *
     * @param height the height
     * @see #setPrefWidth
     * @return the preferred width that this node should be resized to during layout
     */
    public final double prefWidth(double height) {
        double override = getPrefWidth();
        if (override == USE_COMPUTED_SIZE) {
            if (prefWidthCache == -1) prefWidthCache = recalculatePrefWidth(height);
            return prefWidthCache;
        } else if (override == USE_PREF_SIZE) {
            return prefWidth(height);
        }
        return override;
    }

    /**
     * Called during layout to determine the preferred height for this node.
     * Returns the value from <code>prefHeight(forWidth)</code> unless
     * the application overrode the preferred height by setting the prefHeight property.
     *
     * @param width the width
     * @see #setPrefHeight
     * @return the preferred height that this node should be resized to during layout
     */
    public final double prefHeight(double width) {
        double override = getPrefHeight();
        if (override == USE_COMPUTED_SIZE) {
            if (prefHeightCache == -1) prefHeightCache = recalculatePrefHeight(width);
            return prefHeightCache;
        } else if (override == USE_PREF_SIZE) {
            return prefHeight(width);
        }
        return override;
    }

    /**
     * Called during layout to determine the maximum width for this node.
     * Returns the value from <code>maxWidth(forHeight)</code> unless
     * the application overrode the maximum width by setting the maxWidth property.
     *
     * @param height the height
     * @see #setMaxWidth
     * @return the maximum width that this node should be resized to during layout
     */
    public final double maxWidth(double height) {
        double override = getMaxWidth();
        if (override == USE_COMPUTED_SIZE) {
            if (maxWidthCache == -1) maxWidthCache = recalculateMaxWidth(height);
            return maxWidthCache;
        } else if (override == USE_PREF_SIZE) {
            return prefWidth(height);
        }
        return override;
    }

    /**
     * Called during layout to determine the maximum height for this node.
     * Returns the value from <code>maxHeight(forWidth)</code> unless
     * the application overrode the maximum height by setting the maxHeight property.
     *
     * @param width the width
     * @see #setMaxHeight
     * @return the maximum height that this node should be resized to during layout
     */
    public final double maxHeight(double width) {
        double override = getMaxHeight();
        if (override == USE_COMPUTED_SIZE) {
            if (maxHeightCache == -1) maxHeightCache = recalculateMaxHeight(width);
            return maxHeightCache;
        } else if (override == USE_PREF_SIZE) {
            return prefHeight(width);
        }
        return override;
    }

    // Implementation of the Resizable interface.
    // Because only the skin can know the min, pref, and max sizes, these
    // functions are implemented to delegate to skin. If there is no skin then
    // we simply return 0 for all the values since a Control without a Skin
    // doesn't render
    private double recalculateMinWidth(double height) {
        recomputeSkinSize();
        return getSkinNode() == null ? 0 : getSkinNode().minWidth(height);
    }
    private double recalculateMinHeight(double width) {
        recomputeSkinSize();
        return getSkinNode() == null ? 0 : getSkinNode().minHeight(width);
    }
    private double recalculateMaxWidth(double height) {
        recomputeSkinSize();
        return getSkinNode() == null ? 0 : getSkinNode().maxWidth(height);
    }
    private double recalculateMaxHeight(double width) {
        recomputeSkinSize();
        return getSkinNode() == null ? 0 : getSkinNode().maxHeight(width);
    }
    private double recalculatePrefWidth(double height) {
        recomputeSkinSize();
        return getSkinNode() == null? 0 : getSkinNode().prefWidth(height);
    }
    private double recalculatePrefHeight(double width) {
        recomputeSkinSize();
        return getSkinNode() == null? 0 : getSkinNode().prefHeight(width);
    }
    
    private void recomputeSkinSize() {
        if (!skinSizeComputed && getScene() != null && getScene().getRoot() != null) {
            // RT-14094, RT-16754: We need the skins of the popup
            // and it children before the stage is visible so we
            // can calculate the popup position based on content
            // size.
            getScene().getRoot().impl_processCSS(true);
            skinSizeComputed = true;
        }
    }
//    public double getBaselineOffset() { return getSkinNode() == null? 0 : getSkinNode().getBaselineOffset(); }

    /**
     * Called from several places whenever the children of the Control
     * may need to be updated (principally, when the tool tip changes,
     * the skin changes, or the skin.node changes).
     */
    private void updateChildren() {
        prefWidthCache = -1;
        prefHeightCache = -1;
        minWidthCache = -1;
        minHeightCache = -1;
        maxWidthCache = -1;
        maxHeightCache = -1;
        skinSizeComputed = false;
        final Node n = getSkinNode();
        if (n != null) bridge.getChildren().setAll(n);
        else bridge.getChildren().clear();
    }

    /***************************************************************************
     *                                                                         *
     *                         StyleSheet Handling                             *
     *                                                                         *
     **************************************************************************/

    private static class StyleableProperties {
        private static final StyleableProperty<CSSBridge,String> SKIN = 
            new StyleableProperty<CSSBridge,String>("-fx-skin",
                StringConverter.getInstance()) {

            @Override
            public boolean isSettable(CSSBridge n) {
                return n.skin == null || !n.skin.isBound();
            }

            @Override
            public WritableValue<String> getWritableValue(CSSBridge n) {
                return n.skinClassNameProperty();
            }
        };

        private static final List<StyleableProperty> STYLEABLES;
        static {
            final List<StyleableProperty> styleables =
                new ArrayList<StyleableProperty>();
            Collections.addAll(styleables,
                SKIN
            );
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }
    
    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    public static List<StyleableProperty> impl_CSS_STYLEABLES() {
        return StyleableProperties.STYLEABLES;
    }


    /**
     * RT-19263
     * @treatAsPrivate implementation detail
     * @deprecated This is an experimental API that is not intended for general use and is subject to change in future versions
     */
    @Deprecated
    public List<StyleableProperty> impl_getStyleableProperties() {
        return impl_CSS_STYLEABLES();
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    protected void impl_pseudoClassStateChanged(String s) {
        bridge.impl_pseudoClassStateChanged(s);
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    protected Styleable styleable; 
    
    /**
     * RT-19263
     * @treatAsPrivate implementation detail
     * @deprecated This is an experimental API that is not intended for general use and is subject to change in future versions
     */
    @Deprecated // SB-dependency: RT-21094 has been filed to track this
    public Styleable impl_getStyleable() {
        if (styleable == null) {
            styleable = new Styleable() {

                @Override
                public String getId() {
                    return PopupControl.this.getId();
                }

                @Override
                public List<String> getStyleClass() {
                    return PopupControl.this.getStyleClass();
                }

                @Override
                public String getStyle() {
                    return PopupControl.this.getStyle();
                }

                @Override
                public Styleable getStyleableParent() {
                    return null;
                }

                @Override
                public List<StyleableProperty> getStyleableProperties() {
                    return PopupControl.this.impl_getStyleableProperties();
                }                
                
                @Override
                public Node getNode() {
                    return bridge;
                }

            };
        }
        return styleable;
    }    

    protected class CSSBridge extends Group {
        private String currentSkinClassName = null;
        @Override public void impl_pseudoClassStateChanged(String s) {
            super.impl_pseudoClassStateChanged(s);
        }
        
        @Override public List<StyleableProperty> impl_getStyleableProperties() {
            return PopupControl.this.impl_getStyleableProperties();
        }

        /**
         * Requests a layout pass to be performed before the next scene is
         * rendered. This is batched up asynchronously to happen once per
         * "pulse", or frame of animation.
         * <p/>
         * If this parent is either a layout root or unmanaged, then it will be
         * added directly to the scene's dirty layout list, otherwise requestLayout
         * will be invoked on its parent.
         */
        @Override public void requestLayout() {
            prefWidthCache = -1;
            prefHeightCache = -1;
            minWidthCache = -1;
            minHeightCache = -1;
            maxWidthCache = -1;
            maxHeightCache = -1;
            skinSizeComputed = false;
            super.requestLayout();
        }

        /**
        * Skin is responsible for rendering this {@code PopupControl}. From the
        * perspective of the {@code PopupControl}, the {@code Skin} is a black box.
        * It listens and responds to changes in state in a {@code PopupControl}.
        * <p>
        * There is a one-to-one relationship between a {@code PopupControl} and its
        * {@code Skin}. Every {@code Skin} maintains a back reference to the
        * {@code PopupControl}.
        * <p>
        * A skin may be null.
        */
        private ObjectProperty<Skin<?>> skin = new ObjectPropertyBase<Skin<?>>() {
            // We store a reference to the oldValue so that we can handle
            // changes in the skin properly in the case of binding. This is
            // only needed because invalidated() does not currently take
            // a reference to the old value.
            private Skin<?> oldValue;

            @Override
            public void set(Skin<?> v) {

                if (v == null 
                    ? oldValue == null
                    : oldValue != null && v.getClass().equals(oldValue.getClass()))
                    return;

                super.set(v);

                // Collect the name of the currently installed skin class. We do this
                // so that subsequent updates from CSS to the same skin class will not
                // result in reinstalling the skin
                currentSkinClassName = v == null ? null : v.getClass().getName();

                // if someone calls setSkin, we need to make it look like they 
                // called set on skinClassName in order to keep CSS from overwriting
                // the skin. 
                skinClassNameProperty().set(currentSkinClassName);

            }
            
            @Override protected void invalidated() {
                // Let CSS know that this property has been manually changed
                // Dispose of the old skin
                if (oldValue != null) oldValue.dispose();
                // Get the new value, and save it off as the new oldValue
                final Skin<?> skin = oldValue = getValue();
                // Update the children list with the new skin node
                updateChildren();
                // DEBUG: Log that we've changed the skin
                final PlatformLogger logger = Logging.getControlsLogger();
                if (logger.isLoggable(PlatformLogger.FINEST)) {
                    logger.finest("Stored skin[" + getValue() + "] on " + this);
                }
            }

            @Override
            public Object getBean() {
                return PopupControl.CSSBridge.this;
            }

            @Override
            public String getName() {
                return "skin";
            }
        };
        
        /**
        * Keeps a reference to the name of the class currently acting as the skin.
        */
        private StringProperty skinClassName = null;
        private StringProperty skinClassNameProperty() {
            if (skinClassName == null) {
                skinClassName = new StyleableStringProperty() {
                    
                    @Override
                    public void set(String v) {
                        // do not allow the skin to be set to null through CSS
                        if (v == null || v.isEmpty() || v.equals(get())) return;
                        super.set(v);
                    }
                    
                    @Override
                    public void invalidated() {

                        final Skin currentSkin = skinProperty().get();

                        //
                        // if the current skin is not null, then 
                        // see if then check to see if the current skin's class name
                        // is the same as skinClassName. If it is, then there is
                        // no need to load the skin class. Note that the only time 
                        // this would be the case is if someone called setSkin since
                        // the skin would be set ahead of the skinClassName 
                        // (skinClassName is set from the skinProperty's invalidated
                        // method, so the skin would be set, then the skinClassName).
                        // If the skinClassName is set first (via CSS), then this
                        // invalidated method won't get called unless the value
                        // has changed (thus, we won't reload the same skin).
                        // 
                        if (get() != null) {
                            if (!get().equals(currentSkinClassName)) {
                                loadSkinClass();
                            }
                        // CSS should not set skin to null
    //                    } else {
    //                        setSkin(null);
                        }
                    }

                    @Override
                    public Object getBean() {
                        return PopupControl.CSSBridge.this;
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
                final String msg = 
                    "Empty -fx-skin property specified for popup control " + this;   
                final List<String> errors = StyleManager.getInstance().getErrors();
                if (errors != null) {
                    errors.add(msg); // RT-19884
                } 
                Logging.getControlsLogger().severe(msg);
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
                    if (parameterTypes.length == 1 && PopupControl.class.isAssignableFrom(parameterTypes[0])) {
                        skinConstructor = c;
                        break;
                    }
                }

                if (skinConstructor == null) {
                    final String msg = 
                        "No valid constructor defined in '" + skinClassName 
                        + "' for popup control " + this 
                        + ".\r\nYou must provide a constructor that accepts a single "
                        + "PopupControl parameter in " + skinClassName + ".";
                    final List<String> errors = StyleManager.getInstance().getErrors();
                    if (errors != null) {
                        errors.add(msg); // RT-19884
                    } 
                    Logging.getControlsLogger().severe(msg);
                    return;
                } else {
                    Skin<?> skinInstance = (Skin<?>) skinConstructor.newInstance(PopupControl.this);
                    // Do not call setSkin here since it has the side effect of
                    // also setting the skinClassName!
                    skinProperty().set(skinInstance);
                }

            } catch (InvocationTargetException e) {
                final String msg = 
                    "Failed to load skin '" + skinClassName 
                    + "' for popup control " + this;
                final List<String> errors = StyleManager.getInstance().getErrors();
                if (errors != null) {
                    errors.add(msg + " :" + e.getLocalizedMessage()); // RT-19884
                } 
                Logging.getControlsLogger().severe(msg, e.getCause());
            } catch (Exception e) {
                final String msg = 
                    "Failed to load skin '" + skinClassName 
                    + "' for popup control " + this;
                final List<String> errors = StyleManager.getInstance().getErrors();
                if (errors != null) {
                    errors.add(msg + " :" + e.getLocalizedMessage()); // RT-19884
                } 
                Logging.getControlsLogger().severe(msg, e.getCause());
            }
        }
        
    }
}
