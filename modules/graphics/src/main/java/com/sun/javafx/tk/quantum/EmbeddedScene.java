/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.embed.HostDragStartListener;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.EventType;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.InputMethodRequests;
import javafx.scene.input.InputMethodTextRun;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import java.nio.IntBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import com.sun.javafx.cursor.CursorFrame;
import com.sun.javafx.embed.AbstractEvents;
import com.sun.javafx.embed.EmbeddedSceneDTInterface;
import com.sun.javafx.embed.EmbeddedSceneInterface;
import com.sun.javafx.embed.HostInterface;
import com.sun.javafx.scene.input.KeyCodeMap;
import com.sun.javafx.scene.traversal.Direction;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.tk.TKClipboard;
import com.sun.javafx.tk.Toolkit;
import com.sun.prism.paint.Color;
import com.sun.prism.paint.Paint;
import com.sun.glass.ui.Pixels;

final class EmbeddedScene extends GlassScene implements EmbeddedSceneInterface {

    // TODO: synchronize access to embedder from ET and RT
    private HostInterface host;

    private UploadingPainter        painter;
    private PaintRenderJob          paintRenderJob;
    
    private final EmbeddedSceneDnD embeddedDnD;

    private volatile IntBuffer  texBits;
    private volatile int        texLineStride; // pre-scaled
    private volatile float      texScaleFactor = 1.0f;
 
    public EmbeddedScene(HostInterface host, boolean depthBuffer, boolean msaa) {
        super(depthBuffer, msaa);
        sceneState = new EmbeddedState(this);

        this.host = host;
        this.embeddedDnD = new EmbeddedSceneDnD(this);

        PaintCollector collector = PaintCollector.getInstance();
        painter = new UploadingPainter(this);
        paintRenderJob = new PaintRenderJob(this, collector.getRendered(), painter);
    }

    @Override
    public void dispose() {
        assert host != null;
        QuantumToolkit.runWithRenderLock(() -> {
            host.setEmbeddedScene(null);
            host = null;
            updateSceneState();
            painter = null;
            paintRenderJob = null;
            texBits = null;
            return null;
        });
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
        return embeddedDnD.createDragboard(isDragSource);
    }

    @Override
    public void enableInputMethodEvents(boolean enable) {
        if (QuantumToolkit.verbose) {
            System.err.println("EmbeddedScene.enableInputMethodEvents " + enable);
        }
    }

    @Override
    public void finishInputMethodComposition() {
        if (QuantumToolkit.verbose) {
            System.err.println("EmbeddedScene.finishInputMethodComposition");
        }
    }
    
    @Override
    public void setPixelScaleFactor(float scale) {
        painter.setPixelScaleFactor(scale);
        entireSceneNeedsRepaint();
    }

