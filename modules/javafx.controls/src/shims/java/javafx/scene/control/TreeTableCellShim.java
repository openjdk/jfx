/*
 * Copyright (c) 2015, 2023, Oracle and/or its affiliates. All rights reserved.
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
package javafx.scene.control;

public class TreeTableCellShim<S,T> extends TreeTableCell<S,T> {

    /**
     * Flag which is only used in conjunction with {@link #lockItemOnStartEdit} to lock the item when
     * the {@link #startEdit()} method is called.
     */
    private boolean isStartEdit = false;
    /**
     * Flag to lock the item value when an edit process is started.
     * While normally the {@link #updateItem(Object, boolean)} will change the underlying item,
     * when locked the item will not be changed.
     */
    private boolean lockItemOnStartEdit = false;

    @Override
    public void startEdit() {
        isStartEdit = true;
        super.startEdit();
    }

    @Override
    public void updateItem(T item, boolean empty) {
        // startEdit() was called and wants to update the cell. When locked, we will ignore the update request.
        if (lockItemOnStartEdit && isStartEdit) {
            isStartEdit = false;
            return;
        }

        super.updateItem(item, empty);
    }

    public void setLockItemOnStartEdit(boolean lockItemOnEdit) {
        this.lockItemOnStartEdit = lockItemOnEdit;
    }

    public static <S, T> TreeTablePosition<S, T> getEditingCellAtStartEdit(TreeTableCell<S, T> cell) {
        return cell.getEditingCellAtStartEdit();
    }

}
