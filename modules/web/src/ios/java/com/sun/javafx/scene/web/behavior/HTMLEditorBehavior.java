/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

import javafx.scene.web.HTMLEditor;
import java.util.ArrayList;
import java.util.List;
import com.sun.javafx.scene.control.behavior.BehaviorBase;
import com.sun.javafx.scene.control.behavior.KeyBinding;
import com.sun.javafx.scene.web.skin.HTMLEditorSkin;
import static javafx.scene.input.KeyCode.B;
import static javafx.scene.input.KeyCode.I;
import static javafx.scene.input.KeyCode.U;
import static javafx.scene.input.KeyCode.F12;
import static javafx.scene.input.KeyCode.TAB;


/**
 * HTML editor behavior.
 */
public class HTMLEditorBehavior extends BehaviorBase<HTMLEditor> {
    protected static final List<KeyBinding> HTML_EDITOR_BINDINGS = new ArrayList<KeyBinding>();

    static {
        HTML_EDITOR_BINDINGS.add(new KeyBinding(B, "bold").shortcut());
        HTML_EDITOR_BINDINGS.add(new KeyBinding(I, "italic").shortcut());
        HTML_EDITOR_BINDINGS.add(new KeyBinding(U, "underline").shortcut());
        
        HTML_EDITOR_BINDINGS.add(new KeyBinding(F12, "F12"));
        HTML_EDITOR_BINDINGS.add(new KeyBinding(TAB, "TraverseNext").ctrl());
        HTML_EDITOR_BINDINGS.add(new KeyBinding(TAB, "TraversePrevious").ctrl().shift());
    }

    public HTMLEditorBehavior(HTMLEditor htmlEditor) {
        super(htmlEditor, HTML_EDITOR_BINDINGS);
    }

    @Override
    protected void callAction(String name) {
        if ("bold".equals(name) || "italic".equals(name) || "underline".equals(name)) {
            HTMLEditor editor = getControl();
            HTMLEditorSkin editorSkin = (HTMLEditorSkin)editor.getSkin();
            editorSkin.keyboardShortcuts(name);
        } else if ("F12".equals(name)) {
            getControl().getImpl_traversalEngine().selectFirst().requestFocus();
        } else {
            super.callAction(name);
        }
    }
}
