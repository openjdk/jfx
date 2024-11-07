/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.layout;

import com.sun.glass.ui.WindowOverlayMetrics;
import com.sun.javafx.stage.StageHelper;
import com.sun.javafx.tk.quantum.WindowStage;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Dimension2D;
import javafx.geometry.HorizontalDirection;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.ContextMenuEvent;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Subscription;

/**
 * Base class for a client-area header bar that is used as a replacement for the system-provided header bar in
 * stages with the {@link StageStyle#EXTENDED} style. This class is intended for application developers to use
 * as a starting point for custom header bar implementations, and it enables the <em>click-and-drag to move</em>
 * and <em>double-click to maximize</em> behaviors that are usually afforded by system-provided header bars.
 * The entire {@code HeaderBarBase} background is draggable by default, but its content is not. Applications
 * can specify draggable content nodes of the {@code HeaderBarBase} with the {@link #setDraggable} method.
 * <p>
 * Some platforms support a system menu that can be summoned by right-clicking the draggable area.
 * This platform-provided menu will only be shown if the {@link ContextMenuEvent#CONTEXT_MENU_REQUESTED}
 * event that is targeted at {@code HeaderBarBase} is not consumed by the application.
 *
 * @apiNote Most application developers should use the {@link HeaderBar} implementation instead of
 *          creating a custom header bar.
 * @see HeaderBar
 * @since 24
 */
public abstract class HeaderBarBase extends Region {

    private static final Dimension2D EMPTY = new Dimension2D(0, 0);
    private static final String DRAGGABLE = "headerbar-draggable";

    /**
     * Specifies whether the child and its subtree is a draggable part of the {@code HeaderBar}.
     * <p>
     * If set to a non-null value, the value will apply for the entire subtree of the child unless
     * another node in the subtree specifies a different value. Setting the value to {@code null}
     * will remove the flag.
     *
     * @param child the child node
     * @param value a {@code Boolean} value indicating whether the child and its subtree is draggable,
     *              or {@code null} to remove the flag
     */
    public static void setDraggable(Node child, Boolean value) {
        Pane.setConstraint(child, DRAGGABLE, value);
    }

    /**
     * Returns whether the child and its subtree is a draggable part of the {@code HeaderBar}.
     *
     * @param child the child node
     * @return a {@code Boolean} value indicating whether the child and its subtree is draggable,
     *         or {@code null} if not set
     */
    public static Boolean isDraggable(Node child) {
        return (Boolean)Pane.getConstraint(child, DRAGGABLE);
    }

    private Subscription subscription;
    private WindowOverlayMetrics currentMetrics;
    private boolean currentFullScreen;

    /**
     * Constructor called by subclasses.
     */
    protected HeaderBarBase() {
        var stage = sceneProperty()
            .flatMap(Scene::windowProperty)
            .map(w -> w instanceof Stage s ? s : null);

        stage.flatMap(Window::showingProperty)
            .orElse(false)
            .subscribe(this::onShowingChanged);

        stage.flatMap(Stage::fullScreenProperty)
            .orElse(false)
            .subscribe(this::onFullScreenChanged);
    }

    private void onShowingChanged(boolean showing) {
        if (!showing) {
            if (subscription != null) {
                subscription.unsubscribe();
                subscription = null;
            }
        } else if (getScene().getWindow() instanceof Stage stage
                   && StageHelper.getPeer(stage) instanceof WindowStage windowStage) {
            subscription = Subscription.combine(
                windowStage
                    .getPlatformWindow()
                    .getWindowOverlayMetrics()
                    .subscribe(this::onMetricsChanged),
                windowStage
                    .getPlatformWindow()
                    .registerHeaderBar(this));
        }
    }

    private void onMetricsChanged(WindowOverlayMetrics metrics) {
        currentMetrics = metrics;
        updateInsets();
    }

    private void onFullScreenChanged(boolean fullScreen) {
        currentFullScreen = fullScreen;
        updateInsets();
    }

    private void updateInsets() {
        if (currentFullScreen || currentMetrics == null) {
            leftSystemInset.set(EMPTY);
            rightSystemInset.set(EMPTY);
            minSystemHeight.set(0);
            return;
        }

        if (currentMetrics.placement() == HorizontalDirection.LEFT) {
            leftSystemInset.set(currentMetrics.size());
            rightSystemInset.set(EMPTY);
        } else if (currentMetrics.placement() == HorizontalDirection.RIGHT) {
            leftSystemInset.set(EMPTY);
            rightSystemInset.set(currentMetrics.size());
        } else {
            leftSystemInset.set(EMPTY);
            rightSystemInset.set(EMPTY);
        }

        minSystemHeight.set(currentMetrics.minHeight());
    }

    /**
     * Describes the size of the left system inset, which is an area reserved for the
     * minimize, maximize, and close window buttons. If there are no window buttons on
     * the left side of the window, the returned area is an empty {@code Dimension2D}.
     * <p>
     * Note that the left system inset refers to the physical left side of the window,
     * independent of layout orientation.
     */
    private final ReadOnlyObjectWrapper<Dimension2D> leftSystemInset =
        new ReadOnlyObjectWrapper<>(this, "leftInset", new Dimension2D(0, 0)) {
            @Override
            protected void invalidated() {
                requestLayout();
            }
        };

    public final ReadOnlyObjectWrapper<Dimension2D> leftSystemInsetProperty() {
        return leftSystemInset;
    }

    public final Dimension2D getLeftSystemInset() {
        return leftSystemInset.get();
    }

    /**
     * Describes the size of the right system inset, which is an area reserved for the
     * minimize, maximize, and close window buttons. If there are no window buttons on
     * the right side of the window, the returned area is an empty {@code Dimension2D}.
     * <p>
     * Note that the right system inset refers to the physical right side of the window,
     * independent of layout orientation.
     */
    private final ReadOnlyObjectWrapper<Dimension2D> rightSystemInset =
        new ReadOnlyObjectWrapper<>(this, "rightInset", EMPTY) {
            @Override
            protected void invalidated() {
                requestLayout();
            }
        };

    public final ReadOnlyObjectWrapper<Dimension2D> rightSystemInsetProperty() {
        return rightSystemInset;
    }

    public final Dimension2D getRightSystemInset() {
        return rightSystemInset.get();
    }

    /**
     * The absolute minimum height of {@link #leftSystemInsetProperty() leftSystemInset} and
     * {@link #rightSystemInsetProperty() rightSystemInset}. This is a platform-dependent value
     * that a {@code HeaderBarBase} implementation can use to define a reasonable minimum height
     * for the header bar area.
     */
    private final ReadOnlyDoubleWrapper minSystemHeight =
        new ReadOnlyDoubleWrapper(this, "minSystemHeight") {
            @Override
            protected void invalidated() {
                requestLayout();
            }
        };

    public final ReadOnlyDoubleProperty minSystemHeightProperty() {
        return minSystemHeight.getReadOnlyProperty();
    }

    public final double getMinSystemHeight() {
        return minSystemHeight.get();
    }
}
