/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit;

import java.io.StringReader;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.LinkedList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Window;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public final class WebPage {

    static {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
System.out.println ("[JVDBG] I will load the webview system library");
                System.loadLibrary("webview");
                return null;
            }
        });
    }
    public static int DND_DST_DROP;
    public static int DND_SRC_DROP;
    public static int DND_DST_ENTER;
    public static int DND_DST_EXIT;
    public static int DND_DST_OVER;
    private int x, y;
    private int width, height;
    private float zoomFactor;
    private int smoothFactor;
    private boolean contextMenuEnabled;
    private final List<LoadListenerClient> loadListenerClients =
            new LinkedList<LoadListenerClient>();
    WebEngine engine;
    NativeWebView nativeWebView;
    private String url;
    private boolean visible = false;

    public WebPage(WebEngine engine) {
        this.engine = engine;
        createNativePeer();
    }

    static void unlockPage() {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    static void lockPage() {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    public void setZoomFactor(float zf, boolean b) {
        zoomFactor = zf;
    }

    public void setFontSmoothingType(int sf) {
        smoothFactor = sf;
    }

    public void setContextMenuEnabled(boolean cme) {
        contextMenuEnabled = cme;
    }

    public boolean isDirty() {
        return true;
    }

    public boolean isRepaintPending() {
        return false;
    }

    public void dropRenderFrames() {
        // this should not be called if webview is not shown
        // System.out.println("--> dropRenderFrames");
    }

    public boolean isDragConfirmed() {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    public int dispatchDragOperation(int wkDndEventType, String[] toArray, String[] toArray0, int i, int i0, int i1, int i2, int wkDndAction) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    public void confirmStartDrag() {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    public void setJavaScriptEnabled(boolean get) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void setUserStyleSheetLocation(String dataUrl) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void setUserAgent(String get) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public String getUserAgent() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void dispose() {
        getNativePeer().dispose();
    }

    public long getMainFrame() {
        return 0;
    }

    public void load(long mainFrame, String content, String contentType) {
        getNativePeer().loadContent(content, contentType);
    }

    public void refresh(long mainFrame) {        
    }

    public Object executeScript(long mainFrame, String script) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void stop(long mainFrame) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public Document getDocument(long mainFrame) {
        String xmlString = getNativePeer().getHtmlContent();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xmlString)));  
            return document;
        }
        catch (Exception e) {
            System.err.println ("Cannot parse "+xmlString+" due to "+e);
            return null;
        }
    }

    public void setDeveloperExtrasEnabled(boolean b) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void connectInspectorFrontend() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void disconnectInspectorFrontend() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void dispatchInspectorMessageFromFrontend(String message) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public AccessControlContext getAccessControlContext() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void open(long mainFrame, String url) {
        getNativePeer().loadUrl(url);
    }

    public void addLoadListenerClient(LoadListenerClient l) {
        if (!loadListenerClients.contains(l)) {
            loadListenerClients.add(l);
        }
    }

    public String getTitle(long mainFrame) {
        return engine.getTitle();
    }

    long getPage() {
        return 0;
    }

    public BackForwardList createBackForwardList() {
        return new BackForwardList(this);
    }

    private void repaintAll() {
    }

    public void moveAndResize(float minX, float minY, float width, float height) {
        Scene scene = getView().getScene();
        Window window = scene.getWindow();
        this.x = (int) (minX + scene.getX() + window.getX());
        this.y = (int) (minY + scene.getY() + window.getY());
        this.width = (int) width;
        this.height = (int) height;
        getNativePeer().moveAndResize(this.x, this.y, this.width, this.height);
    }

    private WebView getView() {
        return engine.getView();
    }

    private void createNativePeer() {
        nativeWebView = new NativeWebView(this);
    }

    private NativeWebView getNativePeer() {
        return nativeWebView;
    }

    public void setVisible(boolean visible) {
        if (this.visible != visible) {
            getNativePeer().setVisible(visible);
            this.visible = visible;
        }
    }

    void fireLoadEvent(int frameID, int state, String url,
            String contentType, double progress, int errorCode) {        
        for (LoadListenerClient l : loadListenerClients) {
            l.dispatchLoadEvent(frameID, state, url, contentType, progress, errorCode);
        }
    }
}
