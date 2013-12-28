/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates.
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
package com.javafx.experiments.scheduleapp.control;

import static com.javafx.experiments.scheduleapp.Theme.*;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;

public class CheckBoxItem<T> extends PopoverBoxItem<T> {
    private static final double GAP = 10;
    private final Text name = new Text();
    private final ImageView arrow = new ImageView(TICK_IMAGE);
    private final BooleanProperty checked = new SimpleBooleanProperty(this, "checked", true);
    public final boolean isChecked() { return checked.get(); }
    public final void setChecked(boolean value) { checked.set(value); }
    public final BooleanProperty checkedProperty() { return checked; }

    public CheckBoxItem(String name, T item, boolean checked) {
        super(name, item);
        this.checked.set(checked);
        this.name.textProperty().bind(nameProperty());
        this.name.setTextOrigin(VPos.TOP);
        this.name.getStyleClass().setAll(".text");
        getChildren().addAll(this.name, arrow);
        arrow.visibleProperty().bind(checkedProperty());

        addEventHandler(
                MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                setChecked(!isChecked());
            }
        });
    }

    @Override protected double computePrefWidth(double height) {
        final Insets insets = getInsets();
        final Node graphic = getGraphic();
        return insets.getLeft() +
                (graphic == null ? 0 : (graphic.prefWidth(-1) + GAP)) +
                name.prefWidth(-1) + GAP + arrow.prefWidth(-1) + insets.getRight();
    }

    @Override protected double computePrefHeight(double width) {
        final Insets insets = getInsets();
        double h = Math.max(name.prefHeight(name.prefWidth(-1)), arrow.prefHeight(-1));
        final Node graphic = getGraphic();
        if (graphic != null) {
            h = Math.max(h, graphic.prefHeight(graphic.prefWidth(-1)));
        }
        return insets.getTop() + h + insets.getBottom();
    }

    @Override protected void layoutChildren() {
        final Insets insets = getInsets();
        final double top = insets.getTop();
        double left = insets.getLeft();
        double rightSide = getWidth() - insets.getRight();
        final double height = getHeight() - top - insets.getBottom();

        final Node graphic = getGraphic();
        if (graphic != null) {
            double graphicWidth = graphic.prefWidth(-1);
            double graphicHeight = graphic.prefHeight(graphicWidth);
            graphic.resizeRelocate((int) left, (int) top, (int) graphicWidth, (int) ((graphicHeight - height) / 2));
            left += graphicWidth + GAP;
        }

        double nameWidth = name.prefWidth(-1);
        double nameHeight = name.prefHeight(nameWidth);
        name.resizeRelocate((int)(left + .5), (int)((top + (height - nameHeight) / 2) + .5), (int)(nameWidth + .5), (int)(nameHeight + .5));

        double arrowWidth = arrow.prefWidth(-1);
        double arrowHeight = arrow.prefHeight(-1);
        arrow.relocate((int) ((rightSide - arrowWidth) + .5), (int) ((top + (height - arrowHeight) / 2) + .5));
    }
}
