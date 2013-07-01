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
import com.sun.javafx.embed.EmbeddedSceneDragStartListenerInterface;
import com.sun.javafx.embed.EmbeddedSceneDropTargetInterface;
import java.util.concurrent.Callable;

import com.sun.javafx.tk.TKClipboard;
import javafx.application.Platform;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

final class EmbeddedSceneDnD {

    private final GlassSceneDnDEventHandler dndHandler;

    private EmbeddedSceneDragStartListenerInterface dragStartListener;
    private EmbeddedSceneDragSourceInterface fxDragSource;
    private EmbeddedSceneDropTargetInterface fxDropTarget;
    
    private ClipboardAssistance clipboardAssistant;
    
    private Thread hostThread;

    public EmbeddedSceneDnD(final GlassScene scene) {
        this.dndHandler = new GlassSceneDnDEventHandler(scene);
    }
    
    private void startDrag() {
        assert Platform.isFxApplicationThread();

        assert fxDragSource == null;

        fxDragSource = new EmbeddedSceneDragSource(this, dndHandler);
        
        final TransferMode dragAction = TransferMode.COPY;
        
        dragStartListener.dragStarted(fxDragSource, dragAction);
    }

    private void setHostThread() {
        if (hostThread == null) {
            hostThread = Thread.currentThread();
        }
    }

    public boolean isHostThread() {
        return (Thread.currentThread() == hostThread);
    }
    
    public boolean isValid(EmbeddedSceneDragSourceInterface ds) {
        assert Platform.isFxApplicationThread();
        assert ds != null;
        assert fxDragSource == ds;
        assert clipboardAssistant != null;
        return true;
    }
    
    public boolean isValid(EmbeddedSceneDropTargetInterface dt) {
        assert Platform.isFxApplicationThread();
        assert dt != null;
        assert fxDropTarget == dt;
        return true;
    }
    
    public boolean isFxDragSource() {
        assert Platform.isFxApplicationThread();

        return (fxDragSource != null);
    }

    public void onDragSourceReleased(final EmbeddedSceneDragSourceInterface ds) {
        assert isValid(ds);

        fxDragSource = null;
        clipboardAssistant = null;
        
        FxEventLoop.leaveNestedLoop();
    }

    public void onDropTargetReleased(final EmbeddedSceneDropTargetInterface dt) {
        assert isValid(dt);

        fxDropTarget = null;
        if (!isFxDragSource()) {
            clipboardAssistant = null;
        }
    }
    
    // Should be called from Scene.DnDGesture.createDragboard only!
    public TKClipboard createDragboard() {
        assert Platform.isFxApplicationThread();
        assert fxDropTarget == null;
        assert clipboardAssistant == null;
        assert fxDragSource == null;
        
        clipboardAssistant = new ClipboardAssistanceImpl(null);
        return QuantumClipboard.getDragboardInstance(clipboardAssistant);
    }

    public void setDragStartListener(EmbeddedSceneDragStartListenerInterface l) {
        setHostThread();
        
        assert isHostThread();
        
        this.dragStartListener = l;
    }
    
    public EmbeddedSceneDropTargetInterface createDropTarget() {
        setHostThread();

        assert isHostThread();
        assert fxDropTarget == null;

        return FxEventLoop.sendEvent(new Callable<EmbeddedSceneDropTargetInterface>() {

            @Override
            public EmbeddedSceneDropTargetInterface call() {
                fxDropTarget = new EmbeddedSceneDropTarget(EmbeddedSceneDnD.this,
                                                           dndHandler);

                return fxDropTarget;
            }
        });
    }
    
    public ClipboardAssistance getClipboardAssistance(
            final EmbeddedSceneDragSourceInterface source) {
        assert Platform.isFxApplicationThread();
        assert isFxDragSource() ? isValid(source) : true;
        assert isFxDragSource() ? (clipboardAssistant != null) : true;
        
        if (clipboardAssistant == null) {
            clipboardAssistant = new ClipboardAssistanceImpl(source);
        }
        return clipboardAssistant;
    }

    private class ClipboardAssistanceImpl extends ClipboardAssistance {
        final EmbeddedSceneDragSourceInterface source;
                
        private boolean isValid() {
            assert Platform.isFxApplicationThread();
            assert EmbeddedSceneDnD.this.clipboardAssistant == this;
            
            return true;
        }
        
        ClipboardAssistanceImpl(final EmbeddedSceneDragSourceInterface source) {
            super("DND-Embedded");
            
            this.source = source;
        }

        @Override
        public void flush() {
            assert isValid();

            super.flush();
            
            startDrag();

            FxEventLoop.enterNestedLoop();
        }

        @Override
        public void emptyCache() {
            assert isValid();

            super.emptyCache();
        }

        @Override
        public Object getData(final String mimeType) {
            assert isValid();

            if (source == null) {
                return super.getData(mimeType);
            }

            return source.getData(mimeType);
        }

        @Override
        public void setData(final String mimeType, final Object data) {
            assert isValid();
            
            if (source != null) {
                return;
            }

            super.setData(mimeType, data);
        }

        @Override
        public void setSupportedActions(final int supportedActions) {
            assert isValid();
            
            if (source != null) {
                return;
            }

            super.setSupportedActions(supportedActions);
        }

        @Override
        public int getSupportedSourceActions() {
            assert isValid();

            if (source == null) {
                return super.getSupportedSourceActions();
            }

            return QuantumClipboard.transferModesToClipboardActions(source.
                    getSupportedActions());
        }

        @Override
        public void setTargetAction(int actionDone) {
            assert isValid();

            throw new UnsupportedOperationException();
        }

        @Override
        public String[] getMimeTypes() {
            assert isValid();

            if (source == null) {
                return super.getMimeTypes();
            }

            return source.getMimeTypes();
        }
    }
}
