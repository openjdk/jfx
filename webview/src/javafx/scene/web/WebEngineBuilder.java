package javafx.scene.web;

import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.util.Builder;
import javafx.util.Callback;

/**
 * The builder for the {@link WebEngine} class.
 *
 * @author Sergey Malenkov
 */
public final class WebEngineBuilder
        implements Builder<WebEngine> {

    /**
     * Creates new builder for the {@link WebEngine} class.
     *
     * @return the {@code WebEngineBuilder} object
     */
    public static WebEngineBuilder create() {
        return new WebEngineBuilder();
    }

    /**
     * Creates an instance of the {@link WebEngine} class
     * based on the properties set on this builder.
     */
    public WebEngine build() {
        WebEngine engine = new WebEngine();
        applyTo(engine);
        return engine;
    }

    /**
     * Applies initialized values to the properties of the {@link WebEngine} class.
     *
     * @param engine  the {@link WebEngine} object to initialize
     */
    public void applyTo(WebEngine engine) {
        if (confirmHandlerSet) {
            engine.setConfirmHandler(confirmHandler);
        }
        if (createPopupHandlerSet) {
            engine.setCreatePopupHandler(createPopupHandler);
        }
        if (javaScriptEnabledSet) {
            engine.setJavaScriptEnabled(javaScriptEnabled);
        }
        if (onAlertSet) {
            engine.setOnAlert(onAlert);
        }
        if (onResizedSet) {
            engine.setOnResized(onResized);
        }
        if (onStatusChangedSet) {
            engine.setOnStatusChanged(onStatusChanged);
        }
        if (onVisibilityChangedSet) {
            engine.setOnVisibilityChanged(onVisibilityChanged);
        }
        if (promptHandlerSet) {
            engine.setPromptHandler(promptHandler);
        }
        if (locationSet) {
            engine.load(location);
        }
        if (userAgentSet) {
            engine.setUserAgent(userAgent);
        }
        if (userStyleSheetLocationSet) {
            engine.setUserStyleSheetLocation(userStyleSheetLocation);
        }
    }

    /**
     * Sets the {@link WebEngine#confirmHandlerProperty() confirmHandler}
     * property for the instance constructed by this builder.
     *
     * @param value  new value of the {@code confirmHandler} property
     * @return this builder
     */
    public WebEngineBuilder confirmHandler(Callback<String, Boolean> value) {
        confirmHandler = value;
        confirmHandlerSet = true;
        return this;
    }

    private Callback<String, Boolean> confirmHandler;
    private boolean confirmHandlerSet;

    /**
     * Sets the {@link WebEngine#createPopupHandlerProperty() createPopupHandler}
     * property for the instance constructed by this builder.
     *
     * @param value  new value of the {@code createPopupHandler} property
     * @return this builder
     */
    public WebEngineBuilder createPopupHandler(Callback<PopupFeatures, WebEngine> value) {
        createPopupHandler = value;
        createPopupHandlerSet = true;
        return this;
    }

    private Callback<PopupFeatures, WebEngine> createPopupHandler;
    private boolean createPopupHandlerSet;

    /**
     * Sets the {@link WebEngine#javaScriptEnabledProperty() javaScriptEnabled}
     * property for the instance constructed by this builder.
     *
     * @param value  new value of the {@code javaScriptEnabled} property
     * @return this builder
     */
    public WebEngineBuilder javaScriptEnabled(boolean value) {
        javaScriptEnabled = value;
        javaScriptEnabledSet = true;
        return this;
    }

    private boolean javaScriptEnabled;
    private boolean javaScriptEnabledSet;

    /**
     * Sets the {@link WebEngine#onAlertProperty() onAlert}
     * property for the instance constructed by this builder.
     *
     * @param value  new value of the {@code onAlert} property
     * @return this builder
     */
    public WebEngineBuilder onAlert(EventHandler<WebEvent<String>> value) {
        onAlert = value;
        onAlertSet = true;
        return this;
    }

    private EventHandler<WebEvent<String>> onAlert;
    private boolean onAlertSet;

    /**
     * Sets the {@link WebEngine#onResizedProperty() onResized}
     * property for the instance constructed by this builder.
     *
     * @param value  new value of the {@code onResized} property
     * @return this builder
     */
    public WebEngineBuilder onResized(EventHandler<WebEvent<Rectangle2D>> value) {
        onResized = value;
        onResizedSet = true;
        return this;
    }

    private EventHandler<WebEvent<Rectangle2D>> onResized;
    private boolean onResizedSet;

    /**
     * Sets the {@link WebEngine#onStatusChangedProperty() onStatusChanged}
     * property for the instance constructed by this builder.
     *
     * @param value  new value of the {@code onStatusChanged} property
     * @return this builder
     */
    public WebEngineBuilder onStatusChanged(EventHandler<WebEvent<String>> value) {
        onStatusChanged = value;
        onStatusChangedSet = true;
        return this;
    }

    private EventHandler<WebEvent<String>> onStatusChanged;
    private boolean onStatusChangedSet;

    /**
     * Sets the {@link WebEngine#onVisibilityChangedProperty() onVisibilityChanged}
     * property for the instance constructed by this builder.
     *
     * @param value  new value of the {@code onVisibilityChanged} property
     * @return this builder
     */
    public WebEngineBuilder onVisibilityChanged(EventHandler<WebEvent<Boolean>> value) {
        onVisibilityChanged = value;
        onVisibilityChangedSet = true;
        return this;
    }

    private EventHandler<WebEvent<Boolean>> onVisibilityChanged;
    private boolean onVisibilityChangedSet;

    /**
     * Sets the {@link WebEngine#promptHandlerProperty() promptHandler}
     * property for the instance constructed by this builder.
     *
     * @param value  new value of the {@code promptHandler} property
     * @return this builder
     */
    public WebEngineBuilder promptHandler(Callback<PromptData, String> value) {
        promptHandler = value;
        promptHandlerSet = true;
        return this;
    }

    private Callback<PromptData, String> promptHandler;
    private boolean promptHandlerSet;

    /**
     * Sets the {@link WebEngine#locationProperty() location}
     * property for the instance constructed by this builder.
     *
     * @param value  new value of the {@code location} property
     * @return this builder
     */
    public WebEngineBuilder location(String value) {
        location = value;
        locationSet = true;
        return this;
    }

    private String location;
    private boolean locationSet;

    /**
     * Sets the {@link WebEngine#userAgentProperty() userAgent}
     * property for the instance constructed by this builder.
     *
     * @param value  new value of the {@code userAgent} property
     * @return this builder
     */
    public WebEngineBuilder userAgent(String value) {
        userAgent = value;
        userAgentSet = true;
        return this;
    }

    private String userAgent;
    private boolean userAgentSet;

    /**
     * Sets the {@link WebEngine#userStyleSheetLocationProperty() userStyleSheetLocation}
     * property for the instance constructed by this builder.
     *
     * @param value  new value of the {@code userStyleSheetLocation} property
     * @return this builder
     */
    public WebEngineBuilder userStyleSheetLocation(String value) {
        userStyleSheetLocation = value;
        userStyleSheetLocationSet = true;
        return this;
    }

    private String userStyleSheetLocation;
    private boolean userStyleSheetLocationSet;
}
