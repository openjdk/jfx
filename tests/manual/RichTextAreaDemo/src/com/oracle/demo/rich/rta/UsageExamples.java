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

package com.oracle.demo.rich.rta;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import jfx.incubator.scene.control.input.FunctionTag;
import jfx.incubator.scene.control.input.KeyBinding;
import jfx.incubator.scene.control.rich.CodeArea;
import jfx.incubator.scene.control.rich.LineNumberDecorator;
import jfx.incubator.scene.control.rich.RichTextArea;
import jfx.incubator.scene.control.rich.TextPos;
import jfx.incubator.scene.control.rich.model.SimpleViewOnlyStyledModel;
import jfx.incubator.scene.control.rich.model.StyleAttributeMap;

/**
 * The usage examples used in the documentation.
 */
public class UsageExamples {
    void createViewOnly() {
        SimpleViewOnlyStyledModel m = new SimpleViewOnlyStyledModel();
        // add text segment using CSS style name (requires a style sheet)
        m.addSegment("RichTextArea ", null, "HEADER");
        // add text segment using direct style
        m.addSegment("Demo", "-fx-font-size:200%;");
        // newline
        m.nl();

        RichTextArea textArea = new RichTextArea(m);
    }

    static RichTextArea appendStyledText() {
        // create styles
        StyleAttributeMap heading = StyleAttributeMap.builder().setBold(true).setUnderline(true).setFontSize(18).build();
        StyleAttributeMap mono = StyleAttributeMap.builder().setFontFamily("Monospaced").build();

        RichTextArea textArea = new RichTextArea();
        // build the content
        textArea.appendText("RichTextArea\n", heading);
        textArea.appendText("Example:\nText is ", StyleAttributeMap.EMPTY);
        textArea.appendText("monospaced.\n", mono);
        return textArea;
    }

    void richTextAreaExample() {
        RichTextArea textArea = new RichTextArea();
        // insert two paragraphs "A" and "B"
        StyleAttributeMap bold = StyleAttributeMap.builder().setBold(true).build();
        textArea.appendText("A\nB", bold);
    }

    private static CodeArea codeAreaExample() {
        CodeArea codeArea = new CodeArea();
        codeArea.setWrapText(true);
        codeArea.setLineNumbersEnabled(true);
        codeArea.setText("Lorem\nIpsum");
        return codeArea;
    }

    private static final FunctionTag PRINT_TO_CONSOLE = new FunctionTag();

    void customNavigation() {
        RichTextArea richTextArea = new RichTextArea();

        // creates a new key binding mapped to an external function
        richTextArea.getInputMap().register(KeyBinding.shortcut(KeyCode.W), () -> {
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
        richTextArea.getInputMap().registerFunction(RichTextArea.Tags.PASTE_PLAIN_TEXT, () -> { });
        richTextArea.pastePlainText(); // becomes a no-op
        // revert back to the default behavior
        richTextArea.getInputMap().restoreDefaultFunction(RichTextArea.Tags.PASTE_PLAIN_TEXT);

        // sets a side decorator
        richTextArea.setLeftDecorator(new LineNumberDecorator());

        richTextArea.getInputMap().registerFunction(PRINT_TO_CONSOLE, () -> {
            // new functionality
            System.out.println("PRINT_TO_CONSOLE executed");
        });

        // change the functionality of an existing key binding
        richTextArea.getInputMap().registerFunction(RichTextArea.Tags.MOVE_WORD_NEXT_START, () -> {
            // refers to custom logic
            TextPos p = getCustomNextWordPosition(richTextArea);
            richTextArea.select(p);
        });
    }

    void testGeneric() {
        MyControl c = new MyControl();
        c.getInputMap().registerFunction(MyControl.MY_TAG, () -> {
            c.newFunctionImpl();
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

        public void newFunctionImpl() {
            // custom functionality
        }
    }

    public static class App extends Application {
        public App() {
            System.out.println("test app: F1 appends at the end, F2 inserts at the start, F3 clears selection.");
        }

        @Override
        public void start(Stage stage) throws Exception {
            RichTextArea t = true ? appendStyledText() : codeAreaExample();
            stage.setScene(new Scene(t));
            t.selectionProperty().addListener((s,p,c) -> {
                System.out.println("selection: " + c);
            });
            t.anchorPositionProperty().addListener((s,p,c) -> {
                System.out.println("anchor: " + c);
            });
            t.caretPositionProperty().addListener((s,p,c) -> {
                System.out.println("caret: " + c);
            });
            t.getInputMap().register(KeyBinding.of(KeyCode.F1), () -> {
                t.insertText(TextPos.ZERO, "F1", StyleAttributeMap.EMPTY);
            });
            t.getInputMap().register(KeyBinding.of(KeyCode.F2), () -> {
                t.insertText(TextPos.ZERO, "\n", StyleAttributeMap.EMPTY);
            });
            t.getInputMap().register(KeyBinding.of(KeyCode.F3), () -> {
                t.clearSelection();
            });
            stage.show();
        }
    }

    public static void main(String[] args) {
        App.launch(App.class, args);
    }
}
