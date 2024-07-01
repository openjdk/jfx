/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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

import javafx.scene.control.ComboBox;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.SelectionModel;
import javafx.scene.input.KeyCode;

public class ComboBoxListViewBehavior<T> extends ComboBoxBaseBehavior<T> {

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     *
     */
    public ComboBoxListViewBehavior(ComboBoxBase<T> c) {
        super(c);
    }

    @Override
    protected void populateSkinInputMap() {
        super.populateSkinInputMap();

        registerFunction(ComboBox.SELECT_PREV, this::selectPrevious);
        registerFunction(ComboBox.SELECT_NEXT, this::selectNext);

        registerKey(KeyCode.UP, ComboBox.SELECT_PREV);
        registerKey(KeyCode.DOWN, ComboBox.SELECT_NEXT);
    }

    /***************************************************************************
     *                                                                         *
     * Key event handling                                                      *
     *                                                                         *
     **************************************************************************/

    private ComboBox<T> getComboBox() {
        return (ComboBox<T>) getControl();
    }

    private void selectPrevious(ComboBoxBase<T> c) {
        // NOTE: ComboBoxBase<T> does not have getSelectionModel().  design problem?
        SelectionModel<T> sm = getComboBox().getSelectionModel();
        if (sm == null) return;
        sm.selectPrevious();
    }

    private void selectNext(ComboBoxBase<T> c) {
        // NOTE: ComboBoxBase<T> does not have getSelectionModel().  design problem?
        SelectionModel<T> sm = getComboBox().getSelectionModel();
        if (sm == null) return;
        sm.selectNext();
    }
}
