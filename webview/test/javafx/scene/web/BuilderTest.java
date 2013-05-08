/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package javafx.scene.web;

import javafx.beans.property.ReadOnlyProperty;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.text.FontSmoothingType;
import javafx.util.Callback;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class BuilderTest extends TestBase {
    
    // These test values should differ from default ones
    
    // WebEngine
    private final static Callback<String, Boolean> CONFIRM_HANDLER =
            new Callback<String, Boolean>() {
                public Boolean call(String arg) { return false; }
            };
    private final static Callback<PopupFeatures, WebEngine> CREATE_POPUP_HANDLER =
            new Callback<PopupFeatures, WebEngine>() {
                public WebEngine call(PopupFeatures arg) { return null; }
            };
    private final static Callback<PromptData, String> PROMPT_HANDLER =
            new Callback<PromptData, String>() {
                public String call(PromptData arg) { return ""; }
            };
    private final static EventHandler<WebEvent<String>> ON_ALERT =
            new EventHandler<WebEvent<String>>() {
                public void handle(WebEvent<String> arg) { }
            };
    private final static EventHandler<WebEvent<Rectangle2D>> ON_RESIZED =
            new EventHandler<WebEvent<Rectangle2D>>() {
                public void handle(WebEvent<Rectangle2D> arg) { }
            };
    private final static EventHandler<WebEvent<String>> ON_STATUS_CHANGED =
            new EventHandler<WebEvent<String>>() {
                public void handle(WebEvent<String> arg) { }
            };
    private final static EventHandler<WebEvent<Boolean>> ON_VISIBILITY_CHANGED =
            new EventHandler<WebEvent<Boolean>>() {
                public void handle(WebEvent<Boolean> arg) { }
            };
    private final static boolean JAVA_SCRIPT_ENABLED = false;
    private final static String LOCATION = "scheme://no.such.url";
    private final static String USER_AGENT = "JavaFX/WebView";
    private final static String USER_STYLE_SHEET_LOCATION = "";
    
    // WebView
    private final static FontSmoothingType FONT_SMOOTHING_TYPE  = FontSmoothingType.GRAY;
    private final static boolean CONTEXT_MENU_ENABLED = false;
    private final static double FONT_SCALE = 2.1;
    private final static double MAX_HEIGHT = 33.3;
    private final static double MAX_WIDTH = 1000.0;
    private final static double MIN_HEIGHT = 11.1;
    private final static double MIN_WIDTH = 10.0;
    private final static double PREF_HEIGHT = 22.2;
    private final static double PREF_WIDTH = 100.0;
    private final static double ZOOM = .77;
    
    @Test public void testWebEngineBuilder() {
        submit(new Runnable() { public void run() {
            WebEngineBuilder builder = WebEngineBuilder.create();
            WebEngine web = builder
                    .confirmHandler(CONFIRM_HANDLER)
                    .createPopupHandler(CREATE_POPUP_HANDLER)
                    .promptHandler(PROMPT_HANDLER)
                    .onAlert(ON_ALERT)
                    .onResized(ON_RESIZED)
                    .onStatusChanged(ON_STATUS_CHANGED)
                    .onVisibilityChanged(ON_VISIBILITY_CHANGED)
                    .javaScriptEnabled(JAVA_SCRIPT_ENABLED)
                    .location(LOCATION)
                    .userAgent(USER_AGENT)
                    .userStyleSheetLocation(USER_STYLE_SHEET_LOCATION)
                    .build();
            verify(web.confirmHandlerProperty(), CONFIRM_HANDLER);
            verify(web.createPopupHandlerProperty(), CREATE_POPUP_HANDLER);
            verify(web.promptHandlerProperty(), PROMPT_HANDLER);
            verify(web.onAlertProperty(), ON_ALERT);
            verify(web.onResizedProperty(), ON_RESIZED);
            verify(web.onStatusChangedProperty(), ON_STATUS_CHANGED);
            verify(web.onVisibilityChangedProperty(), ON_VISIBILITY_CHANGED);
            verify(web.javaScriptEnabledProperty(), JAVA_SCRIPT_ENABLED);
            verify(web.locationProperty(), LOCATION);
            verify(web.userAgentProperty(), USER_AGENT);
            verify(web.userStyleSheetLocationProperty(), USER_STYLE_SHEET_LOCATION);
        }});
    }
    
    @Test public void testWebViewBuilder() {
        submit(new Runnable() { public void run() {
            WebViewBuilder builder = WebViewBuilder.create();
            WebView view = builder
                    .contextMenuEnabled(CONTEXT_MENU_ENABLED)
                    .fontScale(FONT_SCALE)
                    .fontSmoothingType(FONT_SMOOTHING_TYPE)
                    .maxHeight(MAX_HEIGHT)
                    .minHeight(MIN_HEIGHT)
                    .prefHeight(PREF_HEIGHT)
                    .maxWidth(MAX_WIDTH)
                    .minWidth(MIN_WIDTH)
                    .prefWidth(PREF_WIDTH)
                    .zoom(ZOOM)
                    .build();

            verify(view.contextMenuEnabledProperty(), CONTEXT_MENU_ENABLED);
            verify(view.fontScaleProperty(), FONT_SCALE);
            verify(view.fontSmoothingTypeProperty(), FONT_SMOOTHING_TYPE);
            verify(view.maxHeightProperty(), MAX_HEIGHT);
            verify(view.minHeightProperty(), MIN_HEIGHT);
            verify(view.prefHeightProperty(), PREF_HEIGHT);
            verify(view.maxWidthProperty(), MAX_WIDTH);
            verify(view.minWidthProperty(), MIN_WIDTH);
            verify(view.prefWidthProperty(), PREF_WIDTH);
            verify(view.zoomProperty(), ZOOM);
        }});
    }
    
    private void verify(ReadOnlyProperty p, Object value) {
        assertEquals(p.getName(), value, p.getValue());
    }
}
