/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.oracle.demo.richtext.codearea;

import java.nio.charset.Charset;
import java.util.Base64;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Window;
import javafx.util.StringConverter;
import com.oracle.demo.richtext.rta.FontOption;
import com.oracle.demo.richtext.rta.ROptionPane;
import com.oracle.demo.richtext.util.FX;
import jfx.incubator.scene.control.richtext.CodeArea;
import jfx.incubator.scene.control.richtext.SyntaxDecorator;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.CodeTextModel;
import jfx.incubator.scene.control.richtext.model.StyleAttribute;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;

/**
 * Main Panel contains CodeArea, split panes for quick size adjustment, and an option pane.
 *
 * @author Andy Goryachev
 */
public class CodeAreaDemoPane extends BorderPane {
    public final ROptionPane op;
    public final CodeArea control;

    public CodeAreaDemoPane(CodeTextModel m) {
        FX.name(this, "CodeAreaDemoPane");
        control = new CodeArea(m);

        SplitPane hsplit = new SplitPane(control, pane());
        FX.name(hsplit, "hsplit");
        hsplit.setBorder(null);
        hsplit.setDividerPositions(1.0);
        hsplit.setOrientation(Orientation.HORIZONTAL);

        SplitPane vsplit = new SplitPane(hsplit, pane());
        FX.name(vsplit, "vsplit");
        vsplit.setBorder(null);
        vsplit.setDividerPositions(1.0);
        vsplit.setOrientation(Orientation.VERTICAL);

        FontOption fontOption = new FontOption("font", false, control.fontProperty());

        CheckBox editable = new CheckBox("editable");
        FX.name(editable, "editable");
        editable.selectedProperty().bindBidirectional(control.editableProperty());

        CheckBox wrapText = new CheckBox("wrap text");
        FX.name(wrapText, "wrapText");
        wrapText.selectedProperty().bindBidirectional(control.wrapTextProperty());

        CheckBox displayCaret = new CheckBox("display caret");
        FX.name(displayCaret, "displayCaret");
        displayCaret.selectedProperty().bindBidirectional(control.displayCaretProperty());

        CheckBox fatCaret = new CheckBox("fat caret");
        FX.name(fatCaret, "fatCaret");
        fatCaret.selectedProperty().addListener((s, p, on) -> {
            Node n = control.lookup(".caret");
            if (n != null) {
                if (on) {
                    n.setStyle(
                        "-fx-stroke-width:2; -fx-stroke:red; -fx-effect:dropshadow(gaussian,rgba(0,0,0,.5),5,0,1,1);");
                } else {
                    n.setStyle(null);
                }
            }
        });

        CheckBox highlightCurrentLine = new CheckBox("highlight current line");
        FX.name(highlightCurrentLine, "highlightCurrentLine");
        highlightCurrentLine.selectedProperty().bindBidirectional(control.highlightCurrentParagraphProperty());

        ComboBox<Integer> tabSize = new ComboBox<>();
        FX.name(tabSize, "tabSize");
        tabSize.getItems().setAll(1, 2, 3, 4, 8, 16);
        tabSize.getSelectionModel().selectedItemProperty().addListener((s, p, v) -> {
            control.setTabSize(v);
        });

        CheckBox customPopup = new CheckBox("custom popup menu");
        FX.name(customPopup, "customPopup");
        customPopup.selectedProperty().addListener((s, p, v) -> {
            setCustomPopup(v);
        });

        ComboBox<Insets> contentPadding = new ComboBox<>();
        FX.name(contentPadding, "contentPadding");
        contentPadding.setConverter(new StringConverter<Insets>() {
            @Override
            public String toString(Insets x) {
                if (x == null) {
                    return "null";
                }
                return String.format(
                    "T%d, B%d, L%d, R%d",
                    (int)x.getTop(),
                    (int)x.getBottom(),
                    (int)x.getLeft(),
                    (int)x.getRight()
                );
            }

            @Override
            public Insets fromString(String s) {
                return null;
            }
        });
        contentPadding.getItems().setAll(
            null,
            new Insets(1),
            new Insets(2),
            new Insets(10),
            new Insets(22.22),
            new Insets(50),
            new Insets(100),
            new Insets(5, 10, 15, 20)
        );
        contentPadding.getSelectionModel().selectedItemProperty().addListener((s, p, v) -> {
            control.setContentPadding(v);
        });

        ComboBox<Double> lineSpacing = new ComboBox<>();
        FX.name(lineSpacing, "lineSpacing");
        lineSpacing.getItems().setAll(
            0.0,
            5.0,
            31.415
        );
        lineSpacing.getSelectionModel().selectedItemProperty().addListener((s, p, v) -> {
            setLineSpacing(v);
        });

        CheckBox lineNumbers = new CheckBox("line numbers");
        FX.name(lineNumbers, "lineNumbers");
        lineNumbers.selectedProperty().bindBidirectional(control.lineNumbersEnabledProperty());

        ComboBox<SyntaxDecorator> syntax = new ComboBox<>();
        FX.name(syntax, "syntax");
        syntax.getItems().addAll(
            null,
            new DemoSyntaxDecorator(),
            new JavaSyntaxDecorator()
        );
        syntax.setConverter(new StringConverter<SyntaxDecorator>() {
            @Override
            public String toString(SyntaxDecorator x) {
                return x == null ? "<NULL>" : x.toString();
            }

            @Override
            public SyntaxDecorator fromString(String s) {
                return null;
            }
        });
        syntax.getSelectionModel().selectedItemProperty().addListener((s, p, v) -> {
            control.setSyntaxDecorator(v);
        });

        op = new ROptionPane();
        op.option(editable);
        op.label("Font:");
        op.option(fontOption);
        op.option(wrapText);
        op.option(displayCaret);
        op.option(fatCaret);
        op.option(highlightCurrentLine);
        op.option(lineNumbers);
        op.label("Tab Size:");
        op.option(tabSize);
        op.option(customPopup);
        op.label("Content Padding:");
        op.option(contentPadding);
        op.label("Line Spacing:");
        op.option(lineSpacing);
        op.label("Syntax Highlighter:");
        op.option(syntax);

        setCenter(vsplit);
        setRight(op);

        contentPadding.getSelectionModel().selectFirst();
        lineSpacing.getSelectionModel().selectFirst();
        syntax.getSelectionModel().selectFirst();
    }

