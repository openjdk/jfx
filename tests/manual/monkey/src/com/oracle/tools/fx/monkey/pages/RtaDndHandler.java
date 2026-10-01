/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.tools.fx.monkey.pages;

import java.io.File;
import java.util.List;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.SelectionSegment;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.FileListFormatHandler;
import jfx.incubator.scene.control.richtext.model.StyledInput;

/**
 * Standard Drag and Drop Handler for RichTextArea.
 */
public class RtaDndHandler {
    private RtaDndHandler() {
    }

    public static void install(RichTextArea editor) {
        editor.addEventFilter(MouseEvent.MOUSE_PRESSED, (ev) -> {
            // select under right click, unless extended selection exists
            if (ev.getButton() == MouseButton.SECONDARY) {
                if (ev.isAltDown() || ev.isControlDown() || ev.isMetaDown() || ev.isShiftDown()
                    || ev.isShortcutDown()) {
                    return;
                }
                SelectionSegment sel = editor.getSelection();
                if ((sel == null) || sel.isCollapsed()) {
                    TextPos p = editor.getTextPosition(ev.getScreenX(), ev.getScreenY());
                    if (p != null) {
                        editor.select(p);
                    }
                }
            }
        });

        editor.getInputMap().addHandler(DragEvent.DRAG_OVER, (ev) -> {
            if (ev.getDragboard().hasFiles()) {
                editor.setDropTarget(ev.getScreenX(), ev.getScreenY());
                // check for image types using extension maybe?
                ev.acceptTransferModes(TransferMode.COPY);
                ev.consume();
            }
        });
        editor.getInputMap().addHandler(DragEvent.DRAG_EXITED, (ev) -> {
            editor.clearDropTarget();
        });
        editor.getInputMap().addHandler(DragEvent.DRAG_DROPPED, (ev) -> {
            if (ev.getDragboard().hasFiles()) {
                List<File> files = ev.getDragboard().getFiles();
                TextPos p = editor.getDropTarget();
                if (p != null) {
                    try {
                        StyledInput in = FileListFormatHandler.getInstance().createStyledInput(files, null);
                        editor.clearSelection();
                        editor.replaceText(p, p, in);
                    } catch (Exception e) {
                        editor.errorFeedback();
                    }
                    ev.consume();
                }
            }
        });
    }
}
