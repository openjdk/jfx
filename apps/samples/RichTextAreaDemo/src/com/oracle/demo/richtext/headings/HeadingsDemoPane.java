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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import com.oracle.demo.richtext.rta.ROptionPane;
import com.oracle.demo.richtext.util.FX;
import jfx.incubator.scene.control.richtext.RichTextArea;

public class HeadingsDemoPane extends BorderPane {

    public final ROptionPane op;

    public HeadingsDemoPane() {
        HeadingsRTA control = new HeadingsRTA();
        control.loadDocument(loadDocument());

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

        CheckBox wrapText = new CheckBox("wrap text");
        FX.name(wrapText, "wrapText");
        wrapText.selectedProperty().bindBidirectional(control.wrapTextProperty());

        CheckBox lineNumbers = new CheckBox("line numbers");
        FX.name(lineNumbers, "lineNumbers");
        lineNumbers.selectedProperty().addListener((_, _, s) -> {
            control.getHeadingsDecorator().showLineNumbersProperty().set(s);
        });

        op = new ROptionPane();
        op.label("Headings Demo");
        op.option(wrapText);
        op.option(lineNumbers);
        op.option(addButton("Collapse All", () -> control.collapseAllSections()));
        op.option(addButton("Expand All", () -> control.expandAllSections()));

        setCenter(vsplit);
        setRight(op);
    }

    public Button addButton(String name, Runnable action) {
        Button b = new Button(name);
        b.setOnAction((ev) -> {
            action.run();
        });
        return b;
    }

    protected static Pane pane() {
        Pane p = new Pane();
        SplitPane.setResizableWithParent(p, false);
        p.setStyle("-fx-background-color:#dddddd;");
        return p;
    }

    private String loadDocument() {
        try (InputStream is = getClass().getResourceAsStream("document.txt")) {
            if (is == null) {
                throw new IOException("Resource not found: document.txt");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }
}