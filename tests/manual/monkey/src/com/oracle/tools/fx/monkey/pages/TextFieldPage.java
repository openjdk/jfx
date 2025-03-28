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

import javafx.scene.AccessibleAttribute;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.skin.TextFieldSkin;
import com.oracle.tools.fx.monkey.Loggers;
import com.oracle.tools.fx.monkey.sheets.TextFieldPropertySheet;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.HasSkinnable;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;

/**
 * TextField Page.
 */
public class TextFieldPage extends TestPaneBase implements HasSkinnable {
    private final TextField control;
    private final CheckBox inScroll;

    public TextFieldPage() {
        this(new TextField() {
            @Override
            public Object queryAccessibleAttribute(AccessibleAttribute a, Object... ps) {
                Object v = super.queryAccessibleAttribute(a, ps);
                Loggers.accessibility.log(a, v);
                return v;
            }
        }, "TextFieldPage");
    }

    protected TextFieldPage(TextField f, String name) {
        super(name);
        this.control = f;

        inScroll = new CheckBox("in scroll pane");
        FX.name(inScroll, "inScrollPane");
        inScroll.setOnAction((ev) -> updateScroll());

        OptionPane op = new OptionPane();
        TextFieldPropertySheet.appendTo(op, control, () -> {
            op.separator();
            op.option(inScroll);
        });

        setContent(control);
        setOptions(op);
    }

    private void updateScroll() {
        if(inScroll.isSelected()) {
            ScrollPane sp = new ScrollPane(control);
            setContent(sp);
        } else {
            setContent(control);
        }
    }

    @Override
    public void nullSkin() {
        control.setSkin(null);
    }

    @Override
    public void newSkin() {
        control.setSkin(new TextFieldSkin(control));
    }
}
