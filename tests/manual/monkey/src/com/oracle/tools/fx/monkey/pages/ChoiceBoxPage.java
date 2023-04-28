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
package com.oracle.tools.fx.monkey.pages;

import javafx.scene.control.ChoiceBox;
import com.oracle.tools.fx.monkey.util.ItemSelector;
import com.oracle.tools.fx.monkey.util.OptionPane;
import com.oracle.tools.fx.monkey.util.TestPaneBase;

/**
 * ChoiceBox Page
 */
public class ChoiceBoxPage extends TestPaneBase {
    private ChoiceBox<String> control;

    public ChoiceBoxPage() {
        setId("ChoiceBoxPage");

        control = new ChoiceBox();

        ItemSelector<String[]> itemSelector = new ItemSelector<>(
            "itemSelector",
            (t) -> control.getItems().setAll(t),
            "0", mk(0),
            "1", mk(1),
            "2", mk(2),
            "5", mk(5),
            "100", mk(100),
            "1_000", mk(1_000));

        // TODO converter

        OptionPane p = new OptionPane();
        p.label("Items:");
        p.option(itemSelector.node());

        setContent(control);
        setOptions(p);
    }

    private static String[] mk(int size) {
        String[] ss = new String[size];
        for (int i = 0; i < size; i++) {
            ss[i] = ("Item " + i);
        }
        return ss;
    }
}
