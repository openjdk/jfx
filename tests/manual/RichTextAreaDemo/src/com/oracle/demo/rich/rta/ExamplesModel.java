/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.demo.rich.rta;

import javafx.beans.property.SimpleStringProperty;
import javafx.incubator.scene.control.rich.StyleResolver;
import javafx.incubator.scene.control.rich.TextPos;
import javafx.incubator.scene.control.rich.model.RichParagraph;
import javafx.incubator.scene.control.rich.model.StyleAttrs;
import javafx.incubator.scene.control.rich.model.StyledTextModelViewOnlyBase;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

/** This model contains code examples used in the documentation. */
public class ExamplesModel extends StyledTextModelViewOnlyBase {
    /** properties in the model allow for inline controls */
    private final SimpleStringProperty exampleProperty = new SimpleStringProperty();

    public ExamplesModel() {
    }

    @Override
    public int size() {
        return 10;
    }

    @Override
    public String getPlainText(int index) {
        return getParagraph(index).getPlainText();
    }

    @Override
    public StyleAttrs getStyleAttrs(StyleResolver resolver, TextPos pos) {
        return null;
    }

    @Override
    public RichParagraph getParagraph(int index) {
        switch(index) {
        case 0:
            {
                StyleAttrs a1 = StyleAttrs.builder().setBold(true).build();
                RichParagraph.Builder b = RichParagraph.builder();
                b.addSegment("Example: ", a1);
                b.addSegment("spelling, highlights");
                b.addSquiggly(9, 8, Color.RED);
                b.addHighlight(19, 4, Color.rgb(255, 128, 128, 0.5));
                b.addHighlight(20, 7, Color.rgb(128, 255, 128, 0.5));
                return b.build();
            }
        case 4:
            {
                RichParagraph.Builder b = RichParagraph.builder();
                b.addSegment("Input field: ");
                // creates an embedded control bound to a property within this model
                b.addInlineNode(() -> {
                   TextField t = new TextField();
                   t.textProperty().bindBidirectional(exampleProperty);
                   return t;
                });
                return b.build();
            }
        }
        return RichParagraph.builder().build();
    }
}
