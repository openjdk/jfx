/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.SPACE;
import static javafx.scene.input.KeyCode.TAB;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.control.TitledPane;
import javafx.scene.input.MouseEvent;

import com.sun.javafx.scene.control.skin.AccordionSkin;
import com.sun.javafx.scene.control.skin.TitledPaneSkin;
import com.sun.javafx.scene.traversal.Direction;

public class TitledPaneBehavior extends BehaviorBase<TitledPane> {

    private TitledPane titledPane;

    public TitledPaneBehavior(TitledPane pane) {
        super(pane);
        this.titledPane = pane;
    }

    /***************************************************************************
     *                                                                         *
     * Key event handling                                                      *
     *                                                                         *
     **************************************************************************/

    private static final String PRESS_ACTION = "Press";

    protected static final List<KeyBinding> TITLEDPANE_BINDINGS = new ArrayList<KeyBinding>();
    static {
        TITLEDPANE_BINDINGS.add(new KeyBinding(ENTER, PRESS_ACTION));
        TITLEDPANE_BINDINGS.add(new KeyBinding(SPACE, PRESS_ACTION));
        TITLEDPANE_BINDINGS.add(new KeyBinding(TAB, "TraverseNext"));
        TITLEDPANE_BINDINGS.add(new KeyBinding(TAB, "TraversePrevious").shift());
    }

    @Override protected List<KeyBinding> createKeyBindings() {
            return TITLEDPANE_BINDINGS;
    }

    @Override protected void callAction(String name) {
        if (PRESS_ACTION.equals(name)) {
            TitledPane tp = getControl();
            if (tp.isCollapsible() && tp.isFocused()) {
                tp.setExpanded(!tp.isExpanded());
                tp.requestFocus();
            }
        } else if ("TraverseNext".equals(name)) {
            TitledPane tp = getControl();
            TitledPaneSkin tps = (TitledPaneSkin)tp.getSkin();
            tps.getContentRegion().getImpl_traversalEngine().getTopLeftFocusableNode();
            // Are there any focusable node in the TitlePane content
            if (!tp.isExpanded() || tps.getContentRegion().getImpl_traversalEngine().registeredNodes.isEmpty()) {
                super.callAction(name);
            }
        } else if ("TraversePrevious".equals(name)) {
            super.callAction(name);
        } else {
            super.callAction(name);
        }
    }

    /***************************************************************************
     *                                                                         *
     * Mouse event handling                                                    *
     *                                                                         *
     **************************************************************************/

    @Override public void mousePressed(MouseEvent e) {
        super.mousePressed(e);
        TitledPane tp = getControl();
        tp.requestFocus();
    }

    /**************************************************************************
     *                         State and Functions                            *
     *************************************************************************/

    public void expand() {
        titledPane.setExpanded(true);
    }

    public void collapse() {
        titledPane.setExpanded(false);
    }

    public void toggle() {
        titledPane.setExpanded(!titledPane.isExpanded());
    }

}

