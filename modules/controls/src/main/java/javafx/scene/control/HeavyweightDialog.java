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

import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Screen;
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
    private final StackPane sceneRoot;
    
    private DialogPane dialogPane;
    


    /**************************************************************************
     * 
     * Constructors
     * 
     **************************************************************************/

    HeavyweightDialog(Dialog<?> dialog) {
        this.dialog = dialog;
        this.stage = new Stage() {
            @Override public void centerOnScreen() {
                double x = getX();
                double y = getY();
                
                // if the user has specified an x/y location, use it
                if (!Double.isNaN(x) && !Double.isNaN(y)) {
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
                
                Window owner = getOwner();
                if (owner != null) {
                    Scene scene = owner.getScene();

                    // scene.getY() seems to represent the y-offset from the top of the titlebar to the
                    // start point of the scene, so it is the titlebar height
                    final double titleBarHeight = scene.getY();

                    // because Stage does not seem to centre itself over its owner, we
                    // do it here.
                    final double dialogWidth = sceneRoot.prefWidth(-1);
                    final double dialogHeight = sceneRoot.prefHeight(-1);

                    if (owner.getX() < 0 || owner.getY() < 0) {
                        // Fix for #165
                        Screen screen = Screen.getPrimary();
                        double maxW = screen.getVisualBounds().getWidth();
                        double maxH = screen.getVisualBounds().getHeight();

                        x = maxW / 2.0 - dialogWidth / 2.0;
                        y = maxH / 2.0 - dialogHeight / 2.0 + titleBarHeight;
                    } else {
                        x = owner.getX() + (scene.getWidth() / 2.0) - (dialogWidth / 2.0);
                        y = owner.getY() +  titleBarHeight + (scene.getHeight() / 2.0) - (dialogHeight / 2.0);
                    }

                    setX(x);
                    setY(y);
                } else {
                    super.centerOnScreen();
                }
            }
        };
        stage.setResizable(false);
        
        stage.setOnCloseRequest(windowEvent -> {
            // We only allow the dialog to be closed abnormally (i.e. via the X button)
            // when there is a cancel button in the dialog, or when there is only
            // one button in the dialog. In all other cases, we disable the ability
            // (as best we can) to close a dialog abnormally.
            boolean denyClose = true;

            // if the close was normal, we don't need to call close ourselves,
            // so just return
            if (dialog.closeWasNormal) return;

            // if we are here, the close was abnormal, so we must call close to
            // clean up, if we don't consume the event to cancel closing...
            DialogPane dialogPane = dialog.getDialogPane();
            if (dialogPane != null) {

                List<ButtonType> buttons = dialogPane.getButtonTypes();
                if (buttons.size() != 1) {
                    // look for cancel button type
                    for (ButtonType button : buttons) {
                        if (button == null) continue;
                        ButtonData type = button.getButtonData();
                        if (type == ButtonData.CANCEL_CLOSE) {
                            denyClose = false;
                            break;
                        }
                    }
                }
            }
            
            // if we are here, we consume the event to prevent closing the dialog
            if (denyClose) {
                windowEvent.consume();
            } else {
                close(false);
            }
        });

        sceneRoot = new StackPane();
        sceneRoot.getStyleClass().setAll("dialog");
        
        scene = new Scene(sceneRoot);
//        scene.setFill(Color.TRANSPARENT);
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
//        root.setCenter(dialogPane);
        sceneRoot.getChildren().setAll(dialogPane);
        
        // TODO There is still more work to be done here:
        // 1) Handling when the dialog pane pref sizes change dynamically (and resizing the stage)
        // 2) Animating the resize (if deemed desirable)
        stage.sizeToScene();
    }

    @Override public void show() {
        dialogPane.heightProperty().addListener(o -> stage.centerOnScreen());
        stage.show();
    }

    @Override public void showAndWait() {
        dialogPane.heightProperty().addListener(o -> stage.centerOnScreen());
        stage.showAndWait();
    }

    boolean isClosing = false;
    @Override public void close(boolean closeWasNormal) {
        if (isClosing) return;
        
        isClosing = true;
        dialog.closeWasNormal = closeWasNormal;
        
        // enabling this code has the effect of firing the Dialog.onHiding and onHidden events twice
        if (dialog.isShowing()) {
            dialog.close();
        }
        
        if (stage.isShowing()) {
            stage.hide();
        }
        isClosing = false;
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
}
