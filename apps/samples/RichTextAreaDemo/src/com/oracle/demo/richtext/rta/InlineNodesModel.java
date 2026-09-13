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
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import jfx.incubator.scene.control.richtext.model.SimpleViewOnlyStyledModel;

/**
 * A demo model with inline Nodes.
 *
 * @author Andy Goryachev
 */
public class InlineNodesModel extends SimpleViewOnlyStyledModel {
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

        addWithStyleNames("Inline Nodes", UNDER, LARGE);
        nl();
        // trailing text
        addNodeSegment(() -> {
            TextField f = new TextField();
            f.setPrefColumnCount(20);
            f.textProperty().bindBidirectional(textField);
            return f;
        });
        addWithStyleNames(" ", LARGE);
        addNodeSegment(() -> new Button("OK"));
        addWithStyleNames(" trailing segment.", LARGE); // FIX cannot navigate over this segment
        nl();

        // leading text
        addWithStyleNames("Leading text", LARGE);
        addNodeSegment(() -> {
            TextField f = new TextField();
            f.setPrefColumnCount(20);
            f.textProperty().bindBidirectional(textField);
            return f;
        });
        addWithStyleNames("- in between text-", LARGE);
        addNodeSegment(() -> new Button("Find"));
        nl();

        // leading and trailing text
        addWithStyleNames("Leading text", LARGE);
        addNodeSegment(() -> {
            TextField f = new TextField();
            f.setPrefColumnCount(20);
            f.textProperty().bindBidirectional(textField);
            return f;
        });
        addWithStyleNames("- in between text-", LARGE);
        addNodeSegment(() -> new Button("Find"));
        addWithStyleNames(" trailing segment.", LARGE);
        nl();

        // adjacent nodes
        addNodeSegment(() -> new Button("One"));
        addNodeSegment(() -> new Button("Two"));
        addNodeSegment(() -> new Button("Three"));
        addNodeSegment(() -> new Button("Four"));
        addNodeSegment(() -> new Button("Five"));
        nl();
        addWithStyleNames("", LARGE);
        nl();

        addWithStyleNames("A regular text segment for reference.", LARGE);
        nl();
        addWithStyleNames("The End █", LARGE);
        nl();
    }
}
