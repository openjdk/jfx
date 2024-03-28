/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.control.skin.AccordionSkin;
import com.oracle.tools.fx.monkey.sheets.ControlPropertySheet;
import com.oracle.tools.fx.monkey.util.FX;
import com.oracle.tools.fx.monkey.util.HasSkinnable;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.SequenceNumber;
import com.oracle.tools.fx.monkey.util.TestPaneBase;
import com.oracle.tools.fx.monkey.util.Utils;

/**
 * Accordion Page.
 */
public class AccordionPage extends TestPaneBase implements HasSkinnable {
    private final Accordion control;

    public AccordionPage() {
        super("AccordionPage");

        control = new Accordion();
        addPane();

        // TODO MenuButtons with more options
        Button addButton = FX.button("Add Pane", this::addPane);

        Button removeButton = FX.button("Remove", this::removePane);

        OptionPane op = new OptionPane();
        op.section("Accordion");
        op.option("Panes:", Utils.buttons(addButton, removeButton));
        ControlPropertySheet.appendTo(op, control);

        setContent(control);
        setOptions(op);
    }

    private void addPane() {
        String name = SequenceNumber.next();
        System.nanoTime();
        Button b = FX.button(name, () -> {
            System.out.println(name);
        });
        TitledPane p = new TitledPane(name, b);
        control.getPanes().add(p);
    }

    private void removePane() {
        int sz = control.getPanes().size();
        if (sz > 0) {
            control.getPanes().remove(0);
        }
    }

    @Override
    public void nullSkin() {
        control.setSkin(null);
    }

    @Override
    public void newSkin() {
        control.setSkin(new AccordionSkin(control));
    }
}
