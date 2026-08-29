/*
 * Copyright (c) 2025, 2026, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Popup;
import javafx.stage.PopupWindow.AnchorLocation;
import javafx.stage.WindowEvent;
import com.oracle.tools.fx.monkey.options.BooleanOption;
import com.oracle.tools.fx.monkey.options.EnumOption;
import com.oracle.tools.fx.monkey.sheets.Options;
import com.oracle.tools.fx.monkey.util.CustomPane;
import com.oracle.tools.fx.monkey.util.Formats;
import com.oracle.tools.fx.monkey.util.ImageTools;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;
import com.oracle.tools.fx.monkey.util.Utils;

/**
 * Popup Page.
 */
public class PopupPage extends TestPaneBase {

    enum PContent {
        COMPLEX("Complex UI"),
        IMAGE("Image"),
        LABELS("Labels"),
        ;
        private final String text;
        PContent(String text) { this.text = text; }
        @Override public String toString() { return text; }
    }

    private final ToggleButton button;
    private final Label status;
    private Popup popup;
    private final SimpleObjectProperty<AnchorLocation> anchorLocation = new SimpleObjectProperty<>(AnchorLocation.WINDOW_TOP_LEFT);
    private final SimpleBooleanProperty autoFix = new SimpleBooleanProperty();
    private final SimpleBooleanProperty autoHide = new SimpleBooleanProperty();
    private final SimpleBooleanProperty consumeAutoHidingEvents = new SimpleBooleanProperty();
    private final SimpleBooleanProperty focused = new SimpleBooleanProperty();
    private final SimpleBooleanProperty hideOnEscape = new SimpleBooleanProperty();
    private final SimpleDoubleProperty opacity = new SimpleDoubleProperty(1.0);
    private final SimpleDoubleProperty renderScaleX = new SimpleDoubleProperty(1.0);
    private final SimpleDoubleProperty renderScaleY = new SimpleDoubleProperty(1.0);
    private final SimpleBooleanProperty showing = new SimpleBooleanProperty();
    private final SimpleObjectProperty<PContent> content = new SimpleObjectProperty<>(PContent.LABELS);

    public PopupPage() {
        super("PopupPage");

        button = new ToggleButton("Show Popup");
        button.setOnAction((ev) -> {
            togglePopup();
        });

        status = new Label();
        status.setFont(Font.font("Monospace"));

        OptionPane op = createOptionPane();

        HBox hb = new HBox(4, button);

        BorderPane p = new BorderPane();
        p.setTop(hb);
        p.setBottom(status);
        p.setPadding(new Insets(10));

        setContent(p);
        setOptions(op);
    }

    private OptionPane createOptionPane() {
        OptionPane op = new OptionPane();

        // popup
        op.section("Popup");
        op.option("Anchor Location:", new EnumOption<>("anchorLocation", AnchorLocation.class, anchorLocation));
        op.option(new BooleanOption("autoFix", "auto fix", autoFix));
        op.option(new BooleanOption("autoHide", "auto hide", autoHide));
        op.option(new BooleanOption("consumeAutoHiding", "consume auto hiding events", consumeAutoHidingEvents));
        op.option("Content:", new EnumOption<>("popupContent", PContent.class, content));
        op.option(new BooleanOption("hideOnEscape", "hide on Escape", hideOnEscape));

        // window
        op.section("Window");
        op.option(new BooleanOption("focused", "focused", focused));
        op.option("Opacity:", Options.opacity("opacity", opacity));
        op.option("Render Scale X:", Options.scale("renderScaleX", renderScaleX));
        op.option("Render Scale Y:", Options.scale("renderScaleY", renderScaleY));
        return op;
    }

    private Popup createPopup() {
        Node content = createContent();
        Popup p = new Popup();
        p.addEventFilter(WindowEvent.ANY, (ev) -> {
            System.out.println(ev);
        });

        // popup
        p.setAnchorLocation(anchorLocation.get());
        // TODO     setAnchorX(double)
        // TODO     setAnchorY(double)
        p.setAutoFix(autoFix.get());
        p.setAutoHide(autoHide.get());
        p.setConsumeAutoHidingEvents(consumeAutoHidingEvents.get());
        p.setHideOnEscape(hideOnEscape.get());
        p.getContent().setAll(content);

        // window
        Utils.link(focused, p.focusedProperty(), null);
        p.opacityProperty().bindBidirectional(opacity);
        p.renderScaleXProperty().bindBidirectional(renderScaleX);
        p.renderScaleYProperty().bindBidirectional(renderScaleY);
        Utils.link(showing, p.showingProperty(), null);
        return p;
    }

    private Node createContent() {
        PContent c = content.get();
        if (c == null) {
            c = PContent.LABELS;
        }
        return switch (c) {
        case IMAGE -> ImageTools.createImageView(100, 100);
        case LABELS -> createMenus();
        case COMPLEX -> CustomPane.create();
        };
    }

    private static Node createMenus() {
        VBox b = new VBox();
        b.getChildren().setAll(
            new Label("One"),
            new Label("Two"));
        return b;
    }

    private void togglePopup() {
        if (popup == null) {
            popup = createPopup();
            Point2D p = button.localToScreen(0, button.getHeight());
            popup.show(button, p.getX(), p.getY());
            popup.showingProperty().addListener((_, _, on) -> {
                if (!on) {
                    button.setSelected(false);
                    popup = null;
                    clearStatus();
                }
            });
            status.textProperty().bind(Bindings.createStringBinding(
                () -> {
                    String s = getStatusText(popup);
                    System.out.println(s.replace('\n', ' '));
                    return s;
                },
                popup.xProperty(),
                popup.yProperty(),
                popup.widthProperty(),
                popup.heightProperty()
            ));
        } else {
            popup.hide();
            popup = null;
            button.setSelected(false);
            clearStatus();
        }
    }

    private void clearStatus() {
        status.textProperty().unbind();
        status.setText(null);
    }

    @Override
    public void deactivate() {
        close();
    }

    private void close() {
        if (popup != null) {
            popup.hide();
            popup = null;
            button.setSelected(false);
        }
    }

    private static String getStatusText(Popup p) {
        return
            "P: " + f(p.getX()) + ", " + f(p.getY()) + "\n" +
            "S: " + f(p.getWidth()) + ", " + f(p.getHeight());
    }

    private static String f(double v) {
        return Formats.formatDouble(v);
    }
}
