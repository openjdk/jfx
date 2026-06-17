/*
 * Copyright (c) 2025, 2026, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.fx.monkey.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.PopupWindow.AnchorLocation;

/**
 * Base class for button with a custom popup container.
 */
public class PopupButton extends Button {

    private Supplier<Node> generator;
    private Popup popup;
    private List<Runnable> onShown;

    public PopupButton(Supplier<Node> gen) {
        setContentSupplier(gen);
    }

    public PopupButton() {
    }

    public final void setContentSupplier(Supplier<Node> gen) {
        this.generator = gen;
    }

    public final void onShown(Runnable r) {
        if (onShown == null) {
            onShown = new CopyOnWriteArrayList();
        }
        onShown.add(r);
    }

    private void handleOnShown() {
        if (onShown != null) {
            for (Runnable r : onShown) {
                r.run();
            }
        }
    }

    public final void hidePopup() {
        if (popup != null) {
            popup.hide();
        }
    }

    private Popup createPopup(Node n) {
        StackPane content = new StackPane(n);
        content.setStyle("""
            -fx-background-color: -fx-outer-border, -fx-body-color;
            -fx-background-insets: 0, 1;
            -fx-padding: 1em 1em 1em 1em;
            -fx-effect: dropshadow( gaussian , rgba(0,0,0,0.2) , 12, 0.0 , 0 , 8 );
            """);

        Popup p = new Popup();
        p.setAnchorLocation(AnchorLocation.WINDOW_TOP_LEFT);
        p.setAutoHide(true);
        p.getContent().add(content);
        p.setOnShown((_) -> {
            handleOnShown();
        });
        return p;
    }

    public final void togglePopup() {
        if (popup == null) {
            Node n = generator.get();
            popup = createPopup(n);
            popup.setOnHidden((_) -> {
                if (popup != null) {
                    popup = null;
                }
            });
            Point2D p = localToScreen(0.0, getHeight());
            popup.show(this, p.getX(), p.getY());
        } else {
            popup.hide();
            popup = null;
        }
    }
}
