/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package javafx.scene.web;

import com.sun.javafx.beans.annotations.NoBuilder;
import com.sun.javafx.css.converters.BooleanConverter;
import com.sun.javafx.css.converters.EnumConverter;
import com.sun.javafx.css.converters.SizeConverter;
import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.PickRay;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.scene.input.PickResultChooser;
import javafx.css.Styleable;
import com.sun.javafx.sg.PGNode;
import com.sun.javafx.sg.prism.NGWebView;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.StyleableBooleanProperty;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TransferMode;
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
 */
@NoBuilder
final public class WebView extends Parent {

    private static final Map<Object, Integer> idMap = new HashMap<Object, Integer>();

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

        registerEventHandlers();
        stagePulseListener = new TKPulseListener() {
            @Override public void pulse() {
                handleStagePulse();
            }
        };
        focusedProperty().addListener(new ChangeListener<Boolean>() {

            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                if (page != null) {
                    // Traversal direction is not currently available in FX.
                    WCFocusEvent focusEvent = new WCFocusEvent(
                        isFocused() ? WCFocusEvent.FOCUS_GAINED
                                : WCFocusEvent.FOCUS_LOST,
                    WCFocusEvent.UNKNOWN);
                    page.dispatchFocusEvent(focusEvent);
                }
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
            impl_markDirty(DirtyBits.NODE_GEOMETRY);
            impl_geomChanged();
        }
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
     * @since 2.2
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
     * Specifies whether context menu is enabled.
     *
     * @defaultValue true
     * @since 2.2
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
     * CssMetaData of its super classes.
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * {@inheritDoc}
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

        if (page == null) return;

        boolean reallyVisible = impl_isTreeVisible()
                && getScene() != null
                && getScene().getWindow() != null
                && getScene().getWindow().isShowing();

        if (reallyVisible) {
            if (page.isDirty()) {
                Scene.impl_setAllowPGAccess(true);

                getNGWebView().update(); // creates new render queues
                if (page.isRepaintPending()) {
                    impl_markDirty(DirtyBits.WEBVIEW_VIEW);
                }
                Scene.impl_setAllowPGAccess(false);
            }
        } else {
            page.dropRenderFrames();
        }
    }

