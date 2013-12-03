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
package com.oracle.javafx.scenebuilder.app.selectionbar;

import com.oracle.javafx.scenebuilder.app.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.images.ImageUtils;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.AbstractFxmlPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.report.ErrorReport;
import com.oracle.javafx.scenebuilder.kit.editor.report.ErrorReportEntry;
import com.oracle.javafx.scenebuilder.kit.editor.selection.ObjectSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.Selection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import java.net.URL;
import java.util.List;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/**
 *
 */
public class SelectionBarController extends AbstractFxmlPanelController {

    @FXML
    private MenuButton pathMenuButton;
    @FXML
    private HBox pathBox;

    private final Image selectionChevronImage;
    private final Image warningBadgeImage;

    public SelectionBarController(EditorController editorController) {
        super(SelectionBarController.class.getResource("SelectionBar.fxml"), editorController); //NOI18N

        // Initialize selection chevron image
        final URL selectionChevronURL = SelectionBarController.class.getResource("selection-chevron.png"); //NOI18N
        assert selectionChevronURL != null;
        selectionChevronImage = new Image(selectionChevronURL.toExternalForm());
        warningBadgeImage = ImageUtils.getWarningBadgeImage();
    }

    /*
     * AbstractPanelController
     */
    @Override
    protected void fxomDocumentDidChange(FXOMDocument oldDocument) {
        if (pathMenuButton != null) {
            updateSelectionBar();
        }
    }

    @Override
    protected void sceneGraphRevisionDidChange() {
        if (pathMenuButton != null) {
            updateSelectionBar();
        }
    }

    @Override
    protected void jobManagerRevisionDidChange() {
        sceneGraphRevisionDidChange();
    }

    @Override
    protected void editorSelectionDidChange() {
        if (pathMenuButton != null) {
            updateSelectionBar();
        }
    }

    /*
     * AbstractFxmlPanelController
     */
    @Override
    protected void controllerDidLoadFxml() {

        // Sanity checks
        assert pathMenuButton != null;
        assert pathBox != null;

        // Update
        updateSelectionBar();
    }

    /*
     * Private
     */
    private void updateSelectionBar() {
        final Selection selection = getEditorController().getSelection();

        pathMenuButton.getItems().clear();
        pathBox.getChildren().clear();

        if (selection.isEmpty()) {
            pathBox.getChildren().add(new Label(I18N.getString("selectionbar.no.selected")));
        } else {
            if (selection.getGroup() instanceof ObjectSelectionGroup) {
                final ObjectSelectionGroup osg = (ObjectSelectionGroup) selection.getGroup();
                assert osg.getItems().isEmpty() == false;

                FXOMObject fxomObject = osg.getItems().iterator().next();
                while (fxomObject != null) {
                    final DesignHierarchyMask mask = new DesignHierarchyMask(fxomObject);
                    final String entryText = makeEntryText(mask);

                    final MenuItem menuItem = new MenuItem();
                    menuItem.setText(entryText);
                    menuItem.setGraphic(new ImageView(mask.getClassNameIcon()));
                    menuItem.setUserData(fxomObject);
                    menuItem.setOnAction(menuItemHandler);
                    pathMenuButton.getItems().add(0, menuItem);

                    final Hyperlink boxItem = new Hyperlink();
                    boxItem.setText(entryText);
                    final Node graphic;
                    final List<ErrorReportEntry> entries = getErrorReportEntries(fxomObject);
                    if (entries != null) {
                        assert !entries.isEmpty();
                        final ImageView classNameImageView
                                = new ImageView(mask.getClassNameIcon());
                        final ImageView warningBadgeImageView
                                = new ImageView(warningBadgeImage);
                        final StackPane iconsStack = new StackPane();
                        iconsStack.getChildren().setAll(classNameImageView, warningBadgeImageView);
                        // Update tooltip with the first entry
                        final Tooltip iconsTooltip = new Tooltip(entries.get(0).toString());

                        // We use a label to set a tooltip over the node icon 
                        // (StackPane does not allow to set tooltips)
                        graphic = new Label();
                        ((Label) graphic).setGraphic(iconsStack);
                        ((Label) graphic).setTooltip(iconsTooltip);
                    } else {
                        graphic = new ImageView(mask.getClassNameIcon());
                    }
                    boxItem.setGraphic(graphic);
                    boxItem.setFocusTraversable(false);
                    boxItem.setUserData(fxomObject);
                    boxItem.setOnAction(hyperlinkHandler);
                    pathBox.getChildren().add(0, boxItem);

                    // The last 2 box item should never show ellipsis
                    if (pathBox.getChildren().size() <= 3) {
                        boxItem.setMinWidth(Region.USE_PREF_SIZE);
                        HBox.setHgrow(boxItem, Priority.ALWAYS);
                    } else {
                        boxItem.setMinWidth(graphic.getBoundsInLocal().getWidth());
                    }

                    fxomObject = mask.getParentFXOMObject();
                    // Add selection chevron if needed
                    if (fxomObject != null) {
                        // We cannot share the image view to avoid 
                        // Children: duplicate children added
                        ImageView img = new ImageView(selectionChevronImage);
                        StackPane sp = new StackPane();
                        sp.getChildren().add(img);
                        sp.setMinWidth(selectionChevronImage.getWidth());
                        pathBox.getChildren().add(0, sp);
                    }
                }

            } else {
                pathBox.getChildren().add(new Label(I18N.getString("selectionbar.not.object")));
            }
        }
    }

    private String makeEntryText(DesignHierarchyMask mask) {
        final StringBuilder result = new StringBuilder();

        result.append(mask.getClassNameInfo());
        final String description = mask.getSingleLineDescription();
        if (description != null) {
            result.append(" : "); //NOI18N
            result.append(description);
        }
        return result.toString();
    }

    private final EventHandler<ActionEvent> menuItemHandler = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent t) {
            assert t.getSource() instanceof MenuItem;
            final MenuItem menuItem = (MenuItem) t.getSource();
            assert menuItem.getUserData() instanceof FXOMObject;
            handleSelect((FXOMObject) menuItem.getUserData());
        }
    };

    private final EventHandler<ActionEvent> hyperlinkHandler = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent t) {
            assert t.getSource() instanceof Hyperlink;
            final Hyperlink hyperlink = (Hyperlink) t.getSource();
            assert hyperlink.getUserData() instanceof FXOMObject;
            handleSelect((FXOMObject) hyperlink.getUserData());
            hyperlink.setVisited(false);
        }
    };

    private void handleSelect(FXOMObject fxomObject) {
        final Selection selection = getEditorController().getSelection();

        assert fxomObject.getFxomDocument() == getEditorController().getFxomDocument();

        selection.select(fxomObject);
    }

    private List<ErrorReportEntry> getErrorReportEntries(FXOMObject fxomObject) {
        assert fxomObject != null;
        final ErrorReport errorReport = getEditorController().getErrorReport();
        return errorReport.query(fxomObject, false);
    }
}
