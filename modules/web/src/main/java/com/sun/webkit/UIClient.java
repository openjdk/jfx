/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit;

import com.sun.webkit.graphics.WCImage;
import com.sun.webkit.graphics.WCRectangle;

public interface UIClient {

    public WebPage createPage(
            boolean menu, boolean status, boolean toolbar, boolean resizable);
    public void closePage();
    public void showView();
    public WCRectangle getViewBounds();
    public void setViewBounds(WCRectangle bounds);

    public void setStatusbarText(String text);

    public void alert(String text);
    public boolean confirm(String text);
    public String prompt(String text, String defaultValue);

    public String[] chooseFile(String initialFileName, boolean multiple);
    public void print();

    public void startDrag(
            WCImage frame,
            int imageOffsetX, int imageOffsetY,
            int eventPosX, int eventPosY,
            String[] mimeTypes,
            Object[] values);
    public void confirmStartDrag();
    public boolean isDragConfirmed();
}
