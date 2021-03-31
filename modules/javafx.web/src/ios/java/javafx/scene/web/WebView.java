/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.web;


import javafx.application.Platform;
import javafx.css.CssMetaData;
import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.css.converter.BooleanConverter;
import javafx.css.converter.EnumConverter;
import javafx.css.converter.SizeConverter;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.PickRay;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.input.PickResultChooser;
import com.sun.javafx.scene.SceneHelper;
import com.sun.java.scene.web.WebViewHelper;

import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.sg.prism.web.NGWebView;
import com.sun.javafx.tk.TKPulseListener;
import com.sun.javafx.tk.Toolkit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.css.Styleable;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.text.FontSmoothingType;

/**
 * {@code WebView} is a {@link javafx.scene.Node} that manages a
 * {@link WebEngine} and displays its content. The associated {@code WebEngine}
 * is created automatically at construction time and cannot be changed
 * afterwards. {@code WebView} handles mouse and some keyboard events, and
 * manages scrolling automatically, so there's no need to put it into a
 * {@code ScrollPane}.
 *
 * <p>{@code WebView} objects must be created and accessed solely from the
 * FX thread.
 * @since JavaFX 2.0
 */
final public class WebView extends Parent {
    static {
        WebViewHelper.setWebViewAccessor(new WebViewHelper.WebViewAccessor() {
            @Override
                public NGNode doCreatePeer(Node node) {
                return ((WebView) node).doCreatePeer();
            }

            @Override
                public void doUpdatePeer(Node node) {
                ((WebView) node).doUpdatePeer();
            }

            @Override
            public void doTransformsChanged(Node node) {
                ((WebView) node).doTransformsChanged();
            }

            @Override
            public BaseBounds doComputeGeomBounds(Node node,
                    BaseBounds bounds, BaseTransform tx) {
                return ((WebView) node).doComputeGeomBounds(bounds, tx);
            }

            @Override
            public boolean doComputeContains(Node node, double localX, double localY) {
                return ((WebView) node).doComputeContains(localX, localY);
            }

            @Override
            public void doPickNodeLocal(Node node, PickRay localPickRay, PickResultChooser result) {
                // Not supported yet
            }
        });

        System.loadLibrary("webview");
    }

    private static final boolean DEFAULT_CONTEXT_MENU_ENABLED = true;
    private static final FontSmoothingType DEFAULT_FONT_SMOOTHING_TYPE = FontSmoothingType.LCD;
    private static final double DEFAULT_ZOOM = 1.0;
    private static final double DEFAULT_FONT_SCALE = 1.0;
    private static final double DEFAULT_MIN_WIDTH = 0;
    private static final double DEFAULT_MIN_HEIGHT = 0;
    private static final double DEFAULT_PREF_WIDTH = 800;
    private static final double DEFAULT_PREF_HEIGHT = 600;
    private static final double DEFAULT_MAX_WIDTH = Double.MAX_VALUE;
    private static final double DEFAULT_MAX_HEIGHT = Double.MAX_VALUE;

    private final WebEngine engine;
    // pointer to native WebViewImpl
    private final long handle;
    private boolean nativeVisible = true;

    /**
     * The stage pulse listener registered with the toolkit.
     * This field guarantees that the listener will exist throughout
     * the whole lifetime of the WebView node. This field is necessary
     * because the toolkit references its stage pulse listeners weakly.
     */
    private final TKPulseListener stagePulseListener;

    /**
     * Returns the {@code WebEngine} object.
     */
    public final WebEngine getEngine() {
        return engine;
    }

    private final ReadOnlyDoubleWrapper width = new ReadOnlyDoubleWrapper(this, "width");

    /**
     * Returns width of this {@code WebView}.
     */
    public final double getWidth() {
        return width.get();
    }

    /**
     * Width of this {@code WebView}.
     */
    public ReadOnlyDoubleProperty widthProperty() {
        return width.getReadOnlyProperty();
    }

