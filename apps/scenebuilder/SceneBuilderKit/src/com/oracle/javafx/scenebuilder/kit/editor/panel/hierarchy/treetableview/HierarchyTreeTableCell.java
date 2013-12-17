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
package com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.treetableview;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.images.ImageUtils;
import com.oracle.javafx.scenebuilder.kit.editor.images.ImageUtilsBase;
import com.oracle.javafx.scenebuilder.kit.editor.job.ModifyFxIdJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.ModifyObjectJob;
import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.HierarchyItem;
import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.AbstractHierarchyPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.AbstractHierarchyPanelController.DisplayOption;
import static com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.AbstractHierarchyPanelController.HIERARCHY_READWRITE_LABEL;
import static com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.AbstractHierarchyPanelController.HIERARCHY_READONLY_LABEL;
import static com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.AbstractHierarchyPanelController.TREE_CELL_GRAPHIC;
import com.oracle.javafx.scenebuilder.kit.editor.util.InlineEditController;
import com.oracle.javafx.scenebuilder.kit.editor.util.InlineEditController.Type;
import com.oracle.javafx.scenebuilder.kit.editor.report.ErrorReport;
import com.oracle.javafx.scenebuilder.kit.editor.report.ErrorReportEntry;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMIntrinsic;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.metadata.Metadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.DesignHierarchyMask;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.net.URL;
import java.util.List;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Paint;
import javafx.util.Callback;

/**
 * TreeTableCells used by the hierarchy TreeTableView.
 *
 * p
 *
 * @param <T>
 * @param <S>
 */
public class HierarchyTreeTableCell<T extends HierarchyItem, S extends HierarchyItem> extends TreeTableCell<HierarchyItem, HierarchyItem> {

    public enum Column {

        CLASS_NAME, DISPLAY_INFO
    }
    private final Column column;
    private final AbstractHierarchyPanelController panelController;

    // Style class used for lookup
    static final String HIERARCHY_TREE_TABLE_CELL = "hierarchy-tree-table-cell";

    private final HBox graphic = new HBox();
    private final Label placeHolderLabel = new Label();
    private final Label classNameInfoLabel = new Label();
    private final Label displayInfoLabel = new Label();
    private final ImageView placeHolderImageView = new ImageView();
    private final ImageView classNameImageView = new ImageView();
    private final ImageView warningBadgeImageView = new ImageView();
    private final ImageView includedFileImageView = new ImageView();
    // Stack used to add badges over the top of the node icon
    private final StackPane iconsStack = new StackPane();
    // We use a label to set a tooltip over the node icon 
    // (StackPane does not allow to set tooltips)
    private final Label iconsLabel = new Label();
    private final Tooltip warningBadgeTooltip = new Tooltip();

    // Listener for the display option used to update the display info label
    final ChangeListener<DisplayOption> displayOptionListener = new ChangeListener<DisplayOption>() {

        @Override
        public void changed(ObservableValue<? extends DisplayOption> ov, DisplayOption t, DisplayOption t1) {
            // Update display info for non empty cells
            if (!isEmpty() && getItem() != null && !getItem().isEmpty()) {
                final String displayInfo = getItem().getDisplayInfo(t1);
                displayInfoLabel.setText(displayInfo);
                displayInfoLabel.setManaged(getItem().hasDisplayInfo(t1));
                displayInfoLabel.setVisible(getItem().hasDisplayInfo(t1));
            }
        }
    };

