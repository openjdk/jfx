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

import javafx.scene.Group;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.Border;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import com.oracle.tools.fx.monkey.util.FontSelector;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.ShowCharacterRuns;
import com.oracle.tools.fx.monkey.util.Templates;
import com.oracle.tools.fx.monkey.util.TestPaneBase;
import com.oracle.tools.fx.monkey.util.TextSelector;

/**
 * Text Page
 */
public class TextPage extends TestPaneBase {
    private final TextSelector textSelector;
    private final FontSelector fontSelector;
    private final CheckBox showChars;
    private final ScrollPane scroll;
    private final CheckBox wrap;
    private Text control;

    public TextPage() {
        setId("TextPage");

        textSelector = TextSelector.fromPairs(
            "textSelector",
            (t) -> updateText(),
            Templates.multiLineTextPairs()
        );

        fontSelector = new FontSelector("font", (f) -> updateText());

        showChars = new CheckBox("show characters");
        showChars.setId("showChars");
        showChars.selectedProperty().addListener((p) -> {
            updateText();
        });

        wrap = new CheckBox("wrap width");
        wrap.setId("wrap");
        wrap.selectedProperty().addListener((p) -> {
            updateWrap(wrap.selectedProperty().get());
        });

        OptionPane p = new OptionPane();
        p.label("Text:");
        p.option(textSelector.node());
        p.label("Font:");
        p.option(fontSelector.fontNode());
        p.label("Font Size:");
        p.option(fontSelector.sizeNode());
        p.option(wrap);
        p.option(showChars);

        scroll = new ScrollPane();
        scroll.setBorder(Border.EMPTY);
        scroll.setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        scroll.setFitToWidth(false);

        setContent(scroll);
        setOptions(p);

        textSelector.selectFirst();
        fontSelector.selectSystemFont();
    }

    protected void updateText() {
        String text = textSelector.getSelectedText();
        Font f = fontSelector.getFont();

        control = new Text(text);
        control.setFont(f);
        Group group = new Group(control);
        scroll.setContent(group);

        updateWrap(wrap.isSelected());

        if (showChars.isSelected()) {
            Group g = ShowCharacterRuns.createFor(control);
            group.getChildren().add(g);
        }
    }

    protected void updateWrap(boolean on) {
        if (on) {
            control.wrappingWidthProperty().bind(scroll.viewportBoundsProperty().map((b) -> b.getWidth()));
        } else {
            control.wrappingWidthProperty().unbind();
            control.setWrappingWidth(0);
        }
    }
}
