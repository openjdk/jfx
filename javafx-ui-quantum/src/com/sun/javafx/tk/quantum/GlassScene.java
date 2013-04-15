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
import javafx.scene.input.InputMethodRequests;
import javafx.stage.StageStyle;
import com.sun.glass.ui.Clipboard;
import com.sun.glass.ui.ClipboardAssistance;
import com.sun.glass.ui.View;
import com.sun.javafx.geom.PickRay;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.Vec3d;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.GeneralTransform3D;
import com.sun.javafx.geom.transform.NoninvertibleTransformException;
import com.sun.javafx.sg.PGCamera;
import com.sun.javafx.sg.PGNode;
import com.sun.javafx.sg.prism.NGCamera;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.sg.prism.SceneChangeListener;
import com.sun.javafx.tk.TKClipboard;
import com.sun.javafx.tk.TKDragGestureListener;
import com.sun.javafx.tk.TKDragSourceListener;
import com.sun.javafx.tk.TKDropTargetListener;
import com.sun.javafx.tk.TKScene;
import com.sun.javafx.tk.TKSceneListener;
import com.sun.javafx.tk.TKScenePaintListener;
import com.sun.prism.camera.PrismCameraImpl;
import com.sun.prism.camera.PrismParallelCameraImpl;
import com.sun.prism.camera.PrismPerspectiveCameraImpl;
import com.sun.prism.impl.PrismSettings;
import com.sun.prism.paint.Color;
import com.sun.prism.paint.Paint;

import sun.util.logging.PlatformLogger;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;

abstract class GlassScene implements TKScene, SceneChangeListener {

    protected boolean verbose;

    protected GlassStage glassStage;

    protected TKSceneListener sceneListener;
    protected TKDragGestureListener dragGestureListener;
    protected TKDragSourceListener dragSourceListener;
    protected TKDropTargetListener dropTargetListener;
    protected InputMethodRequests inputMethodRequests;
    private TKScenePaintListener scenePaintListener;

    private TKClipboard dragSourceClipboard;

    private NGNode root;
    private PrismCameraImpl camera;
    private Paint fillPaint;

    private boolean entireSceneDirty = true;
    private boolean dirty = true;
    private boolean doPresent = true;

    private boolean depthBuffer = false;

    private final SceneState sceneState;

    private AccessControlContext accessCtrlCtx = null;

    protected GlassScene(boolean verbose) {
        this(verbose, false);
    }

    protected GlassScene(boolean verbose, boolean depthBuffer) {
        this.verbose = verbose;
        this.depthBuffer = depthBuffer;
        sceneState = new SceneState(this);
    }

    // To be used by subclasses to enforce context check
    final AccessControlContext getAccessControlContext() {
        if (accessCtrlCtx == null) {
            throw new RuntimeException("Scene security context has not been set!");
        }
        return accessCtrlCtx;
    }

    @Override public final void setSecurityContext(AccessControlContext ctx) {
        if (accessCtrlCtx != null) {
            throw new RuntimeException("Scene security context has been already set!");
        }
        accessCtrlCtx = ctx;
    }

    @Override
    public void waitForSynchronization() {
        AbstractPainter.renderLock.lock();
    }

    @Override
    public void releaseSynchronization() {
        // The UI thread has just synchronized the render tree and
        // is about to release the lock so that the render thread
        // can process the new tree.  Capture the current state of
        // the view (such as the width and height) so that the view
        // state matches the state in the render tree
        updateSceneState();
        AbstractPainter.renderLock.unlock();
    }

    boolean getDepthBuffer() {
        return depthBuffer;
    }

    protected abstract boolean isSynchronous();

    @Override public void setScene(Object scene) {
        // Overridden in subclasses
    }

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
    public void setRoot(PGNode root) {
        this.root = (NGNode)root;
        entireSceneNeedsRepaint();
    }

    protected NGNode getRoot() {
        return root;
    }

    PrismCameraImpl getCamera() {
        return camera;
    }

    // List of all attached PGLights
    private Object lights[];

    public Object[] getLights() { return lights; }

    public void setLights(Object[] lights) { this.lights = lights; }

    @Override
    public void setCamera(PGCamera camera) {
        if (camera != null) {
            this.camera = ((NGCamera) camera).getCameraImpl();
        } else {
            this.camera = PrismParallelCameraImpl.getInstance();
        }
        entireSceneNeedsRepaint();
    }

