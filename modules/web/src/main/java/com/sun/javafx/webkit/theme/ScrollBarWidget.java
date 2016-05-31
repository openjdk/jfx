/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.webkit.theme;

import com.sun.webkit.graphics.ScrollBarTheme;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;

public final class ScrollBarWidget extends ScrollBar implements RenderThemeImpl.Widget {
    static {
        ScrollBarWidgetHelper.setScrollBarWidgetAccessor(new ScrollBarWidgetHelper.ScrollBarWidgetAccessor() {
            @Override
            public void doUpdatePeer(Node node) {
                ((ScrollBarWidget) node).doUpdatePeer();
            }
        });
    }

    private ScrollBarThemeImpl sbtImpl;

    {
        // To initialize the class helper at the begining each constructor of this class
        ScrollBarWidgetHelper.initHelper(this);
    }

    public ScrollBarWidget(ScrollBarThemeImpl sbtImpl) {
        this.sbtImpl = sbtImpl;
        setOrientation(Orientation.VERTICAL);
        setMin(0);
        setManaged(false);
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private void doUpdatePeer() {
        initializeThickness();
    }

    @Override
    public RenderThemeImpl.WidgetType getType() {
        return RenderThemeImpl.WidgetType.SCROLLBAR;
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        initializeThickness();
    }

    private boolean thicknessInitialized = false;
    private void initializeThickness() {
        if (!thicknessInitialized) {
            ScrollBar testSB = sbtImpl.getTestSBRef();
            if (testSB == null) {
                return;
            }
            int thickness = (int) testSB.prefWidth(-1);
            if (thickness != 0 && ScrollBarTheme.getThickness() != thickness) {
                ScrollBarTheme.setThickness(thickness);
            }
            thicknessInitialized = true;
        }
    }
}
