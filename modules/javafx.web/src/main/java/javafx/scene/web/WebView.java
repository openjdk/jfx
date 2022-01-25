/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.java.scene.web.WebViewHelper;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.value.WritableValue;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.css.converter.ColorConverter;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import javafx.scene.text.FontSmoothingType;
import javafx.stage.Stage;
import javafx.stage.Window;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javafx.css.converter.BooleanConverter;
import javafx.css.converter.EnumConverter;
import javafx.css.converter.SizeConverter;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.PickRay;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.SceneHelper;
import com.sun.javafx.scene.input.PickResultChooser;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.sg.prism.web.NGWebView;
import com.sun.javafx.tk.TKPulseListener;
import com.sun.javafx.tk.Toolkit;
import com.sun.javafx.webkit.InputMethodClientImpl;
import com.sun.javafx.webkit.KeyCodeMap;
import com.sun.webkit.WebPage;
import com.sun.webkit.event.WCFocusEvent;
import com.sun.webkit.event.WCInputMethodEvent;
import com.sun.webkit.event.WCKeyEvent;
import com.sun.webkit.event.WCMouseEvent;
import com.sun.webkit.event.WCMouseWheelEvent;

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

    private static final Map<Object, Integer> ID_MAP = Map.ofEntries(
        Map.entry(MouseButton.NONE, WCMouseEvent.NOBUTTON),
        Map.entry(MouseButton.PRIMARY, WCMouseEvent.BUTTON1),
        Map.entry(MouseButton.MIDDLE, WCMouseEvent.BUTTON2),
        Map.entry(MouseButton.SECONDARY, WCMouseEvent.BUTTON3),

        Map.entry(MouseEvent.MOUSE_PRESSED, WCMouseEvent.MOUSE_PRESSED),
        Map.entry(MouseEvent.MOUSE_RELEASED, WCMouseEvent.MOUSE_RELEASED),
        Map.entry(MouseEvent.MOUSE_MOVED, WCMouseEvent.MOUSE_MOVED),
        Map.entry(MouseEvent.MOUSE_DRAGGED, WCMouseEvent.MOUSE_DRAGGED),

        Map.entry(KeyEvent.KEY_PRESSED, WCKeyEvent.KEY_PRESSED),
        Map.entry(KeyEvent.KEY_RELEASED, WCKeyEvent.KEY_RELEASED),
        Map.entry(KeyEvent.KEY_TYPED, WCKeyEvent.KEY_TYPED));

    private static final boolean DEFAULT_CONTEXT_MENU_ENABLED = true;
    private static final FontSmoothingType DEFAULT_FONT_SMOOTHING_TYPE = FontSmoothingType.LCD;
    private static final Color DEFAULT_PAGE_FILL = Color.WHITE;
    private static final double DEFAULT_ZOOM = 1.0;
    private static final double DEFAULT_FONT_SCALE = 1.0;
    private static final double DEFAULT_MIN_WIDTH = 0;
    private static final double DEFAULT_MIN_HEIGHT = 0;
    private static final double DEFAULT_PREF_WIDTH = 800;
    private static final double DEFAULT_PREF_HEIGHT = 600;
    private static final double DEFAULT_MAX_WIDTH = Double.MAX_VALUE;
    private static final double DEFAULT_MAX_HEIGHT = Double.MAX_VALUE;

    private final WebPage page;
    private final WebEngine engine;
    private volatile InputMethodClientImpl imClient;

    /**
     * The stage pulse listener registered with the toolkit.
     * This field guarantees that the listener will exist throughout
     * the whole lifetime of the WebView node. This field is necessary
     * because the toolkit references its stage pulse listeners weakly.
     */
    private final TKPulseListener stagePulseListener;

    /**
     * Returns the {@code WebEngine} object.
     * @return the WebEngine
     */
    public final WebEngine getEngine() {
        return engine;
    }

    private final ReadOnlyDoubleWrapper width = new ReadOnlyDoubleWrapper(this, "width");

    public final double getWidth() {
        return width.get();
    }

    /**
     * Width of this {@code WebView}.
     * @return the width property
     */
    public ReadOnlyDoubleProperty widthProperty() {
        return width.getReadOnlyProperty();
    }

    private final ReadOnlyDoubleWrapper height = new ReadOnlyDoubleWrapper(this, "height");

    public final double getHeight() {
        return height.get();
    }

    /**
     * Height of this {@code WebView}.
     * @return the height property
     */
    public ReadOnlyDoubleProperty heightProperty() {
        return height.getReadOnlyProperty();
    }

    /*
     * Zoom factor applied to the entire page contents.
     */
    private DoubleProperty zoom;

    public final void setZoom(double value) {
        WebEngine.checkThread();
        zoomProperty().set(value);
    }

    public final double getZoom() {
        return (this.zoom != null)
                ? this.zoom.get()
                : DEFAULT_ZOOM;
    }

    /**
     * The current zoom factor applied to the entire page contents.
     *
     * @return the zoom property
     * @defaultValue 1.0
     *
     * @since JavaFX 8.0
     */
    public final DoubleProperty zoomProperty() {
        if (zoom == null) {
            zoom = new StyleableDoubleProperty(DEFAULT_ZOOM) {
                @Override public void invalidated() {
                    Toolkit.getToolkit().checkFxUserThread();
                    page.setZoomFactor((float) get(), false);
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
                    page.setZoomFactor((float)get(), true);
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

    {
        // To initialize the class helper at the begining each constructor of this class
        WebViewHelper.initHelper(this);
    }
    /**
     * Creates a {@code WebView} object.
     */
    public WebView() {
        setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        getStyleClass().add("web-view");
        engine = new WebEngine();
        engine.setView(this);
        page = engine.getPage();
        page.setFontSmoothingType(DEFAULT_FONT_SMOOTHING_TYPE.ordinal());
        page.setBackgroundColor(DEFAULT_PAGE_FILL);

        registerEventHandlers();
        stagePulseListener = () -> {
            handleStagePulse();
        };
        focusedProperty().addListener((ov, t, t1) -> {
            if (page != null) {
                // Traversal direction is not currently available in FX.
                WCFocusEvent focusEvent = new WCFocusEvent(
                    isFocused() ? WCFocusEvent.FOCUS_GAINED
                            : WCFocusEvent.FOCUS_LOST,
                WCFocusEvent.UNKNOWN);
                page.dispatchFocusEvent(focusEvent);
            }
        });
        setFocusTraversable(true);
        Toolkit.getToolkit().addStageTkPulseListener(stagePulseListener);
    }

    // Resizing support. Allows arbitrary growing and shrinking.
    // Designed after javafx.scene.control.Control

    @Override public boolean isResizable() {
        return true;
    }

    @Override public void resize(double width, double height) {
        if ((width != this.width.get()) || (height != this.height.get())) {
            this.width.set(width);
            this.height.set(height);
            NodeHelper.markDirty(this, DirtyBits.NODE_GEOMETRY);
            NodeHelper.geomChanged(this);
        }
    }

    /**
     * Called during layout to determine the minimum width for this node.
     *
     * @return the minimum width that this node should be resized to during layout
     */
    @Override public final double minWidth(double height) {
        final double result = getMinWidth();
        return Double.isNaN(result) || result < 0 ? 0 : result;
    }

    /**
     * Called during layout to determine the minimum height for this node.
     *
     * @return the minimum height that this node should be resized to during layout
     */
    @Override public final double minHeight(double width) {
        final double result = getMinHeight();
        return Double.isNaN(result) || result < 0 ? 0 : result;
    }


    /**
     * Called during layout to determine the preferred width for this node.
     *
     * @return the preferred width that this node should be resized to during layout
     */
    @Override public final double prefWidth(double height) {
        final double result = getPrefWidth();
        return Double.isNaN(result) || result < 0 ? 0 : result;
    }

    /**
     * Called during layout to determine the preferred height for this node.
     *
     * @return the preferred height that this node should be resized to during layout
     */
    @Override public final double prefHeight(double width) {
        final double result = getPrefHeight();
        return Double.isNaN(result) || result < 0 ? 0 : result;
    }
    /**
     * Called during layout to determine the maximum width for this node.
     *
     * @return the maximum width that this node should be resized to during layout
     */
    @Override public final double maxWidth(double height) {
        final double result = getMaxWidth();
        return Double.isNaN(result) || result < 0 ? 0 : result;
    }

    /**
     * Called during layout to determine the maximum height for this node.
     *
     * @return the maximum height that this node should be resized to during layout
     */
    @Override public final double maxHeight(double width) {
        final double result = getMaxHeight();
        return Double.isNaN(result) || result < 0 ? 0 : result;
    }

    /**
     * Minimum width property.
     * @return the minWidth property
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

    public final void setMinWidth(double value) {
        minWidthProperty().set(value);
    }

    public final double getMinWidth() {
        return (this.minWidth != null)
                ? this.minWidth.get()
                : DEFAULT_MIN_WIDTH;
    }

    /**
     * Minimum height property.
     * @return the minHeight property
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

    public final void setMinHeight(double value) {
        minHeightProperty().set(value);
    }

    public final double getMinHeight() {
        return (this.minHeight != null)
                ? this.minHeight.get()
                : DEFAULT_MIN_HEIGHT;
    }

    /**
     * Convenience method for setting minimum width and height.
     * @param minWidth the minimum width
     * @param minHeight the minimum height
     */
    public void setMinSize(double minWidth, double minHeight) {
        setMinWidth(minWidth);
        setMinHeight(minHeight);
    }

    /**
     * Preferred width property.
     * @return the prefWidth property
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

    public final void setPrefWidth(double value) {
        prefWidthProperty().set(value);
    }

    public final double getPrefWidth() {
        return (this.prefWidth != null)
                ? this.prefWidth.get()
                : DEFAULT_PREF_WIDTH;
    }

    /**
     * Preferred height property.
     * @return the prefHeight property
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

    public final void setPrefHeight(double value) {
        prefHeightProperty().set(value);
    }

    public final double getPrefHeight() {
        return (this.prefHeight != null)
                ? this.prefHeight.get()
                : DEFAULT_PREF_HEIGHT;
    }

    /**
     * Convenience method for setting preferred width and height.
     * @param prefWidth the preferred width
     * @param prefHeight the preferred height
     */
    public void setPrefSize(double prefWidth, double prefHeight) {
        setPrefWidth(prefWidth);
        setPrefHeight(prefHeight);
    }

    /**
     * Maximum width property.
     * @return the maxWidth property
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

    public final void setMaxWidth(double value) {
        maxWidthProperty().set(value);
    }

    public final double getMaxWidth() {
        return (this.maxWidth != null)
                ? this.maxWidth.get()
                : DEFAULT_MAX_WIDTH;
    }

    /**
     * Maximum height property.
     * @return the maxHeight property
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

    public final void setMaxHeight(double value) {
        maxHeightProperty().set(value);
    }

    public final double getMaxHeight() {
        return (this.maxHeight != null)
                ? this.maxHeight.get()
                : DEFAULT_MAX_HEIGHT;
    }

    /**
     * Convenience method for setting maximum width and height.
     * @param maxWidth the maximum width
     * @param maxHeight the maximum height
     */
    public void setMaxSize(double maxWidth, double maxHeight) {
        setMaxWidth(maxWidth);
        setMaxHeight(maxHeight);
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
                    page.setFontSmoothingType(get().ordinal());
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
     * Specifies the background color of the web page.
     *
     * <p>With this property, the {@code WebView} control's background
     * can have any solid color, including some or complete
     * level of transparency.
     * However, if the HTML content being loaded sets its own
     * background color, that color will take precedence.
     *
     * @defaultValue {@code Color.WHITE}
     *
     * @since 18
     */
    private ObjectProperty<Color> pageFill;

    public final void setPageFill(Color value) {
        pageFillProperty().set(value);
    }

    public final Color getPageFill() {
        return pageFill == null ? DEFAULT_PAGE_FILL : pageFill.get();
    }

    public final ObjectProperty<Color> pageFillProperty() {
        if (pageFill == null) {
            pageFill = new StyleableObjectProperty<>(DEFAULT_PAGE_FILL) {

                @Override
                protected void invalidated() {
                    Toolkit.getToolkit().checkFxUserThread();
                    Color color = get();
                    page.setBackgroundColor(color);
                }

                @Override
                public CssMetaData<WebView,Color> getCssMetaData() {
                    return WebView.StyleableProperties.PAGE_FILL;
                }

                @Override
                public Object getBean() {
                    return WebView.this;
                }

                @Override
                public String getName() {
                    return "pageFill";
                }
            };
        }
        return pageFill;
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
                    page.setContextMenuEnabled(get());
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

        private static final CssMetaData<WebView, Color> PAGE_FILL =
                new CssMetaData<>("-fx-page-fill",
                        ColorConverter.getInstance(), DEFAULT_PAGE_FILL) {

                    @Override
                    public boolean isSettable(WebView n) {
                        return n.pageFill == null || !n.pageFill.isBound();
                    }

                    @Override
                    public StyleableProperty<Color> getStyleableProperty(WebView n) {
                        return (StyleableProperty<Color>)(WritableValue<Color>)n.pageFillProperty();
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
            styleables.add(PAGE_FILL);
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
     * Gets the {@code CssMetaData} associated with this class, which may include the
     * {@code CssMetaData} of its superclasses.
     * @return the {@code CssMetaData}
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

    // To handle stage pulse we need to know if currently webview and
    // tree is visible or not
    private boolean isTreeReallyVisible() {
        if (getScene() == null) {
            return false;
        }

        final Window window = getScene().getWindow();

        if (window == null) {
            return false;
        }

        boolean iconified = (window instanceof Stage) ? ((Stage)window).isIconified() : false;

        return NodeHelper.isTreeVisible(this)
               && window.isShowing()
               && window.getWidth() > 0
               && window.getHeight() > 0
               && !iconified;
    }

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

        if (page == null) return;

        boolean reallyVisible = isTreeReallyVisible();

        if (reallyVisible) {
            if (page.isDirty()) {
                SceneHelper.setAllowPGAccess(true);
                final NGWebView peer = NodeHelper.getPeer(this);
                peer.update(); // creates new render queues
                if (page.isRepaintPending()) {
                    NodeHelper.markDirty(this, DirtyBits.WEBVIEW_VIEW);
                }
                SceneHelper.setAllowPGAccess(false);
            }
        } else {
            page.dropRenderFrames();
        }
    }

    private void processMouseEvent(MouseEvent ev) {
        if (page == null) {
            return;
        }

        // RT-24511
        EventType<? extends MouseEvent> type = ev.getEventType();
        double x = ev.getX();
        double y = ev.getY();
        double screenX = ev.getScreenX();
        double screenY = ev.getScreenY();
        if (type == MouseEvent.MOUSE_EXITED) {
            type = MouseEvent.MOUSE_MOVED;
            x = Short.MIN_VALUE;
            y = Short.MIN_VALUE;
            Point2D screenPoint = localToScreen(x, y);
            if (screenPoint == null) {
                return;
            }
            screenX = screenPoint.getX();
            screenY = screenPoint.getY();
        }

        final Integer id = ID_MAP.get(type);
        final Integer button = ID_MAP.get(ev.getButton());
        if (id == null || button == null) {
            // not supported by webkit
            return;
        }
        WCMouseEvent mouseEvent =
                new WCMouseEvent(id, button,
                    ev.getClickCount(), (int) x, (int) y,
                    (int) screenX, (int) screenY,
                    System.currentTimeMillis(),
                    ev.isShiftDown(), ev.isControlDown(), ev.isAltDown(),
                    ev.isMetaDown(), ev.isPopupTrigger());
        page.dispatchMouseEvent(mouseEvent);
        ev.consume();
    }

    private void processScrollEvent(ScrollEvent ev) {
        if (page == null) {
            return;
        }
        double dx = - ev.getDeltaX() * getFontScale() * getScaleX();
        double dy = - ev.getDeltaY() * getFontScale() * getScaleY();
        WCMouseWheelEvent wheelEvent =
                new WCMouseWheelEvent((int)ev.getX(), (int)ev.getY(),
                    (int)ev.getScreenX(), (int)ev.getScreenY(),
                    System.currentTimeMillis(),
                    ev.isShiftDown(), ev.isControlDown(), ev.isAltDown(),
                    ev.isMetaDown(), (float)dx, (float)dy);
        page.dispatchMouseWheelEvent(wheelEvent);
        ev.consume();
    }

    private void processKeyEvent(KeyEvent ev) {
        if (page == null) return;

        String text = null;
        String keyIdentifier = null;
        int windowsVirtualKeyCode = 0;
        if(ev.getEventType() == KeyEvent.KEY_TYPED) {
            text = ev.getCharacter();
        } else {
            KeyCodeMap.Entry keyCodeEntry = KeyCodeMap.lookup(ev.getCode());
            keyIdentifier = keyCodeEntry.getKeyIdentifier();
            windowsVirtualKeyCode = keyCodeEntry.getWindowsVirtualKeyCode();
        }

        WCKeyEvent keyEvent = new WCKeyEvent(
                ID_MAP.get(ev.getEventType()),
                text,
                keyIdentifier,
                windowsVirtualKeyCode,
                ev.isShiftDown(), ev.isControlDown(),
                ev.isAltDown(), ev.isMetaDown(), System.currentTimeMillis());
        if (page.dispatchKeyEvent(keyEvent)) {
            ev.consume();
        }
    }

    private InputMethodClientImpl getInputMethodClient() {
         if (imClient == null) {
             synchronized (this) {
                 if (imClient == null) {
                     imClient = new InputMethodClientImpl(this, page);
                 }
             }
         }
         return imClient;
    }

    private void processInputMethodEvent(InputMethodEvent ie) {
        if (page == null) {
            return;
        }

        if (!getInputMethodClient().getInputMethodState()) {
            ie.consume();
            return;
        }

        WCInputMethodEvent imEvent = InputMethodClientImpl.convertToWCInputMethodEvent(ie);
        if (page.dispatchInputMethodEvent(imEvent)) {
            ie.consume();
            return;
        }
    }

    private static final int WK_DND_ACTION_NONE = 0x0;
    private static final int WK_DND_ACTION_COPY = 0x1;
    private static final int WK_DND_ACTION_MOVE = 0x2;
    private static final int WK_DND_ACTION_LINK = 0x40000000;

    private static int getWKDndEventType(EventType  et) {
        int commandId = 0;
        if (et == DragEvent.DRAG_ENTERED)
            commandId = WebPage.DND_DST_ENTER;
        else if (et == DragEvent.DRAG_EXITED)
            commandId = WebPage.DND_DST_EXIT;
        else if (et == DragEvent.DRAG_OVER)
            commandId = WebPage.DND_DST_OVER;
        else if (et == DragEvent.DRAG_DROPPED)
            commandId = WebPage.DND_DST_DROP;
        return commandId;
    }

    private static int getWKDndAction(TransferMode... tms) {
        int dndActionId = WK_DND_ACTION_NONE;
        for (TransferMode tm : tms) {
           if (tm == TransferMode.COPY)
               dndActionId |=  WK_DND_ACTION_COPY;
           else if (tm == TransferMode.MOVE)
               dndActionId |=  WK_DND_ACTION_MOVE;
           else if (tm == TransferMode.LINK)
               dndActionId |=  WK_DND_ACTION_LINK;
        }
        return dndActionId;
    }

    private static TransferMode[] getFXDndAction(int wkDndAction) {
        LinkedList<TransferMode> tms = new LinkedList<TransferMode>();
        if ((wkDndAction & WK_DND_ACTION_COPY) != 0)
            tms.add(TransferMode.COPY);
        if ((wkDndAction & WK_DND_ACTION_MOVE) != 0)
            tms.add(TransferMode.MOVE);
        if ((wkDndAction & WK_DND_ACTION_LINK) != 0)
            tms.add(TransferMode.LINK);
        return tms.toArray(new TransferMode[0]);
    }

    private LinkedList<String> mimes;
    private LinkedList<String> values;

    private void registerEventHandlers() {
        addEventHandler(KeyEvent.ANY,
                event -> {
                    processKeyEvent(event);
                });
        addEventHandler(MouseEvent.ANY,
                event -> {
                    processMouseEvent(event);
                    if (event.isDragDetect() && !page.isDragConfirmed()) {
                        //postpone drag recognition:
                        //Webkit cannot resolve here is it a drag
                        //or selection.
                        event.setDragDetect(false);
                    }
                });
        addEventHandler(ScrollEvent.SCROLL,
                event -> {
                    processScrollEvent(event);
                });
        setOnInputMethodTextChanged(
                event -> {
                    processInputMethodEvent(event);
                });

        //Drop target implementation:
        EventHandler<DragEvent> destHandler = event -> {
            try {
                Dragboard db = event.getDragboard();
                if (mimes == null || values == null) {
                    mimes = new LinkedList<>();
                    values = new LinkedList<>();
                    for (DataFormat df : db.getContentTypes()) {
                        //TODO: extend to non-string serialized values.
                        //Please, look at the native code.
                        Object content = db.getContent(df);
                        if (content != null) {
                            for (String mime : df.getIdentifiers()) {
                                mimes.add(mime);
                                values.add(content.toString());
                            }
                        }
                    }
                }
                if (!mimes.isEmpty()) {
                    int wkDndEventType = getWKDndEventType(event.getEventType());
                    int wkDndAction = page.dispatchDragOperation(
                        wkDndEventType,
                        mimes.toArray(new String[0]), values.toArray(new String[0]),
                        (int)event.getX(), (int)event.getY(),
                        (int)event.getScreenX(), (int)event.getScreenY(),
                        getWKDndAction(db.getTransferModes().toArray(new TransferMode[0])));

                    //we cannot accept nothing on drop (we skip FX exception)
                    if (!(wkDndEventType == WebPage.DND_DST_DROP && wkDndAction == WK_DND_ACTION_NONE)) {
                        event.acceptTransferModes(getFXDndAction(wkDndAction));
                    }
                    event.consume();
                }
            } catch (SecurityException ex) {
                // Just ignore the exception
                //ex.printStackTrace();
            }
        };
        setOnDragEntered(destHandler);
        setOnDragExited(destHandler);
        setOnDragOver(destHandler);
        setOnDragDropped(destHandler);

        //Drag source implementation:
        setOnDragDetected(event -> {
               if (page.isDragConfirmed()) {
                   mimes = null;
                   values = null;
                   page.confirmStartDrag();
                   event.consume();
               }
           });
        setOnDragDone(event -> {
                mimes = null;
                values = null;
                page.dispatchDragOperation(
                    WebPage.DND_SRC_DROP,
                    null, null,
                    (int)event.getX(), (int)event.getY(),
                    (int)event.getScreenX(), (int)event.getScreenY(),
                    getWKDndAction(event.getAcceptedTransferMode()));
                event.consume();
            });

        setInputMethodRequests(getInputMethodClient());
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private void doPickNodeLocal(PickRay pickRay, PickResultChooser result) {
        NodeHelper.intersects(this, pickRay, result);
    }

    @Override protected ObservableList<Node> getChildren() {
        return super.getChildren();
    }

    // Node stuff

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
    private void doTransformsChanged() {
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

        if (NodeHelper.isDirty(this, DirtyBits.NODE_CONTENTS)) {
            peer.setPage(page);
        }
        if (NodeHelper.isDirty(this, DirtyBits.NODE_GEOMETRY)) {
            peer.resize((float)getWidth(), (float)getHeight());
        }
        if (NodeHelper.isDirty(this, DirtyBits.WEBVIEW_VIEW)) {
            peer.requestRender();
        }
    }

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
            public void doPickNodeLocal(Node node, PickRay localPickRay,
                    PickResultChooser result) {
                ((WebView) node).doPickNodeLocal(localPickRay, result);
            }
        });
    }
}
