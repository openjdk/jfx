/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates.
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
package com.oracle.javafx.scenebuilder.kit.editor.panel.library;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.EditorPlatform;
import com.oracle.javafx.scenebuilder.kit.editor.drag.source.LibraryDragSource;
import com.oracle.javafx.scenebuilder.kit.editor.images.ImageUtils;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.library.BuiltinLibrary;
import com.oracle.javafx.scenebuilder.kit.library.LibraryItem;
import java.net.URL;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Window;

/**
 * ListCell for the Library panel.
 * Used to dynamically construct items and their graphic, as well as set the cursor.
 * @param <T>
 */
class LibraryListCell extends ListCell<LibraryListItem> {
    private final EditorController editorController;
    
    private final HBox graphic = new HBox();
    private final ImageView iconImageView = new ImageView();
    private final Label classNameLabel = new Label();
    private final Label qualifierLabel = new Label();
    private final Label sectionLabel = new Label();
    private final URL missingIconURL = ImageUtils.getNodeIconURL("MissingIcon.png"); //NOI18N
    private static final String EMPTY_QUALIFIER_ID = " (empty)"; //NOI18N
    
    public LibraryListCell(final EditorController ec) {
        super();
        this.editorController = ec;
        
        graphic.getStyleClass().add("list-cell-graphic"); //NOI18N
        classNameLabel.getStyleClass().add("library-classname-label"); //NOI18N
        qualifierLabel.getStyleClass().add("library-qualifier-label"); //NOI18N
        sectionLabel.getStyleClass().add("library-section-label"); //NOI18N
        
        graphic.getChildren().add(iconImageView);
        graphic.getChildren().add(classNameLabel);
        graphic.getChildren().add(qualifierLabel);
        graphic.getChildren().add(sectionLabel);
        
        HBox.setHgrow(sectionLabel, Priority.ALWAYS);
        sectionLabel.setMaxWidth(Double.MAX_VALUE);
        
        final EventHandler<MouseEvent> mouseEventHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(final MouseEvent e) {
                handleMouseEvent(e);
            }
        };
        // Mouse events
        this.addEventHandler(MouseEvent.ANY, mouseEventHandler);
        
