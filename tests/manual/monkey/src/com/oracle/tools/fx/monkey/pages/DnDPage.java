/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import com.oracle.tools.fx.monkey.options.BooleanOption;
import com.oracle.tools.fx.monkey.options.DoubleOption;
import com.oracle.tools.fx.monkey.options.EnumOption;
import com.oracle.tools.fx.monkey.sheets.Options;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;
import com.oracle.tools.fx.monkey.util.Utils;

/**
 * Drag and Drop Test Page.
 */
public class DnDPage extends TestPaneBase {
    enum DragType {
        DRAG_AND_DROP,
        FULL,
        SIMPLE
    }

    private final Label source;
    private final Label target;
    private final SimpleObjectProperty<DragType> type = new SimpleObjectProperty<>(DragType.DRAG_AND_DROP);
    private final SimpleBooleanProperty acceptsCopy = new SimpleBooleanProperty(true);
    private final SimpleBooleanProperty acceptsLink = new SimpleBooleanProperty();
    private final SimpleBooleanProperty acceptsMove = new SimpleBooleanProperty();
    private final SimpleObjectProperty<Image> dragImage = new SimpleObjectProperty<>();
    private final SimpleDoubleProperty offsetX = new SimpleDoubleProperty();
    private final SimpleDoubleProperty offsetY = new SimpleDoubleProperty();

    public DnDPage() {
        super("DnDPage");

        source = new Label("DRAG ME");
        source.setStyle("-fx-font-size: 24; -fx-border-width:1; -fx-border-color:red;");

        target = new Label("DROP HERE");
        target.setStyle("-fx-font-size: 24; -fx-border-width:1; -fx-border-color:red;");

        HBox hb = new HBox(5, source, target);
        hb.setAlignment(Pos.CENTER);
        hb.setPadding(new Insets(10));

        source.setOnDragDetected((ev) -> {
            DragType t = type.get();
            switch(t) {
            case DRAG_AND_DROP:
                Dragboard db = source.startDragAndDrop(TransferMode.ANY);
                ClipboardContent content = new ClipboardContent();
                content.putString(source.getText());
                db.setContent(content);

                Image im = dragImage.get();
                db.setDragView(im);
                db.setDragViewOffsetX(offsetX.get());
                db.setDragViewOffsetY(offsetY.get());
                break;
            case FULL:
                source.startFullDrag();
                break;
            case SIMPLE:
                break;
            default:
                throw new Error("?" + t);
            }
        });

        source.setOnDragDone((ev) -> {
            //print(ev);
        });

        target.setOnDragOver((ev) -> {
            //print(ev);
            if (ev.getGestureSource() != target && ev.getDragboard().hasString()) {
                ArrayList<TransferMode> a = new ArrayList<>();
                if (acceptsCopy.get()) {
                    a.add(TransferMode.COPY);
                }
                if (acceptsLink.get()) {
                    a.add(TransferMode.LINK);
                }
                if (acceptsMove.get()) {
                    a.add(TransferMode.MOVE);
                }
                TransferMode[] modes = a.toArray(TransferMode[]::new);
                ev.acceptTransferModes(modes);
            }
        });

        target.setOnDragEntered((ev) -> {
            //print(ev);
            target.setBackground(Background.fill(Color.YELLOW));
        });

        target.setOnDragExited((ev) -> {
            //print(ev);
            target.setBackground(null);
        });

        target.setOnDragDropped((ev) -> {
            //print(ev);
        });

        // listeners
        source.addEventHandler(DragEvent.ANY, (ev) -> {
            print(ev);
        });
        target.addEventHandler(DragEvent.ANY, (ev) -> {
            print(ev);
        });
        source.addEventHandler(MouseEvent.ANY, (ev) -> {
            print(ev);
        });
        target.addEventHandler(MouseEvent.ANY, (ev) -> {
            print(ev);
        });

        BorderPane bp = new BorderPane();
        bp.setTop(hb);

        OptionPane op = new OptionPane();
        op.section("Source");
        op.option("Drag Mode:", createTypeOption("type", type));
        op.label("Drag View Image:");
        op.option(Options.createImageOption("image", dragImage));
        op.option("Offset X:", doubleOption("offsetX", offsetX));
        op.option("Offset Y:", doubleOption("offsetY", offsetY));
        op.section("Target");
        op.option(new BooleanOption("copy", "accepts COPY", acceptsCopy));
        op.option(new BooleanOption("link", "accepts LINK", acceptsLink));
        op.option(new BooleanOption("move", "accepts MOVE", acceptsMove));

        setContent(bp);
        setOptions(op);
    }

    private Node createTypeOption(String name, SimpleObjectProperty<DragType> p) {
        EnumOption<DragType> op = new EnumOption<>(name, false, DragType.class, p);
        return op;
    }

    private Node doubleOption(String name, DoubleProperty p) {
        DoubleOption d = new DoubleOption(name, p);
        d.addChoice("0", Double.valueOf(0));
        d.addChoice("10", 10.0);
        d.addChoice("33.3", 33.3);
        d.addChoice("100", 100.0);
        d.addChoice("Double.MAX_VALUE", Double.MAX_VALUE);
        d.addChoice("Double.MIN_VALUE", Double.MIN_VALUE);
        d.addChoice("Double.POSITIVE_INFINITY", Double.POSITIVE_INFINITY);
        d.addChoice("NaN", Double.NaN);
        d.selectInitialValue();
        return d;
    }

    private void print(DragEvent ev) {
        StringBuilder sb = new StringBuilder();
        sb.append("{event=" + ev.getEventType());
        sb.append(", x/y=(").append(Utils.f2(ev.getX())).append(", ").append(Utils.f2(ev.getY()));
        sb.append("), screen=(").append(Utils.f2(ev.getScreenX())).append(", ").append(Utils.f2(ev.getScreenY()));
        sb.append("), scene=(").append(Utils.f2(ev.getSceneX())).append(", ").append(Utils.f2(ev.getSceneY()));
        sb.append(")}");
        System.out.println(sb);
    }

    private void print(MouseEvent ev) {
        StringBuilder sb = new StringBuilder();
        sb.append("{event=" + ev.getEventType());
        sb.append(", x/y=(").append(Utils.f2(ev.getX())).append(", ").append(Utils.f2(ev.getY()));
        sb.append("), screen=(").append(Utils.f2(ev.getScreenX())).append(", ").append(Utils.f2(ev.getScreenY()));
        sb.append("), scene=(").append(Utils.f2(ev.getSceneX())).append(", ").append(Utils.f2(ev.getSceneY()));
        sb.append(")}");
        System.out.println(sb);
    }
}
