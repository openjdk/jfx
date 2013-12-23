/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit.plugin;

public interface PluginListener {
    /**
     * Plagin-intiated redraw in bounds
     *
     * @param x
     * @param y
     * @param width
     * @param height
     * @param eraseBackground
     */
    void   fwkRedraw(
            final int x,  final int y,
            final int width, final int height,
            final boolean eraseBackground);

    /**
     * Plagin-intiated event
     * 
     * @param eventId
     * @param name
     * @param params
     * @return
     */
    String fwkEvent(
            final int eventId,
            final String name, final String params);
}
