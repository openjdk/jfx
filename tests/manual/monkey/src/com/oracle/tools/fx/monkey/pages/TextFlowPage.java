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

import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Font;
import javafx.scene.text.HitInfo;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
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
 * TextFlow Page
 */
public class TextFlowPage extends TestPaneBase {
    private final TextSelector textSelector;
    private final TextField styleField;
    private final FontSelector fontSelector;
    private final CheckBox showChars;
    private final CheckBox showCaretPath;
    private final TextFlow control;
    private final Label pickResult;
    private final Label hitInfo;
    private final Label hitInfo2;
    private final Path caretPath;
    private String currentText;
    private static final String INLINE = "\u0000_INLINE";
    private static final String RICH_TEXT = "\u0000_RICH";

    public TextFlowPage() {
        FX.name(this, "TextFlowPage");

        control = new TextFlow();
        control.addEventHandler(MouseEvent.ANY, this::handleMouseEvent);

        styleField = new TextField();
        styleField.setOnAction((ev) -> {
            String s = styleField.getText();
            if (Utils.isBlank(s)) {
                s = null;
            }
            control.setStyle(s);
        });

        pickResult = new Label();

        hitInfo = new Label();

        hitInfo2 = new Label();

        caretPath = new Path();
        caretPath.setStrokeWidth(1);
        caretPath.setStroke(Color.RED);
        caretPath.setManaged(false);

        textSelector = TextSelector.fromPairs(
            "textSelector",
            (t) -> updateText(),
            Utils.combine(
                Templates.multiLineTextPairs(),
                "Inline Nodes", INLINE,
                "Rich Text", RICH_TEXT,
                "Accadian", Templates.AKKADIAN
            )
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

        showCaretPath = new CheckBox("show caret path");
        FX.name(showCaretPath, "showCaretPath");
        showCaretPath.selectedProperty().addListener((p) -> {
            updateControl();
        });

        OptionPane op = new OptionPane();
        op.label("Text:");
        op.option(textSelector.node());
        op.option(editButton);
        op.label("Font:");
        op.option(fontSelector.fontNode());
        op.label("Font Size:");
        op.option(fontSelector.sizeNode());
        op.option(showChars);
        op.option(showCaretPath);
        op.label("Direct Style:");
        op.option(styleField);
        //
        op.option(new Separator(Orientation.HORIZONTAL));
        op.label("Pick Result:");
        op.option(pickResult);
        op.label("Text.hitTest:");
        op.option(hitInfo2);
        op.label("TextFlow.hitTest:");
        op.option(hitInfo);
        op.label("Note: " + (FX.isMac() ? "⌘" : "ctrl") + "-click for caret shape");

        setContent(control);
        setOptions(op);

        fontSelector.selectSystemFont();
        textSelector.selectFirst();
    }

    private void updateText() {
        currentText = textSelector.getSelectedText();
        updateControl();
    }

    private void updateControl() {
        Font f = fontSelector.getFont();
        Node[] ts = createTextArray(currentText, f);
        control.getChildren().setAll(ts);

        caretPath.getElements().clear();
        control.getChildren().add(caretPath);

        if (showChars.isSelected()) {
            Group g = ShowCharacterRuns.createFor(control);
            control.getChildren().add(g);
        }

        if (showCaretPath.isSelected()) {
            int len = FX.getTextLength(control);
            for (int i = 0; i < len; i++) {
                PathElement[] es = control.caretShape(i, true);
                caretPath.getElements().addAll(es);
            }
        }
    }

    private Node[] createTextArray(String text, Font f) {
        if (INLINE.equals(text)) {
            return new Node[] {
                t("Inline Nodes:", f),
                new Button("Left"),
                t(" ", f),
                new Button("Right"),
                t("trailing", f)
            };
        } else if (RICH_TEXT.equals(text)) {
            return new Node[] {
                t("Rich Text: ", f),
                t("BOLD ", f, "-fx-font-weight:bold;"),
                t("BOLD ", f, "-fx-font-weight:bold;"),
                t("BOLD ", f, "-fx-font-weight:900;"),
                t("italic ", f, "-fx-font-style:italic;"),
                t("underline ", f, "-fx-underline:true;"),
                t(Templates.TWO_EMOJIS, f),
                t(Templates.CLUSTERS, f)
            };
        } else {
            return new Node[] { t(text, f) };
        }
    }

    private static Text t(String text, Font f) {
        Text t = new Text(text);
        t.setFont(f);
        return t;
    }

    private static Text t(String text, Font f, String style) {
        Text t = new Text(text);
        t.setFont(f);
        t.setStyle(style);
        return t;
    }

    private void handleMouseEvent(MouseEvent ev) {
        PickResult pick = ev.getPickResult();
        Node n = pick.getIntersectedNode();
        hitInfo2.setText(null);
        if (n == null) {
            pickResult.setText("null");
        } else {
            pickResult.setText(n.getClass().getSimpleName() + "." + n.hashCode());
            if (n instanceof Text t) {
                Point3D p3 = pick.getIntersectedPoint();
                Point2D p = new Point2D(p3.getX(), p3.getY());
                HitInfo h = t.hitTest(p);
                hitInfo2.setText(String.valueOf(h));
            }
        }

        Point2D p = new Point2D(ev.getX(), ev.getY());
        HitInfo h = control.hitTest(p);
        hitInfo.setText(String.valueOf(h));

        if (ev.getEventType() == MouseEvent.MOUSE_CLICKED) {
            if (ev.isShortcutDown()) {
                showCaretShape(new Point2D(ev.getX(), ev.getY()));
            }
        }
    }

    private void showCaretShape(Point2D p) {
        HitInfo h = control.hitTest(p);
        System.out.println("hit=" + h);
        PathElement[] pe = control.caretShape(h.getCharIndex(), h.isLeading());
        caretPath.getElements().setAll(pe);
    }
}
