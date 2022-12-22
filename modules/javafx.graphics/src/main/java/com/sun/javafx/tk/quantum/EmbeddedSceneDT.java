/*
 * Copyright (c) 2012, 2022, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.javafx.embed.EmbeddedSceneDSInterface;
import com.sun.javafx.embed.EmbeddedSceneDTInterface;
import javafx.scene.input.TransferMode;

final class EmbeddedSceneDT implements EmbeddedSceneDTInterface {

    private final EmbeddedSceneDnD dnd;
    private final GlassSceneDnDEventHandler dndHandler;
    private EmbeddedSceneDSInterface dragSource;
    private ClipboardAssistance assistant;

    public EmbeddedSceneDT(final EmbeddedSceneDnD dnd,
                           final GlassSceneDnDEventHandler dndHandler) {
        this.dnd = dnd;
        this.dndHandler = dndHandler;
    }

    private void close() {
        dnd.onDropTargetReleased(this);
        assistant = null;
    }

    @Override
    public TransferMode handleDragEnter(final int x, final int y, final int xAbs,
                                        final int yAbs,
                                        final TransferMode recommendedDropAction,
                                        final EmbeddedSceneDSInterface ds)
    {
        assert dnd.isHostThread();

        return dnd.executeOnFXThread(() -> {

            dragSource = ds;
            assistant = new EmbeddedDTAssistant(dragSource);

            return dndHandler.handleDragEnter(x, y, xAbs, yAbs,
                                              recommendedDropAction,
                                              assistant);
        });
    }

    @Override
    public void handleDragLeave() {
        assert dnd.isHostThread();

        dnd.executeOnFXThread(() -> {
            assert assistant != null;
            try {
                dndHandler.handleDragLeave(assistant);
            } finally {
                close();
            }
            return null;
        });
    }

    @Override
    public TransferMode handleDragDrop(final int x, final int y, final int xAbs,
                                       final int yAbs,
                                       final TransferMode recommendedDropAction) {
        assert dnd.isHostThread();

        return dnd.executeOnFXThread(() -> {
            assert assistant != null;
            try {
                return dndHandler.handleDragDrop(x, y, xAbs, yAbs,
                                                 recommendedDropAction,
                                                 assistant);
            } finally {
                close();
            }
        });
    }

    @Override
    public TransferMode handleDragOver(final int x, final int y, final int xAbs,
                                       final int yAbs,
                                       final TransferMode recommendedDropAction) {
        assert dnd.isHostThread();

        return dnd.executeOnFXThread(() -> {
            assert assistant != null;
            return dndHandler.handleDragOver(x, y, xAbs, yAbs,
                                             recommendedDropAction,
                                             assistant);
        });
    }

    private static class EmbeddedDTAssistant extends ClipboardAssistance {

        private EmbeddedSceneDSInterface dragSource;

        EmbeddedDTAssistant(EmbeddedSceneDSInterface source) {
            super("DND-Embedded");
            dragSource = source;
        }

        @Override
        public void flush() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object getData(final String mimeType) {
            return dragSource.getData(mimeType);
        }

        @Override
        public int getSupportedSourceActions() {
            return QuantumClipboard.transferModesToClipboardActions(dragSource.getSupportedActions());
        }

        @Override
        public void setTargetAction(int actionDone) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String[] getMimeTypes() {
            return dragSource.getMimeTypes();
        }
    }
}
