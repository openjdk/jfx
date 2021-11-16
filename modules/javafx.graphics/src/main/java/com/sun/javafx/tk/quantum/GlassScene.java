/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.input.InputMethodRequests;
import javafx.stage.StageStyle;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.atomic.AtomicBoolean;
import com.sun.glass.ui.Clipboard;
import com.sun.glass.ui.ClipboardAssistance;
import com.sun.glass.ui.View;
import com.sun.javafx.sg.prism.NGCamera;
import com.sun.javafx.sg.prism.NGLightBase;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.tk.TKClipboard;
import com.sun.javafx.tk.TKDragGestureListener;
import com.sun.javafx.tk.TKDragSourceListener;
import com.sun.javafx.tk.TKDropTargetListener;
import com.sun.javafx.tk.TKScene;
import com.sun.javafx.tk.TKSceneListener;
import com.sun.javafx.tk.TKScenePaintListener;
import com.sun.prism.impl.PrismSettings;
import com.sun.prism.paint.Color;
import com.sun.prism.paint.Paint;

abstract class GlassScene implements TKScene {

    private GlassStage stage;

    protected TKSceneListener sceneListener;
    protected TKDragGestureListener dragGestureListener;
    protected TKDragSourceListener dragSourceListener;
    protected TKDropTargetListener dropTargetListener;
    protected InputMethodRequests inputMethodRequests;
    private TKScenePaintListener scenePaintListener;

    private NGNode root;
    private NGCamera camera;
    protected Paint fillPaint;

    // Write from FX thread, read from render thread
    private volatile boolean entireSceneDirty = true;

    private boolean doPresent = true;
    private final AtomicBoolean painting = new AtomicBoolean(false);

    private final boolean depthBuffer;
    private final boolean msaa;

    SceneState sceneState;

    @SuppressWarnings("removal")
    private AccessControlContext accessCtrlCtx = null;

    protected GlassScene(boolean depthBuffer, boolean msaa) {
        this.msaa = msaa;
        this.depthBuffer = depthBuffer;
        sceneState = new SceneState(this);
    }

    @Override
    public void dispose() {
        assert stage == null; // dispose() is called after setStage(null)
        setTKScenePaintListener(null);
        root = null;
        camera = null;
        fillPaint = null;
        sceneListener = null;
        dragGestureListener = null;
        dragSourceListener = null;
        dropTargetListener = null;
        inputMethodRequests = null;
        sceneState = null;
    }

    // To be used by subclasses to enforce context check
    @SuppressWarnings("removal")
    @Override
    public final AccessControlContext getAccessControlContext() {
        if (accessCtrlCtx == null) {
            throw new RuntimeException("Scene security context has not been set!");
        }
        return accessCtrlCtx;
    }

    @SuppressWarnings("removal")
    public final void setSecurityContext(AccessControlContext ctx) {
        if (accessCtrlCtx != null) {
            throw new RuntimeException("Scene security context has been already set!");
        }
        AccessControlContext acc = AccessController.getContext();
        // JDK doesn't provide public APIs to get ACC intersection,
        // so using this ugly workaround
        accessCtrlCtx = GlassStage.doIntersectionPrivilege(
                () -> AccessController.getContext(), acc, ctx);
    }

    public void waitForRenderingToComplete() {
        PaintCollector.getInstance().waitForRenderingToComplete();
    }

    @Override
    public void waitForSynchronization() {
        ViewPainter.renderLock.lock();
    }

    @Override
    public void releaseSynchronization(boolean updateState) {
        // The UI thread has just synchronized the render tree and
        // is about to release the lock so that the render thread
        // can process the new tree.  Capture the current state of
        // the view (such as the width and height) so that the view
        // state matches the state in the render tree
        if (updateState) {
            updateSceneState();
        }
        ViewPainter.renderLock.unlock();
    }

    boolean getDepthBuffer() {
        return depthBuffer;
    }

    boolean isMSAA() {
        return msaa;
    }

    protected abstract boolean isSynchronous();

    @Override public void setTKSceneListener(final TKSceneListener listener) {
        this.sceneListener = listener;
    }

    @Override public synchronized void setTKScenePaintListener(final TKScenePaintListener listener) {
        this.scenePaintListener = listener;
    }

    public void setTKDropTargetListener(final TKDropTargetListener listener) {
        this.dropTargetListener = listener;
    }

    public void setTKDragSourceListener(final TKDragSourceListener listener) {
        this.dragSourceListener = listener;
    }

    public void setTKDragGestureListener(final TKDragGestureListener listener) {
        this.dragGestureListener = listener;
    }

    public void setInputMethodRequests(final InputMethodRequests requests) {
        this.inputMethodRequests = requests;
    }

    @Override
    public void setRoot(NGNode root) {
        this.root = root;
        entireSceneNeedsRepaint();
    }

    protected NGNode getRoot() {
        return root;
    }

    NGCamera getCamera() {
        return camera;
    }

    // List of all attached PGLights
    private NGLightBase[] lights;

