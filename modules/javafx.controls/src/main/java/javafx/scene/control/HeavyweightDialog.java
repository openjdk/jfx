/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
package javafx.scene.control;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

// Not public API (class is package-protected), so no JavaDoc is required.
class HeavyweightDialog extends FXDialog {

    /* ************************************************************************
     *
     * Private fields
     *
     **************************************************************************/

    final Stage stage = new Stage() {
        @Override public void centerOnScreen() {
            Window owner = HeavyweightDialog.this.getOwner();
            if (owner != null) {
                positionStage();
            } else {
                if (getWidth() > 0 && getHeight() > 0) {
                    super.centerOnScreen();
                }
            }
        }
    };

    private Scene scene;

    private final Parent DUMMY_ROOT = new Region();
    private final Dialog<?> dialog;
    private DialogPane dialogPane;

    private double prefX = Double.NaN;
    private double prefY = Double.NaN;



    /* ************************************************************************
     *
     * Constructors
     *
     **************************************************************************/

    HeavyweightDialog(Dialog<?> dialog) {
        this.dialog = dialog;

        stage.setResizable(false);

        stage.setOnCloseRequest(windowEvent -> {
            if (requestPermissionToClose(dialog)) {
                dialog.close();
            } else {
                // if we are here, we consume the event to prevent closing the dialog
                windowEvent.consume();
            }
        });

        stage.addEventHandler(KeyEvent.KEY_PRESSED, keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ESCAPE) {
                if (!keyEvent.isConsumed() && requestPermissionToClose(dialog)) {
                    dialog.close();
                    keyEvent.consume();
                }
            }
        });
    }



    /* ************************************************************************
     *
     * Public API
     *
     **************************************************************************/

    @Override void initStyle(StageStyle style) {
        stage.initStyle(style);
    }

    @Override StageStyle getStyle() {
        return stage.getStyle();
    }

    @Override public void initOwner(Window newOwner) {
        updateStageBindings(stage.getOwner(), newOwner);
        stage.initOwner(newOwner);
    }

    @Override public Window getOwner() {
        return stage.getOwner();
    }

    @Override public void initModality(Modality modality) {
        stage.initModality(modality == null? Modality.APPLICATION_MODAL : modality);
    }

    @Override public Modality getModality() {
        return stage.getModality();
    }

    @Override public void setDialogPane(DialogPane dialogPane) {
        this.dialogPane = dialogPane;

        if (scene == null) {
            scene = new Scene(dialogPane);
            stage.setScene(scene);
        } else {
            scene.setRoot(dialogPane);
        }

        dialogPane.autosize();
        stage.sizeToScene();
    }

    @Override public void show() {
        scene.setRoot(dialogPane);
        stage.centerOnScreen();
        stage.show();
    }

    @Override public void showAndWait() {
        scene.setRoot(dialogPane);
        stage.centerOnScreen();
        stage.showAndWait();
    }

    @Override public void close() {
        if (stage.isShowing()) {
            stage.hide();
        }

        // Refer to RT-40687 for more context
        if (scene != null) {
            scene.setRoot(DUMMY_ROOT);
        }
    }

    @Override public ReadOnlyBooleanProperty showingProperty() {
        return stage.showingProperty();
    }

    @Override public Window getWindow() {
        return stage;
    }

    @Override public Node getRoot() {
        return stage.getScene().getRoot();
    }

    // --- x
    @Override public double getX() {
        return stage.getX();
    }

    @Override public void setX(double x) {
        stage.setX(x);
    }

    @Override public ReadOnlyDoubleProperty xProperty() {
        return stage.xProperty();
    }

    // --- y
    @Override public double getY() {
        return stage.getY();
    }

    @Override public void setY(double y) {
        stage.setY(y);
    }

    @Override public ReadOnlyDoubleProperty yProperty() {
        return stage.yProperty();
    }

    @Override ReadOnlyDoubleProperty heightProperty() {
        return stage.heightProperty();
    }

    @Override void setHeight(double height) {
        stage.setHeight(height);
    }

    @Override double getSceneHeight() {
        return scene == null ? 0 : scene.getHeight();
    }

    @Override ReadOnlyDoubleProperty widthProperty() {
        return stage.widthProperty();
    }

    @Override void setWidth(double width) {
        stage.setWidth(width);
    }

    @Override BooleanProperty resizableProperty() {
        return stage.resizableProperty();
    }

    @Override StringProperty titleProperty() {
        return stage.titleProperty();
    }

    @Override ReadOnlyBooleanProperty focusedProperty() {
        return stage.focusedProperty();
    }

    @Override public void sizeToScene() {
        stage.sizeToScene();
    }



    /* ************************************************************************
     *
     * Private implementation
     *
     **************************************************************************/

    private void positionStage() {
        double x = getX();
        double y = getY();

        // if the user has specified an x/y location, use it
        if (!Double.isNaN(x) && !Double.isNaN(y) &&
             Double.compare(x, prefX) != 0 && Double.compare(y, prefY) != 0) {
            // weird, but if I don't call setX/setY here, the stage
            // isn't where I expect it to be (in instances where a single
            // dialog is shown and closed multiple times). I expect the
            // second showing to be in the place the dialog was when it
            // was closed the first time, but on Windows it jumps to the
            // top-left of the screen.
            setX(x);
            setY(y);
            return;
        }

        // Firstly we need to force CSS and layout to happen, as the dialogPane
        // may not have been shown yet (so it has no dimensions)
        dialogPane.applyCss();
        dialogPane.layout();

        final Window owner = getOwner();
        final Scene ownerScene = owner.getScene();

        // scene.getY() seems to represent the y-offset from the top of the titlebar to the
        // start point of the scene, so it is the titlebar height
        final double titleBarHeight = ownerScene.getY();

        // because Stage does not seem to centre itself over its owner, we
        // do it here.

        // then we can get the dimensions and position the dialog appropriately.
        final double dialogWidth = dialogPane.prefWidth(-1);
        final double dialogHeight = dialogPane.prefHeight(dialogWidth);

//        stage.sizeToScene();

        x = owner.getX() + (ownerScene.getWidth() / 2.0) - (dialogWidth / 2.0);
        y = owner.getY() + titleBarHeight / 2.0 + (ownerScene.getHeight() / 2.0) - (dialogHeight / 2.0);

        prefX = x;
        prefY = y;

        setX(x);
        setY(y);
    }

    // this method ensures the internal dialog stage is bound to the owner window
    // properties as appropriate
    private void updateStageBindings(Window oldOwner, Window newOwner) {
        final Scene dialogScene = stage.getScene();

        if (oldOwner != null && oldOwner instanceof Stage) {
            Stage oldStage = (Stage) oldOwner;
            Bindings.unbindContent(stage.getIcons(), oldStage.getIcons());
            stage.renderScaleXProperty().unbind();
            stage.renderScaleYProperty().unbind();

            Scene oldScene = oldStage.getScene();
            if (scene != null && dialogScene != null) {
                Bindings.unbindContent(dialogScene.getStylesheets(), oldScene.getStylesheets());
            }
        }

        // put the icons and stylesheets of the owner window into the dialog
        if (newOwner instanceof Stage) {
            Stage newStage = (Stage) newOwner;
            Bindings.bindContent(stage.getIcons(), newStage.getIcons());
            stage.renderScaleXProperty().bind(newStage.renderScaleXProperty());
            stage.renderScaleYProperty().bind(newStage.renderScaleYProperty());

            Scene newScene = newStage.getScene();
            if (scene != null && dialogScene != null) {
                Bindings.bindContent(dialogScene.getStylesheets(), newScene.getStylesheets());
            }
        }
    }
}
