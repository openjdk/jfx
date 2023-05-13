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

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

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