    private void processMouseEvent(MouseEvent ev) {
        if (page == null) {
            return;
        }
        final Integer id = idMap.get(ev.getEventType());
        if (id == null) {
            // not supported by webkit
            return;
        }
        WCMouseEvent mouseEvent =
                new WCMouseEvent(id, idMap.get(ev.getButton()),
                    ev.getClickCount(), (int)ev.getX(), (int)ev.getY(),
                    (int)ev.getScreenX(), (int)ev.getScreenY(),
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
                idMap.get(ev.getEventType()),
                text,
                keyIdentifier,
                windowsVirtualKeyCode,
                ev.isShiftDown(), ev.isControlDown(),
                ev.isAltDown(), ev.isMetaDown());
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

    private void registerEventHandlers() {
        addEventHandler(KeyEvent.ANY,
            new EventHandler<KeyEvent>() {
                @Override public void handle(final KeyEvent event) {
                    processKeyEvent(event);
                }
            });
        addEventHandler(MouseEvent.ANY,
            new EventHandler<MouseEvent>() {
                @Override public void handle(final MouseEvent event) {
                    processMouseEvent(event);
                    if (event.isDragDetect() && !page.isDragConfirmed()) {
                        //postpone drag recognition:
                        //Webkit cannot resolve here is it a drag
                        //or selection.
                        event.setDragDetect(false);
                    }
                }
            });
        addEventHandler(ScrollEvent.SCROLL,
            new EventHandler<ScrollEvent>() {
                @Override public void handle(final ScrollEvent event) {
                    processScrollEvent(event);
                }
            });
        setOnInputMethodTextChanged(
            new EventHandler<InputMethodEvent>() {
                @Override public void handle(final InputMethodEvent event) {
                    processInputMethodEvent(event);
                }
            });

        //Drop target implementation:
        EventHandler<DragEvent> destHandler = new EventHandler <DragEvent>() {
            @Override public void handle(DragEvent event) {
                Dragboard db = event.getDragboard();
                LinkedList<String> mimes = new LinkedList<String>();
                LinkedList<String> values = new LinkedList<String>();
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
                if (!mimes.isEmpty()) {
                    int wkDndEventType = getWKDndEventType(event.getEventType());
                    int wkDndAction = page.dispatchDragOperation(
                        wkDndEventType,
                        mimes.toArray(new String[0]), values.toArray(new String[0]),
                        (int)event.getX(), (int)event.getY(),
                        (int)event.getScreenX(), (int)event.getScreenY(),
                        getWKDndAction(db.getTransferModes().toArray(new TransferMode[0])));

                    //we cannot accept nothing on drop (we skip FX exception)
                    if ( !(wkDndEventType == WebPage.DND_DST_DROP && wkDndAction == WK_DND_ACTION_NONE) )
                        event.acceptTransferModes(getFXDndAction(wkDndAction));
                    event.consume();
                }
            }
        };
        setOnDragEntered(destHandler);
        setOnDragExited(destHandler);
        setOnDragOver(destHandler);
        setOnDragDropped(destHandler);

        //Drag source implementation:
        setOnDragDetected( new EventHandler<MouseEvent>() {
             @Override public void handle(MouseEvent event) {
                    if (page.isDragConfirmed()) {
                        page.confirmStartDrag();
                        event.consume();
                    }
                }
            });
        setOnDragDone( new EventHandler<DragEvent>() {
            @Override public void handle(DragEvent event) {
                    page.dispatchDragOperation(
                        WebPage.DND_SRC_DROP,
                        null, null,
                        (int)event.getX(), (int)event.getY(),
                        (int)event.getScreenX(), (int)event.getScreenY(),
                        getWKDndAction(event.getAcceptedTransferMode()));
                    event.consume();
                }
            });

        setInputMethodRequests(getInputMethodClient());
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected void impl_pickNodeLocal(PickRay pickRay, PickResultChooser result) {
        impl_intersects(pickRay, result);
    }

    @Override protected ObservableList<Node> getChildren() {
        return super.getChildren();
    }

    // Node stuff

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected PGNode impl_createPGNode() {
        return new NGWebView();
    }

    private NGWebView getNGWebView() {
        return (NGWebView)impl_getPGNode();
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override public BaseBounds impl_computeGeomBounds(BaseBounds bounds, BaseTransform tx) {
        bounds.deriveWithNewBounds(0, 0, 0, (float) getWidth(), (float)getHeight(), 0);
        tx.transform(bounds, bounds);
        return bounds;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override protected boolean impl_computeContains(double localX, double localY) {
        // Note: Local bounds contain test is already done by the caller. (Node.contains()).
        return true;
    }

    /**
     * @treatAsPrivate implementation detail
     * @deprecated This is an internal API that is not intended for use and will be removed in the next version
     */
    @Deprecated
    @Override public void impl_updatePG() {
        super.impl_updatePG();
        NGWebView peer = getNGWebView();

        if (impl_isDirty(DirtyBits.NODE_CONTENTS)) {
            peer.setPage(page);
        }
        if (impl_isDirty(DirtyBits.NODE_GEOMETRY)) {
            peer.resize((float)getWidth(), (float)getHeight());
        }
        if (impl_isDirty(DirtyBits.WEBVIEW_VIEW)) {
            peer.requestRender();
        }
    }

    static {
        idMap.put(MouseButton.NONE, WCMouseEvent.NOBUTTON);
        idMap.put(MouseButton.PRIMARY, WCMouseEvent.BUTTON1);
        idMap.put(MouseButton.MIDDLE, WCMouseEvent.BUTTON2);
        idMap.put(MouseButton.SECONDARY, WCMouseEvent.BUTTON3);

        idMap.put(MouseEvent.MOUSE_PRESSED, WCMouseEvent.MOUSE_PRESSED);
        idMap.put(MouseEvent.MOUSE_RELEASED, WCMouseEvent.MOUSE_RELEASED);
        idMap.put(MouseEvent.MOUSE_MOVED, WCMouseEvent.MOUSE_MOVED);
        idMap.put(MouseEvent.MOUSE_DRAGGED, WCMouseEvent.MOUSE_DRAGGED);

        idMap.put(KeyEvent.KEY_PRESSED, WCKeyEvent.KEY_PRESSED);
        idMap.put(KeyEvent.KEY_RELEASED, WCKeyEvent.KEY_RELEASED);
        idMap.put(KeyEvent.KEY_TYPED, WCKeyEvent.KEY_TYPED);
    }
}