    public HierarchyTreeTableCell(final AbstractHierarchyPanelController c, final Column column) {
        super();
        this.panelController = c;
        this.column = column;

        iconsStack.getChildren().setAll(
                classNameImageView,
                warningBadgeImageView);
        iconsLabel.setGraphic(iconsStack);
        // RT-31645 : we cannot dynamically update the HBox graphic children 
        // in the cell.updateItem method.
        // We set once the graphic children, then we update the managed property
        // of the children depending on the cell item. 
        switch (column) {
            case CLASS_NAME:
                graphic.getChildren().setAll(
                        includedFileImageView,
                        placeHolderImageView,
                        iconsLabel,
                        placeHolderLabel,
                        classNameInfoLabel);
                break;
            case DISPLAY_INFO:
                graphic.getChildren().setAll(
                        displayInfoLabel);
                break;
            default:
                // Should never occurs
                assert false;
                break;
        }

        // Add style class used when invoking lookupAll
        this.getStyleClass().add(HIERARCHY_TREE_TABLE_CELL);

        // CSS
        graphic.getStyleClass().add(TREE_CELL_GRAPHIC);
        updatePlaceHolder();
        classNameInfoLabel.getStyleClass().add(HIERARCHY_READONLY_LABEL);
        displayInfoLabel.getStyleClass().add(HIERARCHY_READWRITE_LABEL);
        // Layout
        classNameInfoLabel.setMinWidth(Control.USE_PREF_SIZE);
        displayInfoLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(displayInfoLabel, Priority.ALWAYS);

        panelController.displayOptionProperty().addListener(
                new WeakChangeListener<>(displayOptionListener));

        // Key events
        final EventHandler<KeyEvent> keyEventHandler = new EventHandler<KeyEvent>() {
            @Override
            public void handle(final KeyEvent e) {
                filterKeyEvent(e);
            }
        };
        this.addEventFilter(KeyEvent.ANY, keyEventHandler);

        // Mouse events
        final EventHandler<MouseEvent> mouseEventHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(final MouseEvent e) {
                filterMouseEvent(e);
            }
        };
        this.addEventFilter(MouseEvent.ANY, mouseEventHandler);
    }

    public Column getColumn() {
        return column;
    }

    @Override
    public void updateItem(HierarchyItem item, boolean empty) {
        super.updateItem(item, empty);

        // The cell is not empty (TreeItem is not null) 
        // AND the TreeItem value is not null
        if (!empty && item != null) {
            switch (column) {
                case CLASS_NAME:
                    updateClassNameColumn(item);
                    break;
                case DISPLAY_INFO:
                    updateDisplayInfoColumn(item);
                    break;
                default:
                    // Should never occurs
                    assert false;
                    break;
            }
            setGraphic(graphic);
            setText(null);
        } else {
            assert item == null;
            setGraphic(null);
            setText(null);
        }
    }

    public final void updatePlaceHolder() {
        final Paint paint = panelController.getParentRingColor();
        placeHolderLabel.setTextFill(paint);
        final BorderWidths bw = new BorderWidths(1);
        final BorderStroke bs = new BorderStroke(paint, BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY, bw);
        final Border b = new Border(bs);
        placeHolderLabel.setBorder(b);
    }

    private void filterKeyEvent(final KeyEvent ke) {
        // empty
    }

    private void filterMouseEvent(final MouseEvent me) {
        if (me.getEventType() == MouseEvent.MOUSE_PRESSED
                && me.getButton() == MouseButton.PRIMARY) {

            // Mouse pressed on a non empty cell :
            // => we may start inline editing
            if (isEmpty() == false) { // (1)
                if (me.getClickCount() >= 2) {
                    // Start inline editing the display info on double click
                    switch (column) {
                        case DISPLAY_INFO:
                            final HierarchyItem item = getItem();
                            assert item != null; // Because of (1)
                            final DisplayOption option = panelController.getDisplayOption();
                            if (item.hasDisplayInfo(option)) {
                                startEditingDisplayInfo();
                                // Consume the event so the native expand/collapse behavior is not performed
                                me.consume();
                            }
                            break;
                        default:
                            // No inline editing on other columns
                            break;
                    }
                }
            }
        }
    }

    /**
     * *************************************************************************
     * Inline editing
     *
     * We cannot use the FX inline editing because it occurs on selection +
     * simple mouse click
     * *************************************************************************
     */
    public void startEditingDisplayInfo() {
        assert getItem().hasDisplayInfo(panelController.getDisplayOption());
        final InlineEditController inlineEditController
                = panelController.getEditorController().getInlineEditController();
        final TextInputControl editor;
        final Type type;
        final String initialValue;

        // 1) Build the TextInputControl used to inline edit
        //----------------------------------------------------------------------
        // INFO display option may use either a TextField or a TextArea
        if (panelController.getDisplayOption() == DisplayOption.INFO) {
            final String info = getItem().getDescription();
            final Object sceneGraphObject = getItem().getFxomObject().getSceneGraphObject();
            if (sceneGraphObject instanceof TextArea || DesignHierarchyMask.containsLineFeed(info)) {
                type = Type.TEXT_AREA;
            } else {
                type = Type.TEXT_FIELD;
            }
            // displayInfoLabel.getText() may be truncated to a single line description
            // We set the initial value with the full description value
            initialValue = getItem().getDescription();
        } //
        // FXID and NODEID options use a TextField
        else {
            type = Type.TEXT_FIELD;
            initialValue = getText();
        }
        editor = inlineEditController.createTextInputControl(type, this, initialValue);
        // CSS
        final ObservableList<String> styleSheets
                = panelController.getPanelRoot().getStylesheets();
        editor.getStylesheets().addAll(styleSheets);
        editor.getStyleClass().add(InlineEditController.INLINE_EDITOR);

        // 2) Build the COMMIT Callback
        // This callback will be invoked to commit the new value
        // It returns true if the value is unchanged or if the commit succeeded, 
        // false otherwise
        //----------------------------------------------------------------------
        final Callback<String, Boolean> requestCommit = new Callback<String, Boolean>() {

            @Override
            public Boolean call(String newValue) {
                // TODO fix DTL-5881 : Inline editing must handle not valid input value
                // 1) Check the input value is valid
                // 2) If valid, commit the new value and return true
                // 3) Otherwise, return false
                final HierarchyItem item = getItem();
                final FXOMObject fxomObject = item.getFxomObject();
                final DisplayOption option = panelController.getDisplayOption();
                final EditorController editorController = panelController.getEditorController();
                switch (option) {
                    case INFO:
                    case NODEID:
                        if (fxomObject instanceof FXOMInstance) {
                            final FXOMInstance fxomInstance = (FXOMInstance) fxomObject;
                            final PropertyName propertyName = item.getPropertyNameForDisplayInfo(option);
                            assert propertyName != null;
                            final ValuePropertyMetadata vpm
                                    = Metadata.getMetadata().queryValueProperty(fxomInstance, propertyName);
                            final ModifyObjectJob job
                                    = new ModifyObjectJob(fxomInstance, vpm, newValue, editorController);
                            if (job.isExecutable()) {
                                editorController.getJobManager().push(job);
                            }
                        }
                        break;
                    case FXID:
                        final ModifyFxIdJob job
                                = new ModifyFxIdJob(fxomObject, newValue, editorController);
                        if (job.isExecutable()) {
                            editorController.getJobManager().push(job);
                        }
                        break;
                    default:
                        assert false;
                        return false;
                }
                return true;
            }
        };

        inlineEditController.startEditingSession(editor, this, requestCommit);
    }

    private void updateClassNameColumn(HierarchyItem item) {
        assert item != null;
        final FXOMObject fxomObject = item.getFxomObject();

        // Update ImageViews
        final Image placeHolderImage = item.getPlaceHolderImage();
        placeHolderImageView.setImage(placeHolderImage);
        placeHolderImageView.setManaged(placeHolderImage != null);

        final Image classNameImage = item.getClassNameIcon();
        classNameImageView.setImage(classNameImage);
        classNameImageView.setManaged(classNameImage != null);

        // Included file
        if (fxomObject instanceof FXOMIntrinsic
                && ((FXOMIntrinsic) fxomObject).getType() == FXOMIntrinsic.Type.FX_INCLUDE) {
            final URL resource = ImageUtilsBase.getNodeIconURL("Included.png"); //NOI18N
            includedFileImageView.setImage(ImageUtils.getImage(resource));
            includedFileImageView.setManaged(true);
        } else {
            includedFileImageView.setImage(null);
            includedFileImageView.setManaged(false);
        }

        final List<ErrorReportEntry> entries = getErrorReportEntries(item);
        if (entries != null) {
            assert !entries.isEmpty();
            // Update tooltip with the first entry
            warningBadgeTooltip.setText(entries.get(0).toString());
            warningBadgeImageView.setImage(ImageUtils.getWarningBadgeImage());
            warningBadgeImageView.setManaged(true);
            iconsLabel.setTooltip(warningBadgeTooltip);
        } else {
            warningBadgeTooltip.setText(null);
            warningBadgeImageView.setImage(null);
            warningBadgeImageView.setManaged(false);
            iconsLabel.setTooltip(null);
        }

        // Update Labels
        final String placeHolderInfo = item.getPlaceHolderInfo();
        placeHolderLabel.setText(placeHolderInfo);
        placeHolderLabel.setManaged(item.isEmpty());
        placeHolderLabel.setVisible(item.isEmpty());

        final String classNameInfo = item.getClassNameInfo();
        classNameInfoLabel.setText(classNameInfo);
        classNameInfoLabel.setManaged(classNameInfo != null);
        classNameInfoLabel.setVisible(classNameInfo != null);
    }

    private void updateDisplayInfoColumn(HierarchyItem item) {
        assert item != null;
        final DisplayOption option = panelController.getDisplayOption();
        final String displayInfo = item.getDisplayInfo(option);
        displayInfoLabel.setText(displayInfo);
        displayInfoLabel.setManaged(item.hasDisplayInfo(option));
        displayInfoLabel.setVisible(item.hasDisplayInfo(option));
    }

    private List<ErrorReportEntry> getErrorReportEntries(HierarchyItem item) {
        if (item == null || item.isEmpty()) {
            return null;
        }
        final EditorController editorController = panelController.getEditorController();
        final ErrorReport errorReport = editorController.getErrorReport();
        final FXOMObject fxomObject = item.getFxomObject();
        assert fxomObject != null;
        final TreeItem<HierarchyItem> treeItem = getTreeTableRow().getTreeItem();
        return treeItem == null ? null : errorReport.query(fxomObject, !treeItem.isExpanded());
    }
}
