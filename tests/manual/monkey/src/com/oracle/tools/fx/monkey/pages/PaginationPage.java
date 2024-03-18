/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.tools.fx.monkey.pages;

import javafx.beans.property.ObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Pagination;
import javafx.scene.control.skin.PaginationSkin;
import javafx.scene.image.ImageView;
import javafx.util.Callback;
import com.oracle.tools.fx.monkey.options.IntOption;
import com.oracle.tools.fx.monkey.options.ObjectOption;
import com.oracle.tools.fx.monkey.sheets.ControlPropertySheet;
import com.oracle.tools.fx.monkey.util.HasSkinnable;
import com.oracle.tools.fx.monkey.util.ImageTools;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;

/**
 * Pagination Control Page.
 */
public class PaginationPage extends TestPaneBase implements HasSkinnable {
    private final Pagination control;

    public PaginationPage() {
        super("PaginationPage");
        
        control = new Pagination();
        
        OptionPane op = new OptionPane();
        op.section("Pagination");
        op.option("Current Page Index:", new IntOption("currentPageIndex", 0, Integer.MAX_VALUE, control.currentPageIndexProperty()));
        op.option("Max Page Indicator Count:", new IntOption("maxPageIndicatorCount", 0, Integer.MAX_VALUE, control.maxPageIndicatorCountProperty()));
        // TODO INDETERMINATE
        op.option("Page Count:", new IntOption("pageCount", 1, Integer.MAX_VALUE, control.pageCountProperty()));
        op.option("Page Factory:", createPageFactoryOptions("pageFactory", control.pageFactoryProperty()));
        
        ControlPropertySheet.appendTo(op, control);
        
        setContent(control);
        setOptions(op);
    }
    
    private Callback<Integer, Node> createImagesFactory() {
        return (ix) -> {
            String s = String.valueOf(ix);
            return new ImageView(ImageTools.createImage(s, 256, 256));
        };
    }

    private Node createPageFactoryOptions(String name, ObjectProperty<Callback<Integer, Node>> p) {
        ObjectOption<Callback<Integer, Node>> op = new ObjectOption<>(name, p);
        op.addChoice("Images", createImagesFactory()); 
        op.addChoice("<null>", null);
        return op;
    }

    @Override
    public void nullSkin() {
        control.setSkin(null);
    }

    @Override
    public void newSkin() {
        control.setSkin(new PaginationSkin(control));
    }
}
