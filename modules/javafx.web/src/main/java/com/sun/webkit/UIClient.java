/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

    public boolean canRunBeforeUnloadConfirmPanel();
    public boolean runBeforeUnloadConfirmPanel(String message);

    public String[] chooseFile(String initialFileName, boolean multiple, String mimeFilters);
    public void print();

    public void startDrag(
            WCImage frame,
            int imageOffsetX, int imageOffsetY,
            int eventPosX, int eventPosY,
            String[] mimeTypes, Object[] values,
            boolean isImageSource);
    public void confirmStartDrag();
    public boolean isDragConfirmed();
}
