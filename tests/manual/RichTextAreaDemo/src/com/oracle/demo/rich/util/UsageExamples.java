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

package com.oracle.demo.rich.util;

import javafx.scene.control.ScrollBar;
import javafx.scene.input.KeyCode;
import jfx.incubator.scene.control.input.FunctionTag;
import jfx.incubator.scene.control.input.KeyBinding;
import jfx.incubator.scene.control.rich.ConfigurationParameters;
import jfx.incubator.scene.control.rich.RichTextArea;
import jfx.incubator.scene.control.rich.TextPos;
import jfx.incubator.scene.control.rich.model.SimpleViewOnlyStyledModel;
import jfx.incubator.scene.control.rich.model.StyleAttrs;
import jfx.incubator.scene.control.rich.skin.LineNumberDecorator;

/**
 * The usage examples used in the documentation.
 */
class UsageExamples {
    void createViewOnly() {
        SimpleViewOnlyStyledModel m = new SimpleViewOnlyStyledModel();
        // add text segment using CSS style name (requires a style sheet)
        m.addSegment("RichTextArea ", null, "HEADER");
        // add text segment using direct style
        m.addSegment("Demo", "-fx-font-size:200%;", null);
        // newline
        m.nl();

        RichTextArea t = new RichTextArea(m);
    }

    void appendStyledText() {
        // create styles
        StyleAttrs heading = StyleAttrs.builder().setBold(true).setFontSize(24).build();
        StyleAttrs plain = StyleAttrs.builder().setFontFamily("Monospaced").build();

        RichTextArea rta = new RichTextArea();
        // build the content
        rta.appendText("Heading\n", heading);
        rta.appendText("Plain monospaced text.\n", plain);
    }

    private static final FunctionTag PRINT_TO_CONSOLE = new FunctionTag();

    void customNavigation() {
        // sets a custom vertical scroll bar
        ConfigurationParameters cp = ConfigurationParameters.
            builder().
            verticalScrollBar(ScrollBar::new).
            build();
        RichTextArea richTextArea = new RichTextArea(cp, null);

        // creates a new key binding mapped to an external function
        richTextArea.getInputMap().register(KeyBinding.shortcut(KeyCode.W), (c) -> {
            System.out.println("console!");
        });

        // unbind old key bindings
        var old = richTextArea.getInputMap().getKeyBindingsFor(RichTextArea.Tags.PASTE_PLAIN_TEXT);
        for (KeyBinding k : old) {
            richTextArea.getInputMap().unbind(k);
        }
        // map a new key binding
        richTextArea.getInputMap().registerKey(KeyBinding.shortcut(KeyCode.W), RichTextArea.Tags.PASTE_PLAIN_TEXT);

        // redefine a function
        richTextArea.getInputMap().registerFunction(RichTextArea.Tags.PASTE_PLAIN_TEXT, (c) -> { });
        richTextArea.pastePlainText(); // becomes a no-op
        // revert back to the default behavior
        richTextArea.getInputMap().restoreDefaultFunction(RichTextArea.Tags.PASTE_PLAIN_TEXT);

        // sets a side decorator
        richTextArea.setLeftDecorator(new LineNumberDecorator());

        richTextArea.getInputMap().registerFunction(PRINT_TO_CONSOLE, (c) -> {
            // new functionality
            System.out.println("PRINT_TO_CONSOLE executed");
        });

        // change the functionality of an existing key binding
        richTextArea.getInputMap().registerFunction(RichTextArea.Tags.MOVE_WORD_NEXT, (c) -> {
            // refers to custom logic
            TextPos p = getCustomNextWordPosition(richTextArea);
            richTextArea.select(p);
        });
    }

    private TextPos getCustomNextWordPosition(RichTextArea richTextArea) {
        return null;
    }

    public static class MyControl extends RichTextArea {
        // function tag allows the user to customize key bindings
        public static final FunctionTag MY_TAG = new FunctionTag();

        public MyControl() {
            // register custom functionality with the input map
            getInputMap().registerFunction(MY_TAG, this::newFunctionImpl);
            // create a key binding
            getInputMap().registerKey(KeyBinding.shortcut(KeyCode.W), MY_TAG);
        }

        private void newFunctionImpl(MyControl c) {
            // custom functionality
        }
    }
}
