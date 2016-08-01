/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.control.inputmap.InputMap;
import com.sun.javafx.scene.control.inputmap.KeyBinding;
import javafx.scene.control.ToolBar;

import static javafx.scene.input.KeyCode.*;
import static com.sun.javafx.scene.control.inputmap.InputMap.KeyMapping;

/**
 * A Behavior implementation for ToolBars.
 */
public class ToolBarBehavior extends BehaviorBase<ToolBar> {
    private final InputMap<ToolBar> toolBarInputMap;

    public ToolBarBehavior(ToolBar toolbar) {
        super(toolbar);

        // create a map for toolbar-specific mappings (this reuses the default
        // InputMap installed on the control, if it is non-null, allowing us to pick up any user-specified mappings)
        toolBarInputMap = createInputMap();

        // toolbar-specific mappings for key and mouse input
        addDefaultMapping(toolBarInputMap,
            new KeyMapping(new KeyBinding(F5).ctrl(), e -> {
                if (!toolbar.getItems().isEmpty()) {
                    toolbar.getItems().get(0).requestFocus();
                }
            })
        );
    }

    @Override public InputMap<ToolBar> getInputMap() {
        return toolBarInputMap;
    }
}

