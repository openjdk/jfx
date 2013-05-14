package javafx.scene.web;

import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.ParentBuilder;
import javafx.scene.text.FontSmoothingType;
import javafx.util.Builder;
import javafx.util.Callback;

/**
 * The builder for the {@link WebView} class.
 *
 * @author Sergey Malenkov
 */
public final class WebViewBuilder
        extends ParentBuilder<WebViewBuilder>
        implements Builder<WebView> {

    /**
     * Creates new builder for the {@link WebView} class.
     *
     * @return the {@code WebViewBuilder} object
     */
    public static WebViewBuilder create() {
        return new WebViewBuilder();
    }

    /**
     * Creates an instance of the {@link WebView} class
     * based on the properties set on this builder.
     */
    public WebView build() {
        WebView x = new WebView();
        applyTo(x);
        return x;
    }

    /**
     * Applies initialized values to the properties of the {@link WebView} class.
     *
     * @param view  the {@link WebView} object to initialize
     */
    public void applyTo(WebView view) {
        super.applyTo(view);
        if (contextMenuEnabledSet) {
            view.setContextMenuEnabled(contextMenuEnabled);
        }
        if (fontScaleSet) {
            view.setFontScale(fontScale);
        }
        if (fontSmoothingTypeSet) {
            view.setFontSmoothingType(fontSmoothingType);
        }
        if (maxHeightSet) {
            view.setMaxHeight(maxHeight);
        }
        if (maxWidthSet) {
            view.setMaxWidth(maxWidth);
        }
        if (minHeightSet) {
            view.setMinHeight(minHeight);
        }
        if (minWidthSet) {
            view.setMinWidth(minWidth);
        }
        if (prefHeightSet) {
            view.setPrefHeight(prefHeight);
        }
        if (prefWidthSet) {
            view.setPrefWidth(prefWidth);
        }
        if (zoomSet) {
            view.setZoom(zoom);
        }
        if (engineBuilder != null) {
            engineBuilder.applyTo(view.getEngine());
        }
    }

    /**
     * Sets the {@link WebView#contextMenuEnabledProperty() contextMenuEnabled}
     * property for the instance constructed by this builder.
     *
     * @param value new value of the {@code contextMenuEnabled} property
     * @return this builder
     */
    public WebViewBuilder contextMenuEnabled(boolean value) {
        contextMenuEnabled = value;
        contextMenuEnabledSet = true;
        return this;
    }

    private boolean contextMenuEnabled;
    private boolean contextMenuEnabledSet;

    /**
     * Sets the {@link WebView#fontScaleProperty() fontScale}
     * property for the instance constructed by this builder.
     *
     * @param value  new value of the {@code fontScale} property
     * @return this builder
     */
    public WebViewBuilder fontScale(double value) {
        fontScale = value;
        fontScaleSet = true;
        return this;
    }

    private double fontScale;
    private boolean fontScaleSet;

    /**
     * Sets the {@link WebView#fontSmoothingTypeProperty() fontSmoothingType}
     * property for the instance constructed by this builder.
     *
     * @param value  new value of the {@code fontSmoothingType} property
     * @return this builder
     */
    public WebViewBuilder fontSmoothingType(FontSmoothingType value) {
        fontSmoothingType = value;
        fontSmoothingTypeSet = true;
        return this;
    }

    private FontSmoothingType fontSmoothingType;
    private boolean fontSmoothingTypeSet;

    /**
     * Sets the {@link WebView#maxHeightProperty() maxHeight}
     * property for the instance constructed by this builder.
     *
     * @param value  new value of the {@code maxHeight} property
     * @return this builder
     */
    public WebViewBuilder maxHeight(double value) {
        maxHeight = value;
        maxHeightSet = true;
        return this;
    }

    private double maxHeight;
    private boolean maxHeightSet;

    /**
     * Sets the {@link WebView#maxWidthProperty() maxWidth}
     * property for the instance constructed by this builder.
     *
     * @param value  new value of the {@code maxWidth} property
     * @return this builder
     */
    public WebViewBuilder maxWidth(double value) {
        maxWidth = value;
        maxWidthSet = true;
        return this;
    }

    private double maxWidth;
    private boolean maxWidthSet;

    /**
     * Sets the {@link WebView#minHeightProperty() minHeight}
     * property for the instance constructed by this builder.
     *
     * @param value  new value of the {@code minHeight} property
     * @return this builder
     */
    public WebViewBuilder minHeight(double value) {
        minHeight = value;
        minHeightSet = true;
        return this;
    }

    private double minHeight;
    private boolean minHeightSet;

    /**
     * Sets the {@link WebView#minWidthProperty() minWidth}
     * property for the instance constructed by this builder.
     *
     * @param value  new value of the {@code minWidth} property
     * @return this builder
     */
    public WebViewBuilder minWidth(double value) {
        minWidth = value;
        minWidthSet = true;
        return this;
    }

    private double minWidth;
    private boolean minWidthSet;

    /**
     * Sets the {@link WebView#prefHeightProperty() prefHeight}
     * property for the instance constructed by this builder.
     *
     * @param value  new value of the {@code prefHeight} property
     * @return this builder
     */
    public WebViewBuilder prefHeight(double value) {
        prefHeight = value;
        prefHeightSet = true;
        return this;
    }

    private double prefHeight;
    private boolean prefHeightSet;

    /**
     * Sets the {@link WebView#prefWidthProperty() prefWidth}
     * property for the instance constructed by this builder.
     *
     * @param value  new value of the {@code prefWidth} property
     * @return this builder
     */
    public WebViewBuilder prefWidth(double value) {
        prefWidth = value;
        prefWidthSet = true;
        return this;
    }

    private double prefWidth;
    private boolean prefWidthSet;

    /**
     * Sets the {@link WebView#zoomProperty() zoom}
     * property for the instance constructed by this builder.
     *
     * @param value  new value of the {@code zoom} property
     * @return this builder
     */
    public WebViewBuilder zoom(double value) {
        zoom = value;
        zoomSet = true;
        return this;
    }

    private double zoom;
    private boolean zoomSet;

    /**
     * Sets the {@link WebEngine#confirmHandlerProperty() confirmHandler}
     * property for the {@link WebView#getEngine() engine}
     * property of the instance constructed by this builder.
     *
     * @param value  new value of the {@code confirmHandler} property
     * @return this builder
     */
    public WebViewBuilder confirmHandler(Callback<String, Boolean> value) {
        engineBuilder().confirmHandler(value);
        return this;
    }

    /**
     * Sets the {@link WebEngine#createPopupHandlerProperty() createPopupHandler}
     * property for the {@link WebView#getEngine() engine}
     * property of the instance constructed by this builder.
     *
     * @param value  new value of the {@code createPopupHandler} property
     * @return this builder
     */
    public WebViewBuilder createPopupHandler(Callback<PopupFeatures, WebEngine> value) {
        engineBuilder().createPopupHandler(value);
        return this;
    }

    /**
     * Sets the {@link WebEngine#onAlertProperty() onAlert}
     * property for the {@link WebView#getEngine() engine}
     * property of the instance constructed by this builder.
     *
     * @param value  new value of the {@code onAlert} property
     * @return this builder
     */
    public WebViewBuilder onAlert(EventHandler<WebEvent<String>> value) {
        engineBuilder().onAlert(value);
        return this;
    }

    /**
     * Sets the {@link WebEngine#onResizedProperty() onResized}
     * property for the {@link WebView#getEngine() engine}
     * property of the instance constructed by this builder.
     *
     * @param value  new value of the {@code onResized} property
     * @return this builder
     */
    public WebViewBuilder onResized(EventHandler<WebEvent<Rectangle2D>> value) {
        engineBuilder().onResized(value);
        return this;
    }

    /**
     * Sets the {@link WebEngine#onStatusChangedProperty() onStatusChanged}
     * property for the {@link WebView#getEngine() engine}
     * property of the instance constructed by this builder.
     *
     * @param value  new value of the {@code onStatusChanged} property
     * @return this builder
     */
    public WebViewBuilder onStatusChanged(EventHandler<WebEvent<String>> value) {
        engineBuilder().onStatusChanged(value);
        return this;
    }

    /**
     * Sets the {@link WebEngine#onVisibilityChangedProperty() onVisibilityChanged}
     * property for the {@link WebView#getEngine() engine}
     * property of the instance constructed by this builder.
     *
     * @param value  new value of the {@code onVisibilityChanged} property
     * @return this builder
     */
    public WebViewBuilder onVisibilityChanged(EventHandler<WebEvent<Boolean>> value) {
        engineBuilder().onVisibilityChanged(value);
        return this;
    }

    /**
     * Sets the {@link WebEngine#promptHandlerProperty() promptHandler}
     * property for the {@link WebView#getEngine() engine}
     * property of the instance constructed by this builder.
     *
     * @param value  new value of the {@code promptHandler} property
     * @return this builder
     */
    public WebViewBuilder promptHandler(Callback<PromptData, String> value) {
        engineBuilder().promptHandler(value);
        return this;
    }

    /**
     * Sets the {@link WebEngine#locationProperty() location}
     * property for the {@link WebView#getEngine() engine}
     * property of the instance constructed by this builder.
     *
     * @param value  new value of the {@code location} property
     * @return this builder
     */
    public WebViewBuilder location(String value) {
        engineBuilder().location(value);
        return this;
    }

    private WebEngineBuilder engineBuilder;

    private WebEngineBuilder engineBuilder() {
        if (engineBuilder == null) {
            engineBuilder = WebEngineBuilder.create();
        }
        return engineBuilder;
    }
}
