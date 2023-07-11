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
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

/**
 * Mocks a Notebook Page that Provides a SQL Query Engine Interface
 */
public class NotebookModel2 extends SimpleReadOnlyStyledModel {
    private final SimpleStringProperty query = new SimpleStringProperty();
    private final SimpleObjectProperty<Object> result = new SimpleObjectProperty<>();
    private static final String QUERY = "SELECT * FROM Book WHERE price > 100.00;";
    
    public NotebookModel2() {
        String ARABIC = "arabic";
        String CODE = "code";
        String RED = "red";
        String GREEN = "green";
        String UNDER = "underline";
        String GRAY = "gray";
        String LARGE = "large";
        String EQ = "equation";
        String SUB = "sub";
        
        addSegment("SQL Select", "-fx-font-size:200%;", UNDER);
        nl(2);
        addSegment("The SQL ", null, GRAY);
        addSegment("SELECT ", "-fx-font-weight:bold;"); // FIX does not work on mac
        addSegment("statement returns a result set of records, from one or more tables.", null, GRAY);
        nl(2);
        addSegment("A SELECT statement retrieves zero or more rows from one or more database tables or database views. In most applications, SELECT is the most commonly used data manipulation language (DML) command. As SQL is a declarative programming language, SELECT queries specify a result set, but do not specify how to calculate it. The database translates the query into a \"query plan\" which may vary between executions, database versions and database software. This functionality is called the \"query optimizer\" as it is responsible for finding the best possible execution plan for the query, within applicable constraints.", null, GRAY);
        nl(2);
        addSegment(QUERY, "-fx-font-weight:bold;"); // FIX does not work on mac
        nl(2);
        addNodeSegment(() -> {
            TextField f = new TextField();
            f.setPrefColumnCount(50);
            f.textProperty().bindBidirectional(query);
            return f;
        });
        addSegment(" ", null, GRAY);
        addNodeSegment(() -> {
            Button b = new Button("Run");
            b.setOnAction((ev) -> execute());
            return b;
        });
        nl(2);
        addSegment("Result:", null, GRAY);
        nl();
        addParagraph(() -> new ResultParagraph(result));
        nl(2);
        addSegment("Source: Wikipedia");
        nl();
        addSegment("https://en.wikipedia.org/wiki/Select_(SQL)", null, GREEN, UNDER);
    }
    
    protected void execute() {
        String q = query.get().toLowerCase();
        if(q.equals(QUERY.toLowerCase())) {
            result.set(generate());
        } else {
            result.set("This query is not supported by the demo engine.");
        }
    }

    private String[] generate() {
        return new String[] {
            "Title|Author|Price",
            "SQL Examples and Guide|J.Goodwell|145.55",
            "The Joy of SQL|M.C.Eichler|250.00",
            "An Introduction to SQL|Q.Adams|101.99",
        };
    }
}
