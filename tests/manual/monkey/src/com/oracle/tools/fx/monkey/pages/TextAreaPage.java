/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.FontSelector;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.Templates;
import com.oracle.tools.fx.monkey.util.TestPaneBase;
import com.oracle.tools.fx.monkey.util.TextSelector;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;

/**
 * TextArea Page
 */
public class TextAreaPage extends TestPaneBase {
    private final TextArea control;
    private final TextSelector textSelector;

    public TextAreaPage() {
        FX.name(this, "TextAreaPage");

        control = new TextArea();
        control.setPromptText("<prompt>");

        textSelector = TextSelector.fromPairs(
            "textSelector",
            (t) -> control.setText(t),
            Templates.multiLineTextPairs()
        );

        FontSelector fontSelector = new FontSelector("font", control::setFont);

        CheckBox wrap = new CheckBox("wrap text");
        FX.name(wrap, "wrapText");
        wrap.selectedProperty().addListener((s, p, on) -> {
            control.setWrapText(on);
        });

        CheckBox editable = new CheckBox("editable");
        FX.name(editable, "editable");
        editable.selectedProperty().bindBidirectional(control.editableProperty());

        TextSelector promptChoice = Templates.promptChoice("promptChoice", control::setPromptText);
        promptChoice.addPair("Multiline", "1\n2\n3\n4");

        OptionPane p = new OptionPane();
        p.label("Text:");
        p.option(textSelector.node());
        p.label("Font:");
        p.option(fontSelector.fontNode());
        p.label("Font Size:");
        p.option(fontSelector.sizeNode());
        p.option(wrap);
        p.option(editable);
        p.label("Prompt:");
        p.option(promptChoice.node());

        setContent(control);
        setOptions(p);

        textSelector.selectFirst();
        fontSelector.selectSystemFont();
        promptChoice.select(null);
    }
}
