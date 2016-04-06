/*
 * Copyright (c) 2000, 2016, Oracle and/or its affiliates. All rights reserved.
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

package dragdrop;

import javafx.event.EventHandler;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;

public class DndTextEdit extends SimpleTextEdit {

    public DndTextEdit() {

        skin.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                requestFocus();
                int pos = getPos(event.getX());
                if (!isInSelection(pos)) {
                    if (pos >= 0) {
                        setCaretPos(pos);
                    }
                    clearSelection();
                }
            }
        });

        skin.setOnDragDetected(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent event) {
                int pos = getPos(event.getX());
                if (isInSelection(pos)) {
                    Dragboard db = skin.startDragAndDrop(TransferMode.ANY);
                    ClipboardContent data = new ClipboardContent();
                    data.putString(getSelection());
                    db.setContent(data);
                }
            }
        });

        skin.setOnDragEntered(new EventHandler<DragEvent>() {
            @Override public void handle(DragEvent event) {
                if (!isFocused()) {
                    showCaret();
                }
            }
        });

        skin.setOnDragExited(new EventHandler<DragEvent>() {
            @Override public void handle(DragEvent event) {
                if (!isFocused()) {
                    removeCaret();
                }
            }
        });

        skin.setOnDragOver(new EventHandler<DragEvent>() {
            @Override public void handle(DragEvent event) {
                int pos = getPos(event.getX());
                setCaretPos(pos);
                if (event.getDragboard().hasString()) {
                    event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                }
            }
        });

        skin.setOnDragDropped(new EventHandler<DragEvent>() {
            @Override public void handle(DragEvent event) {
                int pos = getPos(event.getX());
                if (event.getGestureSource() == skin && isInSelection(pos)) {
                    event.setDropCompleted(false);
                    return;
                }

                setCaretPos(pos);

                Dragboard db = event.getDragboard();
                if (db.hasString()) {
                    String s = db.getString();
                    if (event.getGestureSource() == skin) {
                        removeSelection();
                    }
                    insert(s);
                    event.setDropCompleted(true);
                    requestFocus();
                    return;
                }

                event.setDropCompleted(false);
            }
        });

        skin.setOnDragDone(new EventHandler<DragEvent>() {
            @Override public void handle(DragEvent event) {
                if (event.getTransferMode() == TransferMode.MOVE) {
                    removeSelection();
                }

                if (event.getTransferMode() != null) {
                    clearSelection();
                }
            }
        });
    }

}
