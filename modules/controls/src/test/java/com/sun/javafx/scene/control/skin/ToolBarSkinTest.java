/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control.skin;

import static org.junit.Assert.assertEquals;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * This fails with IllegalStateException because of the toolkit's check for the FX application thread
 */
public class ToolBarSkinTest {
    private ToolBar toolbar;
    private ToolBarSkinMock skin;

    @Before public void setup() {
        toolbar = new ToolBar();
        toolbar.getItems().addAll(new Button("Cut"), new Button("Copy"));
        skin = new ToolBarSkinMock(toolbar);
        // Set some padding so that any places where padding was being
        // computed but wasn't expected will be caught.
        toolbar.setPadding(new Insets(10, 10, 10, 10));
        toolbar.setSkin(skin);

    }

    @Test public void horizontalMaxHeightTracksPreferred() {
        toolbar.setOrientation(Orientation.HORIZONTAL);
        toolbar.setPrefHeight(100);
        assertEquals(100, toolbar.maxHeight(-1), 0);
    }

    @Test public void verticalMaxWidthTracksPreferred() {
        toolbar.setOrientation(Orientation.VERTICAL);
        toolbar.setPrefWidth(100);
        assertEquals(100, toolbar.maxWidth(-1), 0);
    }

    public static final class ToolBarSkinMock extends ToolBarSkin {
        boolean propertyChanged = false;
        int propertyChangeCount = 0;
        public ToolBarSkinMock(ToolBar toolbar) {
            super(toolbar);
        }
        
        @Override protected void handleControlPropertyChanged(String p) {
            super.handleControlPropertyChanged(p);
            propertyChanged = true;
            propertyChangeCount++;
        }
    }
}
