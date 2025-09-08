/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import com.oracle.tools.fx.monkey.options.BooleanOption;
import com.oracle.tools.fx.monkey.options.EnumOption;
import com.oracle.tools.fx.monkey.options.GraphicOption;
import com.oracle.tools.fx.monkey.sheets.Options;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;
import com.oracle.tools.fx.monkey.util.Utils;

/**
 * Dialog/DialogPane Page.
 */
public class DialogPage extends TestPaneBase {
    private final ToggleButton button;
    private Dialog dialog;
    // dialog
    private final SimpleStringProperty contentText = new SimpleStringProperty();
    private final SimpleStringProperty headerText = new SimpleStringProperty();
    private final SimpleObjectProperty<Modality> modality = new SimpleObjectProperty<>(Modality.NONE);
    private final SimpleBooleanProperty owner = new SimpleBooleanProperty();
    private final SimpleBooleanProperty resizable = new SimpleBooleanProperty(true);
    private final SimpleBooleanProperty showing = new SimpleBooleanProperty();
    private final SimpleObjectProperty<StageStyle> stageStyle = new SimpleObjectProperty<>(StageStyle.DECORATED);
    private final SimpleStringProperty title = new SimpleStringProperty();
    // dialog pane
    private final SimpleBooleanProperty useDialogPane = new SimpleBooleanProperty(true);
    private final SimpleObjectProperty<Node> dpContent = new SimpleObjectProperty<>();
    private final SimpleStringProperty dpContentText = new SimpleStringProperty();
    private final SimpleObjectProperty<Node> dpExpandableContent = new SimpleObjectProperty<>();
    private final SimpleBooleanProperty dpExpanded = new SimpleBooleanProperty();
    private final SimpleObjectProperty<Node> dpGraphic = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<Node> dpGHeader = new SimpleObjectProperty<>();
    private final SimpleStringProperty dpHeaderText = new SimpleStringProperty();

    public DialogPage() {
        super("DialogPage");

        button = new ToggleButton("Show Dialog");
        button.setOnAction((ev) -> {
            toggleDialog();
        });

        OptionPane op = createOptionPane();

        HBox p = new HBox(4, button);
        p.setPadding(new Insets(4));

        setContent(p);
        setOptions(op);
    }

    private OptionPane createOptionPane() {
        OptionPane op = new OptionPane();

        // dialog pane
        op.section("DialogPane");
        op.option(new BooleanOption("set DialogPane", "setDialogPane", useDialogPane));
        op.option("Content:", new GraphicOption("dpContent", dpContent));
        op.option("Expandable Content:", new GraphicOption("dpExpandableContent", dpExpandableContent));
        op.option(new BooleanOption("expanded", "dpExpanded", dpExpanded));
        op.option("Graphic:", new GraphicOption("dpGraphic", dpGraphic));
        op.option("Header:", new GraphicOption("dpHeader", dpGHeader));
        op.option("Header Text:", textChoices("dpHeaderText", dpHeaderText));

        // dialog
        op.section("Dialog");
        op.option("Content Text:", textChoices("contentText", contentText));
        // graphic
        op.option("Header Text:", textChoices("headerText", headerText));
        op.option(new BooleanOption("resizable", "resizable", resizable));
        op.option("Title:", textChoices("title", title));

        // init
        op.section("Initialization");
        op.option("Stage Style:", new EnumOption("stageStyle", StageStyle.class, stageStyle));
        op.option("Modality:", new EnumOption("modality", Modality.class, modality));
        op.option(new BooleanOption("owner", "set owner", owner));

        return op;
    }

    private Dialog createDialog() {
        Dialog d = new Dialog();
        d.setResult(new Object()); // allow to close

        // init
        d.initStyle(stageStyle.get());
        d.initModality(modality.get());
        d.initOwner(owner.get() ? FX.getParentWindow(this) : null);
        
        // dialog
        d.contentTextProperty().bindBidirectional(contentText);
        d.headerTextProperty().bindBidirectional(headerText);
        d.resizableProperty().bindBidirectional(resizable);
        d.titleProperty().bindBidirectional(title);

        // dialog pane
        if (useDialogPane.get()) {
            DialogPane p = new DialogPane();
            p.contentProperty().bindBidirectional(dpContent);
            p.expandableContentProperty().bindBidirectional(dpExpandableContent);
            p.expandedProperty().bindBidirectional(dpExpanded);
            p.graphicProperty().bindBidirectional(dpGraphic);
            p.headerProperty().bindBidirectional(dpGHeader);
            p.headerTextProperty().bindBidirectional(dpHeaderText);
            d.setDialogPane(p);
        }

        // window
        Utils.link(showing, d.showingProperty(), null);
        return d;
    }

    private static Node textChoices(String name, SimpleStringProperty p) {
        return Options.textOption(name, true, true, p);
    }

    private void toggleDialog() {
        if (dialog == null) {
            dialog = createDialog();
            dialog.show();
            dialog.showingProperty().addListener((s, p, on) -> {
                if (!on) {
                    button.setSelected(false);
                    dialog = null;
                }
            });
        } else {
            dialog.hide();
            dialog = null;
            button.setSelected(false);
        }
    }

    private void close() {
        if (dialog != null) {
            dialog.hide();
            dialog = null;
            button.setSelected(false);
        }
    }

    @Override
    public void deactivate() {
        close();
    }
}