    Paint getFillPaint() {
        return fillPaint;
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
    public void markDirty() {
        sceneChanged();
    }

    public void entireSceneNeedsRepaint() {
        if (Platform.isFxApplicationThread()) {
            entireSceneDirty = true;
            sceneChanged();
        }  else {
            Platform.runLater(new Runnable() {
                @Override public void run() {
                    entireSceneDirty = true;
                    sceneChanged();
                }
            });
        }
    }

    public boolean isEntireSceneDirty() {
        return entireSceneDirty;
    }

    public void clearEntireSceneDirty() {
        entireSceneDirty = false;
    }

    @Override public void requestFocus() {
        if (glassStage != null) {
            glassStage.requestFocus();
        }
    }

    @Override
    public TKClipboard createDragboard(boolean isDragSource) {
        ClipboardAssistance assistant = new ClipboardAssistance(Clipboard.DND) {
            @Override public void actionPerformed(final int performedAction) {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    @Override
                    public Void run() {
                        if ((dragSourceClipboard != null) && (dragSourceListener != null)) {
                            dragSourceListener.dragDropEnd(0, 0, 0, 0,
                                    QuantumToolkit.clipboardActionToTransferMode(performedAction));
                        }
                        dragSourceClipboard = null;
                        return null;
                    }
                }, getAccessControlContext());
            }
        };
        QuantumClipboard dragboard = QuantumClipboard.getDragboardInstance(assistant);
        if (isDragSource) {
            dragSourceClipboard = dragboard;
        }
        return dragboard;
    }

    void setGlassStage(GlassStage stage) {
        glassStage = stage;
        if (glassStage != null) {
            sceneChanged();
        } else {
            // the scene is no longer associated with a stage, remove from
            // the dirty list and clear. it will be marked dirty if it becomes
            // active again
            PaintCollector.getInstance().removeDirtyScene(this);
        }
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

    void repaint() {
        // Overridden in subclasses
    }

    void stageVisible(boolean visible) {
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

    // Note: Class variables use with care, not MT safe.
    private static final double[] projMatValues = new double[16];
    private static final Vec3d ptCc = new Vec3d();
    private static final Vec3d ptEc = new Vec3d();
    private static final Vec3d ptWc = new Vec3d();
    private static final Vec3d eyeEc = new Vec3d();
    private static final Vec3d eyeWc = new Vec3d();

    //TODO: 3D - Need to handle movable camera ...
    @Override public PickRay computePickRay(float x, float y, PickRay pickRay) {
        GeneralTransform3D projTx = camera.getProjectionTransform(null);
        projTx.get(projMatValues);

        Affine3D viewTx = camera.getViewTransform(null);
        Rectangle vp = camera.getViewport(null);

        double xCc = (x / vp.width) * 2.0 - 1.0;
        double yCc = (y / vp.height) * -2.0 + 1.0;

        if (!(camera instanceof PrismPerspectiveCameraImpl)) {
            // Parallel projection
            if ((pickRay == null) || (!pickRay.isParallel())) {
                pickRay = new PickRay(x, y);
            } else {
                pickRay.set(x, y);
            }
        } else {
            // Perspective projection
            double[] m = projMatValues;
            double zEc = (1.0 - m[15]) / m[14];
            double zCc = m[10] * zEc + m[11];
            ptCc.set(xCc, yCc, zCc);

            // Invert the projection transform. Note that we reuse projTx
            // to avoid constructing another transform.
            projTx.invert();

            // Transform the Cc point into Ec via the inverse projection transform
            projTx.transform(ptCc, ptEc);
            try {
                // Invert the view transform. Note that we reuse projTx
                // to avoid constructing another transform.
                // TODO: if we decide to define picking in Ec rather
                // than Wc then this step becomes unnecessary; instead it will
                // be handled as part of the ModelView transform
                viewTx.invert();
            } catch (NoninvertibleTransformException ex) {
                String logname = ViewScene.class.getName();
                PlatformLogger.getLogger(logname).severe("computePickRay", ex);
            }

            viewTx.transform(ptEc, ptWc);

            eyeEc.set(0.0, 0.0, 0.0);
            viewTx.transform(eyeEc, eyeWc);

            //            String msg = "computePickRay:" + "\n" +
            //                    "  ptCc = " + ptCc + "\n" +
            //                    "  ptEc = " + ptEc + "\n" +
            //                    "  ptWc = " + ptWc + "\n" +
            //                    "  eyeEc = " + eyeEc + "  eyeWc = " + eyeWc;
            //            System.err.println(msg);

            if ((pickRay == null) || (pickRay.isParallel())) {
                pickRay = new PickRay(eyeWc, ptWc);
            } else {
                pickRay.set(eyeWc, ptWc);
            }
            pickRay.getDirectionNoClone().sub(eyeWc);
        }

        return pickRay;
    }

    /* com.sun.javafx.tk.TKSceneListener */

    @Override public void sceneChanged() {
        if (glassStage instanceof PopupStage) {
            GlassScene popupScene = ((PopupStage)glassStage).getOwnerScene();
            if (popupScene != null) {
                popupScene.sceneChanged();
            }
        }
        if (glassStage != null) {
            // don't mark this scene dirty and add it to the dirty scene list if
            // it is not attached to a Stage. When it does get attached the
            // scene will be marked dirty anyway.
            PaintCollector.getInstance().addDirtyScene(GlassScene.this);
        }
    }

    public final synchronized void frameRendered() {
        if (scenePaintListener != null) {
            scenePaintListener.frameRendered();
        }
    }

    public final synchronized void setDirty(boolean value) {
        dirty = value;
    }

    public final synchronized boolean getDirty() {
        return dirty;
    }

    public final synchronized void setDoPresent(boolean value) {
        doPresent = value;
    }

    public final synchronized boolean getDoPresent() {
        return doPresent;
    }

    protected final Color getClearColor() {
        WindowStage windowStage = glassStage instanceof WindowStage ? (WindowStage)glassStage : null;
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

    protected Paint getCurrentPaint() {
        WindowStage windowStage = glassStage instanceof WindowStage ? (WindowStage)glassStage : null;
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
