/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.scene.SceneHelper;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.util.Subscription;

/**
 * Shows an overlay over a {@link javafx.scene.Scene}.
 * The overlay is not part of the scene graph, and not accessible by applications.
 */
final class ViewSceneOverlay {

    private final Scene scene;
    private final ViewPainter painter;
    private final Subscription subscriptions;
    private Parent root;
    private boolean rootDirty;
    private double width, height;

    ViewSceneOverlay(Scene scene, ViewPainter painter) {
        this.scene = scene;
        this.painter = painter;
        this.subscriptions = Subscription.combine(
            scene.rootProperty().subscribe(this::onSceneRootChanged),
            scene.effectiveNodeOrientationProperty().subscribe(this::onEffectiveNodeOrientationInvalidated));
    }

    public void dispose() {
        subscriptions.unsubscribe();
    }

    public void reapplyCSS() {
        if (root != null) {
            NodeHelper.reapplyCSS(root);
        }
    }

    public void processCSS() {
        if (root != null) {
            NodeHelper.processCSS(root);
        }
    }

    public void resize(double width, double height) {
        this.width = width;
        this.height = height;
    }

    public void layout() {
        if (scene == null) {
            return;
        }

        var window = scene.getWindow();

        if (root != null && window != null) {
            root.resize(width, height);
            root.layout();
            NodeHelper.updateBounds(root);
        }
    }

    public void setRoot(Parent root) {
        if (this.root == root) {
            return;
        }

        if (this.root != null) {
            NodeHelper.setParent(this.root, null);
            NodeHelper.setScenes(this.root, null, null);
            NodeHelper.setInheritOrientationFromScene(this.root, false);
        }

        this.root = root;

        if (root != null) {
            NodeHelper.setParent(root, scene.getRoot());
            NodeHelper.setScenes(root, scene, null);
            NodeHelper.setInheritOrientationFromScene(root, true);
        }

        rootDirty = true;
    }

    public void synchronize() {
        if (rootDirty || (root != null && !NodeHelper.isDirtyEmpty(root))) {
            rootDirty = false;

            if (root != null) {
                syncPeer(root);
                painter.setOverlayRoot(NodeHelper.getPeer(root));
            } else {
                painter.setOverlayRoot(null);
                SceneHelper.getPeer(scene).entireSceneNeedsRepaint();
            }
        }
    }

    private void syncPeer(Node node) {
        NodeHelper.syncPeer(node);

        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                syncPeer(child);
            }
        } else if (node instanceof SubScene subScene) {
            syncPeer(subScene.getRoot());
        }

        if (node.getClip() != null) {
            syncPeer(node.getClip());
        }
    }

    private void onSceneRootChanged(Parent sceneRoot) {
        if (root != null) {
            NodeHelper.setParent(root, sceneRoot);
        }
    }

    private void onEffectiveNodeOrientationInvalidated(NodeOrientation unused) {
        if (root != null) {
            NodeHelper.nodeResolvedOrientationInvalidated(root);
        }
    }
}