        setOnDragDetected(new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent t) {
//                System.out.println("LibraryListCell - setOnDragDetected.handle");
                final LibraryListItem listItem = LibraryListCell.this.getItem();
                final FXOMDocument fxomDocument = editorController.getFxomDocument();
                
                if ((listItem != null) && (fxomDocument != null)) {
                    final LibraryItem item = LibraryListCell.this.getItem().getLibItem();
                    if (item != null) {
                        final ListView<LibraryListItem> list = LibraryListCell.this.getListView();
                        final Dragboard db = list.startDragAndDrop(TransferMode.COPY);

                        final Window ownerWindow = getScene().getWindow();
                        final LibraryDragSource dragSource 
                                = new LibraryDragSource(item, fxomDocument, ownerWindow);
                        assert editorController.getDragController().getDragSource() == null;
                        assert dragSource.isAcceptable();
                        editorController.getDragController().begin(dragSource);

                        db.setContent(dragSource.makeClipboardContent());
                        db.setDragView(dragSource.makeDragView());
                    }
                }
            }
        });
    }

    @Override
    public void updateItem(LibraryListItem item, boolean empty) {
        super.updateItem(item, empty);
        setText(null);

        if (!empty && item != null) {
            updateLayout(item);
            if (item.getLibItem() != null) {
                // A qualifier needed to discriminate items is kept in the ID:
                // this applies to orientation as well as empty qualifiers.
                // FX8 qualifier is not kept as there's no ambiguity there.
                String id = item.getLibItem().getName();
                if (id.contains(BuiltinLibrary.getFX8Qualifier())) {
                    id = id.substring(0, id.indexOf(BuiltinLibrary.getFX8Qualifier()));
                }
                // If QE were about to test a localized version the ID should
                // remain unchanged.
                if (id.contains(BuiltinLibrary.getEmptyQualifier())) {
                    id = id.replace(BuiltinLibrary.getEmptyQualifier(), EMPTY_QUALIFIER_ID);
                }
                graphic.setId(id); // for QE
            }
            
            setGraphic(graphic);
        } else {
            setGraphic(null);
        }
    }

    private Cursor getOpenHandCursor() {
        // DTL-6477
        if (EditorPlatform.IS_WINDOWS) {
            return ImageUtils.getOpenHandCursor();
        } else {
            return Cursor.OPEN_HAND;
        }
    }

    private Cursor getClosedHandCursor() {
        // DTL-6477
        if (EditorPlatform.IS_WINDOWS) {
            return ImageUtils.getClosedHandCursor();
        } else {
            return Cursor.CLOSED_HAND;
        }
    }
    
    private void handleMouseEvent(MouseEvent me) {
        // Handle cursor
        final Scene scene = getScene();
        
        if (scene == null) {
            return;
        }
        
        // When another window is focused (just like the preview window), 
        // we use default cursor
        if (scene.getWindow() != null && !scene.getWindow().isFocused()) {
            setCursor(Cursor.DEFAULT);
            return;
        }
        
        final LibraryListItem listItem = getItem();
        LibraryItem item = null;
        
        if (listItem != null) {
            item = listItem.getLibItem();
        }
        
        boolean isSection = false;
        if (listItem != null && listItem.getSectionName() != null) {
            isSection = true;
        }

        if (me.getEventType() == MouseEvent.MOUSE_ENTERED) {
            if (isEmpty() || isSection) {
                setCursor(Cursor.DEFAULT);
            } else {
                setCursor(getOpenHandCursor());
            }
        } else if (me.getEventType() == MouseEvent.MOUSE_PRESSED) {
            if (isEmpty() || isSection) {
                setCursor(Cursor.DEFAULT);
            } else {
                setCursor(getClosedHandCursor());
            }
        } else if (me.getEventType() == MouseEvent.MOUSE_RELEASED) {
            if (isEmpty() || isSection) {
                setCursor(Cursor.DEFAULT);
            } else {
                setCursor(getOpenHandCursor());
            }
        } else if (me.getEventType() == MouseEvent.MOUSE_EXITED) {
            setCursor(Cursor.DEFAULT);
        } else if (me.getEventType() == MouseEvent.MOUSE_CLICKED) {
             // On double click ask for addition of the drag able item on Content
            if (me.getClickCount() == 2 && me.getButton() == MouseButton.PRIMARY) {
                if (!isEmpty() && !isSection && item != null) {
                    if (editorController.canPerformInsert(item)) {
                        editorController.performInsert(item);
                    }
                }
            }
        }
    }

    private void updateLayout(LibraryListItem listItem) {
        assert listItem != null;
        
        if (listItem.getLibItem() != null) {
            final LibraryItem item = listItem.getLibItem();
            // The classname shall be space character free (it is an API name).
            // If there is a space character then it means a qualifier comes
            // right after.
            String classname = getClassName(item.getName());
            iconImageView.setManaged(true);
            classNameLabel.setManaged(true);
            qualifierLabel.setManaged(true);
            sectionLabel.setManaged(false);
            iconImageView.setVisible(true);
            classNameLabel.setVisible(true);
            qualifierLabel.setVisible(true);
            sectionLabel.setVisible(false);
            classNameLabel.setText(classname);
            qualifierLabel.setText(getQualifier(item.getName()));
            // getIconURL can return null, this is deliberate.
            URL iconURL = item.getIconURL();
            // Use missing icon 
            if (iconURL == null) {
                iconURL = missingIconURL;
            }
            iconImageView.setImage(new Image(iconURL.toExternalForm()));
        } else if (listItem.getSectionName() != null) {
            iconImageView.setManaged(false);
            classNameLabel.setManaged(false);
            qualifierLabel.setManaged(false);
            sectionLabel.setManaged(true);
            iconImageView.setVisible(false);
            classNameLabel.setVisible(false);
            qualifierLabel.setVisible(false);
            sectionLabel.setVisible(true);
            sectionLabel.setText(listItem.getSectionName());
        }
    }
    
    private String getClassName(String input) {
        if (!input.contains(" ")) { //NOI18N
            return input;
        } else {
            return input.substring(0, input.lastIndexOf(" ")); //NOI18N
        }
    }
    
    private String getQualifier(String input) {
        if (!input.contains(" ")) { //NOI18N
            return ""; //NOI18N
        } else {
            return input.substring(input.indexOf(" "), input.length()); //NOI18N
        }
    }
}