    // Called by EmbeddedPainter on the render thread under renderLock
    void uploadPixels(Pixels pixels) {
        texBits = (IntBuffer)pixels.getPixels();
        texLineStride = pixels.getWidthUnsafe();
        texScaleFactor = pixels.getScaleUnsafe();
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
        Platform.runLater(() -> {
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                if (sceneListener != null) {
                    sceneListener.changedSize(width, height);
                }
                return null;
            }, getAccessControlContext());
        });
    }

    /**
     * @param dest the destination buffer
     * @param width the logical width of the buffer
     * @param height the logical height of the buffer
     * @param scale the scale factor
     * @return 
     */
    @Override
    public boolean getPixels(final IntBuffer dest, final int width, final int height) {
        return QuantumToolkit.runWithRenderLock(() -> {
            int scaledWidth = width;
            int scaledHeight = height;

            // The dest buffer scale factor is expected to match painter.getPixelScaleFactor().
            if (painter.getPixelScaleFactor() != texScaleFactor || texBits == null) {
                return false;
            }
            scaledWidth = (int)Math.round(scaledWidth * texScaleFactor);
            scaledHeight = (int)Math.round(scaledHeight * texScaleFactor);
        
            dest.rewind();
            texBits.rewind();
            if (dest.capacity() != texBits.capacity()) {
                // Calculate the intersection of the dest & src images.
                int w = Math.min(scaledWidth, texLineStride);
                int h = Math.min(scaledHeight, texBits.capacity() / texLineStride);

                // Copy the intersection to the dest.
                // The backed array of the textureBits may not be available,
                // so not relying on it.
                int[] linebuf = new int[w];
                for (int i = 0; i < h; i++) {
                    texBits.position(i * texLineStride);
                    texBits.get(linebuf, 0, w);
                    dest.position(i * scaledWidth);
                    dest.put(linebuf);
                }
                return true;
            }
            dest.put(texBits);
            return true;
        });
    }
    
    @Override
    protected Color getClearColor() {
        if (fillPaint != null && fillPaint.getType() == Paint.Type.COLOR &&
            ((Color)fillPaint).getAlpha() == 0f)
        {
            return (Color)fillPaint;
        }
        return super.getClearColor();
    }

    @Override
    public void mouseEvent(final int type, final int button,
                           final boolean primaryBtnDown, final boolean middleBtnDown, final boolean secondaryBtnDown,
                           final int x, final int y, final int xAbs, final int yAbs,
                           final boolean shift, final boolean ctrl, final boolean alt, final boolean meta,
                           final int wheelRotation, final boolean popupTrigger)
    {
        Platform.runLater(() -> {
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                if (sceneListener == null) {
                    return null;
                }
                // Click events are generated in Scene, so we don't expect them here
                assert type != AbstractEvents.MOUSEEVENT_CLICKED;
                if (type == AbstractEvents.MOUSEEVENT_WHEEL) {
                    sceneListener.scrollEvent(ScrollEvent.SCROLL, 0, -wheelRotation, 0, 0, 40.0, 40.0,
                            0, 0, 0, 0, 0,
                            x, y, xAbs, yAbs, shift, ctrl, alt, meta, false, false);
                } else {
                    EventType<MouseEvent> eventType = AbstractEvents.mouseIDToFXEventID(type);
                    sceneListener.mouseEvent(eventType, x, y, xAbs, yAbs,
                            AbstractEvents.mouseButtonToFXMouseButton(button),
                            popupTrigger, false, // do we know if it's synthesized? RT-20142
                            shift, ctrl, alt, meta,
                            primaryBtnDown, middleBtnDown, secondaryBtnDown);
                }
                return null;
            }, getAccessControlContext());
        });
    }

    @Override
    public void inputMethodEvent(final EventType<InputMethodEvent> type,
                                 final ObservableList<InputMethodTextRun> composed, final String committed,
                                 final int caretPosition) {
        Platform.runLater(() -> {
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                if (sceneListener != null) {
                    sceneListener.inputMethodEvent(type, composed, committed, caretPosition);
                }
                return null;
            });
        });
    }

    @Override
    public void menuEvent(final int x, final int y, final int xAbs, final int yAbs, final boolean isKeyboardTrigger) {
        Platform.runLater(() -> {
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                if (sceneListener != null) {
                    sceneListener.menuEvent(x, y, xAbs, yAbs, isKeyboardTrigger);
                }
                return null;
            }, getAccessControlContext());
        });
    }

    @Override
    public void keyEvent(final int type, final int key, final char[] ch, final int modifiers) {
        Platform.runLater(() -> {
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                if (sceneListener != null) {
                    boolean shiftDown = (modifiers & AbstractEvents.MODIFIER_SHIFT) != 0;
                    boolean controlDown = (modifiers & AbstractEvents.MODIFIER_CONTROL) != 0;
                    boolean altDown = (modifiers & AbstractEvents.MODIFIER_ALT) != 0;
                    boolean metaDown = (modifiers & AbstractEvents.MODIFIER_META) != 0;

                    String str = new String(ch);
                    String text = str; // TODO: this must be a text like "HOME", "F1", or "A"
                    javafx.scene.input.KeyEvent keyEvent = new javafx.scene.input.KeyEvent(
                            AbstractEvents.keyIDToFXEventType(type),
                            str, text,
                            KeyCodeMap.valueOf(key),
                            shiftDown, controlDown, altDown, metaDown);
                    sceneListener.keyEvent(keyEvent);
                }
                return null;
            }, getAccessControlContext());
        });
    }

    @Override
    public void setCursor(final Object cursor) {
        super.setCursor(cursor);
        host.setCursor((CursorFrame) cursor);
    }

    @Override
    public void setDragStartListener(HostDragStartListener l) {
        embeddedDnD.setDragStartListener(l);
    }

    @Override
    public EmbeddedSceneDTInterface createDropTarget() {
        return embeddedDnD.createDropTarget();
    }

    @Override
    public InputMethodRequests getInputMethodRequests() {
        return inputMethodRequests;
    }
}
