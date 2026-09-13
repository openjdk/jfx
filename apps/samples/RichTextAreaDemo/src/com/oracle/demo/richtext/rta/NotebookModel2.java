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
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import jfx.incubator.scene.control.richtext.model.SimpleViewOnlyStyledModel;

/**
 * Mocks a Notebook Page that Provides a SQL Query Engine Interface
 *
 * @author Andy Goryachev
 */
public class NotebookModel2 extends SimpleViewOnlyStyledModel {
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

        addWithInlineAndStyleNames("SQL Select", "-fx-font-size:200%;", UNDER);
        nl(2);
        addWithStyleNames("The SQL ", GRAY);
        addWithInlineStyle("SELECT ", "-fx-font-weight:bold;"); // FIX does not work on mac
        addWithStyleNames("statement returns a result set of records, from one or more tables.", GRAY);
        nl(2);
        addWithStyleNames("A SELECT statement retrieves zero or more rows from one or more database tables or database views. In most applications, SELECT is the most commonly used data manipulation language (DML) command. As SQL is a declarative programming language, SELECT queries specify a result set, but do not specify how to calculate it. The database translates the query into a \"query plan\" which may vary between executions, database versions and database software. This functionality is called the \"query optimizer\" as it is responsible for finding the best possible execution plan for the query, within applicable constraints.", GRAY);
        nl(2);
        addWithInlineStyle(QUERY, "-fx-font-weight:bold;"); // FIX does not work on mac
        nl(2);
        addNodeSegment(() -> {
            TextField f = new TextField();
            f.setPrefColumnCount(50);
            f.textProperty().bindBidirectional(query);
            return f;
        });
        addWithStyleNames(" ", GRAY);
        addNodeSegment(() -> {
            Button b = new Button("Run");
            b.setOnAction((ev) -> execute());
            return b;
        });
        nl(2);
        addWithStyleNames("Result:", GRAY);
        nl();
        addParagraph(() -> new ResultParagraph(result));
        nl(2);
        addSegment("Source: Wikipedia");
        nl();
        addWithStyleNames("https://en.wikipedia.org/wiki/Select_(SQL)", GREEN, UNDER);
    }

    protected void execute() {
        String q = query.get();
        if (q == null) {
            q = "";
        }
        q = q.toLowerCase();
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
