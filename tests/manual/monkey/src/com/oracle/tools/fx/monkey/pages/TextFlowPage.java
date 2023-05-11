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

import com.oracle.tools.fx.monkey.util.FontSelector;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.ShowCharacterRuns;
import com.oracle.tools.fx.monkey.util.Templates;
import com.oracle.tools.fx.monkey.util.TestPaneBase;
import com.oracle.tools.fx.monkey.util.TextSelector;
import com.oracle.tools.fx.monkey.util.Utils;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Font;
import javafx.scene.text.HitInfo;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * TextFlow Page
 */
public class TextFlowPage extends TestPaneBase {
    protected final TextSelector textSelector;
    protected final FontSelector fontSelector;
    protected final CheckBox showChars;
    protected final CheckBox showCaretPath;
    protected final TextFlow control;
    protected final Label pickResult;
    protected final Label hitInfo;
    protected final Label hitInfo2;
    protected final Path caretPath;
    private static final String INLINE = "$INLINE";
    private static final String RICH_TEXT = "$RICH";

    public TextFlowPage() {
        setId("TextFlowPage");

        control = new TextFlow();
        control.addEventHandler(MouseEvent.ANY, this::handleMouseEvent);

        pickResult = new Label();

        hitInfo = new Label();

        hitInfo2 = new Label();

        caretPath = new Path();
        caretPath.setStrokeWidth(1);
        caretPath.setStroke(Color.RED);
        caretPath.setManaged(false);

        textSelector = TextSelector.fromPairs(
            "textSelector",
            (t) -> updateControl(),
            Utils.combine(
                Templates.multiLineTextPairs(),
                "Inline Nodes", INLINE,
                "Rich Text", RICH_TEXT
            )
        );

        fontSelector = new FontSelector("font", (f) -> updateControl());

        showChars = new CheckBox("show characters");
        showChars.setId("showChars");
        showChars.selectedProperty().addListener((p) -> {
            updateControl();
        });

        showCaretPath = new CheckBox("show caret path");
        showCaretPath.setId("showCaretPath");
        showCaretPath.selectedProperty().addListener((p) -> {
            updateControl();
        });

        OptionPane p = new OptionPane();
        p.label("Text:");
        p.option(textSelector.node());
        p.label("Font:");
        p.option(fontSelector.fontNode());
        p.label("Font Size:");
        p.option(fontSelector.sizeNode());
        p.option(showChars);
        p.option(showCaretPath);
        p.option(new Separator(Orientation.HORIZONTAL));
        p.label("Pick Result:");
        p.option(pickResult);
        p.label("Text.hitTest:");
        p.option(hitInfo2);
        p.label("TextFlow.hitTest:");
        p.option(hitInfo);

        setContent(control);
        setOptions(p);

        fontSelector.selectSystemFont();
        textSelector.selectFirst();
    }

    protected void updateControl() {
        Font f = fontSelector.getFont();
        String text = textSelector.getSelectedText();
        Node[] ts = createTextArray(text, f);
        control.getChildren().setAll(ts);

        if (showChars.isSelected()) {
            Group g = ShowCharacterRuns.createFor(control);
            control.getChildren().add(g);
        }

        if (showCaretPath.isSelected()) {
            caretPath.getElements().clear();
            control.getChildren().add(caretPath);

            int len = computeTextLength(control);
            for (int i = 0; i < len; i++) {
                PathElement[] es = control.caretShape(i, true);
                caretPath.getElements().addAll(es);
            }
        }
    }

    /** TextFlow.getTextLength() */
    private static int computeTextLength(TextFlow f) {
        int len = 0;
        for (Node n: f.getChildrenUnmodifiable()) {
            if (n instanceof Text t) {
                len += t.getText().length();
            }
            // embedded nodes do not have an associated text
        }
        return len;
    }

    protected Node[] createTextArray(String text, Font f) {
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
                t("italic ", f, "-fx-font-style:italic;"),
                t("underline ", f, "-fx-underline:true;"),
                t(Templates.TWO_EMOJIS, f)
            };
        } else {
            return new Node[] { t(text, f) };
        }
    }

    protected static Text t(String text, Font f) {
        Text t = new Text(text);
        t.setFont(f);
        return t;
    }

    protected static Text t(String text, Font f, String style) {
        Text t = new Text(text);
        t.setFont(f);
        t.setStyle(style);
        return t;
    }

    protected void handleMouseEvent(MouseEvent ev) {
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
    }
}
