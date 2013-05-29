/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit.plugin;

import java.io.IOException;

import com.sun.webkit.graphics.WCGraphicsContext;

public interface Plugin {
    public final static int EVENT_BEFOREACTIVATE = -4;
    public final static int EVENT_FOCUSCHANGE = -1;

    /**
     * Sets focus to plugin by webkit
     */
    public void requestFocus();

    /**
     * Sets plugin location in native container
     * @param x
     * @param y
     * @param width
     * @param height
     */
    public void setNativeContainerBounds(int x, int y, int width, int height);

    /**
     * Initiates plugin life circle.
     * @param pl
     */
    void activate(Object nativeContainer, PluginListener pl);

    /**
     * Destroys plugin.
     */
    void destroy();

    /**
     * Makes plugin visible/hidden.
     * @param isVisible
     */
    void setVisible(boolean isVisible);

    /**
     * Makes plugin interactive if possible.
     * @param isEnable
     */
    public void setEnabled(boolean enabled);

    /**
     * Sets new position for plugin.
     * @param x
     * @param y
     * @param width
     * @param height
     */
    void setBounds(int x, int y, int width, int height);

    /**
     * Script action over the plugin
     * @param subObjectId
     * @param methodName
     * @param args
     * @return the
     * @throws java.io.IOException
     */
    Object invoke(
            String subObjectId,
            String methodName,
            Object[] args) throws IOException;

    
    /**
     * Paints plugin by Webkit request in selected bounds
     * @param g
     * @param x
     * @param y
     * @param width
     * @param height
     */
     public void paint(WCGraphicsContext g, int intX, int intY, int intWidth, int intHeight);

     public boolean handleMouseEvent(
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
            long timeStamp);
}
