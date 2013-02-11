/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.pgstub;

import com.sun.javafx.geom.PickRay;
import com.sun.javafx.geom.Vec3d;
import com.sun.javafx.sg.PGCamera;
import com.sun.javafx.sg.PGNode;
import com.sun.javafx.tk.TKClipboard;
import com.sun.javafx.tk.TKScene;
import com.sun.javafx.tk.TKSceneListener;
import com.sun.javafx.tk.TKScenePaintListener;

import javafx.scene.input.Dragboard;

import java.security.AccessControlContext;

public class StubScene implements TKScene {

    StubStage stage;
    private TKSceneListener listener;
    private Object cursor;

    @Override
    public void setSecurityContext(AccessControlContext ctx) {
        // ignore
    }

    public void waitForSynchronization() {
        //ignore
    }

    public void releaseSynchronization() {
        // ignore
    }

    public void setTKSceneListener(TKSceneListener listener) {
        this.listener = listener;
    }

    public void setScene(Object scene) {
        // ignore
    }

    public void setRoot(PGNode root) {
        // ignore
    }

    public void markDirty() {
        // ignore
    }

    public void setCamera(PGCamera ci) {
        // ignore
    }

    public PickRay computePickRay(float x, float y, PickRay pickRay) {
        // static parallel pickray ignoring camera and everything
        if (pickRay == null) {
            pickRay = new PickRay();
        }
        pickRay.setOrigin(new Vec3d(x, y, -1000));
        pickRay.setDirection(new Vec3d(0, 0, 1000));
        return pickRay;
    }

    public void setFillPaint(Object fillPaint) {
        // ignore
    }

    public void setCursor(Object cursor) {
        this.cursor = cursor;
    }

    public Object getCursor() {
        return cursor;
    }

    public void requestFocus() {
        if (stage != null) {
            stage.requestFocus();
        }
    }

    public void enableInputMethodEvents(boolean enable) {
        // ignore
    }

    public void entireSceneNeedsRepaint() {
    }

    @Override
    public TKClipboard createDragboard(boolean isDragSource) {
        return StubToolkit.createDragboard();
    }

    @Override
    public void setTKScenePaintListener(TKScenePaintListener listener) {
        // not implemented
    }

    public TKSceneListener getListener() {
        return listener;
    }
}
