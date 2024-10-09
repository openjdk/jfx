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

package com.oracle.demo.richtext.rta;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * Part of the code cell.
 *
 * @author Andy Goryachev
 */
public class ResultParagraph extends BorderPane {
    SimpleObjectProperty<Object> result = new SimpleObjectProperty<Object>();

    public ResultParagraph(SimpleObjectProperty<Object> src) {
        result.bind(src);
        result.addListener((s, p, c) -> {
            update();
        });
        update();
        setPrefSize(600, 200);
    }

    protected void update() {
        Node n = getNode();
        setCenter(n);
    }

    protected Node getNode() {
        Object r = result.get();
        if (r instanceof String s) {
            Text t = new Text(s);
            t.setStyle("-fx-fill:red;");

            TextFlow f = new TextFlow();
            f.getChildren().add(t);
            return f;
        } else if (r instanceof String[] ss) {
            DataFrame f = DataFrame.parse(ss);
            TableView<String[]> t = new TableView<>();

            String[] cols = f.getColumnNames();
            for (int i=0; i<cols.length; i++) {
                String col = cols[i];
                TableColumn<String[], String> c = new TableColumn<>(col);
                int ix = i;
                c.setCellValueFactory((d) -> {
                    String[] row = d.getValue();
                    String s = row[ix];
                    return new SimpleStringProperty(s);
                });
                t.getColumns().add(c);
            }
            for (int i = 0; i < f.getRowCount(); i++) {
                t.getItems().add(f.getRow(i));
            }
            t.prefWidthProperty().bind(widthProperty());
            return t;
        }
        return null;
    }
}
