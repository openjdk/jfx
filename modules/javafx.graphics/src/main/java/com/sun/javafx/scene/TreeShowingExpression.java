/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene;

import com.sun.javafx.binding.ExpressionHelper;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Window;

/**
 * Used to observe changes in tree showing status for a {@link Node}.  For a Node's tree to be showing
 * it must be visible, its ancestors must be visible, the node must be part of a {@link Scene} and
 * the scene must have a {@link Window} which is currently showing.<p>
 *
 * This class provides the exact same functionality as {@link NodeHelper#isTreeShowing(Node)} in
 * an observable form.
 */
public class TreeShowingExpression extends BooleanExpression {
    private final ChangeListener<Boolean> windowShowingChangedListener = (obs, oldVal, newVal) -> updateTreeShowing();

    private final ChangeListener<Window> sceneWindowChangedListener = (scene, oldWindow, newWindow) -> {
        if (oldWindow != null) {
            oldWindow.showingProperty().removeListener(windowShowingChangedListener);
        }
        if (newWindow != null) {
            newWindow.showingProperty().addListener(windowShowingChangedListener);
        }
        updateTreeShowing();
    };

    private final ChangeListener<Scene> nodeSceneChangedListener = (node, oldScene, newScene) -> {
        if (oldScene != null) {
            oldScene.windowProperty().removeListener(sceneWindowChangedListener);
        }
        if (newScene != null) {
            newScene.windowProperty().addListener(sceneWindowChangedListener);
        }

        sceneWindowChangedListener.changed(
            null,
            oldScene == null ? null : oldScene.getWindow(),
            newScene == null ? null : newScene.getWindow()
        );
    };

    private final Node node;

    private ExpressionHelper<Boolean> helper;
    private boolean valid;
    private boolean treeShowing;

    /**
     * Constructs a new instance.
     *
     * @param node a {@link Node} for which the tree showing status should be observed, cannot be null
     */
    public TreeShowingExpression(Node node) {
        this.node = node;
        this.node.sceneProperty().addListener(nodeSceneChangedListener);

        NodeHelper.treeVisibleProperty(node).addListener(windowShowingChangedListener);

        nodeSceneChangedListener.changed(null, null, node.getScene());  // registers listeners on Window/Showing
    }

    /**
     * Cleans up any listeners that this class may have registered on the {@link Node}
     * that was supplied at construction.
     */
    public void dispose() {
        node.sceneProperty().removeListener(nodeSceneChangedListener);

        NodeHelper.treeVisibleProperty(node).removeListener(windowShowingChangedListener);

        valid = false;  // prevents unregistration from triggering an invalidation notification
        nodeSceneChangedListener.changed(null, node.getScene(), null);  // unregisters listeners on Window/Showing
    }

    private void updateTreeShowing() {
        boolean newValue = NodeHelper.isTreeShowing(node);

        if (newValue != treeShowing) {
            treeShowing = newValue;
            invalidate();
        }
    }

    @Override
    public void addListener(InvalidationListener listener) {
        helper = ExpressionHelper.addListener(helper, this, listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        helper = ExpressionHelper.removeListener(helper, listener);
    }

    @Override
    public void addListener(ChangeListener<? super Boolean> listener) {
        helper = ExpressionHelper.addListener(helper, this, listener);
    }

    @Override
    public void removeListener(ChangeListener<? super Boolean> listener) {
        helper = ExpressionHelper.removeListener(helper, listener);
    }

    protected void invalidate() {
        if (valid) {
            valid = false;
            ExpressionHelper.fireValueChangedEvent(helper);
        }
    }

    @Override
    public boolean get() {
        if (!valid) {
            updateTreeShowing();
            valid = true;
        }

        return treeShowing;
    }
}
