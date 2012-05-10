/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.scene.control.behavior;

import java.util.ArrayList;
import java.util.List;

import com.sun.javafx.PlatformUtil;

import static javafx.scene.input.KeyCode.*;
import static javafx.scene.input.KeyEvent.*;

/**
 */
public class TextInputControlBindings {
    protected static final List<KeyBinding> BINDINGS = new ArrayList<KeyBinding>();
    static {
        // caret movement
        BINDINGS.add(new KeyBinding(RIGHT, KEY_PRESSED,       "Forward"));
        BINDINGS.add(new KeyBinding(KP_RIGHT, KEY_PRESSED,    "Forward"));
        BINDINGS.add(new KeyBinding(LEFT, KEY_PRESSED,        "Backward"));
        BINDINGS.add(new KeyBinding(KP_LEFT, KEY_PRESSED,     "Backward"));
        BINDINGS.add(new KeyBinding(UP, KEY_PRESSED,          "Home"));
        BINDINGS.add(new KeyBinding(KP_UP, KEY_PRESSED,       "Home"));
        BINDINGS.add(new KeyBinding(HOME, KEY_PRESSED,        "Home"));
        BINDINGS.add(new KeyBinding(DOWN, KEY_PRESSED,        "End"));
        BINDINGS.add(new KeyBinding(KP_DOWN, KEY_PRESSED,     "End"));
        BINDINGS.add(new KeyBinding(END, KEY_PRESSED,         "End"));
        BINDINGS.add(new KeyBinding(ENTER, KEY_PRESSED,       "Fire"));
        // deletion
        BINDINGS.add(new KeyBinding(BACK_SPACE, KEY_PRESSED,  "DeletePreviousChar"));
        BINDINGS.add(new KeyBinding(DELETE, KEY_PRESSED,      "DeleteNextChar"));
        // cut/copy/paste
        BINDINGS.add(new KeyBinding(CUT, KEY_PRESSED,         "Cut"));
        BINDINGS.add(new KeyBinding(DELETE, KEY_PRESSED,      "Cut").shift());
        BINDINGS.add(new KeyBinding(COPY, KEY_PRESSED,        "Copy"));
        BINDINGS.add(new KeyBinding(PASTE, KEY_PRESSED,       "Paste"));
        BINDINGS.add(new KeyBinding(INSERT, KEY_PRESSED,      "Paste").shift());// does this belong on mac?
        // selection
        BINDINGS.add(new KeyBinding(RIGHT, KEY_PRESSED,       "SelectForward").shift());
        BINDINGS.add(new KeyBinding(KP_RIGHT, KEY_PRESSED,    "SelectForward").shift());
        BINDINGS.add(new KeyBinding(LEFT, KEY_PRESSED,        "SelectBackward").shift());
        BINDINGS.add(new KeyBinding(KP_LEFT, KEY_PRESSED,     "SelectBackward").shift());
        BINDINGS.add(new KeyBinding(UP, KEY_PRESSED,          "SelectHome").shift());
        BINDINGS.add(new KeyBinding(KP_UP, KEY_PRESSED,       "SelectHome").shift());
        BINDINGS.add(new KeyBinding(DOWN, KEY_PRESSED,        "SelectEnd").shift());
        BINDINGS.add(new KeyBinding(KP_DOWN, KEY_PRESSED,     "SelectEnd").shift());

        BINDINGS.add(new KeyBinding(BACK_SPACE, KEY_PRESSED,  "DeletePreviousChar").shift());
        BINDINGS.add(new KeyBinding(DELETE, KEY_PRESSED,      "DeleteNextChar").shift());

        // platform specific settings
        if (PlatformUtil.isMac()) {
            BINDINGS.add(new KeyBinding(HOME, KEY_PRESSED,       "SelectHomeExtend").shift());
            BINDINGS.add(new KeyBinding(END, KEY_PRESSED,        "SelectEndExtend").shift());

            BINDINGS.add(new KeyBinding(HOME, KEY_PRESSED,       "Home").meta());
            BINDINGS.add(new KeyBinding(END, KEY_PRESSED,        "End").meta());
            BINDINGS.add(new KeyBinding(LEFT, KEY_PRESSED,       "Home").meta());
            BINDINGS.add(new KeyBinding(KP_LEFT, KEY_PRESSED,    "Home").meta());
            BINDINGS.add(new KeyBinding(RIGHT, KEY_PRESSED,      "End").meta());
            BINDINGS.add(new KeyBinding(KP_RIGHT, KEY_PRESSED,   "End").meta());
            BINDINGS.add(new KeyBinding(LEFT, KEY_PRESSED,       "PreviousWord").alt());
            BINDINGS.add(new KeyBinding(KP_LEFT, KEY_PRESSED,    "PreviousWord").alt());
            BINDINGS.add(new KeyBinding(RIGHT, KEY_PRESSED,      "NextWord").alt());
            BINDINGS.add(new KeyBinding(KP_RIGHT, KEY_PRESSED,   "NextWord").alt());
            BINDINGS.add(new KeyBinding(DELETE, KEY_PRESSED,     "DeleteNextWord").meta());
            BINDINGS.add(new KeyBinding(BACK_SPACE, KEY_PRESSED, "DeletePreviousWord").meta());
            BINDINGS.add(new KeyBinding(X, KEY_PRESSED,          "Cut").meta());
            BINDINGS.add(new KeyBinding(C, KEY_PRESSED,          "Copy").meta());
            BINDINGS.add(new KeyBinding(INSERT, KEY_PRESSED,     "Copy").meta());
            BINDINGS.add(new KeyBinding(V, KEY_PRESSED,          "Paste").meta());
            BINDINGS.add(new KeyBinding(HOME, KEY_PRESSED,       "SelectHome").shift().meta());
            BINDINGS.add(new KeyBinding(END, KEY_PRESSED,        "SelectEnd").shift().meta());
            BINDINGS.add(new KeyBinding(LEFT, KEY_PRESSED,       "SelectHomeExtend").shift().meta());
            BINDINGS.add(new KeyBinding(KP_LEFT, KEY_PRESSED,    "SelectHomeExtend").shift().meta());
            BINDINGS.add(new KeyBinding(RIGHT, KEY_PRESSED,      "SelectEndExtend").shift().meta());
            BINDINGS.add(new KeyBinding(KP_RIGHT, KEY_PRESSED,   "SelectEndExtend").shift().meta());
            BINDINGS.add(new KeyBinding(A, KEY_PRESSED,          "SelectAll").meta());
            BINDINGS.add(new KeyBinding(LEFT, KEY_PRESSED,       "SelectPreviousWord").shift().alt());
            BINDINGS.add(new KeyBinding(KP_LEFT, KEY_PRESSED,    "SelectPreviousWord").shift().alt());
            BINDINGS.add(new KeyBinding(RIGHT, KEY_PRESSED,      "SelectNextWord").shift().alt());
            BINDINGS.add(new KeyBinding(KP_RIGHT, KEY_PRESSED,   "SelectNextWord").shift().alt());
            BINDINGS.add(new KeyBinding(Z, KEY_PRESSED,          "Undo").meta());
            BINDINGS.add(new KeyBinding(Z, KEY_PRESSED,          "Redo").shift().meta());
        } else {
            BINDINGS.add(new KeyBinding(HOME, KEY_PRESSED,       "SelectHome").shift());
            BINDINGS.add(new KeyBinding(END, KEY_PRESSED,        "SelectEnd").shift());

            BINDINGS.add(new KeyBinding(HOME, KEY_PRESSED,       "Home").ctrl());
            BINDINGS.add(new KeyBinding(END, KEY_PRESSED,        "End").ctrl());
            BINDINGS.add(new KeyBinding(LEFT, KEY_PRESSED,       "PreviousWord").ctrl());
            BINDINGS.add(new KeyBinding(KP_LEFT, KEY_PRESSED,    "PreviousWord").ctrl());
            BINDINGS.add(new KeyBinding(RIGHT, KEY_PRESSED,      "NextWord").ctrl());
            BINDINGS.add(new KeyBinding(KP_RIGHT, KEY_PRESSED,   "NextWord").ctrl());
            BINDINGS.add(new KeyBinding(H, KEY_PRESSED,          "DeletePreviousChar").ctrl());
            BINDINGS.add(new KeyBinding(DELETE, KEY_PRESSED,     "DeleteNextWord").ctrl());
            BINDINGS.add(new KeyBinding(BACK_SPACE, KEY_PRESSED, "DeletePreviousWord").ctrl());
            BINDINGS.add(new KeyBinding(X, KEY_PRESSED,          "Cut").ctrl());
            BINDINGS.add(new KeyBinding(C, KEY_PRESSED,          "Copy").ctrl());
            BINDINGS.add(new KeyBinding(INSERT, KEY_PRESSED,     "Copy").ctrl());
            BINDINGS.add(new KeyBinding(V, KEY_PRESSED,          "Paste").ctrl());
            BINDINGS.add(new KeyBinding(HOME, KEY_PRESSED,       "SelectHome").ctrl().shift());
            BINDINGS.add(new KeyBinding(END, KEY_PRESSED,        "SelectEnd").ctrl().shift());
            BINDINGS.add(new KeyBinding(LEFT, KEY_PRESSED,       "SelectPreviousWord").ctrl().shift());
            BINDINGS.add(new KeyBinding(KP_LEFT, KEY_PRESSED,    "SelectPreviousWord").ctrl().shift());
            BINDINGS.add(new KeyBinding(RIGHT, KEY_PRESSED,      "SelectNextWord").ctrl().shift());
            BINDINGS.add(new KeyBinding(KP_RIGHT, KEY_PRESSED,   "SelectNextWord").ctrl().shift());
            BINDINGS.add(new KeyBinding(A, KEY_PRESSED,          "SelectAll").ctrl());
            BINDINGS.add(new KeyBinding(BACK_SLASH, KEY_PRESSED, "Unselect").ctrl());
            if (PlatformUtil.isLinux()) {
                BINDINGS.add(new KeyBinding(Z, KEY_PRESSED,          "Undo").ctrl());
                BINDINGS.add(new KeyBinding(Z, KEY_PRESSED,          "Redo").ctrl().shift());
            } else {  // Windows
                BINDINGS.add(new KeyBinding(Z, KEY_PRESSED,          "Undo").ctrl());
                BINDINGS.add(new KeyBinding(Y, KEY_PRESSED,          "Redo").ctrl());
            }
        }
        // Any other key press first goes to normal text input
        // Note this is KEY_TYPED because otherwise the character is not available in the event.
        BINDINGS.add(new KeyBinding(null, KEY_TYPED, "InputCharacter")
                .alt(OptionalBoolean.ANY)
                .shift(OptionalBoolean.ANY)
                .ctrl(OptionalBoolean.ANY)
                .meta(OptionalBoolean.ANY));
        // Traversal Bindings
        BINDINGS.add(new KeyBinding(TAB, "TraverseNext"));
        BINDINGS.add(new KeyBinding(TAB, "TraversePrevious").shift());
        // TODO XXX DEBUGGING ONLY
//        BINDINGS.add(new KeyBinding(F4, "TraverseDebug").alt().ctrl().shift());
        /*DEBUG*/if (PlatformUtil.isEmbedded()) {
            BINDINGS.add(new KeyBinding(DIGIT9, "UseVK").ctrl().shift());
        }
    }
}
