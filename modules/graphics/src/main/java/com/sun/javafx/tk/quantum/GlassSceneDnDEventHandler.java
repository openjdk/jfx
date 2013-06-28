/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.tk.quantum;

import javafx.application.Platform;

import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

import com.sun.glass.ui.ClipboardAssistance;

import java.security.AccessController;
import java.security.PrivilegedAction;

class GlassSceneDnDEventHandler {

    private final GlassScene scene;

    public GlassSceneDnDEventHandler(final GlassScene scene) {
        this.scene = scene;
    }

    public TransferMode handleDragEnter(final int x, final int y, final int xAbs, final int yAbs,
                                        final TransferMode recommendedTransferMode,
                                        final ClipboardAssistance dropTargetAssistant)
    {
        assert Platform.isFxApplicationThread();
        return AccessController.doPrivileged(new PrivilegedAction<TransferMode>() {
            @Override
            public TransferMode run() {
                if (scene.dropTargetListener != null) {
                    return scene.dropTargetListener.dragEnter(x, y, xAbs, yAbs,
                            recommendedTransferMode);
                }
                return null;
            }
        }, scene.getAccessControlContext());
    }

    public void handleDragLeave(final ClipboardAssistance dropTargetAssistant) {
        assert Platform.isFxApplicationThread();
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                if (scene.dropTargetListener != null) {
                    scene.dropTargetListener.dragExit(0, 0, 0, 0);
                }
                return null;
            }
        }, scene.getAccessControlContext());
    }

    public TransferMode handleDragDrop(final int x, final int y, final int xAbs, final int yAbs,
                                       final TransferMode recommendedTransferMode,
                                       final ClipboardAssistance dropTargetAssistant)
    {
        assert Platform.isFxApplicationThread();
        return AccessController.doPrivileged(new PrivilegedAction<TransferMode>() {
            @Override
            public TransferMode run() {
                if (scene.dropTargetListener != null) {
                    return scene.dropTargetListener.drop(x, y, xAbs, yAbs,
                            recommendedTransferMode);
                }
                return null;
            }
        }, scene.getAccessControlContext());
    }

    public TransferMode handleDragOver(final int x, final int y, final int xAbs, final int yAbs,
                                       final TransferMode recommendedTransferMode,
                                       final ClipboardAssistance dropTargetAssistant)
    {
        assert Platform.isFxApplicationThread();
        return AccessController.doPrivileged(new PrivilegedAction<TransferMode>() {
            @Override
            public TransferMode run() {
                if (scene.dropTargetListener != null) {
                    return scene.dropTargetListener.dragOver(x, y, xAbs, yAbs,
                            recommendedTransferMode);
                }
                return null;
            }
        }, scene.getAccessControlContext());
    }

    public void handleDragStart(final int button, final int x, final int y, final int xAbs, final int yAbs,
                                final ClipboardAssistance dropSourceAssistant)
    {
        assert Platform.isFxApplicationThread();
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                if (scene.dragGestureListener != null) {
                    scene.dragGestureListener.dragGestureRecognized(
                            x, y, xAbs, yAbs, button);
                }
                return null;
            }
        }, scene.getAccessControlContext());
    }

    // Used in case the drag has started from the handleDragStart() above - 
    // it's delivered by Glass itself.
    // Otherwise, see QuantumToolkit.createDragboard() and startDrag()
    public void handleDragEnd(final TransferMode performedTransferMode,
                              final ClipboardAssistance dropSourceAssistant)
    {
        assert Platform.isFxApplicationThread();
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                if (scene.dragSourceListener != null) {
                    scene.dragSourceListener.dragDropEnd(0, 0, 0, 0,
                            performedTransferMode);
                }
                return null;
            }
        }, scene.getAccessControlContext());
    }

}
