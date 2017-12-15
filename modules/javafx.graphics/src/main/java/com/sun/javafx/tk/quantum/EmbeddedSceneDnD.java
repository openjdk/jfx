/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.javafx.embed.HostDragStartListener;
import com.sun.javafx.embed.EmbeddedSceneDTInterface;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.javafx.tk.TKClipboard;

import com.sun.javafx.tk.Toolkit;
import javafx.application.Platform;
import javafx.scene.input.TransferMode;

final class EmbeddedSceneDnD {

    private final GlassSceneDnDEventHandler dndHandler;

    private HostDragStartListener dragStartListener;
    private EmbeddedSceneDSInterface fxDragSource;
    private EmbeddedSceneDTInterface fxDropTarget;

    private Thread hostThread;

    public EmbeddedSceneDnD(final GlassScene scene) {
        this.dndHandler = new GlassSceneDnDEventHandler(scene);
    }

    private void startDrag() {
        assert Platform.isFxApplicationThread();
        assert fxDragSource != null;

        dragStartListener.dragStarted(fxDragSource, TransferMode.COPY);
    }

    private void setHostThread() {
        if (hostThread == null) {
            hostThread = Thread.currentThread();
        }
    }

    public boolean isHostThread() {
        return (Thread.currentThread() == hostThread);
    }

    public void onDragSourceReleased(final EmbeddedSceneDSInterface ds) {
        assert fxDragSource == ds;

        fxDragSource = null;
        Toolkit.getToolkit().exitNestedEventLoop(this, null);
    }

    public void onDropTargetReleased(final EmbeddedSceneDTInterface dt) {

        fxDropTarget = null;
    }

    /*
     * This is a helper method to execute code on FX event thread. It
     * can be implemented using AWT nested event loop, however it just
     * blocks the current thread. This is done by intention, because
     * we need to handle Swing events one by one. If we enter a nested
     * event loop, various weird side-effects are observed, e.g.
     * dragOver() in SwingDnD is executed before dragEnter() is finished
     */
    <T> T executeOnFXThread(final Callable<T> r) {

        // When running under SWT, the main thread is the FX thread
        // so execute the callable right away (return null on failure)
        if (Platform.isFxApplicationThread()) {
            try {
                return r.call();
            } catch (Exception z) {
                // ignore
            }
            return null;
        }

        final AtomicReference<T> result = new AtomicReference<>();
        final CountDownLatch l = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                result.set(r.call());
            } catch (Exception z) {
                // ignore
            } finally {
                l.countDown();
            }
        });

        try {
            l.await();
        } catch (Exception z) {
            // ignore
        }

        return result.get();
    }


    // Should be called from Scene.DnDGesture.createDragboard only!
    public TKClipboard createDragboard(boolean isDragSource) {
        assert Platform.isFxApplicationThread();
        assert fxDragSource == null;

        assert isDragSource;
        ClipboardAssistance assistant = new ClipboardAssistance("DND-Embedded") {
            @Override
            public void flush() {
                super.flush();
                startDrag(); // notify host
                Toolkit.getToolkit().enterNestedEventLoop(EmbeddedSceneDnD.this); // block current thread
            }
        };
        fxDragSource = new EmbeddedSceneDS(this, assistant, dndHandler);
        return QuantumClipboard.getDragboardInstance(assistant, isDragSource);
    }

    public void setDragStartListener(HostDragStartListener l) {
        setHostThread();
        dragStartListener = l;
    }

    public EmbeddedSceneDTInterface createDropTarget() {
        setHostThread();
        return executeOnFXThread(() -> {
            assert fxDropTarget == null;
            fxDropTarget = new EmbeddedSceneDT(EmbeddedSceneDnD.this, dndHandler);
            return fxDropTarget;
        });
    }

}
