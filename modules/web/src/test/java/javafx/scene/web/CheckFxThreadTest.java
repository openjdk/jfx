/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
package javafx.scene.web;

import javafx.scene.text.FontSmoothingType;
import static org.junit.Assert.fail;
import org.junit.Test;


public class CheckFxThreadTest extends TestBase {
    
    @Test public void testWebEngineMethods() {
        try {
            getEngine().load("about:blank");
            fail("WebEngine.load() didn't throw IllegalStateException");
        } catch (IllegalStateException e) {
        }
        
        try {
            getEngine().loadContent("simple HTML paragraph");
            fail("WebEngine.loadContent(String) didn't throw IllegalStateException");
        } catch (IllegalStateException e) {
        }
        
        try {
            getEngine().loadContent("plain text", "text/plain");
            fail("WebEngine.loadContent(String, String) didn't throw IllegalStateException");
        } catch (IllegalStateException e) {
        }
        
        try {
            getEngine().reload();
            fail("WebEngine.reload() didn't throw IllegalStateException");
        } catch (IllegalStateException e) {
        }
        
        try {
            getEngine().getLoadWorker().cancel();
            fail("WebEngine.getLoadWorker().cancel() didn't throw IllegalStateException");
        } catch (IllegalStateException e) {
        }
        
        try {
            getEngine().executeScript("window");
            fail("WebEngine.executeScript() didn't throw IllegalStateException");
        } catch (IllegalStateException e) {
        }
    }
    
    @Test public void testWebEngineProperties() {
        
        // javaScriptEnabled
        try {
            getEngine().setJavaScriptEnabled(false);
            fail("WebEngine.setJavaScriptEnabled() didn't throw IllegalStateException");
        } catch (IllegalStateException e) {
        }

        getEngine().isJavaScriptEnabled();
        
        try {
            getEngine().javaScriptEnabledProperty().set(true);
            fail("WebEngine.javaScriptEnabledProperty.set() didn't throw IllegalStateException");
        } catch (IllegalStateException e) {
        }

        // userStyleSheetLocation
        try {
            getEngine().setUserStyleSheetLocation("file:");
            fail("WebEngine.setUserStyleSheetLocation() didn't throw IllegalStateException");
        } catch (IllegalStateException e) {
        }

        getEngine().getUserStyleSheetLocation();
        
        try {
            getEngine().userStyleSheetLocationProperty().set(null);
            fail("WebEngine.userStyleSheetLocationProperty.set() didn't throw IllegalStateException");
        } catch (IllegalStateException e) {
        }
    }
    
    @Test public void testWebViewProperties() {

        // contextMenuEnabled
        try {
            getView().setContextMenuEnabled(false);
            fail("WebView.setContextMenuEnabled() didn't throw IllegalStateException");
        } catch (IllegalStateException e) {
        }

        getView().isContextMenuEnabled();
        
        try {
            getView().contextMenuEnabledProperty().set(true);
            fail("WebView.contextMenuEnabledProperty().set() didn't throw IllegalStateException");
        } catch (IllegalStateException e) {
        }

        // zoom
        try {
            getView().setZoom(3.0);
            fail("WebView.setZoom() didn't throw IllegalStateException");
        } catch (IllegalStateException e) {
        }

        getView().getZoom();

        try {
            getView().zoomProperty().set(2.0);
            fail("WebView.zoom.set() didn't throw IllegalStateException");
        } catch (IllegalStateException e) {
        }

        // fontScale
        try {
            getView().setFontScale(3.0);
            fail("WebView.setFontScale() didn't throw IllegalStateException");
        } catch (IllegalStateException e) {
        }

        getView().getFontScale();
        
        try {
            getView().fontScaleProperty().set(2.0);
            fail("WebView.fontScaleProperty.set() didn't throw IllegalStateException");
        } catch (IllegalStateException e) {
        }

        // fontSmoothingType
        try {
            getView().setFontSmoothingType(FontSmoothingType.GRAY);
            fail("WebView.setFontSmoothingType() didn't throw IllegalStateException");
        } catch (IllegalStateException e) {
        }

        getView().getFontSmoothingType();
        
        try {
            getView().fontSmoothingTypeProperty().set(FontSmoothingType.LCD);
            fail("WebView.fontSmoothingTypeProperty.set() didn't throw IllegalStateException");
        } catch (IllegalStateException e) {
        }
    }
}
