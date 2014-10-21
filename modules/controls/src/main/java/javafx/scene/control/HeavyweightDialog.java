/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

// Not public API (class is package-protected), so no JavaDoc is required.
class HeavyweightDialog extends FXDialog {

    /**************************************************************************
     * 
     * Private fields
     * 
     **************************************************************************/

    private final Dialog<?> dialog;
    private final Stage stage;
    private final Scene scene;
    private final Pane sceneRoot;
    
    private DialogPane dialogPane;

    private double prefX = Double.NaN;
    private double prefY = Double.NaN;
    


    /**************************************************************************
     * 
     * Constructors
     * 
     **************************************************************************/

    HeavyweightDialog(Dialog<?> dialog) {
        this.dialog = dialog;
        this.stage = new Stage() {
            @Override public void centerOnScreen() {
                Window owner = getOwner();
                if (owner != null) {
                    positionStage();
                } else {
                    super.centerOnScreen();
                }
            }
        };
        stage.setResizable(false);
        
        stage.setOnCloseRequest(windowEvent -> {
            if (requestPermissionToClose(dialog)) {
                dialog.close();
            } else {
                // if we are here, we consume the event to prevent closing the dialog
                windowEvent.consume();
            }
        });

        stage.addEventHandler(KeyEvent.KEY_RELEASED, keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ESCAPE) {
                if (requestPermissionToClose(dialog)) {
                    dialog.close();
                    keyEvent.consume();
                }
            }
        });

        sceneRoot = new Pane() {
            @Override protected void layoutChildren() {
                // we only attempt to layout the dialogPane
                if (dialogPane == null) return;

                // TODO There is still more work to be done here:
                // 1) DONE: Handling when the dialog pane pref sizes change dynamically (and resizing the stage)
                // 2) Animating the resize (if deemed desirable)
                dialogPane.autosize();
                stage.sizeToScene();
            }
        };
        sceneRoot.getStyleClass().setAll("dialog");
        
        scene = new Scene(sceneRoot);
        stage.setScene(scene);
    }



    /**************************************************************************
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
    
    @Override public void initOwner(Window window) {
        stage.initOwner(window);
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
        sceneRoot.getChildren().setAll(dialogPane);
    }

    @Override public void show() {
        stage.centerOnScreen();
        stage.show();
    }

    @Override public void showAndWait() {
        stage.centerOnScreen();
        stage.showAndWait();
    }

    @Override public void close() {
        if (stage.isShowing()) {
            stage.hide();
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



    /**************************************************************************
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

        final Window owner = getOwner();
        final Scene scene = owner.getScene();

        // scene.getY() seems to represent the y-offset from the top of the titlebar to the
        // start point of the scene, so it is the titlebar height
        final double titleBarHeight = scene.getY();

        // because Stage does not seem to centre itself over its owner, we
        // do it here.
        final double dialogWidth = sceneRoot.prefWidth(-1);
        final double dialogHeight = sceneRoot.prefHeight(-1);

        x = owner.getX() + (scene.getWidth() / 2.0) - (dialogWidth / 2.0);
        y = owner.getY() +  titleBarHeight + (scene.getHeight() / 2.0) - (dialogHeight / 2.0);

        prefX = x;
        prefY = y;

        setX(x);
        setY(y);
    }
}
