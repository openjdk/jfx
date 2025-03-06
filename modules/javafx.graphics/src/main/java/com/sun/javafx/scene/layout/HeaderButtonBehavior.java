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

package com.sun.javafx.scene.layout;

import com.sun.javafx.PlatformUtil;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HeaderButtonType;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Subscription;
import java.util.Objects;
import java.util.Optional;

public final class HeaderButtonBehavior implements EventHandler<MouseEvent> {

    private static final PseudoClass MAXIMIZED_PSEUDO_CLASS = PseudoClass.getPseudoClass("maximized");

    private final Node node;
    private final HeaderButtonType type;
    private final Subscription subscription;

    public HeaderButtonBehavior(Node node, HeaderButtonType type) {
        this.node = Objects.requireNonNull(node);
        this.type = Objects.requireNonNull(type);

        ObservableValue<Stage> stage = node.sceneProperty()
            .flatMap(Scene::windowProperty)
            .map(w -> w instanceof Stage s ? s : null);

        if (type == HeaderButtonType.MAXIMIZE) {
            subscription = Subscription.combine(
                stage.flatMap(Stage::resizableProperty).subscribe(this::onResizableChanged),
                stage.flatMap(Stage::fullScreenProperty).subscribe(this::onFullScreenChanged),
                stage.flatMap(Stage::maximizedProperty).subscribe(this::onMaximizedChanged),
                () -> node.removeEventHandler(MouseEvent.MOUSE_RELEASED, this)
            );
        } else {
            subscription = Subscription.combine(
                stage.flatMap(Stage::fullScreenProperty).subscribe(this::onFullScreenChanged),
                () -> node.removeEventHandler(MouseEvent.MOUSE_RELEASED, this)
            );
        }

        node.addEventHandler(MouseEvent.MOUSE_RELEASED, this);

        if (!node.focusTraversableProperty().isBound()) {
            node.setFocusTraversable(false);
        }
    }

    public void dispose() {
        subscription.unsubscribe();
    }

    @Override
    public void handle(MouseEvent event) {
        if (!node.getLayoutBounds().contains(event.getX(), event.getY())) {
            return;
        }

        switch (type) {
            case CLOSE -> getStage().ifPresent(Stage::close);
            case ICONIFY -> getStage().ifPresent(stage -> stage.setIconified(true));
            case MAXIMIZE -> getStage().ifPresent(stage -> {
                    // On macOS, a non-modal window is put into full-screen mode when the maximize button is clicked,
                    // but enlarged to cover the desktop when the option key is pressed at the same time.
                    if (PlatformUtil.isMac() && stage.getModality() == Modality.NONE && !event.isAltDown()) {
                        stage.setFullScreen(!stage.isFullScreen());
                    } else {
                        stage.setMaximized(!stage.isMaximized());
                    }
                });
        }
    }

    private Optional<Stage> getStage() {
        Scene scene = node.getScene();
        if (scene == null) {
            return Optional.empty();
        }

        return scene.getWindow() instanceof Stage stage
            ? Optional.of(stage)
            : Optional.empty();
    }

    private void onResizableChanged(Boolean resizable) {
        if (!node.disableProperty().isBound()) {
            node.setDisable(resizable == Boolean.FALSE);
        }
    }

    private void onFullScreenChanged(Boolean fullScreen) {
        if (!node.visibleProperty().isBound() && !node.managedProperty().isBound()) {
            node.setVisible(fullScreen != Boolean.TRUE);
            node.setManaged(fullScreen != Boolean.TRUE);
        }
    }

    private void onMaximizedChanged(Boolean maximized) {
        node.pseudoClassStateChanged(MAXIMIZED_PSEUDO_CLASS, maximized == Boolean.TRUE);
    }
}
