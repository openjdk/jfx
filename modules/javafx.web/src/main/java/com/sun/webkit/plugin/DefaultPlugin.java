/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit.plugin;

import java.io.IOError;
import java.net.URL;

import com.sun.prism.paint.Color;
import com.sun.webkit.graphics.WCGraphicsContext;


final class DefaultPlugin implements Plugin {

    //private JLabel nullComp;

    private void init(final String pluginDetails) {
        //nullComp = new JLabel(pluginDetails);
        //nullComp.setHorizontalAlignment(JLabel.CENTER);
        //nullComp.setVisible(true);
    }

    DefaultPlugin(URL url, String type, String[] pNames, String[] pValues) {
        init("Default Plugin for: " + (null==url ? "(null)" : url.toExternalForm()));
    }

    @Override
    public void paint(WCGraphicsContext g, int intX, int intY, int intWidth, int intHeight)
    {
        //if(g instanceof  WCGraphics2DContext){
            //nullComp.paint( ((WCGraphics2DContext)g).getImageGraphics() );
        //}
        g.fillRect(x, y, w, h, new Color(2 / 3.0f, 1.0f, 1.0f, 1 / 15.0f));
    }

    @Override
    public void activate(Object nativeContainer, PluginListener pl) {}

    @Override
    public void destroy() {}

    @Override
    public void setVisible(boolean isVisible) {}

    @Override
    public void setEnabled(boolean enabled) {}

    private int x = 0;
    private int y = 0;
    private int w = 0;
    private int h = 0;

    @Override
    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.w = width;
        this.h = height;
        //nullComp.setBounds(new Rectangle(x, y, width, height) ) ;
    }

    @Override
    public Object invoke(String subObjectId, String methodName, Object[] args) throws IOError {
        return null;
    }

    @Override
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
            long timeStamp)
    {
        return false;
    }

    @Override
    public void requestFocus() {}

    @Override
    public void setNativeContainerBounds(int x, int y, int width, int height) {}
}

