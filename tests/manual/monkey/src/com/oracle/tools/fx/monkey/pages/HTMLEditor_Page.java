/*
 * Copyright (c) 2022, 2024, Oracle and/or its affiliates. All rights reserved.
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

import javafx.scene.Node;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.HTMLEditorSkin;
import com.oracle.tools.fx.monkey.options.TextChoiceOption;
import com.oracle.tools.fx.monkey.sheets.ControlPropertySheet;
import com.oracle.tools.fx.monkey.util.HasSkinnable;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;

/**
 * HTMLEditor Page.
 */
public class HTMLEditor_Page extends TestPaneBase implements HasSkinnable {
    private final HTMLEditor control;

    public HTMLEditor_Page() {
        super("HTMLEditorPage");

        control = new HTMLEditor();

        OptionPane op = new OptionPane();
        op.section("HTMLEditor");
        op.option("HTML Text:", createHtmlTextOption());
        ControlPropertySheet.appendTo(op, control);

        setOptions(op);
        setContent(control);
        // TODO set html text
    }

    private Node createHtmlTextOption() {
        TextChoiceOption op = new TextChoiceOption("htmlText", true, null);
        op.addChoice("Simple", "<html><body><h1>Simple HTML</h1>This is a <b>test</b>.</body></html>");
        op.addChoice("<empty HTML>", "<html><body/></html>");
        op.addChoice("<null>", null);
        op.property().addListener((s, p, htmlText) -> {
            control.setHtmlText(htmlText);
        });
        return op;
    }

    @Override
    public void nullSkin() {
        control.setSkin(null);
    }

    @Override
    public void newSkin() {
        control.setSkin(new HTMLEditorSkin(control));
    }
}
