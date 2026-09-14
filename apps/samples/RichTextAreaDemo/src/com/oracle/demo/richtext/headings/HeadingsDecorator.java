/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
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

package com.oracle.demo.richtext.headings;

import java.text.DecimalFormat;
import java.util.Arrays;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import jfx.incubator.scene.control.richtext.SideDecorator;

public class HeadingsDecorator implements SideDecorator {
    private static final String COLLAPSED = "\u25B8"; // ▸
    private static final String EXPANDED = "\u25BE"; // ▾
    private static final DecimalFormat FORMAT = new DecimalFormat("###0");

    private final HeadingsRTA control;
    private final BooleanProperty showLineNumbers = new SimpleBooleanProperty(this, "showLineNumbers", false) {
        @Override
        protected void invalidated() {
            refreshDecorator();
        }
    };

    public HeadingsDecorator(HeadingsRTA control) {
        this.control = control;
    }

    public final BooleanProperty showLineNumbersProperty() {
        return showLineNumbers;
    }

    @Override
    public double getPrefWidth(double height) {
        return showLineNumbersProperty().get() ? 0.0 : 20.0;
    }

    @Override
    public Node getMeasurementNode(int index) {
        String s = FORMAT.format(index + 300);
        char[] cs = new char[s.length()];
        Arrays.fill(cs, '8');
        return createGutterNode(new String(cs), -1);
    }

    @Override
    public Node getNode(int index) {
        return createGutterNode(FORMAT.format(index + 1), index);
    }

    public void refreshDecorator() {
        if (control.getLeftDecorator() == this) {
            control.setLeftDecorator(null);
            control.setLeftDecorator(this);
        }
    }

    private Node createGutterNode(String text, int index) {
        Label chevron = new Label();
        chevron.getStyleClass().add("section-chevron");
        chevron.setOnMousePressed(Event::consume);
        chevron.setOnMouseReleased(Event::consume);
        setChevron(chevron, index);

        Label numberLabel = new Label(text);
        numberLabel.getStyleClass().add("line-number-decorator-label");
        numberLabel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        numberLabel.setMinHeight(1);
        numberLabel.setPrefHeight(1);
        numberLabel.setAlignment(Pos.CENTER_RIGHT);
        numberLabel.visibleProperty().bind(showLineNumbersProperty());
        numberLabel.managedProperty().bind(showLineNumbersProperty());

        HBox container = new HBox(numberLabel, chevron);
        container.getStyleClass().add("line-number-decorator");
        container.setAlignment(Pos.CENTER_RIGHT);
        return container;
    }

    private void setChevron(Label chevron, int index) {
        ParagraphRange section = control.getSection(index);
        boolean collapsed = control.isSectionCollapsed(section);
        chevron.setText(collapsed ? COLLAPSED : EXPANDED);
        chevron.setTooltip(new Tooltip(collapsed ? "Expand this section" : "Collapse this section"));
        chevron.setVisible(section != null);
        chevron.setOnMouseClicked(event -> {
            event.consume();
            control.toggleSection(section);
        });
    }
}
