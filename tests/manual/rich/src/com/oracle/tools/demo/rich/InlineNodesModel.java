/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

/**
 * A demo model with inline Nodes.
 */
public class InlineNodesModel extends SimpleReadOnlyStyledModel {
    private final SimpleStringProperty textField = new SimpleStringProperty();
    
    public InlineNodesModel() {
        String ARABIC = "arabic";
        String CODE = "code";
        String RED = "red";
        String GREEN = "green";
        String UNDER = "underline";
        String GRAY = "gray";
        String LARGE = "large";
        String ITALIC = "italic";

        addSegment("Inline Nodes", null, UNDER, LARGE);
        nl();
        // trailing text
        addNodeSegment(() -> {
            TextField f = new TextField();
            f.setPrefColumnCount(20);
            f.textProperty().bindBidirectional(textField);
            return f;
        });
        addSegment(" ", null, LARGE);
        addNodeSegment(() -> new Button("OK"));
        addSegment(" trailing segment.", null, LARGE); // FIX cannot navigate over this segment
        nl();

        // leading text
        addSegment("Leading text", null, LARGE);
        addNodeSegment(() -> {
            TextField f = new TextField();
            f.setPrefColumnCount(20);
            f.textProperty().bindBidirectional(textField);
            return f;
        });
        addSegment("- in between text-", null, LARGE);
        addNodeSegment(() -> new Button("Find"));
        nl();
        
        // leading and trailing text
        addSegment("Leading text", null, LARGE);
        addNodeSegment(() -> {
            TextField f = new TextField();
            f.setPrefColumnCount(20);
            f.textProperty().bindBidirectional(textField);
            return f;
        });
        addSegment("- in between text-", null, LARGE);
        addNodeSegment(() -> new Button("Find"));
        addSegment(" trailing segment.", null, LARGE);
        nl();
        
        // adjacent nodes
        addNodeSegment(() -> new Button("One"));
        addNodeSegment(() -> new Button("Two"));
        addNodeSegment(() -> new Button("Three"));
        addNodeSegment(() -> new Button("Four"));
        addNodeSegment(() -> new Button("Five"));
        nl();
        addSegment("", null, LARGE);
        nl();
        
        addSegment("A regular text segment for reference.", null, LARGE);
        nl();
        addSegment("The End █", null, LARGE);
        nl();
    }
}
