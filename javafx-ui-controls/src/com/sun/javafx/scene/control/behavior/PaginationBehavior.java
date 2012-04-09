/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.javafx.scene.control.behavior;

import com.sun.javafx.scene.control.Pagination;
import com.sun.javafx.scene.control.skin.PaginationSkin;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;

public class PaginationBehavior<T> extends BehaviorBase<Pagination<T>> {

    /**************************************************************************
     *                          Setup KeyBindings                             *
     *************************************************************************/
    private static final String LEFT = "Left";
    private static final String RIGHT = "Right";

    protected static final List<KeyBinding> PAGINATION_BINDINGS = new ArrayList<KeyBinding>();
    static {
        PAGINATION_BINDINGS.add(new KeyBinding(KeyCode.LEFT, LEFT));
        PAGINATION_BINDINGS.add(new KeyBinding(KeyCode.RIGHT, RIGHT));
        PAGINATION_BINDINGS.addAll(TRAVERSAL_BINDINGS);
    }

    @Override protected List<KeyBinding> createKeyBindings() {
        return PAGINATION_BINDINGS;
    }

    @Override protected void callAction(String name) {
        if (LEFT.equals(name)) {
            PaginationSkin ps = (PaginationSkin)getControl().getSkin();
            ps.getSelectionModel().selectPrevious();
        } else if (RIGHT.equals(name)) {
            PaginationSkin ps = (PaginationSkin)getControl().getSkin();
            ps.getSelectionModel().selectNext();
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
        Pagination p = getControl();
        p.requestFocus();
    }

    /**************************************************************************
     *                         State and Functions                            *
     *************************************************************************/

    public PaginationBehavior(Pagination pagination) {
        super(pagination);
    }
}
