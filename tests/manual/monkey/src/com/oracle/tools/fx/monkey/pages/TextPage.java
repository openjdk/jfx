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

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.Border;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Font;
import javafx.scene.text.HitInfo;
import javafx.scene.text.Text;
import com.oracle.tools.fx.monkey.util.EnterTextDialog;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.FontSelector;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.ShowCharacterRuns;
import com.oracle.tools.fx.monkey.util.Templates;
import com.oracle.tools.fx.monkey.util.TestPaneBase;
import com.oracle.tools.fx.monkey.util.TextSelector;
import com.oracle.tools.fx.monkey.util.Utils;

/**
 * Text Page
 */
public class TextPage extends TestPaneBase {
    private final TextSelector textSelector;
    private final TextField styleField;
    private final FontSelector fontSelector;
    private final CheckBox showChars;
    private final ScrollPane scroll;
    private final CheckBox wrap;
    private final Path caretPath;
    private Text control;
    private String currentText;

    public TextPage() {
        FX.name(this, "TextPage");

        styleField = new TextField();
        styleField.setOnAction((ev) -> {
            String s = styleField.getText();
            if (Utils.isBlank(s)) {
                s = null;
            }
            control.setStyle(s);
        });

        caretPath = new Path();
        caretPath.setStrokeWidth(1);
        caretPath.setStroke(Color.RED);
        caretPath.setManaged(false);

        textSelector = TextSelector.fromPairs(
            "textSelector",
            (t) -> updateText(),
            Templates.multiLineTextPairs()
        );

        fontSelector = new FontSelector("font", (f) -> updateControl());

        Button editButton = new Button("Enter Text");
        editButton.setOnAction((ev) -> {
            new EnterTextDialog(this, (s) -> {
                currentText = s;
                updateControl();
            }).show();
        });

        showChars = new CheckBox("show characters");
        FX.name(showChars, "showChars");
        showChars.selectedProperty().addListener((p) -> {
            updateControl();
        });

        wrap = new CheckBox("wrap width");
        FX.name(wrap, "wrap");
        wrap.selectedProperty().addListener((p) -> {
            updateWrap(wrap.selectedProperty().get());
        });

        OptionPane op = new OptionPane();
        op.label("Text:");
        op.option(textSelector.node());
        op.option(editButton);
        op.label("Font:");
        op.option(fontSelector.fontNode());
        op.label("Font Size:");
        op.option(fontSelector.sizeNode());
        op.option(wrap);
        op.option(showChars);
        op.label("Note: " + (FX.isMac() ? "⌘" : "ctrl") + "-click for caret shape");
        op.label("Direct Style:");
        op.option(styleField);

        scroll = new ScrollPane();
        scroll.setBorder(Border.EMPTY);
        scroll.setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        scroll.setFitToWidth(false);

        setContent(scroll);
        setOptions(op);

        textSelector.selectFirst();
        fontSelector.selectSystemFont();
    }

    private void updateText() {
        currentText = textSelector.getSelectedText();
        updateControl();
    }

    private void updateControl() {
        Font f = fontSelector.getFont();

        control = new Text(currentText);
        control.setFont(f);

        Group group = new Group(control, caretPath);
        scroll.setContent(group);

        updateWrap(wrap.isSelected());

        if (showChars.isSelected()) {
            Group g = ShowCharacterRuns.createFor(control);
            group.getChildren().add(g);
        }

        control.addEventHandler(MouseEvent.MOUSE_PRESSED, (ev) -> {
            PickResult p = ev.getPickResult();
            //System.out.println(p);
        });
        
        control.addEventHandler(MouseEvent.MOUSE_CLICKED, (ev) -> {
            if(ev.isShortcutDown()) {
                showCaretShape(new Point2D(ev.getX(), ev.getY()));
            }
        });
    }

    private void updateWrap(boolean on) {
        if (on) {
            control.wrappingWidthProperty().bind(scroll.viewportBoundsProperty().map((b) -> b.getWidth()));
        } else {
            control.wrappingWidthProperty().unbind();
            control.setWrappingWidth(0);
        }
    }
    
    private void showCaretShape(Point2D p) {
        HitInfo h = control.hitTest(p);
        System.out.println("hit=" + h);
        PathElement[] pe = control.caretShape(h.getCharIndex(), h.isLeading());
        caretPath.getElements().setAll(pe);
    }
}
