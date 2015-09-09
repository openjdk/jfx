/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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

import javafx.scene.control.Pagination;
import com.sun.javafx.scene.control.inputmap.InputMap;
import javafx.scene.input.MouseEvent;

import static javafx.scene.input.KeyCode.*;
import static com.sun.javafx.scene.control.inputmap.InputMap.KeyMapping;
import static com.sun.javafx.scene.control.inputmap.InputMap.MouseMapping;

public class PaginationBehavior extends BehaviorBase<Pagination> {

    private final InputMap<Pagination> paginationInputMap;

    public PaginationBehavior(Pagination pagination) {
        super(pagination);

        // create a map for paginiation-specific mappings (this reuses the default
        // InputMap installed on the control, if it is non-null, allowing us to pick up any user-specified mappings)
        paginationInputMap = createInputMap();

        // then button-specific mappings for key and mouse input
        addDefaultMapping(paginationInputMap,
            new KeyMapping(LEFT, e -> rtl(pagination, this::right, this::left)),
            new KeyMapping(RIGHT, e -> rtl(pagination, this::left, this::right)),
            new MouseMapping(MouseEvent.MOUSE_PRESSED, this::mousePressed)
        );
    }

    @Override public InputMap<Pagination> getInputMap() {
        return paginationInputMap;
    }

    public void mousePressed(MouseEvent e) {
        getNode().requestFocus();
    }

    private void left() {
        movePage(-1);
    }

    private void right() {
        movePage(1);
    }

    private void movePage(int delta) {
        final Pagination pagination = getNode();
        final int currentPageIndex = pagination.getCurrentPageIndex();
        pagination.setCurrentPageIndex(currentPageIndex + delta);
    }
}