    protected static Pane pane() {
        Pane p = new Pane();
        SplitPane.setResizableWithParent(p, false);
        p.setStyle("-fx-background-color:#dddddd;");
        return p;
    }

    public Button addButton(String name, Runnable action) {
        Button b = new Button(name);
        b.setOnAction((ev) -> {
            action.run();
        });

        toolbar().add(b);
        return b;
    }

    public TBar toolbar() {
        if (getTop() instanceof TBar) {
            return (TBar)getTop();
        }

        TBar t = new TBar();
        setTop(t);
        return t;
    }

    public Window getWindow() {
        Scene s = getScene();
        if (s != null) {
            return s.getWindow();
        }
        return null;
    }

    public void setOptions(Node n) {
        setRight(n);
    }

    protected String generateStylesheet(boolean fat) {
        String s = ".rich-text-area .caret { -fx-stroke-width:" + (fat ? 2 : 1) + "; }";
        return "data:text/css;base64," + Base64.getEncoder().encodeToString(s.getBytes(Charset.forName("utf-8")));
    }

    protected void setCustomPopup(boolean on) {
        if (on) {
            ContextMenu m = new ContextMenu();
            m.getItems().add(new MenuItem("Dummy")); // otherwise no popup is shown
            m.addEventFilter(Menu.ON_SHOWING, (ev) -> {
                m.getItems().clear();
                populatePopupMenu(m.getItems());
            });
            control.setContextMenu(m);
        } else {
            control.setContextMenu(null);
        }
    }

    protected void populatePopupMenu(ObservableList<MenuItem> items) {
        boolean sel = control.hasNonEmptySelection();
        boolean paste = true; // would be easier with Actions (findFormatForPaste() != null);

        MenuItem m;
        items.add(m = new MenuItem("Undo"));
        m.setOnAction((ev) -> control.undo());
        m.setDisable(!control.isUndoable());

        items.add(m = new MenuItem("Redo"));
        m.setOnAction((ev) -> control.redo());
        m.setDisable(!control.isRedoable());

        items.add(new SeparatorMenuItem());

        items.add(m = new MenuItem("Cut"));
        m.setOnAction((ev) -> control.cut());
        m.setDisable(!sel);

        items.add(m = new MenuItem("Copy"));
        m.setOnAction((ev) -> control.copy());
        m.setDisable(!sel);

        items.add(m = new MenuItem("Paste"));
        m.setOnAction((ev) -> control.paste());
        m.setDisable(!paste);

        items.add(new SeparatorMenuItem());

        items.add(m = new MenuItem("Select All"));
        m.setOnAction((ev) -> control.selectAll());
    }

    protected <V> void apply(StyleAttribute<V> attr, V val) {
        TextPos ca = control.getCaretPosition();
        TextPos an = control.getAnchorPosition();
        StyleAttributeMap a = StyleAttributeMap.builder().set(attr, val).build();
        control.applyStyle(ca, an, a);
    }

    protected void setLineSpacing(double x) {
        control.setLineSpacing(x);
    }

    private <V> void applyStyle(StyleAttribute<V> a, V val) {
        TextPos ca = control.getCaretPosition();
        TextPos an = control.getAnchorPosition();
        StyleAttributeMap m = StyleAttributeMap.of(a, val);
        control.applyStyle(ca, an, m);
    }

    //

    public static class TBar extends HBox {
        public TBar() {
            setFillHeight(true);
            setAlignment(Pos.CENTER_LEFT);
            setSpacing(2);
        }

        public <T extends Node> T add(T n) {
            getChildren().add(n);
            return n;
        }

        public void addAll(Node... nodes) {
            for (Node n : nodes) {
                add(n);
            }
        }
    }
}
