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

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import jfx.incubator.scene.control.input.FunctionTag;
import jfx.incubator.scene.control.input.KeyBinding;
import jfx.incubator.scene.control.richtext.CodeArea;
import jfx.incubator.scene.control.richtext.LineNumberDecorator;
import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.SimpleViewOnlyStyledModel;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;

/**
 * The usage examples used in the documentation.
 *
 * @author Andy Goryachev
 */
public class UsageExamples {
    // This example is used in the JEP and RichTextArea class javadoc.
    void createViewOnly() {
        SimpleViewOnlyStyledModel m = new SimpleViewOnlyStyledModel();
        // add text segment using CSS style name (requires a stylesheet)
        m.addWithStyleNames("RichTextArea ", "HEADER");
        // add text segment using inline styles
        m.addWithInlineStyle("Demo", "-fx-font-size:200%; -fx-font-weight:bold;");
        // add newline
        m.nl();

        RichTextArea textArea = new RichTextArea(m);
    }

    void createViewOnlyAll() {
        SimpleViewOnlyStyledModel m = new SimpleViewOnlyStyledModel();
        // add text segment using CSS style name (requires a stylesheet)
        m.addWithStyleNames("RichTextArea ", "HEADER");
        // add text segment using inline styles
        m.addWithInlineStyle("Demo ", "-fx-font-size:200%; -fx-font-weight:bold;");
        // with inline and style names
        m.addWithInlineAndStyleNames("... more text", "-fx-text-fill:red;", "STYLE1", "STYLE2");
        // add newline
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
