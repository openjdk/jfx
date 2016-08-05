/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.scene.DirtyBits;
import com.sun.javafx.scene.NodeHelper;
import javafx.animation.Animation.Status;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

public class OverlayWarning extends Group {
    static {
        // This is used by classes in different packages to get access to
        // private and package private methods.
        OverlayWarningHelper.setOverlayWarningAccessor(
                new OverlayWarningHelper.OverlayWarningAccessor() {
            @Override
            public void doUpdatePeer(Node node) {
                ((OverlayWarning) node).doUpdatePeer();
            }

            @Override
            public void doMarkDirty(Node node, DirtyBits dirtyBit) {
                ((OverlayWarning) node).doMarkDirty(dirtyBit);
            }
        });
    }

    private static final float  PAD      = 40f;
    private static final float  RECTW    = 600f;
    private static final float  RECTH    = 100f;
    private static final float  ARC      = 20f;
    private static final int    FONTSIZE = 24;

    private ViewScene               view;
    private SequentialTransition    overlayTransition;
    private boolean                 warningTransition;

    {
        // To initialize the class helper at the begining each constructor of this class
        OverlayWarningHelper.initHelper(this);
    }

    public OverlayWarning(final ViewScene vs) {
        view = vs;

        createOverlayGroup();

        PauseTransition pause = new PauseTransition(Duration.millis(4000));
        FadeTransition fade = new FadeTransition(Duration.millis(1000), this);
        fade.setFromValue(1);
        fade.setToValue(0);

        overlayTransition = new SequentialTransition();
        overlayTransition.getChildren().add(pause);
        overlayTransition.getChildren().add(fade);
        overlayTransition.setOnFinished(event -> {
            warningTransition = false;
            view.getWindowStage().setWarning(null);
        });
    }

    protected ViewScene getView() {
        return view;
    }

    protected final void setView(ViewScene vs) {
        if (view != null) {
            view.getWindowStage().setWarning(null);
        }

        view = vs;
        view.entireSceneNeedsRepaint();
   }

    protected void warn(String msg) {
        text.setText(msg);

        warningTransition = true;
        overlayTransition.play();
    }

    protected void cancel() {
        if (overlayTransition != null &&
            overlayTransition.getStatus() == Status.RUNNING) {
            overlayTransition.stop();
            warningTransition = false;
        }
        view.getWindowStage().setWarning(null);
    }

    protected boolean inWarningTransition() {
        return warningTransition;
    }

    private Text text = new Text();
    private Rectangle background;

    private void createOverlayGroup() {
        final Font font = new Font(Font.getDefault().getFamily(), FONTSIZE);
        final Rectangle2D screenBounds = new Rectangle2D(0, 0,
                view.getSceneState().getScreenWidth(),
                view.getSceneState().getScreenHeight());

        String TEXT_CSS =
            "-fx-effect: dropshadow(two-pass-box, rgba(0,0,0,0.75), 3, 0.0, 0, 2);";
        text.setStroke(Color.WHITE);
        text.setFill(Color.WHITE);
        text.setFont(font);
        text.setWrappingWidth(RECTW - PAD - PAD);
        text.setStyle(TEXT_CSS);
        text.setTextAlignment(TextAlignment.CENTER);

        background = createBackground(text, screenBounds);

        getChildren().add(background);
        getChildren().add(text);
    }

    private Rectangle createBackground(Text text, Rectangle2D screen) {
        Rectangle rectangle = new Rectangle();
        double textW = text.getLayoutBounds().getWidth();
        double textH = text.getLayoutBounds().getHeight();
        double rectX = (screen.getWidth() - RECTW) / 2.0;
        double rectY = (screen.getHeight() / 2.0);

        rectangle.setWidth(RECTW);
        rectangle.setHeight(RECTH);
        rectangle.setX(rectX);
        rectangle.setY(rectY - RECTH);
        rectangle.setArcWidth(ARC);
        rectangle.setArcHeight(ARC);
        rectangle.setFill(Color.gray(0.0, 0.6));

        text.setX(rectX + ((RECTW - textW) / 2.0));
        text.setY(rectY - (RECTH  / 2.0) + ((textH - text.getBaselineOffset()) / 2.0));

        return rectangle;
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private void doUpdatePeer() {
        NodeHelper.updatePeer(text);
        NodeHelper.updatePeer(background);
    }

    @Override
    protected void updateBounds() {
        super.updateBounds();
    }

    /*
     * Note: This method MUST only be called via its accessor method.
     */
    private void doMarkDirty(DirtyBits dirtyBit) {
        view.synchroniseOverlayWarning();
    }
}
