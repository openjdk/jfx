/*
 * Copyright (c) 2008, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.ByteOrder;
import com.sun.glass.ui.Application;
import com.sun.glass.ui.Cursor;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.View;
import com.sun.glass.ui.Window;
import com.sun.javafx.cursor.CursorFrame;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.tk.Toolkit;
import com.sun.prism.GraphicsPipeline;

class ViewScene extends GlassScene {

    private static final String UNSUPPORTED_FORMAT =
        "Transparent windows only supported for BYTE_BGRA_PRE format on LITTLE_ENDIAN machines";

    private View platformView;
    private ViewPainter painter;

    private PaintRenderJob paintRenderJob;

    public ViewScene(boolean depthBuffer, boolean msaa) {
        super(depthBuffer, msaa);

        this.platformView = Application.GetApplication().createView();
        this.platformView.setEventHandler(new GlassViewEventHandler(this));
    }

    @Override protected boolean isSynchronous() {
        return painter != null && painter instanceof PresentingPainter;
    }

    @Override
    protected View getPlatformView() {
        return this.platformView;
    }

    ViewPainter getPainter() {
        return painter;
    }

    @Override
    public void setStage(GlassStage stage) {
        super.setStage(stage);
        if (stage != null) {
            WindowStage wstage  = (WindowStage)stage;
            if (wstage.needsUpdateWindow() || GraphicsPipeline.getPipeline().isUploading()) {
                if (Pixels.getNativeFormat() != Pixels.Format.BYTE_BGRA_PRE ||
                    ByteOrder.nativeOrder() != ByteOrder.LITTLE_ENDIAN) {
                    throw new UnsupportedOperationException(UNSUPPORTED_FORMAT);
                }
                painter = new UploadingPainter(this);
            } else {
                painter = new PresentingPainter(this);
            }
            painter.setRoot(getRoot());
            paintRenderJob = new PaintRenderJob(this, PaintCollector.getInstance().getRendered(), painter);
        }
    }

    WindowStage getWindowStage() {
        return (WindowStage)getStage();
    }

    /* com.sun.javafx.tk.TKScene */

    @Override
    public void dispose() {
        if (platformView != null) {
            QuantumToolkit.runWithRenderLock(() -> {
                platformView.close();
                platformView = null;
                updateSceneState();
                painter = null;
                paintRenderJob = null;
                return null;
            });
        }
        super.dispose();
    }

    @Override public void setRoot(NGNode root) {
        super.setRoot(root);
        if (painter != null) {
            painter.setRoot(root);
        }
    }

    @Override
    public void setCursor(final Object cursor) {
        super.setCursor(cursor);
        Application.invokeLater(() -> {
            final CursorFrame cursorFrame = (CursorFrame) cursor;
            final Cursor platformCursor =
                    CursorUtils.getPlatformCursor(cursorFrame);

            if (platformView != null) {
                Window window = platformView.getWindow();
                if (window != null) {
                    window.setCursor(platformCursor);
                }
            }
        });
    }

    @Override void repaint() {
        if (platformView == null) {
            return;
        }

        if (!setPainting(true)) {
            Toolkit tk = Toolkit.getToolkit();
            tk.addRenderJob(paintRenderJob);
        }
    }

    @Override
    public void enableInputMethodEvents(boolean enable) {
        platformView.enableInputMethodEvents(enable);
    }

    @Override
    public void finishInputMethodComposition() {
        platformView.finishInputMethodComposition();
    }

    @Override public String toString() {
        View view = getPlatformView();
        return (" scene: " + hashCode() + " @ (" + view.getWidth() + "," + view.getHeight() + ")");
    }

    void synchroniseOverlayWarning() {
        try {
            waitForSynchronization();
            OverlayWarning warning = getWindowStage().getWarning();
            if (warning == null) {
                painter.setOverlayRoot(null);
            } else {
                painter.setOverlayRoot(NodeHelper.getPeer(warning));
                warning.updateBounds();
                NodeHelper.updatePeer(warning);
            }
        } finally {
            releaseSynchronization(true);
            entireSceneNeedsRepaint();
        }
    }
}

