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
package com.oracle.tools.fx.monkey.pages;

import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.TitledPane;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.SequenceNumber;
import com.oracle.tools.fx.monkey.util.TestPaneBase;

/**
 * Accordion Page.
 */
public class AccordionPage extends TestPaneBase {
    private final Accordion accordion;

    public AccordionPage() {
        FX.name(this, "AccordionPage");

        accordion = new Accordion();
        addPane();

        Button addButton = new Button("Add Pane");
        addButton.setOnAction((ev) -> addPane());

        Button removeButton = new Button("Remove Pane");
        removeButton.setOnAction((ev) -> removePane());

        OptionPane op = new OptionPane();
        op.add(addButton);
        op.add(removeButton);

        setContent(accordion);
        setOptions(op);
    }

    private void addPane() {
        String name = SequenceNumber.next();
        System.nanoTime();
        Button b = new Button(name);
        b.setOnAction((ev) -> {
            System.out.println(name);
        });
        TitledPane p = new TitledPane(name, b);
        accordion.getPanes().add(p);
    }

    private void removePane() {
        int sz = accordion.getPanes().size();
        if (sz > 0) {
            accordion.getPanes().remove(0);
        }
    }
}
