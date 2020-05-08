/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.web.behavior;

import com.sun.javafx.scene.ParentHelper;
import javafx.scene.web.HTMLEditor;
import java.util.ArrayList;
import java.util.List;
import com.sun.javafx.scene.control.behavior.BehaviorBase;
import com.sun.javafx.scene.control.inputmap.InputMap;
import com.sun.javafx.scene.control.inputmap.KeyBinding;
import com.sun.javafx.scene.control.behavior.FocusTraversalInputMap;

import static com.sun.javafx.scene.control.inputmap.InputMap.KeyMapping;
import javafx.scene.web.HTMLEditorSkin;
import static javafx.scene.input.KeyCode.B;
import static javafx.scene.input.KeyCode.I;
import static javafx.scene.input.KeyCode.U;
import static javafx.scene.input.KeyCode.F12;
import static javafx.scene.input.KeyCode.TAB;


/**
 * HTML editor behavior.
 */
public class HTMLEditorBehavior extends BehaviorBase<HTMLEditor> {
    private final InputMap<HTMLEditor> inputMap;

    public HTMLEditorBehavior(HTMLEditor htmlEditor) {
        super(htmlEditor);

        this.inputMap = createInputMap();
        addDefaultMapping(inputMap,
                new KeyMapping(new KeyBinding(B).shortcut(), e -> keyboardShortcuts(HTMLEditorSkin.Command.BOLD)),
                new KeyMapping(new KeyBinding(I).shortcut(), e -> keyboardShortcuts(HTMLEditorSkin.Command.ITALIC)),
                new KeyMapping(new KeyBinding(U).shortcut(), e -> keyboardShortcuts(HTMLEditorSkin.Command.UNDERLINE)),

                new KeyMapping(new KeyBinding(F12), e -> ParentHelper.getTraversalEngine(getNode()).selectFirst().requestFocus()),
                new KeyMapping(new KeyBinding(TAB).ctrl(), FocusTraversalInputMap::traverseNext),
                new KeyMapping(new KeyBinding(TAB).ctrl().shift(), FocusTraversalInputMap::traversePrevious)
        );
    }

    @Override public InputMap<HTMLEditor> getInputMap() {
        return inputMap;
    }

    private void keyboardShortcuts(HTMLEditorSkin.Command command) {
        HTMLEditor editor = getNode();
        HTMLEditorSkin editorSkin = (HTMLEditorSkin)editor.getSkin();
        editorSkin.performCommand(command);
    }
}
