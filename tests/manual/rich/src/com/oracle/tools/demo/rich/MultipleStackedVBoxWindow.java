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

import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.rich.RichTextArea;
import javafx.scene.control.rich.skin.LineNumberDecorator;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MultipleStackedVBoxWindow extends Stage {
    public MultipleStackedVBoxWindow() {
        RichTextArea a1 = new RichTextArea(NotebookModelStacked.m1());
        a1.setWrapText(true);
        a1.setUseContentHeight(true);
        a1.setLeftDecorator(new LineNumberDecorator());
        
        TextArea t1 = new TextArea("This TextArea has wrap text property set to false.");
        
        TextArea t2 = new TextArea("This TextArea has wrap text property set to true.");
        t2.setWrapText(true);
        
        RichTextArea a2 = new RichTextArea(NotebookModelStacked.m2());
        a2.setWrapText(true);
        a2.setUseContentHeight(true);
        a2.setLeftDecorator(new LineNumberDecorator());

        PrefSizeTester tester = new PrefSizeTester();

        VBox vb = new VBox(
            a1,
            t1,
            a2,
            t2,
            tester
        );
        ScrollPane sp = new ScrollPane(vb);
        sp.setFitToWidth(true);
        Scene scene = new Scene(sp);
        setScene(scene);

        setTitle("Multiple RichTextAreas Stacked in VBox");
        setWidth(600);
        setHeight(1200);
    }
}
