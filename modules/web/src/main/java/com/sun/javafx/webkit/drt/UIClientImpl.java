/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.webkit.drt;

import com.sun.webkit.LoadListenerClient;
import com.sun.webkit.UIClient;
import com.sun.webkit.WebPage;
import com.sun.webkit.graphics.WCImage;
import com.sun.webkit.graphics.WCRectangle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * {@link UIClient} implementation for DRT tests.
 */
final class UIClientImpl implements UIClient {

    private WebPage webPage;
    private final List<UIClient> clients = new ArrayList<UIClient>();

    private WCRectangle bounds = new WCRectangle(0, 0, 800, 600);

    UIClientImpl() {
    }

    void setWebPage(WebPage webPage) {
        this.webPage = webPage;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WebPage createPage(boolean menu, boolean status, boolean toolbar,
            boolean resizable)
    {
        UIClientImpl client = new UIClientImpl();
        final WebPage page = new WebPage(null, client, null, null, null, false);
        client.setWebPage(page);

        page.setBounds(0, 0, 800, 600);
//        webPage.setUsePageCache(true);

        page.addLoadListenerClient(new LoadListenerClient() {
            @Override
            public void dispatchLoadEvent(long frame, int state, String url, String contentType, double progress, int errorCode) {
                if (state == DOCUMENT_AVAILABLE) {
                    DumpRenderTree.drt.dumpUnloadListeners(page, frame);
                }
            }
            @Override
            public void dispatchResourceLoadEvent(long frame, int state, String url, String contentType, double progress, int errorCode) {
            }
        });

        // This call is needed to add the main frame to WebPage.frames list.
        // TODO: investigate why it's not added automatically (via WebPage.fwkFrameCreated) and fix.
        page.getMainFrame();

        clients.add(client);
        return client.webPage;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closePage() {
        Iterator<UIClient> it = clients.iterator();
        while (it.hasNext()) {
            it.next().closePage();
            it.remove();
        }
        if (webPage.getMainFrame() != 0) {
            webPage.dispose();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showView() {
        // look, I'm showing!
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WCRectangle getViewBounds() {
        return bounds;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setViewBounds(WCRectangle bounds) {
        this.bounds = bounds;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setStatusbarText(String text) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void alert(String text) {
        DumpRenderTree.out.printf("ALERT: %s\n", text);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean confirm(String text) {
        DumpRenderTree.out.printf("CONFIRM: %s\n", text);
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String prompt(String text, String defaultValue) {
        DumpRenderTree.out.printf("PROMPT: %s, default text: %s\n", text, defaultValue);
        return defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] chooseFile(String initialFileName, boolean multiple) {
        throw new UnsupportedOperationException("Not supported yet");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void print() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startDrag(WCImage frame, int imageOffsetX, int imageOffsetY,
            int eventPosX, int eventPosY, String[] mimeTypes, Object[] values)
    {
        throw new UnsupportedOperationException("Not supported yet");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void confirmStartDrag() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDragConfirmed() {
        return false;
    }
}
