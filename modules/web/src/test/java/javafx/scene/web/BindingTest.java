/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package javafx.scene.web;

import java.io.File;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.text.FontSmoothingType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import org.junit.Test;
import org.w3c.dom.Document;

public class BindingTest extends TestBase {
    
    @Test public void testWebView() throws InterruptedException {
        submit(new Runnable() { public void run() {
            WebView main = getView();
            WebView test = new WebView();

            test.contextMenuEnabledProperty().bind(main.contextMenuEnabledProperty());
            test.fontScaleProperty().bind(main.fontScaleProperty());
            test.fontSmoothingTypeProperty().bind(main.fontSmoothingTypeProperty());
            test.zoomProperty().bind(main.zoomProperty());
            test.maxHeightProperty().bind(main.maxHeightProperty());
            test.minHeightProperty().bind(main.minHeightProperty());
            test.prefHeightProperty().bind(main.prefHeightProperty());
            test.maxWidthProperty().bind(main.maxWidthProperty());
            test.minWidthProperty().bind(main.minWidthProperty());
            test.prefWidthProperty().bind(main.prefWidthProperty());

            main.setContextMenuEnabled(false);
            main.setFontScale(2.0);
            main.setFontSmoothingType(FontSmoothingType.GRAY);
            main.setZoom(1.5);
            main.setMaxHeight(33.3);
            main.setPrefHeight(22.2);
            main.setMinHeight(11.1);
            main.setMaxWidth(99.9);
            main.setPrefWidth(88.8);
            main.setMinWidth(77.7);
            
            assertEquals("WebView.contextMenuEnabled",
                    main.isContextMenuEnabled(), test.isContextMenuEnabled());
            assertEquals("WebView.fontScale",
                    main.getFontScale(), test.getFontScale(), 0.0);
            assertEquals("WebView.fontSmoothingType",
                    main.getFontSmoothingType(), test.getFontSmoothingType());
            assertEquals("WebView.height",
                    main.getHeight(), test.getHeight(), 0.0);
            assertEquals("WebView.maxHeight",
                    main.getMaxHeight(), test.getMaxHeight(), 0.0);
            assertEquals("WebView.minHeight",
                    main.getMinHeight(), test.getMinHeight(), 0.0);
            assertEquals("WebView.prefHeight",
                    main.getPrefHeight(), test.getPrefHeight(), 0.0);
            assertEquals("WebView.width",
                    main.getWidth(), test.getWidth(), 0.0);
            assertEquals("WebView.maxWidth",
                    main.getMaxWidth(), test.getMaxWidth(), 0.0);
            assertEquals("WebView.minWidth",
                    main.getMinWidth(), test.getMinWidth(), 0.0);
            assertEquals("WebView.prefWidth",
                    main.getPrefWidth(), test.getPrefWidth(), 0.0);
            assertEquals("WebView.zoom", main.getZoom(), test.getZoom(), 0.0);
            assertEquals("WebPage zoom factor",
                    main.getEngine().getPage().getZoomFactor(true),
                    test.getEngine().getPage().getZoomFactor(true),
                    0.0);
        }});
    }

    @Test public void testWebEngineWritableProperties() {
        submit(new Runnable() { public void run() {
            WebEngine web = getEngine();
            WebEngine test = new WebEngine();
            
            test.javaScriptEnabledProperty().bind(web.javaScriptEnabledProperty());
            test.userAgentProperty().bind(web.userAgentProperty());
            test.userStyleSheetLocationProperty().bind(web.userStyleSheetLocationProperty());
            
            web.setJavaScriptEnabled(false);
            web.setUserAgent("JavaFX/WebView");
            web.setUserStyleSheetLocation("");
            
            assertEquals("WebEngine.javaScriptEnabled",
                    web.isJavaScriptEnabled(), test.isJavaScriptEnabled());
            assertEquals("WebEngine.userAgent",
                    web.getUserAgent(), test.getUserAgent());
            assertEquals("WebEngine.userStyleSheetLocation",
                    web.getUserStyleSheetLocation(), test.getUserStyleSheetLocation());

        }});
    }
    
    @Test public void testWebEngineReadonlyProperties() {
        ObjectProperty<Document> doc = new SimpleObjectProperty<Document>();
        ObjectProperty<String> title = new SimpleObjectProperty<String>();
        ObjectProperty<String> loc = new SimpleObjectProperty<String>();

        WebEngine web = getEngine();
        doc.bind(web.documentProperty());
        title.bind(web.titleProperty());
        loc.bind(web.locationProperty());

        load(new File("src/test/resources/html/ipsum.html"));
        assertSame("WebEngine.document", web.getDocument(), doc.get());
        assertSame("WebEngine.title",    web.getTitle(),    title.get());
        assertSame("WebEngine.location", web.getLocation(), loc.get());
    }
}