    private final ReadOnlyDoubleWrapper height = new ReadOnlyDoubleWrapper(this, "height");

    /**
     * Returns height of this {@code WebView}.
     */
    public final double getHeight() {
        return height.get();
    }

    /**
     * Height of this {@code WebView}.
     */
    public ReadOnlyDoubleProperty heightProperty() {
        return height.getReadOnlyProperty();
    }

    /**
     * Zoom factor applied to the whole page contents.
     *
     * @defaultValue 1.0
     */
    private DoubleProperty zoom;

    /**
     * Sets current zoom factor applied to the whole page contents.
     * @param value zoom factor to be set
     * @see #zoomProperty()
     * @see #getZoom()
     * @since JavaFX 8.0
     */
    public final void setZoom(double value) {
        WebEngine.checkThread();
        zoomProperty().set(value);
    }

    /**
     * Returns current zoom factor applied to the whole page contents.
     * @return current zoom factor
     * @see #zoomProperty()
     * @see #setZoom(double value)
     * @since JavaFX 8.0
     */
    public final double getZoom() {
        return (this.zoom != null)
                ? this.zoom.get()
                : DEFAULT_ZOOM;
    }

    /**
     * Returns zoom property object.
     * @return zoom property object
     * @see #getZoom()
     * @see #setZoom(double value)
     * @since JavaFX 8.0
     */
    public final DoubleProperty zoomProperty() {
        if (zoom == null) {
            zoom = new StyleableDoubleProperty(DEFAULT_ZOOM) {
                @Override public void invalidated() {
                    Toolkit.getToolkit().checkFxUserThread();
                }

                @Override public CssMetaData<WebView, Number> getCssMetaData() {
                    return StyleableProperties.ZOOM;
                }
                @Override public Object getBean() {
                    return WebView.this;
                }
                @Override public String getName() {
                    return "zoom";
                }
            };
        }
        return zoom;
    }

    /**
     * Specifies scale factor applied to font. This setting affects
     * text content but not images and fixed size elements.
     *
     * @defaultValue 1.0
     */
    private DoubleProperty fontScale;

    public final void setFontScale(double value) {
        WebEngine.checkThread();
        fontScaleProperty().set(value);
    }

    public final double getFontScale() {
        return (this.fontScale != null)
                ? this.fontScale.get()
                : DEFAULT_FONT_SCALE;
    }

    public DoubleProperty fontScaleProperty() {
        if (fontScale == null) {
            fontScale = new StyleableDoubleProperty(DEFAULT_FONT_SCALE) {
                @Override public void invalidated() {
                    Toolkit.getToolkit().checkFxUserThread();
                }
                @Override public CssMetaData<WebView, Number> getCssMetaData() {
                    return StyleableProperties.FONT_SCALE;
                }
                @Override public Object getBean() {
                    return WebView.this;
                }
                @Override public String getName() {
                    return "fontScale";
                }
            };
        }
        return fontScale;
    }

