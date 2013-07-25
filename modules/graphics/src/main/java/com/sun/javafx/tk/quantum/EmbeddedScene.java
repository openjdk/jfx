/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
import javafx.event.EventType;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import java.nio.IntBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import com.sun.javafx.cursor.CursorFrame;
import com.sun.javafx.embed.AbstractEvents;
import com.sun.javafx.embed.EmbeddedSceneDragStartListenerInterface;
import com.sun.javafx.embed.EmbeddedSceneDropTargetInterface;
import com.sun.javafx.embed.EmbeddedSceneInterface;
import com.sun.javafx.embed.HostInterface;
import com.sun.javafx.scene.traversal.Direction;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.tk.TKClipboard;
import com.sun.javafx.tk.Toolkit;

final class EmbeddedScene extends GlassScene implements EmbeddedSceneInterface {

    // TODO: synchronize access to embedder from ET and RT
    HostInterface host;

    private UploadingPainter        painter;
    private PaintRenderJob          paintRenderJob;

    volatile IntBuffer textureBits;

    int width;
    int height;

    private final EmbeddedSceneDnD dndDelegate;

    public EmbeddedScene(HostInterface host, boolean depthBuffer, boolean antiAliasing) {
        super(depthBuffer, antiAliasing);
        sceneState = new EmbeddedState(this);

        this.host = host;
        this.dndDelegate = new EmbeddedSceneDnD(this);

        PaintCollector collector = PaintCollector.getInstance();
        painter = new UploadingPainter(this);
        paintRenderJob = new PaintRenderJob(this, collector.getRendered(), painter);
    }

    @Override
    public void dispose() {
        assert host != null;
        AbstractPainter.renderLock.lock();
        try {
            host.setEmbeddedScene(null);
            host = null;
            updateSceneState();
        } finally {
            AbstractPainter.renderLock.unlock();
        }
        super.dispose();
    }

    @Override
    void setStage(GlassStage stage) {
        super.setStage(stage);

        assert host != null; // setStage() is called before dispose()
        host.setEmbeddedScene(stage != null ? this : null);
    }

    @Override protected boolean isSynchronous() {
        return false;
    }

    @Override public void setRoot(NGNode root) {
        super.setRoot(root);
        painter.setRoot(root);
    }

    @Override
    public TKClipboard createDragboard(boolean isDragSource) {
        return dndDelegate.createDragboard();
    }

    @Override
    public void enableInputMethodEvents(boolean enable) {
        if (QuantumToolkit.verbose) {
            System.err.println("EmbeddedScene.enableInputMethodEvents " + enable);
        }
    }

    // Called by EmbeddedPainter on the render thread under renderLock
    void sceneRepainted() {
        if (host != null) {
            host.repaint();
        }
    }

    // EmbeddedSceneInterface methods

    @Override
    public void repaint() {
        Toolkit tk = Toolkit.getToolkit();
        tk.addRenderJob(paintRenderJob);
    }

    @Override
    public boolean traverseOut(Direction dir) {
        if (dir == Direction.NEXT) {
            return host.traverseFocusOut(true);
        } else if (dir == Direction.PREVIOUS) {
            return host.traverseFocusOut(false);
        }
        return false;
    }

    @Override
    public void setSize(final int width, final int height) {
        AbstractPainter.renderLock.lock();
        try {
            this.width = width;
            this.height = height;
            updateSceneState();
        } finally {
            AbstractPainter.renderLock.unlock();
        }

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    @Override
                    public Void run() {
                        if (sceneListener != null) {
                            sceneListener.changedSize(width, height);
                        }
                        return null;
                    }
                }, getAccessControlContext());
            }
        });

        entireSceneNeedsRepaint();
    }

    @Override
    public boolean getPixels(IntBuffer dest, int width, int height) {
        AbstractPainter.renderLock.lock();
        try {
            if (textureBits == null) return false;
            dest.rewind();
            textureBits.rewind();
            if (dest.capacity() != textureBits.capacity()) {
                return false;
            }
            dest.put(textureBits);
            return true;
        } finally {
            AbstractPainter.renderLock.unlock();
        }
    }

    @Override
    public void mouseEvent(final int type, final int button,
                           final boolean primaryBtnDown, final boolean middleBtnDown, final boolean secondaryBtnDown,
                           final int clickCount, final int x, final int y, final int xAbs, final int yAbs,
                           final boolean shift, final boolean ctrl, final boolean alt, final boolean meta,
                           final int wheelRotation, final boolean popupTrigger)
    {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    @Override
                    public Void run() {
                        if (sceneListener == null) {
                            return null;
                        }
                        if (type == AbstractEvents.MOUSEEVENT_WHEEL) {
                            sceneListener.scrollEvent(ScrollEvent.SCROLL, 0, -wheelRotation, 0, 0, 40.0, 40.0,
                                    0, 0, 0, 0, 0,
                                    x, y, xAbs, yAbs, shift, ctrl, alt, meta, false, false);
                        } else {
                            EventType<MouseEvent> eventType = AbstractEvents.mouseIDToFXEventID(type);
                            sceneListener.mouseEvent(eventType, x, y, xAbs, yAbs,
                                    AbstractEvents.mouseButtonToFXMouseButton(button), clickCount,
                                    popupTrigger, false, // do we know if it's synthesized? RT-20142
                                    shift, ctrl, alt, meta,
                                    primaryBtnDown, middleBtnDown, secondaryBtnDown);
                        }
                        return null;
                    }
                }, getAccessControlContext());
            }
        });
    }
    

    @Override
    public void menuEvent(final int x, final int y, final int xAbs, final int yAbs, final boolean isKeyboardTrigger) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    @Override
                    public Void run() {
                        if (sceneListener != null) {
                            sceneListener.menuEvent(x, y, xAbs, yAbs, isKeyboardTrigger);
                        }
                        return null;
                    }
                }, getAccessControlContext());
            }
        });
    }

    @Override
    public void keyEvent(final int type, final int key, final char[] ch, final int modifiers) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    @Override
                    public Void run() {
                        if (sceneListener != null) {
                            boolean shiftDown = (modifiers & AbstractEvents.MODIFIER_SHIFT) != 0;
                            boolean controlDown = (modifiers & AbstractEvents.MODIFIER_CONTROL) != 0;
                            boolean altDown = (modifiers & AbstractEvents.MODIFIER_ALT) != 0;
                            boolean metaDown = (modifiers & AbstractEvents.MODIFIER_META) != 0;
                            sceneListener.keyEvent(AbstractEvents.keyIDToFXEventType(type),
                                    key, ch, shiftDown, controlDown, altDown, metaDown);
                        }
                        return null;
                    }
                }, getAccessControlContext());
            }
        });
    }

    @Override
    public void setCursor(final Object cursor) {
        super.setCursor(cursor);
        host.setCursor((CursorFrame) cursor);
    }

    @Override
    public void setDragStartListener(EmbeddedSceneDragStartListenerInterface l) {
        dndDelegate.setDragStartListener(l);
    }

    @Override
    public EmbeddedSceneDropTargetInterface createDropTarget() {
        return dndDelegate.createDropTarget();
    }
}
