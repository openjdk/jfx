/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.javafx.scenebuilder.app.preview;

import com.oracle.javafx.scenebuilder.app.DocumentWindowController;
import com.oracle.javafx.scenebuilder.app.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.EditorPlatform;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.AbstractWindowController;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.util.MathUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

/**
 *
 */
public final class PreviewWindowController extends AbstractWindowController {

    private final EditorController editorController;
    private Timer timer = null;
    private final int WIDTH_WHEN_EMPTY = 320;
    private final int HEIGHT_WHEN_EMPTY = 200;
    private CameraType cameraType;
    private boolean autoResize3DContent = true;
    private static final String NID_PREVIEW_ROOT = "previewRoot"; //NOI18N
    private EditorPlatform.Theme editorControllerTheme;
    private ObservableList<File> sceneStyleSheet;
    private File resource;
    
    /**
     * The type of Camera used by the Preview panel.
     */
    public enum CameraType {

        PARALLEL, PERSPECTIVE
    }

        
    public PreviewWindowController(EditorController editorController, Window owner) {
        super(owner);
        this.editorController = editorController;
        this.editorController.fxomDocumentProperty().addListener(
                new ChangeListener<FXOMDocument>() {
                    @Override
                    public void changed(ObservableValue<? extends FXOMDocument> ov,
                            FXOMDocument od, FXOMDocument nd) {
                        assert editorController.getFxomDocument() == nd;
                        if (od != null) {
                            od.sceneGraphRevisionProperty().removeListener(fxomDocumentRevisionListener);
                        }
                        if (nd != null) {
                            nd.sceneGraphRevisionProperty().addListener(fxomDocumentRevisionListener);
                            requestUpdate();
                        }
                    }
                });
        
        if (editorController.getFxomDocument() != null) {
            editorController.getFxomDocument().sceneGraphRevisionProperty().addListener(fxomDocumentRevisionListener);
        }
        
        this.editorControllerTheme = editorController.getTheme();
        this.editorController.themeProperty().addListener(new ChangeListener<EditorPlatform.Theme>() {

            @Override
            public void changed(ObservableValue<? extends EditorPlatform.Theme> ov,
                    EditorPlatform.Theme t, EditorPlatform.Theme t1) {
                if (t1 != null) {
                    editorControllerTheme = t1;
                    requestUpdate();
                }
            }
        });
        
        this.sceneStyleSheet = editorController.getSceneStyleSheets();
        this.editorController.sceneStyleSheetProperty().addListener(new ChangeListener<ObservableList<File>>() {

            @Override
            public void changed(ObservableValue<? extends ObservableList<File>> ov, ObservableList<File> t, ObservableList<File> t1) {
                if (t1 != null) {
                    sceneStyleSheet = t1;
                    requestUpdate();
                }
            }
        });
        
        this.resource = editorController.getResource();
        this.editorController.resourceProperty().addListener(new ChangeListener<File>() {

            @Override
            public void changed(ObservableValue<? extends File> ov, File t, File t1) {
                resource = t1;
                requestUpdate();
            }
        });
        this.editorController.sampleDataEnabledProperty().addListener(new ChangeListener<Boolean>() {

            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                requestUpdate();
            }
        });
    }
    
    /*
     * AbstractWindowController
     */
    @Override
    protected void makeRoot() {
        // Until the timer used in requestUpdate() expires, so that the root of
        // the scene is updated to the real content, we set a placeholder.
        StackPane sp = new StackPane(new Label(I18N.getString("preview.constructing")));
        sp.setPrefSize(WIDTH_WHEN_EMPTY, HEIGHT_WHEN_EMPTY);
        setRoot(sp);

        requestUpdate();
    }

    @Override
    public void onCloseRequest(WindowEvent event) {
//        System.out.println("PreviewWindowController::onCloseRequest called");
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        getStage().close();
    }

    @Override
    protected void controllerDidCreateStage() {
        updateWindowSize();
        updateWindowTitle();
    }

    @Override
    public void closeWindow() {
//        System.out.println("PreviewWindowController::closeWindow called");
        super.closeWindow();
    }
    
    /*
     * Private
     */

    private final ChangeListener<Number> fxomDocumentRevisionListener
            = new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
