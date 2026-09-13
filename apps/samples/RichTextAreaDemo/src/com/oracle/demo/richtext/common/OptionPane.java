/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
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

package com.oracle.demo.richtext.common;

import java.util.List;
import java.util.stream.Collectors;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import com.oracle.demo.richtext.util.FX;

/**
 * Option Pane - a vertical option sheet.
 */
public class OptionPane extends VBox {

    public OptionPane() {
        FX.name(this, "OptionPane");
        FX.style(this, "option-pane");
        setFillWidth(true);
    }

    public void label(String text) {
        lastSection().add(new Label(text));
    }

    public void option(Node n) {
        lastSection().add(n);
    }

    public void option(String text, Node n) {
        lastSection().add(new Label(text), n);
    }

    public void add(Node n) {
        lastSection().add(n);
    }

    public void separator() {
        lastSection().add(new Separator(Orientation.HORIZONTAL));
    }

    public void section(String name) {
        section(name, new OptionGridPane());
    }

    private List<TitledPane> getPanes() {
        return getChildren().
            stream().
            filter((n) -> n instanceof TitledPane).
            map((n) -> (TitledPane)n).
            collect(Collectors.toList());
    }

    public void section(String name, OptionGridPane content) {
        TitledPane t = new TitledPane(name, content);
        getChildren().add(t);
    }

    private OptionGridPane lastSection() {
        List<TitledPane> panes = getPanes();
        if (panes.size() == 0) {
            section("Properties");
        }
        panes = getPanes();
        TitledPane t = panes.get(panes.size() - 1);
        return (OptionGridPane)t.getContent();
    }

    private static class OptionGridPane extends GridPane {
        private int row;
        private static final Insets MARGIN = new Insets(1, 4, 0, 4);
        private static final Insets PADDING = new Insets(0, 0, 2, 0);

        public OptionGridPane() {
            setPadding(PADDING);
            setMaxWidth(Double.MAX_VALUE);
        }

        public void label(String text) {
            add(new Label(text));
        }

        public void option(Node n) {
            add(n);
        }

        public void add(Node n) {
            add(n, 0, row, 2, 1);
            setMargin(n, MARGIN);
            setFillHeight(n, Boolean.TRUE);
            setFillWidth(n, Boolean.TRUE);
            row++;
        }

        public void add(Node left, Node right) {
            add(left, 0, row);
            setMargin(left, MARGIN);
            setFillHeight(left, Boolean.TRUE);
            setFillWidth(left, Boolean.TRUE);
            setHgrow(left, Priority.ALWAYS);

            add(right, 1, row);
            setMargin(right, MARGIN);
            setFillHeight(right, Boolean.TRUE);
            setFillWidth(right, Boolean.TRUE);

            row++;
        }
    }
}
