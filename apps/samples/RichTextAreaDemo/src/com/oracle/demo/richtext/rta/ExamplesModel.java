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
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import jfx.incubator.scene.control.richtext.StyleResolver;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.RichParagraph;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import jfx.incubator.scene.control.richtext.model.StyledTextModelViewOnlyBase;

/**
 * This model contains code examples used in the documentation.
 *
 * @author Andy Goryachev
 */
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
    public StyleAttributeMap getStyleAttributeMap(StyleResolver resolver, TextPos pos) {
        return null;
    }

    @Override
    public RichParagraph getParagraph(int index) {
        switch(index) {
        case 0:
            {
                StyleAttributeMap a1 = StyleAttributeMap.builder().setBold(true).build();
                RichParagraph.Builder b = RichParagraph.builder();
                b.addSegment("Example: ", a1);
                b.addSegment("spelling, highlights");
                b.addWavyUnderline(9, 8, Color.RED);
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
