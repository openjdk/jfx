/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit;

import static com.sun.webkit.network.URLs.newURL;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.webkit.plugin.Plugin;
import com.sun.webkit.plugin.PluginListener;
import com.sun.webkit.plugin.PluginManager;
import com.sun.webkit.graphics.WCGraphicsContext;
import com.sun.webkit.graphics.WCRectangle;

final class WCPluginWidget extends WCWidget implements PluginListener {

    private final static Logger log =
        Logger.getLogger(WCPluginWidget.class.getName());
    
    private final Plugin plugin;
    private long pData = 0L;//for native code

    private static native void initIDs();
    
    static {
        initIDs();
    };

    private WCPluginWidget(
            WebPage webPage,
            Plugin plugin,
            int width, int height)
    {
        super(webPage);
        this.plugin = plugin;
        setBounds(0, 0, width, height);
        //webPage.getPageClient().XXX; - implement for native level support
        WebPageClient wpc = webPage.getPageClient();
        this.plugin.activate(
                null==wpc ? null : wpc.getContainer(),
                this);
    }

    @Override
    protected void requestFocus() {
        plugin.requestFocus();
    }
    
    private static WCPluginWidget create(
        final WebPage webPage,
        final int width, final int height,
        final String urlString,
        final String mimeType,
        final String[] pNames,
        final String[] pValues)
    {
        URL url = null;
        try {
            url = newURL(urlString);
        } catch (MalformedURLException ex) {
            log.log(Level.FINE, null, ex);
        }
        return new WCPluginWidget(
                webPage,
                PluginManager.createPlugin(url, mimeType, pNames, pValues),
                width, height);
    }

    private void fwkSetNativeContainerBounds(
            int x,
            int y,
            int width,
            int height)
    {
        plugin.setNativeContainerBounds(x, y, width, height);
    }

    @Override
    void setBounds(
            //page coordinates!!!
            int x,
            int y,
            int width,
            int height)
    {
        super.setBounds(x, y, width, height);
        plugin.setBounds(x, y, width, height);
    }

    //TODO: fwk call have to be implemented in WCWidget
    //@Override
    private void setEnabled(boolean enabled){
        plugin.setEnabled(enabled);
    }

    @Override
    protected void setVisible(boolean visible){
        plugin.setVisible(visible);
    }


    @Override
    protected void destroy() {
        pData = 0L;
        plugin.destroy();
    }
    
    private void paint(WCGraphicsContext g,
            //page offsets
            final int x,
            final int y,
            final int width,
            final int height)
    {
        WCRectangle bd = getBounds();
        WCRectangle clip = bd.intersection( new WCRectangle(x, y, width, height) );
        if( !clip.isEmpty() ){
            g.translate(bd.getX(), bd.getY());
            clip.translate(-bd.getX(), -bd.getY());
            g.setClip(clip.getIntX(), clip.getIntY(), clip.getIntWidth(), clip.getIntHeight());
            plugin.paint(
                    g,
                    //client coordinates!!!
                    clip.getIntX(),
                    clip.getIntY(),
                    clip.getIntWidth(),
                    clip.getIntHeight());
        }
    }

    //converts widget coordinates to container
    private native WCRectangle twkConvertToPage(WCRectangle rc);

    private native void twkInvalidateWindowlessPluginRect(
            //client coordinates!!!
            int x,
            int y,
            int width,
            int height);

    private boolean fwkHandleMouseEvent(
            String type,
            int offsetX,
            int offsetY,
            int screenX,
            int screenY,
            int button,
            boolean buttonDown,
            boolean altKey,
            boolean metaKey,
            boolean ctrlKey,
            boolean shiftKey,
            long timeStamp)
    {
        return plugin.handleMouseEvent(
                type,
                offsetX, offsetY,
                screenX, screenY,
                button, buttonDown,
                altKey, metaKey, ctrlKey, shiftKey, timeStamp);
    }

    //PluginListener
    public void fwkRedraw(
            //client coordinates!!!
            final int x,
            final int y,
            final int width,
            final int height,
            final boolean eraseBackground)
    {
        twkInvalidateWindowlessPluginRect(x, y, width, height);
    }

    private native void twkSetPlugunFocused(boolean isFocused);

    public String fwkEvent(
            final int eventId,
            final String name,
            final String params)
    {
        if(Plugin.EVENT_FOCUSCHANGE==eventId && Boolean.parseBoolean(params)){
            twkSetPlugunFocused(Boolean.valueOf(params));
        }
        return "";
    }
}

