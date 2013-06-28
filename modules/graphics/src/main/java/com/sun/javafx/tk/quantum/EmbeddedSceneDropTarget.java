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

import com.sun.glass.ui.ClipboardAssistance;
import com.sun.javafx.embed.EmbeddedSceneDragSourceInterface;
import com.sun.javafx.embed.EmbeddedSceneDropTargetInterface;
import java.util.concurrent.Callable;
import javafx.application.Platform;
import javafx.scene.input.TransferMode;

final class EmbeddedSceneDropTarget implements EmbeddedSceneDropTargetInterface {

    private final EmbeddedSceneDnD dnd;
    private final GlassSceneDnDEventHandler dndHandler;
    private EmbeddedSceneDragSourceInterface dragSource;
    private int dndCounter;

    public EmbeddedSceneDropTarget(final EmbeddedSceneDnD dnd,
                                   final GlassSceneDnDEventHandler dndHandler) {
        this.dnd = dnd;
        this.dndHandler = dndHandler;
    }

    private boolean isDnDCounterValid() {
        assert Platform.isFxApplicationThread();
        assert dndCounter == 1;

        return true;
    }

    private ClipboardAssistance getClipboardAssistance() {
        assert isDnDCounterValid();
        assert dnd.isValid(this);

        return dnd.getClipboardAssistance(dragSource);
    }

    private void close() {
        assert isDnDCounterValid();

        --dndCounter;

        dnd.onDropTargetReleased(this);
    }

    @Override
    public TransferMode handleDragEnter(final int x, final int y, final int xAbs,
                                        final int yAbs,
                                        final TransferMode recommendedDropAction,
                                        final EmbeddedSceneDragSourceInterface dragSource) {
        assert dnd.isHostThread();

        return FxEventLoop.sendEvent(new Callable<TransferMode>() {

            @Override
            public TransferMode call() {
                ++dndCounter;

                assert dnd.isFxDragSource() ? true
                        : EmbeddedSceneDropTarget.this.dragSource == null;

                EmbeddedSceneDropTarget.this.dragSource = dragSource;

                return dndHandler.handleDragEnter(x, y, xAbs, yAbs,
                                                  recommendedDropAction,
                                                  getClipboardAssistance());
            }
        });
    }

    @Override
    public void handleDragLeave() {
        assert dnd.isHostThread();

        FxEventLoop.sendEvent(new Runnable() {

            @Override
            public void run() {
                try {
                    dndHandler.handleDragLeave(getClipboardAssistance());
                } finally {
                    close();
                }
            }
        });
    }

    @Override
    public TransferMode handleDragDrop(final int x, final int y, final int xAbs,
                                       final int yAbs,
                                       final TransferMode recommendedDropAction) {
        assert dnd.isHostThread();

        return FxEventLoop.sendEvent(new Callable<TransferMode>() {

            @Override
            public TransferMode call() {
                try {
                    return dndHandler.handleDragDrop(x, y, xAbs, yAbs,
                                                     recommendedDropAction,
                                                     getClipboardAssistance());
                } finally {
                    close();
                }
            }
        });
    }

    @Override
    public TransferMode handleDragOver(final int x, final int y, final int xAbs,
                                       final int yAbs,
                                       final TransferMode recommendedDropAction) {
        assert dnd.isHostThread();

        return FxEventLoop.sendEvent(new Callable<TransferMode>() {

            @Override
            public TransferMode call() {
                return dndHandler.handleDragOver(x, y, xAbs, yAbs,
                                                 recommendedDropAction,
                                                 getClipboardAssistance());
            }
        });
    }
}
