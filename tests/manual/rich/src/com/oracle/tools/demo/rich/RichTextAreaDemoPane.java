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

package com.oracle.tools.demo.rich;

import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Objects;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.incubator.scene.control.rich.RichTextArea;
import javafx.incubator.scene.control.rich.SideDecorator;
import javafx.incubator.scene.control.rich.TextPos;
import javafx.incubator.scene.control.rich.model.EditableRichTextModel;
import javafx.incubator.scene.control.rich.model.StyleAttribute;
import javafx.incubator.scene.control.rich.model.StyleAttrs;
import javafx.incubator.scene.control.rich.model.StyledTextModel;
import javafx.incubator.scene.control.rich.skin.LineNumberDecorator;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.Window;
import javafx.util.Duration;
import javafx.util.StringConverter;

/**
 * Main Panel contains RichTextArea, split panes for quick size adjustment, and an option pane.
 */
public class RichTextAreaDemoPane extends BorderPane { 
    enum Decorator {
        NULL,
        LINE_NUMBERS,
        COLORS
    }

    private static StyledTextModel globalModel;
    public final ROptionPane op;
    public final RichTextArea control;
    public final ComboBox<ModelChoice> modelField;

    public RichTextAreaDemoPane(boolean useContentSize) {
        FX.name(this, "RichTextAreaDemoPane");

        control = new RichTextArea();
        control.setUseContentHeight(useContentSize);
        control.setUseContentWidth(useContentSize);

        Node contentNode;
        if (useContentSize) {
            contentNode = new ScrollPane(control);
        } else {
            contentNode = control;
        }

        SplitPane hsplit = new SplitPane(contentNode, pane());
        FX.name(hsplit, "hsplit");
        hsplit.setBorder(null);
        hsplit.setDividerPositions(1.0);
        hsplit.setOrientation(Orientation.HORIZONTAL);
        
        SplitPane vsplit = new SplitPane(hsplit, pane());
        FX.name(vsplit, "vsplit");
        vsplit.setBorder(null);
        vsplit.setDividerPositions(1.0);
        vsplit.setOrientation(Orientation.VERTICAL);
        
        modelField = new ComboBox<>();
        FX.name(modelField, "modelField");
        modelField.getItems().setAll(ModelChoice.values());
        
        FontSelector fontSelector = new FontSelector("font", control::setFont);
        
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
        fatCaret.selectedProperty().addListener((s,p,on) -> {
            Node n = control.lookup(".caret");
            if(n != null) {
                if(on) {
                    n.setStyle("-fx-stroke-width:2; -fx-stroke:red; -fx-effect:dropshadow(gaussian,rgba(0,0,0,.5),5,0,1,1);");
                } else {
                    n.setStyle(null);
                }
            }
        });
        
        CheckBox fastBlink = new CheckBox("blink fast");
        FX.name(fastBlink, "fastBlink");
        fastBlink.selectedProperty().addListener((s,p,on) -> {
            control.setCaretBlinkPeriod(on ? Duration.millis(200) : Duration.millis(500));
        });
        
        CheckBox highlightCurrentLine = new CheckBox("highlight current line");
        FX.name(highlightCurrentLine, "highlightCurrentLine");
        highlightCurrentLine.selectedProperty().addListener((s,p,on) -> {
            control.setHighlightCurrentParagraph(on);
        });
        
//        ComboBox<Integer> tabSize = new ComboBox<>();
//        FX.name(tabSize, "tabSize");
//        tabSize.getItems().setAll(1, 2, 3, 4, 8, 16);
//        tabSize.getSelectionModel().selectedItemProperty().addListener((s,p,v) -> {
//            control.setTabSize(v);
//        });
        
        Button reloadModelButton = new Button("Reload Model");
        reloadModelButton.setOnAction((ev) -> reloadModel());
        
        CheckBox customPopup = new CheckBox("custom popup menu");
        FX.name(customPopup, "customPopup");
        customPopup.selectedProperty().addListener((s,p,v) -> {
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
        contentPadding.getSelectionModel().selectedItemProperty().addListener((s,p,v) -> {
            control.setContentPadding(v);
        });
        
        ComboBox<Decorator> leftDecorator = new ComboBox<>();
        FX.name(leftDecorator, "leftDecorator");
        leftDecorator.getItems().setAll(Decorator.values());
        leftDecorator.getSelectionModel().selectedItemProperty().addListener((s,p,v) -> {
            control.setLeftDecorator(createDecorator(v));
        });
        
        ComboBox<Decorator> rightDecorator = new ComboBox<>();
        FX.name(rightDecorator, "rightDecorator");
        rightDecorator.getItems().setAll(Decorator.values());
        rightDecorator.getSelectionModel().selectedItemProperty().addListener((s,p,v) -> {
            control.setRightDecorator(createDecorator(v));
        });
        
        CheckBox trackWidth = new CheckBox("use content width");
        FX.name(trackWidth, "trackWidth");
        trackWidth.selectedProperty().bindBidirectional(control.useContentWidthProperty());
        
        CheckBox trackHeight = new CheckBox("use content height");
        FX.name(trackHeight, "trackHeight");
        trackHeight.selectedProperty().bindBidirectional(control.useContentHeightProperty());
        
        op = new ROptionPane();
        op.label("Model:");
        op.option(modelField);
        op.option(editable);
        op.option(reloadModelButton);
        op.label("Font:");
        op.option(fontSelector.fontNode());
        op.label("Size:");
        op.option(fontSelector.sizeNode());
        op.option(wrapText);
        op.option(displayCaret);
        op.option(fatCaret);
        op.option(fastBlink);
        op.option(highlightCurrentLine);
//        op.label("Tab Size:");
//        op.option(tabSize);
        op.option(customPopup);
        op.label("Content Padding:");
        op.option(contentPadding);
        op.label("Decorators:");
        op.option(leftDecorator);
        op.option(rightDecorator);
        op.option(trackWidth);
        op.option(trackHeight);

        setCenter(vsplit);

        ScrollPane sp = new ScrollPane(op);
        sp.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        sp.setHbarPolicy(ScrollBarPolicy.NEVER);
        setRight(sp);

        modelField.getSelectionModel().selectFirst();
        contentPadding.getSelectionModel().selectFirst();
        leftDecorator.getSelectionModel().selectFirst();
        rightDecorator.getSelectionModel().selectFirst();

        Platform.runLater(() -> {
            // all this to make sure restore settings works correctly with second window loading the same model
            if (globalModel == null) {
                globalModel = createModel();
            }
            control.setModel(globalModel);

            modelField.getSelectionModel().selectedItemProperty().addListener((s, p, c) -> {
                updateModel();
            });
        });
    }

    protected SideDecorator createDecorator(Decorator d) {
        if(d != null) {
            switch(d) {
            case COLORS:
                return new DemoColorSideDecorator();
            case LINE_NUMBERS:
                return new LineNumberDecorator();
            }
        }
        return null;
    }

    protected void updateModel() {
        globalModel = createModel();
        control.setModel(globalModel);
    }
    
    protected void reloadModel() {
        control.setModel(null);
        updateModel();
    }
    
    private StyledTextModel createModel() {
        ModelChoice m = modelField.getSelectionModel().getSelectedItem();
        return ModelChoice.create(m);
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
        if(getTop() instanceof TBar) {
            return (TBar)getTop();
        }
        
        TBar t = new TBar();
        setTop(t);
        return t;
    }
    
    public Window getWindow() {
        Scene s = getScene();
        if(s != null) {
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
        if(on) {
            ContextMenu m = new ContextMenu();
            m.getItems().add(new MenuItem("Dummy")); // otherwise no popup is shown
            m.addEventFilter(Menu.ON_SHOWING, (ev) -> {
                m.getItems().clear();
                populateCustomPopupMenu(m.getItems());
            });
            control.setContextMenu(m);
        } else {
            control.setContextMenu(null);
        }
    }

    protected void populateCustomPopupMenu(ObservableList<MenuItem> items) {
        boolean sel = control.hasNonEmptySelection();
        boolean paste = true; // would be easier with Actions (findFormatForPaste() != null);
        boolean styled = (control.getModel() instanceof EditableRichTextModel);

        items.add(new MenuItem("★ Custom Context Menu"));

        items.add(new SeparatorMenuItem());

        Menu m2;
        MenuItem m;
        CheckMenuItem cm;
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
        
        items.add(m = m2 = new Menu("Copy Special..."));
        {
            DataFormat[] fs = control.getModel().getSupportedDataFormats(true);
            for (DataFormat f : fs) {
                String name = f.toString();
                m2.getItems().add(m = new MenuItem(name));
                m.setOnAction((ev) -> control.copy(f));
            }
        }

        items.add(m = new MenuItem("Paste"));
        m.setOnAction((ev) -> control.paste());
        m.setDisable(!paste);
        
        items.add(m = m2 = new Menu("Paste Special..."));
        m.setDisable(!paste);
        {
            DataFormat[] fs = control.getModel().getSupportedDataFormats(false);
            for (DataFormat f : fs) {
                if (Clipboard.getSystemClipboard().hasContent(f)) {
                    String name = f.toString();
                    m2.getItems().add(m = new MenuItem(name));
                    m2.setOnAction((ev) -> control.paste(f));
                    m2.setDisable(!paste);
                }
            }
        }

        items.add(m = new MenuItem("Paste and Match Style"));
        m.setOnAction((ev) -> control.pastePlainText());
        m.setDisable(!paste);

        if (styled) {
            StyleAttrs a = control.getActiveStyleAttrs();
            items.add(new SeparatorMenuItem());

            items.add(m = new MenuItem("Bold"));
            m.setOnAction((ev) -> applyStyle(StyleAttrs.BOLD, !a.getBoolean(StyleAttrs.BOLD)));
            m.setDisable(!sel);

            items.add(m = new MenuItem("Italic"));
            m.setOnAction((ev) -> applyStyle(StyleAttrs.ITALIC, !a.getBoolean(StyleAttrs.ITALIC)));
            m.setDisable(!sel);

            items.add(m = new MenuItem("Strike Through"));
            m.setOnAction((ev) -> applyStyle(StyleAttrs.STRIKE_THROUGH, !a.getBoolean(StyleAttrs.STRIKE_THROUGH)));
            m.setDisable(!sel);

            items.add(m = new MenuItem("Underline"));
            m.setOnAction((ev) -> applyStyle(StyleAttrs.UNDERLINE, !a.getBoolean(StyleAttrs.UNDERLINE)));
            m.setDisable(!sel);

            items.add(m2 = new Menu("Text Color"));
            colorMenu(m2, sel, Color.BLACK);
            colorMenu(m2, sel, Color.DARKGRAY);
            colorMenu(m2, sel, Color.GRAY);
            colorMenu(m2, sel, Color.LIGHTGRAY);
            colorMenu(m2, sel, Color.GREEN);
            colorMenu(m2, sel, Color.RED);
            colorMenu(m2, sel, Color.BLUE);
            colorMenu(m2, sel, null);

            items.add(m2 = new Menu("Text Size"));
            sizeMenu(m2, sel, 96);
            sizeMenu(m2, sel, 72);
            sizeMenu(m2, sel, 48);
            sizeMenu(m2, sel, 36);
            sizeMenu(m2, sel, 24);
            sizeMenu(m2, sel, 18);
            sizeMenu(m2, sel, 16);
            sizeMenu(m2, sel, 14);
            sizeMenu(m2, sel, 12);
            sizeMenu(m2, sel, 10);
            sizeMenu(m2, sel, 9);
            sizeMenu(m2, sel, 8);
            sizeMenu(m2, sel, 6);

            items.add(m2 = new Menu("Font Family"));
            fontMenu(m2, sel, "System");
            fontMenu(m2, sel, "Serif");
            fontMenu(m2, sel, "Sans-serif");
            fontMenu(m2, sel, "Cursive");
            fontMenu(m2, sel, "Fantasy");
            fontMenu(m2, sel, "Monospaced");
            m2.getItems().add(new SeparatorMenuItem());
            fontMenu(m2, sel, "Arial");
            fontMenu(m2, sel, "Courier New");
            fontMenu(m2, sel, "Times New Roman");
            fontMenu(m2, sel, "null");
        }

        if (styled) {
            StyleAttrs a = control.getActiveStyleAttrs();
            items.add(new SeparatorMenuItem());

            items.add(m2 = new Menu("Alignment"));
            alignmentMenu(m2, "Left", TextAlignment.LEFT);
            alignmentMenu(m2, "Center", TextAlignment.CENTER);
            alignmentMenu(m2, "Right", TextAlignment.RIGHT);
            alignmentMenu(m2, "Justify", TextAlignment.JUSTIFY);

            items.add(m2 = new Menu("Line Spacing"));
            lineSpacingMenu(m2, 0);
            lineSpacingMenu(m2, 1);
            lineSpacingMenu(m2, 10);
            lineSpacingMenu(m2, 30);

            items.add(m2 = new Menu("Space"));
            spaceMenu(m2, "All", 30, 30, 30, 30);
            spaceMenu(m2, "Above", 30, 0, 0, 0);
            spaceMenu(m2, "Below", 0, 0, 30, 0);
            spaceMenu(m2, "Left", 0, 0, 0, 30);
            spaceMenu(m2, "Right", 0, 30, 0, 0);
            spaceMenu(m2, "None", 0, 0, 0, 0);
            
            items.add(cm = new CheckMenuItem("RTL Orientation"));
            cm.setSelected(a.getBoolean(StyleAttrs.RIGHT_TO_LEFT));
            cm.setOnAction((ev) -> applyStyle(StyleAttrs.RIGHT_TO_LEFT, !a.getBoolean(StyleAttrs.RIGHT_TO_LEFT)));

            items.add(m2 = new Menu("Background Color"));
            backgroundMenu(m2, "Gray", Color.GRAY, 1.0);
            backgroundMenu(m2, "Gray 10%", Color.GRAY, 0.1);
            backgroundMenu(m2, "Gray 20%", Color.GRAY, 0.2);
            backgroundMenu(m2, "Yellow", Color.YELLOW, 1.0);

            items.add(m2 = new Menu("Bullet"));
            bulletMenu(m2, a, "None", null);
            bulletMenu(m2, a, "●", "●");
            bulletMenu(m2, a, "○", "○");
            bulletMenu(m2, a, "♣", "♣");
        }

        items.add(new SeparatorMenuItem());

        items.add(m = new MenuItem("Select All"));
        m.setOnAction((ev) -> control.selectAll());
    }

    private void bulletMenu(Menu menu, StyleAttrs a, String name, String bullet) {
        CheckMenuItem m = new CheckMenuItem(name);
        menu.getItems().add(m);
        m.setSelected(Objects.equals(bullet, a.getBullet()));
        m.setOnAction((ev) -> {
            applyStyle(StyleAttrs.BULLET, bullet);
        });
    }

    private void alignmentMenu(Menu menu, String name, TextAlignment a) {
        MenuItem m = new MenuItem(name);
        menu.getItems().add(m);
        m.setOnAction((ev) -> {
            applyStyle(StyleAttrs.TEXT_ALIGNMENT, a);
        });
    }

    private void lineSpacingMenu(Menu menu, double value) {
        MenuItem m = new MenuItem(String.valueOf(value));
        menu.getItems().add(m);
        m.setOnAction((ev) -> {
            applyStyle(StyleAttrs.LINE_SPACING, value);
        });
    }

    private void spaceMenu(Menu menu, String name, double top, double right, double bottom, double left) {
        MenuItem m = new MenuItem(name);
        menu.getItems().add(m);
        m.setOnAction((ev) -> {
            applyStyle(StyleAttrs.SPACE_ABOVE, top);
            applyStyle(StyleAttrs.SPACE_BELOW, bottom);
            applyStyle(StyleAttrs.SPACE_LEFT, left);
            applyStyle(StyleAttrs.SPACE_RIGHT, right);
        });
    }

    private void backgroundMenu(Menu menu, String name, Color color, double alpha) {
        Color c = FX.alpha(color, alpha);
        MenuItem m = new MenuItem(name);
        menu.getItems().add(m);
        m.setOnAction((ev) -> {
            applyStyle(StyleAttrs.BACKGROUND, c);
        });
    }

    private void fontMenu(Menu menu, boolean selected, String family) {
        MenuItem m = new MenuItem(family);
        m.setDisable(!selected);
        m.setOnAction((ev) -> applyStyle(StyleAttrs.FONT_FAMILY, family));
        menu.getItems().add(m);
    }

    private void sizeMenu(Menu menu, boolean selected, double size) {
        MenuItem m = new MenuItem(String.valueOf(size));
        m.setDisable(!selected);
        m.setOnAction((ev) -> applyStyle(StyleAttrs.FONT_SIZE, size));
        menu.getItems().add(m);
    }
    
    private void colorMenu(Menu menu, boolean selected, Color color) {
        int w = 16;
        int h = 16;
        Canvas c = new Canvas(w, h);
        GraphicsContext g = c.getGraphicsContext2D();
        if(color != null) {
            g.setFill(color);
            g.fillRect(0, 0, w, h);
        }
        g.setStroke(Color.DARKGRAY);
        g.strokeRect(0, 0, w, h);
        
        MenuItem m = new MenuItem(null, c);
        m.setDisable(!selected);
        m.setOnAction((ev) -> applyStyle(StyleAttrs.TEXT_COLOR, color));
        menu.getItems().add(m);
    }
    
    private <T> void applyStyle(StyleAttribute<T> a, T val) {
        TextPos ca = control.getCaretPosition();
        TextPos an = control.getAnchorPosition();
        StyleAttrs m = StyleAttrs.of(a, val);
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