    /**
     * Creates a {@code WebView} object.
     */
    public WebView() {
        long[] nativeHandle = new long[1];
        _initWebView(nativeHandle);
        getStyleClass().add("web-view");
        handle = nativeHandle[0];
        engine = new WebEngine();
        engine.setView(this);

        stagePulseListener = () -> {
            handleStagePulse();
        };
        focusedProperty().addListener((ov, t, t1) -> {
        });
        Toolkit.getToolkit().addStageTkPulseListener(stagePulseListener);

        final ChangeListener<Bounds> chListener = new ChangeListener<Bounds>() {

            @Override
            public void changed(ObservableValue<? extends Bounds> observable, Bounds oldValue, Bounds newValue) {
                NodeHelper.transformsChanged(WebView.this);
            }
        };

        parentProperty().addListener(new ChangeListener<Parent>(){

            @Override
            public void changed(ObservableValue<? extends Parent> observable, Parent oldValue, Parent newValue) {
                if (oldValue != null && newValue == null) {
                    // webview has been removed from scene
                    _removeWebView(handle);
                }

                if (oldValue != null) {
                    do {
                        oldValue.boundsInParentProperty().removeListener(chListener);
                        oldValue = oldValue.getParent();
                    } while (oldValue != null);
                }

                if (newValue != null) {
                    do {
                        final Node n = newValue;
                        newValue.boundsInParentProperty().addListener(chListener);
                        newValue = newValue.getParent();
                    } while (newValue != null);
                }
            }

        });

        layoutBoundsProperty().addListener(new ChangeListener<Bounds>() {

            @Override
            public void changed(ObservableValue<? extends Bounds> observable, Bounds oldValue, Bounds newValue) {
                Affine3D trans = calculateNodeToSceneTransform(WebView.this);
                _setTransform(handle,
                trans.getMxx(), trans.getMxy(), trans.getMxz(), trans.getMxt(),
                trans.getMyx(), trans.getMyy(), trans.getMyz(), trans.getMyt(),
                trans.getMzx(), trans.getMzy(), trans.getMzz(), trans.getMzt());

            }

        });

        NodeHelper.treeVisibleProperty(this).addListener(new ChangeListener<Boolean>() {

            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                setNativeVisible(handle, newValue);
            }
        });
    }
    // Resizing support. Allows arbitrary growing and shrinking.
    // Designed after javafx.scene.control.Control

    @Override public boolean isResizable() {
        return true;
    }

    @Override public void resize(double width, double height) {
        this.width.set(width);
        this.height.set(height);
        NodeHelper.markDirty(this, DirtyBits.NODE_GEOMETRY);
        NodeHelper.geomChanged(this);
        _setWidth(handle, width);
        _setHeight(handle, height);
    }

    /**
     * Called during layout to determine the minimum width for this node.
     *
     * @return the minimum width that this node should be resized to during layout
     */
    @Override public final double minWidth(double height) {
        return getMinWidth();
    }

    /**
     * Called during layout to determine the minimum height for this node.
     *
     * @return the minimum height that this node should be resized to during layout
     */
    @Override public final double minHeight(double width) {
        return getMinHeight();
    }


    /**
     * Called during layout to determine the preferred width for this node.
     *
     * @return the preferred width that this node should be resized to during layout
     */
    @Override public final double prefWidth(double height) {
        return getPrefWidth();
    }

    /**
     * Called during layout to determine the preferred height for this node.
     *
     * @return the preferred height that this node should be resized to during layout
     */
    @Override public final double prefHeight(double width) {
        return getPrefHeight();
    }
    /**
     * Called during layout to determine the maximum width for this node.
     *
     * @return the maximum width that this node should be resized to during layout
     */
    @Override public final double maxWidth(double height) {
        return getMaxWidth();
    }

    /**
     * Called during layout to determine the maximum height for this node.
     *
     * @return the maximum height that this node should be resized to during layout
     */
    @Override public final double maxHeight(double width) {
        return getMaxHeight();
    }

    /**
     * Minimum width property.
     */
    public DoubleProperty minWidthProperty() {
        if (minWidth == null) {
            minWidth = new StyleableDoubleProperty(DEFAULT_MIN_WIDTH) {
                @Override
                public void invalidated() {
                    if (getParent() != null) {
                        getParent().requestLayout();
                    }
                }
                @Override
                public CssMetaData<WebView, Number> getCssMetaData() {
                    return StyleableProperties.MIN_WIDTH;
                }
                @Override
                public Object getBean() {
                    return WebView.this;
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

    /**
     * Sets minimum width.
     */
    public final void setMinWidth(double value) {
        minWidthProperty().set(value);
        _setWidth(handle, value);
    }

    /**
     * Returns minimum width.
     */
    public final double getMinWidth() {
        return (this.minWidth != null)
                ? this.minWidth.get()
                : DEFAULT_MIN_WIDTH;
    }

    /**
     * Minimum height property.
     */
    public DoubleProperty minHeightProperty() {
        if (minHeight == null) {
            minHeight = new StyleableDoubleProperty(DEFAULT_MIN_HEIGHT) {
                @Override
                public void invalidated() {
                    if (getParent() != null) {
                        getParent().requestLayout();
                    }
                }
                @Override
                public CssMetaData<WebView, Number> getCssMetaData() {
                    return StyleableProperties.MIN_HEIGHT;
                }
                @Override
                public Object getBean() {
                    return WebView.this;
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

    /**
     * Sets minimum height.
     */
    public final void setMinHeight(double value) {
        minHeightProperty().set(value);
        _setHeight(handle, value);
    }

    /**
     * Sets minimum height.
     */
    public final double getMinHeight() {
        return (this.minHeight != null)
                ? this.minHeight.get()
                : DEFAULT_MIN_HEIGHT;
    }

    /**
     * Convenience method for setting minimum width and height.
     */
    public void setMinSize(double minWidth, double minHeight) {
        setMinWidth(minWidth);
        setMinHeight(minHeight);
        _setWidth(handle, minWidth);
        _setHeight(handle, minHeight);
    }

    /**
     * Preferred width property.
     */
    public DoubleProperty prefWidthProperty() {
        if (prefWidth == null) {
            prefWidth = new StyleableDoubleProperty(DEFAULT_PREF_WIDTH) {
                @Override
                public void invalidated() {
                    if (getParent() != null) {
                        getParent().requestLayout();
                    }
                }
                @Override
                public CssMetaData<WebView, Number> getCssMetaData() {
                    return StyleableProperties.PREF_WIDTH;
                }
                @Override
                public Object getBean() {
                    return WebView.this;
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

    /**
     * Sets preferred width.
     */
    public final void setPrefWidth(double value) {
        prefWidthProperty().set(value);
        _setWidth(handle, value);
    }

    /**
     * Returns preferred width.
     */
    public final double getPrefWidth() {
        return (this.prefWidth != null)
                ? this.prefWidth.get()
                : DEFAULT_PREF_WIDTH;
    }

    /**
     * Preferred height property.
     */
    public DoubleProperty prefHeightProperty() {
        if (prefHeight == null) {
            prefHeight = new StyleableDoubleProperty(DEFAULT_PREF_HEIGHT) {
                @Override
                public void invalidated() {
                    if (getParent() != null) {
                        getParent().requestLayout();
                    }
                }
                @Override
                public CssMetaData<WebView, Number> getCssMetaData() {
                    return StyleableProperties.PREF_HEIGHT;
                }
                @Override
                public Object getBean() {
                    return WebView.this;
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

    /**
     * Sets preferred height.
     */
    public final void setPrefHeight(double value) {
        prefHeightProperty().set(value);
        _setHeight(handle, value);
    }

    /**
     * Returns preferred height.
     */
    public final double getPrefHeight() {
        return (this.prefHeight != null)
                ? this.prefHeight.get()
                : DEFAULT_PREF_HEIGHT;
    }

    /**
     * Convenience method for setting preferred width and height.
     */
    public void setPrefSize(double prefWidth, double prefHeight) {
        setPrefWidth(prefWidth);
        setPrefHeight(prefHeight);
        _setWidth(handle, prefWidth);
        _setHeight(handle, prefHeight);
    }

    /**
     * Maximum width property.
     */
    public DoubleProperty maxWidthProperty() {
        if (maxWidth == null) {
            maxWidth = new StyleableDoubleProperty(DEFAULT_MAX_WIDTH) {
                @Override
                public void invalidated() {
                    if (getParent() != null) {
                        getParent().requestLayout();
                    }
                }
                @Override
                public CssMetaData<WebView, Number> getCssMetaData() {
                    return StyleableProperties.MAX_WIDTH;
                }
                @Override
                public Object getBean() {
                    return WebView.this;
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

    /**
     * Sets maximum width.
     */
    public final void setMaxWidth(double value) {
        maxWidthProperty().set(value);
        _setWidth(handle, value);
    }

    /**
     * Returns maximum width.
     */
    public final double getMaxWidth() {
        return (this.maxWidth != null)
                ? this.maxWidth.get()
                : DEFAULT_MAX_WIDTH;
    }

    /**
     * Maximum height property.
     */
    public DoubleProperty maxHeightProperty() {
        if (maxHeight == null) {
            maxHeight = new StyleableDoubleProperty(DEFAULT_MAX_HEIGHT) {
                @Override
                public void invalidated() {
                    if (getParent() != null) {
                        getParent().requestLayout();
                    }
                }
                @Override
                public CssMetaData<WebView, Number> getCssMetaData() {
                    return StyleableProperties.MAX_HEIGHT;
                }
                @Override
                public Object getBean() {
                    return WebView.this;
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

    /**
     * Sets maximum height.
     */
    public final void setMaxHeight(double value) {
        maxHeightProperty().set(value);
        _setHeight(handle, value);
    }

    /**
     * Returns maximum height.
     */
    public final double getMaxHeight() {
        return (this.maxHeight != null)
                ? this.maxHeight.get()
                : DEFAULT_MAX_HEIGHT;
    }

    /**
     * Convenience method for setting maximum width and height.
     */
    public void setMaxSize(double maxWidth, double maxHeight) {
        setMaxWidth(maxWidth);
        setMaxHeight(maxHeight);
        _setWidth(handle, maxWidth);
        _setHeight(handle, maxHeight);
    }


    /**
     * Specifies a requested font smoothing type : gray or LCD.
     *
     * The width of the bounding box is defined by the widest row.
     *
     * Note: LCD mode doesn't apply in numerous cases, such as various
     * compositing modes, where effects are applied and very large glyphs.
     *
     * @defaultValue FontSmoothingType.LCD
     * @since JavaFX 2.2
     */
    private ObjectProperty<FontSmoothingType> fontSmoothingType;

    public final void setFontSmoothingType(FontSmoothingType value) {
        fontSmoothingTypeProperty().set(value);
    }

    public final FontSmoothingType getFontSmoothingType() {
        return (this.fontSmoothingType != null)
                ? this.fontSmoothingType.get()
                : DEFAULT_FONT_SMOOTHING_TYPE;
    }

    public final ObjectProperty<FontSmoothingType> fontSmoothingTypeProperty() {
        if (this.fontSmoothingType == null) {
            this.fontSmoothingType = new StyleableObjectProperty<FontSmoothingType>(DEFAULT_FONT_SMOOTHING_TYPE) {
                @Override
                public void invalidated() {
                    Toolkit.getToolkit().checkFxUserThread();
                }
                @Override
                public CssMetaData<WebView, FontSmoothingType> getCssMetaData() {
                    return StyleableProperties.FONT_SMOOTHING_TYPE;
                }
                @Override
                public Object getBean() {
                    return WebView.this;
                }
                @Override
                public String getName() {
                    return "fontSmoothingType";
                }
            };
        }
        return this.fontSmoothingType;
    }

    /**
     * Specifies whether context menu is enabled.
     *
     * @defaultValue true
     * @since JavaFX 2.2
     */
    private BooleanProperty contextMenuEnabled;

    public final void setContextMenuEnabled(boolean value) {
        contextMenuEnabledProperty().set(value);
    }

    public final boolean isContextMenuEnabled() {
        return contextMenuEnabled == null
                ? DEFAULT_CONTEXT_MENU_ENABLED
                : contextMenuEnabled.get();
    }

    public final BooleanProperty contextMenuEnabledProperty() {
        if (contextMenuEnabled == null) {
            contextMenuEnabled = new StyleableBooleanProperty(DEFAULT_CONTEXT_MENU_ENABLED) {
                @Override public void invalidated() {
                    Toolkit.getToolkit().checkFxUserThread();
                }

                @Override public CssMetaData<WebView, Boolean> getCssMetaData() {
                    return StyleableProperties.CONTEXT_MENU_ENABLED;
                }

                @Override public Object getBean() {
                    return WebView.this;
                }

                @Override public String getName() {
                    return "contextMenuEnabled";
                }
            };
        }
        return contextMenuEnabled;
    }

    /**
     * Super-lazy instantiation pattern from Bill Pugh.
     */
    private static final class StyleableProperties {

        private static final CssMetaData<WebView, Boolean> CONTEXT_MENU_ENABLED
                = new CssMetaData<WebView, Boolean>(
                "-fx-context-menu-enabled",
                BooleanConverter.getInstance(),
                DEFAULT_CONTEXT_MENU_ENABLED)
        {
            @Override public boolean isSettable(WebView view) {
                return view.contextMenuEnabled == null || !view.contextMenuEnabled.isBound();
            }
            @Override public StyleableProperty<Boolean> getStyleableProperty(WebView view) {
                return (StyleableProperty<Boolean>)view.contextMenuEnabledProperty();
            }
        };

        private static final CssMetaData<WebView, FontSmoothingType> FONT_SMOOTHING_TYPE
                = new CssMetaData<WebView, FontSmoothingType>(
                "-fx-font-smoothing-type",
                new EnumConverter<FontSmoothingType>(FontSmoothingType.class),
                DEFAULT_FONT_SMOOTHING_TYPE) {
            @Override
            public boolean isSettable(WebView view) {
                return view.fontSmoothingType == null || !view.fontSmoothingType.isBound();
            }
            @Override
            public StyleableProperty<FontSmoothingType> getStyleableProperty(WebView view) {
                return (StyleableProperty<FontSmoothingType>)view.fontSmoothingTypeProperty();
            }
        };

        private static final CssMetaData<WebView, Number> ZOOM
                = new CssMetaData<WebView, Number>(
                "-fx-zoom",
                SizeConverter.getInstance(),
                DEFAULT_ZOOM) {
            @Override public boolean isSettable(WebView view) {
                return view.zoom == null || !view.zoom.isBound();
            }
            @Override public StyleableProperty<Number> getStyleableProperty(WebView view) {
                return (StyleableProperty<Number>)view.zoomProperty();
            }
        };

        private static final CssMetaData<WebView, Number> FONT_SCALE
                = new CssMetaData<WebView, Number>(
                "-fx-font-scale",
                SizeConverter.getInstance(),
                DEFAULT_FONT_SCALE) {
            @Override
            public boolean isSettable(WebView view) {
                return view.fontScale == null || !view.fontScale.isBound();
            }
            @Override
            public StyleableProperty<Number> getStyleableProperty(WebView view) {
                return (StyleableProperty<Number>)view.fontScaleProperty();
            }
        };

        private static final CssMetaData<WebView, Number> MIN_WIDTH
                = new CssMetaData<WebView, Number>(
                "-fx-min-width",
                SizeConverter.getInstance(),
                DEFAULT_MIN_WIDTH) {
            @Override
            public boolean isSettable(WebView view) {
                return view.minWidth == null || !view.minWidth.isBound();
            }
            @Override
            public StyleableProperty<Number> getStyleableProperty(WebView view) {
                return (StyleableProperty<Number>)view.minWidthProperty();
            }
        };

        private static final CssMetaData<WebView, Number> MIN_HEIGHT
                = new CssMetaData<WebView, Number>(
                "-fx-min-height",
                SizeConverter.getInstance(),
                DEFAULT_MIN_HEIGHT) {
            @Override
            public boolean isSettable(WebView view) {
                return view.minHeight == null || !view.minHeight.isBound();
            }
            @Override
            public StyleableProperty<Number> getStyleableProperty(WebView view) {
                return (StyleableProperty<Number>)view.minHeightProperty();
            }
        };

        private static final CssMetaData<WebView, Number> MAX_WIDTH
                = new CssMetaData<WebView, Number>(
                "-fx-max-width",
                SizeConverter.getInstance(),
                DEFAULT_MAX_WIDTH) {
            @Override
            public boolean isSettable(WebView view) {
                return view.maxWidth == null || !view.maxWidth.isBound();
            }
            @Override
            public StyleableProperty<Number> getStyleableProperty(WebView view) {
                return (StyleableProperty<Number>)view.maxWidthProperty();
            }
        };

        private static final CssMetaData<WebView, Number> MAX_HEIGHT
                = new CssMetaData<WebView, Number>(
                "-fx-max-height",
                SizeConverter.getInstance(),
                DEFAULT_MAX_HEIGHT) {
            @Override
            public boolean isSettable(WebView view) {
                return view.maxHeight == null || !view.maxHeight.isBound();
            }
            @Override
            public StyleableProperty<Number> getStyleableProperty(WebView view) {
                return (StyleableProperty<Number>)view.maxHeightProperty();
            }
        };

        private static final CssMetaData<WebView, Number> PREF_WIDTH
                = new CssMetaData<WebView, Number>(
                "-fx-pref-width",
                SizeConverter.getInstance(),
                DEFAULT_PREF_WIDTH) {
            @Override
            public boolean isSettable(WebView view) {
                return view.prefWidth == null || !view.prefWidth.isBound();
            }
            @Override
            public StyleableProperty<Number> getStyleableProperty(WebView view) {
                return (StyleableProperty<Number>)view.prefWidthProperty();
            }
        };

        private static final CssMetaData<WebView, Number> PREF_HEIGHT
                = new CssMetaData<WebView, Number>(
                "-fx-pref-height",
                SizeConverter.getInstance(),
                DEFAULT_PREF_HEIGHT) {
            @Override
            public boolean isSettable(WebView view) {
                return view.prefHeight == null || !view.prefHeight.isBound();
            }
            @Override
            public StyleableProperty<Number> getStyleableProperty(WebView view) {
                return (StyleableProperty<Number>)view.prefHeightProperty();
            }
        };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;

        static {
            List<CssMetaData<? extends Styleable, ?>> styleables
                    = new ArrayList<CssMetaData<? extends Styleable, ?>>(Parent.getClassCssMetaData());
            styleables.add(CONTEXT_MENU_ENABLED);
            styleables.add(FONT_SMOOTHING_TYPE);
            styleables.add(ZOOM);
            styleables.add(FONT_SCALE);
            styleables.add(MIN_WIDTH);
            styleables.add(PREF_WIDTH);
            styleables.add(MAX_WIDTH);
            styleables.add(MIN_HEIGHT);
            styleables.add(PREF_HEIGHT);
            styleables.add(MAX_HEIGHT);
            STYLEABLES = Collections.unmodifiableList(styleables);
         }
    }

    /**
     * @return The CssMetaData associated with this class, which may include the
     * CssMetaData of its superclasses.
     * @since JavaFX 8.0
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * {@inheritDoc}
     * @since JavaFX 8.0
     */
    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

    // event handling

    private void handleStagePulse() {
        // The stage pulse occurs before the scene pulse.
        // Here the page content is updated before CSS/Layout/Sync pass
        // is initiated by the scene pulse. The update may
        // change the WebView children and, if so, the children should be
        // processed right away during the scene pulse.

        // The WebView node does not render its pending render queues
        // while it is invisible. Therefore, we should not schedule new
        // render queues while the WebView is invisible to prevent
        // the list of render queues from growing infinitely.
        // Also, if and when the WebView becomes invisible, the currently
        // pending render queues, if any, become obsolete and should be
        // discarded.

        boolean reallyVisible = NodeHelper.isTreeVisible(this)
                && getScene() != null
                && getScene().getWindow() != null
                && getScene().getWindow().isShowing();

        if (reallyVisible) {
            if (NodeHelper.isDirty(this, DirtyBits.WEBVIEW_VIEW)) {
                SceneHelper.setAllowPGAccess(true);
                final NGWebView peer = NodeHelper.getPeer(this);
                peer.update(); // creates new render queues
                SceneHelper.setAllowPGAccess(false);
            }
            setNativeVisible(handle, true);
        } else {
            setNativeVisible(handle, false);
        }
    }

    @Override protected ObservableList<Node> getChildren() {
        return super.getChildren();
    }

    // Node stuff
    {
        // To initialize the class helper at the beginning each constructor of this class
        WebViewHelper.initHelper(this);
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private NGNode doCreatePeer() {
        return new NGWebView();
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private BaseBounds doComputeGeomBounds(BaseBounds bounds, BaseTransform tx) {
        bounds.deriveWithNewBounds(0, 0, 0, (float) getWidth(), (float)getHeight(), 0);
        tx.transform(bounds, bounds);
        return bounds;
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private boolean doComputeContains(double localX, double localY) {
        // Note: Local bounds contain test is already done by the caller. (Node.contains()).
        return true;
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private void doUpdatePeer() {
        final NGWebView peer = NodeHelper.getPeer(this);

        if (NodeHelper.isDirty(this, DirtyBits.NODE_GEOMETRY)) {
            peer.resize((float)getWidth(), (float)getHeight());
        }
        if (NodeHelper.isDirty(this, DirtyBits.WEBVIEW_VIEW)) {
            peer.requestRender();
        }
    }

    private static Affine3D calculateNodeToSceneTransform(Node node) {
        final Affine3D transform = new Affine3D();
        do {
            transform.preConcatenate(NodeHelper.getLeafTransform(node));
            node = node.getParent();
        } while (node != null);

        return transform;
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private void doTransformsChanged() {
        Affine3D trans = calculateNodeToSceneTransform(this);
        _setTransform(handle,
                trans.getMxx(), trans.getMxy(), trans.getMxz(), trans.getMxt(),
                trans.getMyx(), trans.getMyy(), trans.getMyz(), trans.getMyt(),
                trans.getMzx(), trans.getMzy(), trans.getMzz(), trans.getMzt());
    }

    long getNativeHandle() {
        return handle;
    }

    private void setNativeVisible(long handle, boolean v) {
        if (nativeVisible != v) {
            nativeVisible = v;
            _setVisible(handle, v);
        }
    }

    // native callbacks
    private void notifyLoadStarted() {
        checkThreadAndRun(engine::notifyLoadStarted);
    }
    private void notifyLoadFinished(String loc, String content) {
        checkThreadAndRun(() -> engine.notifyLoadFinished(loc, content));
    }
    private void notifyLoadFailed() {
        checkThreadAndRun(engine::notifyLoadFailed);
    }
    private void notifyJavaCall(String arg) {
        checkThreadAndRun(() -> engine.notifyJavaCall(arg));
    }

    private static void checkThreadAndRun(Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }

    /* Inits native WebView and returns its pointer in the given array */
    private native void _initWebView(long[] nativeHandle);

    /* Sets width of the native WebView  */
    private native void _setWidth(long handle, double w);

    /* Sets height of the native WebView  */
    private native void _setHeight(long handle, double h);

    /* Sets visibility of the native WebView  */
    private native void _setVisible(long handle, boolean v);

    /* Removes the native WebView from scene */
    private native void _removeWebView(long handle);

    /* Applies transform on the native WebView  */
    private native void _setTransform(long handle,
            double mxx, double mxy, double mxz, double mxt,
            double myx, double myy, double myz, double myt,
            double mzx, double mzy, double mzz, double mzt);
}