//                    System.out.println("fxomDocumentRevisionListener called");
                    requestUpdate();
                }
            };

    /**
     * There's a delay before the content of the preview is refreshed. If any
     * further modification is brought to the layout before expiration of this
     * we restart the timer. The idea is to lower the resources used to refresh
     * the preview window content.
     */
    private void requestUpdate() {
//        System.out.println("PreviewWindowController::requestUpdate: Called");

        TimerTask timerTask = new TimerTask() {

            @Override
            public void run() {
                // JavaFX data should only be accessed on the JavaFX thread. 
                // => we must wrap the code into a Runnable object and call the Platform.runLater
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        final FXOMDocument fxomDocument = editorController.getFxomDocument();
                        if (fxomDocument != null) {
                            // We clone the FXOMDocument
                            FXOMDocument clone;
                            ResourceBundle resourceBundle = fxomDocument.getResources();
                            
                            if (resource != null) {
                                try {
                                    resourceBundle = new PropertyResourceBundle(new InputStreamReader(new FileInputStream(resource), Charset.forName("UTF-8"))); //NOI18N
                                } catch (IOException ex) {
                                    resourceBundle = fxomDocument.getResources();
                                }
                            }

                            try {
                                clone = new FXOMDocument(fxomDocument.getFxmlText(),
                                        fxomDocument.getLocation(),
                                        fxomDocument.getClassLoader(),
                                        resourceBundle);
                                clone.setSampleDataEnabled(fxomDocument.isSampleDataEnabled());
                            } catch (IOException ex) {
                                throw new RuntimeException("Bug in PreviewWindowController::requestUpdate", ex); //NOI18N
                            }

                            Object sceneGraphRoot = clone.getSceneGraphRoot();
                            final String themeStyleSheetString =
                                    EditorPlatform.getThemeStylesheetURL(editorControllerTheme).toString();

                            if (sceneGraphRoot instanceof Parent) {
                                ((Parent) sceneGraphRoot).setId(NID_PREVIEW_ROOT);
                                setRoot((Parent) updateAutoResizeTransform((Parent) sceneGraphRoot));
                                assert ((Parent) sceneGraphRoot).getScene() == null;
                                ((Parent) sceneGraphRoot).getStylesheets().removeAll(themeStyleSheetString);
                                ((Parent) sceneGraphRoot).getStylesheets().add(themeStyleSheetString);
                                
                                if (sceneStyleSheet != null) {
                                    for (File f : sceneStyleSheet) {
                                        String urlString = ""; //NOI18N
                                        try {
                                            urlString = f.toURI().toURL().toString();
                                        } catch (MalformedURLException ex) {
                                            throw new RuntimeException("Bug in PreviewWindowController", ex); //NOI18N
                                        }
                                        ((Parent) sceneGraphRoot).getStylesheets().removeAll(urlString);
                                        ((Parent) sceneGraphRoot).getStylesheets().add(urlString);
                                    }
                                }
                                                                
                                // Not proven necessary as per my testing
//                                ((Parent) sceneGraphRoot).applyCss();
                            } else if (sceneGraphRoot instanceof Node) {
                                StackPane sp = new StackPane();
                                sp.getStylesheets().add(themeStyleSheetString);
                                
                                if (sceneStyleSheet != null) {
                                    for (File f : sceneStyleSheet) {
                                        try {
                                            ((Parent) sceneGraphRoot).getStylesheets().add(f.toURI().toURL().toString());
                                        } catch (MalformedURLException ex) {
                                            throw new RuntimeException("Bug in PreviewWindowController", ex); //NOI18N
                                        }
                                    }
                                }
                                
                                sp.setId(NID_PREVIEW_ROOT);
                                // With some 3D assets such as TuxRotation the
                                // rendering is wrong unless applyCSS is called.
                                ((Node) sceneGraphRoot).applyCss();
                                sp.getChildren().add(updateAutoResizeTransform((Node) sceneGraphRoot));
                                setRoot(sp);
                            } else {
                                setCameraType(CameraType.PARALLEL);
                                StackPane sp = new StackPane(new Label(I18N.getString("preview.not.node")));
                                sp.setId(NID_PREVIEW_ROOT);
                                sp.setPrefSize(WIDTH_WHEN_EMPTY, HEIGHT_WHEN_EMPTY);
                                setRoot(sp);
                            }
                        } else {
                            setCameraType(CameraType.PARALLEL);
                            StackPane sp = new StackPane(new Label(I18N.getString("preview.no.document")));
                            sp.setId(NID_PREVIEW_ROOT);
                            sp.setPrefSize(WIDTH_WHEN_EMPTY, HEIGHT_WHEN_EMPTY);
                            setRoot(sp);
                        }

                        getScene().setRoot(getRoot());
                        updateWindowSize();
                        updateWindowTitle();
                    }
                });
            }
        };

        // If there is no opened document while Preview window is built we want
        // it to come up immediately.
        long delay = 0;

        if (editorController.getFxomDocument() != null) {
            delay = 1000;
        }

        if (timer != null) {
            timer.cancel();
        }

        timer = new Timer(true);
        timer.schedule(timerTask, delay); // milliseconds
    }
    
    private boolean userResizedPreviewWindow() {
        boolean res = false;
        double sceneHeight = getScene().getHeight();
        double sceneWidth = getScene().getWidth();
        
        if ( sceneHeight > 0 && sceneWidth > 0) {
            double prefHeight = getRoot().prefHeight(-1);
            double prefWidth = getRoot().prefWidth(-1);
            
            if ((! MathUtils.equals(prefHeight, sceneHeight) && ! MathUtils.equals(sceneHeight, HEIGHT_WHEN_EMPTY))
                    || (! MathUtils.equals(prefWidth, sceneWidth) && ! MathUtils.equals(sceneWidth, WIDTH_WHEN_EMPTY))) {
                res = true;
            }
        }
        
        return res;
    }

    private void updateWindowSize() {
        final FXOMDocument fxomDocument = editorController.getFxomDocument();

        if (fxomDocument != null) {
            // When we change the stylesheet (Modena, Caspian) we need to know
            // if the user has resized the preview window: if yes we keep
            // the user size, else we size the layout to the scene.
            if ( ! userResizedPreviewWindow()) {
                getStage().sizeToScene();
            }
        } else {
            getStage().setWidth(WIDTH_WHEN_EMPTY);
            getStage().setHeight(HEIGHT_WHEN_EMPTY);
        }
    }

    private void updateWindowTitle() {
        final FXOMDocument fxomDocument
                = editorController.getFxomDocument();
        getStage().setTitle(DocumentWindowController.makeTitle(fxomDocument));
    }
    
    public final void setCameraType(PreviewWindowController.CameraType ct) {
        cameraType = ct;
        updateCamera();
    }

    void updateCamera() {
        if (getScene() != null) {
            if (cameraType == CameraType.PERSPECTIVE) {
                // Set Perspective Camera
//                System.out.println("Adding a perspective camera to Preview...");
                getScene().setCamera(new PerspectiveCamera(false));
            } else {
                // Set Parallel Camera
//                System.out.println("Adding a parallel camera to Preview...");
                getScene().setCamera(null); // null defaults to Parallel camera
            }
        }
    }

    /**
     * Returns true if this preview panel automatically resize 3D content.
     *
     * @return true if this preview panel automatically resize 3D content.
     */
    public boolean isAutoResize3DContent() {
        return autoResize3DContent;
    }

    /**
     * Enables or disables autoresizing of 3D content.
     *
     * @param autoResize3DContent true if this preview panel should autoresize
     * 3D content.
     */
    public void setAutoResize3DContent(boolean autoResize3DContent) {
        this.autoResize3DContent = autoResize3DContent;
    }

    // If the given node is 3D stuff we add transforms and set perspective
    // camera to become able to display it.
    Node updateAutoResizeTransform(Node whatever) {
        Node res = whatever;
//        System.out.println("PreviewWindowController::updateAutoResizeTransform: Called");
        assert editorController.getFxomDocument() != null;
        final Bounds rootBounds = res.getLayoutBounds();

        if ((rootBounds.getDepth() > 0) && autoResize3DContent) {
            res.getTransforms().clear();
            // Content is 3D.
            // Zoom to get a 500 size.
            final double targetSize = 500.0;
            final double scaleX = targetSize / rootBounds.getWidth();
            final double scaleY = targetSize / rootBounds.getHeight();
            final double scaleZ = targetSize / rootBounds.getDepth();
            final double scale = Math.min(scaleX, Math.min(scaleY, scaleZ));
            final double tX = -rootBounds.getMinX();
            final double tY = -rootBounds.getMinY();
            final double tZ = -rootBounds.getMinZ();
            res.getTransforms().add(new Scale(scale, scale, scale));
            res.getTransforms().add(new Translate(tX, tY, tZ));
//            System.out.println("updateAutoResizeTransform " + scaleX + " - " + scaleY + " - " + scaleZ + " - " + scale);

            // Set the scene camera to PerspectiveCamera, to see 3D nodes correctly.
            setCameraType(CameraType.PERSPECTIVE);
        } else {
            setCameraType(CameraType.PARALLEL);
        }
        
        return res;
    }
}
