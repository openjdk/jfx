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
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.Callable;
import javafx.scene.input.TransferMode;

final class EmbeddedSceneDragSource implements EmbeddedSceneDragSourceInterface {

    private final EmbeddedSceneDnD dnd;
    private final GlassSceneDnDEventHandler dndHandler;

    public EmbeddedSceneDragSource(final EmbeddedSceneDnD dnd,
                                   final GlassSceneDnDEventHandler dndHandler) {
        this.dnd = dnd;
        this.dndHandler = dndHandler;
    }

    private ClipboardAssistance getClipboardAssistance() {
        assert dnd.isValid(this);
        return dnd.getClipboardAssistance(this);
    }

    @Override
    public Set<TransferMode> getSupportedActions() {
        assert dnd.isHostThread();
        return FxEventLoop.sendEvent(new Callable<Set<TransferMode>>() {

            @Override
            public Set<TransferMode> call() {
                return QuantumClipboard.clipboardActionsToTransferModes(getClipboardAssistance().
                        getSupportedSourceActions());
            }
        });
    }

    @Override
    public Object getData(final String mimeType) {
        assert dnd.isHostThread();
        return FxEventLoop.sendEvent(new Callable() {

            @Override
            public Object call() {
                return getClipboardAssistance().getData(mimeType);
            }
        });
    }

    @Override
    public String[] getMimeTypes() {
        assert dnd.isHostThread();
        return FxEventLoop.sendEvent(new Callable<String[]>() {

            @Override
            public String[] call() {
                return getClipboardAssistance().getMimeTypes();
            }
        });
    }

    @Override
    public boolean isMimeTypeAvailable(final String mimeType) {
        assert dnd.isHostThread();
        return (boolean) FxEventLoop.sendEvent(new Callable<Boolean>() {

            @Override
            public Boolean call() {
                return Arrays.asList(getClipboardAssistance().getMimeTypes()).
                        contains(mimeType);
            }
        });
    }

    @Override
    public void dragDropEnd(final TransferMode performedAction) {
        assert dnd.isHostThread();
        FxEventLoop.sendEvent(new Runnable() {

            @Override
            public void run() {
                try {
                    dndHandler.handleDragEnd(performedAction,
                                             getClipboardAssistance());
                } finally {
                    dnd.onDragSourceReleased(EmbeddedSceneDragSource.this);
                }
            }
        });
    }
}