    public NGLightBase[] getLights() { return lights; }

    public void setLights(NGLightBase[] lights) { this.lights = lights; }

    @Override
    public void setCamera(NGCamera camera) {
        this.camera = camera == null ? NGCamera.INSTANCE : camera;
        entireSceneNeedsRepaint();
    }

    @Override
    public void setFillPaint(Object fillPaint) {
        this.fillPaint = (Paint)fillPaint;
        entireSceneNeedsRepaint();
    }

    @Override
    public void setCursor(Object cursor) {
        // Do nothing, cursors are implemented in subclasses
    }

    @Override
    public final void markDirty() {
        sceneChanged();
    }

    public void entireSceneNeedsRepaint() {
        if (Platform.isFxApplicationThread()) {
            entireSceneDirty = true;
            sceneChanged();
        }  else {
            Platform.runLater(() -> {
                entireSceneDirty = true;
                sceneChanged();
            });
        }
    }

    public boolean isEntireSceneDirty() {
        return entireSceneDirty;
    }

    public void clearEntireSceneDirty() {
        entireSceneDirty = false;
    }

    @Override
    public TKClipboard createDragboard(boolean isDragSource) {
        ClipboardAssistance assistant = new ClipboardAssistance(Clipboard.DND) {
            @SuppressWarnings("removal")
            @Override
            public void actionPerformed(final int performedAction) {
                super.actionPerformed(performedAction);
                AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                    try {
                        if (dragSourceListener != null) {
                            dragSourceListener.dragDropEnd(0, 0, 0, 0,
                                    QuantumToolkit.clipboardActionToTransferMode(performedAction));
                        }
                    } finally {
                        QuantumClipboard.releaseCurrentDragboard();
                    }
                    return null;
                }, getAccessControlContext());
            }
        };
        return QuantumClipboard.getDragboardInstance(assistant, isDragSource);
    }

    protected final GlassStage getStage() {
        return stage;
    }

    void setStage(GlassStage stage) {
        this.stage = stage;
        sceneChanged();
    }

    final SceneState getSceneState() {
        return sceneState;
    }

    final void updateSceneState() {
        // should only be called on the event thread
        sceneState.update();
    }

    protected View getPlatformView() {
        return null;
    }

    boolean setPainting(boolean value) {
        return painting.getAndSet(value);
    }

    void repaint() {
        // Overridden in subclasses
    }

    final void stageVisible(boolean visible) {
        // if the stage became invisible (for example before being destroyed)
        // we need to remove the scene from the repainter list to prevent
        // potential leak
        if (!visible && PrismSettings.forceRepaint) {
            PaintCollector.getInstance().removeDirtyScene(this);
        }
        if (visible) {
            PaintCollector.getInstance().addDirtyScene(this);
        }
    }

    public void sceneChanged() {
        if (stage != null) {
            // don't mark this scene dirty and add it to the dirty scene list if
            // it is not attached to a Stage. When it does get attached the
            // scene will be marked dirty anyway.
            PaintCollector.getInstance().addDirtyScene(this);
        } else {
            // the scene is no longer associated with a stage, remove from
            // the dirty list and clear. it will be marked dirty if it becomes
            // active again
            PaintCollector.getInstance().removeDirtyScene(this);
        }
    }

    public final synchronized void frameRendered() {
        if (scenePaintListener != null) {
            scenePaintListener.frameRendered();
        }
    }

    public final synchronized void setDoPresent(boolean value) {
        doPresent = value;
    }

    public final synchronized boolean getDoPresent() {
        return doPresent;
    }

    protected Color getClearColor() {
        WindowStage windowStage = stage instanceof WindowStage ? (WindowStage)stage : null;
        if (windowStage != null && windowStage.getPlatformWindow().isTransparentWindow()) {
            return (Color.TRANSPARENT);
        } else {
            if (fillPaint == null) {
                return Color.WHITE;
            } else if (fillPaint.isOpaque() ||
                    (windowStage != null && windowStage.getPlatformWindow().isUnifiedWindow())) {
                //For bare windows the transparent fill is allowed
                if (fillPaint.getType() == Paint.Type.COLOR) {
                    return (Color)fillPaint;
                } else if (depthBuffer) {
                    // Must set clearColor in order for the depthBuffer to be cleared
                    return Color.TRANSPARENT;
                } else {
                    return null;
                }
            } else {
                return Color.WHITE;
            }
        }
    }

    final Paint getCurrentPaint() {
        WindowStage windowStage = stage instanceof WindowStage ? (WindowStage)stage : null;
        if ((windowStage != null) && windowStage.getStyle() == StageStyle.TRANSPARENT) {
            return Color.TRANSPARENT.equals(fillPaint) ? null : fillPaint;
        }
        if ((fillPaint != null) && fillPaint.isOpaque() && (fillPaint.getType() == Paint.Type.COLOR)) {
            return null;
        }
        return fillPaint;
    }

    @Override public String toString() {
        return (" scene: " + hashCode() + ")");
    }
}
