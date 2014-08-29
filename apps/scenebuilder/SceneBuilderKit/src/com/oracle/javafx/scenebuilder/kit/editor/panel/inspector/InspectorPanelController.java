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
package com.oracle.javafx.scenebuilder.kit.editor.panel.inspector;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.drag.source.AbstractDragSource;
import com.oracle.javafx.scenebuilder.kit.editor.i18n.I18N;
import com.oracle.javafx.scenebuilder.kit.editor.job.BatchModifySelectionJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.Job;
import com.oracle.javafx.scenebuilder.kit.editor.job.ModifyFxIdJob;
import com.oracle.javafx.scenebuilder.kit.editor.job.togglegroup.ModifySelectionToggleGroupJob;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.AnchorPaneConstraintsEditor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.BooleanEditor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.BoundedDoubleEditor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.ColumnResizePolicyEditor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.CursorEditor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.DividerPositionsEditor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.DoubleEditor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.Editor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.EditorUtils;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.EnumEditor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.EventHandlerEditor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.FxIdEditor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.GenericEditor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.I18nStringEditor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.ImageEditor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.InsetsEditor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.IntegerEditor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.Point3DEditor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.PropertiesEditor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.PropertyEditor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.PropertyEditor.LayoutFormat;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.RotateEditor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.StringEditor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.StyleClassEditor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.StyleEditor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.StylesheetEditor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.TextAlignmentEditor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.editors.ToggleGroupEditor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.popupeditors.BoundsPopupEditor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.popupeditors.EffectPopupEditor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.popupeditors.FontPopupEditor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.popupeditors.KeyCombinationPopupEditor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.popupeditors.PaintPopupEditor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.popupeditors.Rectangle2DPopupEditor;
import com.oracle.javafx.scenebuilder.kit.editor.panel.util.AbstractFxmlPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.selection.GridSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.ObjectSelectionGroup;
import com.oracle.javafx.scenebuilder.kit.editor.selection.Selection;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMFxIdIndex;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMInstance;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMObject;
import com.oracle.javafx.scenebuilder.kit.glossary.Glossary;
import com.oracle.javafx.scenebuilder.kit.metadata.Metadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.ValuePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.BooleanPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.BoundsPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.CursorPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.DoublePropertyMetadata.DoubleKind;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.EnumerationPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.EventHandlerPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.FontPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.ImagePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.InsetsPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.IntegerPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.Point3DPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.Rectangle2DPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.StringPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.TableViewResizePolicyPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.ToggleGroupPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.TreeTableViewResizePolicyPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.effect.EffectPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.keycombination.KeyCombinationPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.list.ListValuePropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.property.value.paint.PaintPropertyMetadata;
import com.oracle.javafx.scenebuilder.kit.metadata.util.InspectorPath;
import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import com.oracle.javafx.scenebuilder.kit.metadata.util.ValuePropertyMetadataClassComparator;
import com.oracle.javafx.scenebuilder.kit.metadata.util.ValuePropertyMetadataNameComparator;
import com.oracle.javafx.scenebuilder.kit.util.CssInternal;
import com.oracle.javafx.scenebuilder.kit.util.Deprecation;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TreeMap;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Accordion;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 *
 *
 */
public class InspectorPanelController extends AbstractFxmlPanelController {

    @FXML
    private TitledPane propertiesTitledPane;
    @FXML
    private ScrollPane propertiesScroll;
    @FXML
    private GridPane propertiesSection;
    @FXML
    private TitledPane layoutTitledPane;
    @FXML
    private ScrollPane layoutScroll;
    @FXML
    private GridPane layoutSection;
    @FXML
    private TitledPane codeTitledPane;
    @FXML
    private ScrollPane codeScroll;
    @FXML
    private GridPane codeSection;
    @FXML
    private TitledPane allTitledPane;
    @FXML
    private ScrollPane allScroll;
    @FXML
    private GridPane allContent;
    @FXML
    private StackPane searchStackPane;
    @FXML
    private GridPane searchContent;
    @FXML
    private Accordion accordion;
    @FXML
    private SplitPane inspectorRoot;

    public enum SectionId {

        PROPERTIES,
        LAYOUT,
        CODE,
        NONE
    }

    public enum ViewMode {

        SECTION, // View properties by section (default)
        PROPERTY_NAME, // Flat view of all properties, ordered by name
        PROPERTY_TYPE  // Flat view of all properties, ordered by type
    }

    public enum ShowMode {

        ALL, // Show all the properties (default)
        EDITED // Show only the properties which have been set in the FXML
    }
    //
    private static final String fxmlFile = "Inspector.fxml"; //NOI18N
    private static final String FXID_SUBSECTION_NAME = "Identity";
    private String searchPattern;
    private SectionId previousExpandedSection;
    private PropertyEditor lastPropertyEditorValueChanged = null;
    private boolean dragOnGoing = false;
    //
    // Editor pools
    private final Stack<Editor> i18nStringEditorPool = new Stack<>();
    private final Stack<Editor> stringEditorPool = new Stack<>();
    private final Stack<Editor> doubleEditorPool = new Stack<>();
    private final Stack<Editor> integerEditorPool = new Stack<>();
    private final Stack<Editor> booleanEditorPool = new Stack<>();
    private final Stack<Editor> enumEditorPool = new Stack<>();
    private final Stack<Editor> effectPopupEditorPool = new Stack<>();
    private final Stack<Editor> fontPopupEditorPool = new Stack<>();
    private final Stack<Editor> genericEditorPool = new Stack<>();
    private final Stack<Editor> insetsEditorPool = new Stack<>();
    private final Stack<Editor> boundedDoubleEditorPool = new Stack<>();
    private final Stack<Editor> rotateEditorPool = new Stack<>();
    private final Stack<Editor> anchorPaneConstraintsEditorPool = new Stack<>();
    private final Stack<Editor> styleEditorPool = new Stack<>();
    private final Stack<Editor> styleClassEditorPool = new Stack<>();
    private final Stack<Editor> stylesheetEditorPool = new Stack<>();
    private final Stack<Editor> fxIdEditorPool = new Stack<>();
    private final Stack<Editor> eventHandlerEditorPool = new Stack<>();
    private final Stack<Editor> cursorEditorPool = new Stack<>();
    private final Stack<Editor> paintPopupEditorPool = new Stack<>();
    private final Stack<Editor> imageEditorPool = new Stack<>();
    private final Stack<Editor> boundsPopupEditorPool = new Stack<>();
    private final Stack<Editor> point3DEditorPool = new Stack<>();
    private final Stack<Editor> dividerPositionsEditorPool = new Stack<>();
    private final Stack<Editor> textAlignmentEditorPool = new Stack<>();
    private final Stack<Editor> keyCombinationPopupEditorPool = new Stack<>();
    private final Stack<Editor> columnResizePolicyEditorPool = new Stack<>();
    private final Stack<Editor> rectangle2DPopupEditorPool = new Stack<>();
    private final Stack<Editor> toggleGroupEditorPool = new Stack<>();
    // ...
    //
    // Subsection title pool
    private final Stack<SubSectionTitle> subSectionTitlePool = new Stack<>();
    //
    // Map of editor pools
    private final HashMap<Class<? extends Editor>, Stack<Editor>> editorPools = new HashMap<>();
    //
    // Editors currently in use
    //   Could be a HashMap<SectionId, PropertyEditor> 
    //   if we want to optimize a bit more the property editors usage,
    //   by re-using them directly in the GridPane, instead of using the pools.
    private final List<Editor> editorsInUse = new ArrayList<>();
    //
    // SubSectionTitles currently in use
    private final List<SubSectionTitle> subSectionTitlesInUse = new ArrayList<>();
    //
    private final SectionId[] sections = {SectionId.PROPERTIES, SectionId.LAYOUT, SectionId.CODE};
    //
    // State variables
    private final ObjectProperty<ViewMode> viewModeProperty = new SimpleObjectProperty<>();
    private final ObjectProperty<ShowMode> showModeProperty = new SimpleObjectProperty<>();
    private final ObjectProperty<SectionId> expandedSectionProperty = new SimpleObjectProperty<>();

    // Inspector state
    private SelectionState selectionState;
    private final EditorController editorController;

    private double searchResultDividerPosition;

    /*
     * Public
     */
    public InspectorPanelController(EditorController editorController) {
        super(InspectorPanelController.class.getResource(fxmlFile), I18N.getBundle(), editorController);
        this.editorController = editorController;
        
        viewModeProperty.setValue(ViewMode.SECTION);
        viewModeProperty.addListener(new ChangeListener<ViewMode>() {
            @Override
            public void changed(ObservableValue<? extends ViewMode> arg0,
                    ViewMode previousMode, ViewMode mode) {
                viewModeChanged(previousMode, mode);
            }
        });

        showModeProperty.setValue(ShowMode.ALL);
        showModeProperty.addListener(new ChangeListener<ShowMode>() {
            @Override
            public void changed(ObservableValue<? extends ShowMode> arg0,
                    ShowMode previousMode, ShowMode mode) {
                showModeChanged();
            }
        });

        expandedSectionProperty.setValue(SectionId.PROPERTIES);
        expandedSectionProperty.addListener(new ChangeListener<SectionId>() {
            @Override
            public void changed(ObservableValue<? extends SectionId> arg0,
                    SectionId previousSectionId, SectionId sectionId) {
                expandedSectionChanged();
            }
        });

        // Editor pools init
        editorPools.put(I18nStringEditor.class, i18nStringEditorPool);
        editorPools.put(StringEditor.class, stringEditorPool);
        editorPools.put(DoubleEditor.class, doubleEditorPool);
        editorPools.put(IntegerEditor.class, integerEditorPool);
        editorPools.put(BooleanEditor.class, booleanEditorPool);
        editorPools.put(EnumEditor.class, enumEditorPool);
        editorPools.put(EffectPopupEditor.class, effectPopupEditorPool);
        editorPools.put(FontPopupEditor.class, fontPopupEditorPool);
        editorPools.put(GenericEditor.class, genericEditorPool);
        editorPools.put(InsetsEditor.class, insetsEditorPool);
        editorPools.put(BoundedDoubleEditor.class, boundedDoubleEditorPool);
        editorPools.put(RotateEditor.class, rotateEditorPool);
        editorPools.put(AnchorPaneConstraintsEditor.class, anchorPaneConstraintsEditorPool);
        editorPools.put(StyleEditor.class, styleEditorPool);
        editorPools.put(StyleClassEditor.class, styleClassEditorPool);
        editorPools.put(StylesheetEditor.class, stylesheetEditorPool);
        editorPools.put(FxIdEditor.class, fxIdEditorPool);
        editorPools.put(EventHandlerEditor.class, eventHandlerEditorPool);
        editorPools.put(CursorEditor.class, cursorEditorPool);
        editorPools.put(PaintPopupEditor.class, paintPopupEditorPool);
        editorPools.put(ImageEditor.class, imageEditorPool);
        editorPools.put(BoundsPopupEditor.class, boundsPopupEditorPool);
        editorPools.put(Point3DEditor.class, point3DEditorPool);
        editorPools.put(DividerPositionsEditor.class, dividerPositionsEditorPool);
        editorPools.put(TextAlignmentEditor.class, textAlignmentEditorPool);
        editorPools.put(KeyCombinationPopupEditor.class, keyCombinationPopupEditorPool);
        editorPools.put(ColumnResizePolicyEditor.class, columnResizePolicyEditorPool);
        editorPools.put(Rectangle2DPopupEditor.class, rectangle2DPopupEditorPool);
        editorPools.put(ToggleGroupEditor.class, toggleGroupEditorPool);

        // ...
    }

    public Accordion getAccordion() {
        return accordion;
    }

    public SectionId getExpandedSectionId() {
        if (!isInspectorLoaded()) {
            return null;
        }
        final TitledPane expandedSection = accordion.getExpandedPane();
        final InspectorPanelController.SectionId result;

        if (expandedSection == null) {
            // all sections are collapsed
            result = InspectorPanelController.SectionId.NONE;
        } else if (expandedSection == propertiesTitledPane) {
            result = InspectorPanelController.SectionId.PROPERTIES;
        } else if (expandedSection == layoutTitledPane) {
            result = InspectorPanelController.SectionId.LAYOUT;
        } else if (expandedSection == codeTitledPane) {
            result = InspectorPanelController.SectionId.CODE;
        } else {
            // may happen if the view mode has been changed
            return null;
        }

        return result;
    }

    public ViewMode getViewMode() {
        return viewModeProperty.getValue();
    }
    
    public void setViewMode(ViewMode mode) {
        assert mode != null;
        viewModeProperty.setValue(mode);
    }
    
    private void viewModeChanged(ViewMode previousMode, ViewMode mode) {
        if (!isInspectorLoaded()) {
            return;
        }
        if (previousMode == ViewMode.SECTION) {
            previousExpandedSection = getExpandedSectionId();
        }
        accordion.getPanes().clear();
        switch (mode) {
            case SECTION:
                accordion.getPanes().addAll(propertiesTitledPane, layoutTitledPane, codeTitledPane);
                if (previousExpandedSection != null) {
                    setExpandedSection(previousExpandedSection);
                }
                break;
            case PROPERTY_NAME:
            case PROPERTY_TYPE:
                accordion.getPanes().add(allTitledPane);
                allTitledPane.setExpanded(true);
                rebuild();
                break;
            default:
                throw new IllegalStateException("Unexpected view mode " + mode); //NOI18N
        }
        updateClassNameInSectionTitles();
    }

    public ShowMode getShowMode() {
        return showModeProperty.getValue();
    }

    public void setShowMode(ShowMode mode) {
        assert mode != null;
        showModeProperty.setValue(mode);
    }
    
    private void showModeChanged() {
        if (!isInspectorLoaded()) {
            return;
        }
        rebuild();
    }

    public SectionId getExpandedSection() {
        return expandedSectionProperty.getValue();
    }

    public void setExpandedSection(SectionId sectionId) {
        assert sectionId != null;
        expandedSectionProperty.setValue(sectionId);
    }
    
    private void expandedSectionChanged() {
        if (!isInspectorLoaded()) {
            return;
        }
        final TitledPane tp;

        switch (getExpandedSection()) {
            case NONE:
                tp = null;
                break;
            case PROPERTIES:
                tp = propertiesTitledPane;
                break;
            case LAYOUT:
                tp = layoutTitledPane;
                break;
            case CODE:
                tp = codeTitledPane;
                break;
            default:
                throw new IllegalStateException("Unexpected section id " + getExpandedSection()); //NOI18N
            }

        accordion.setExpandedPane(tp);
    }

    public boolean isEditedMode() {
        return getShowMode() == ShowMode.EDITED;
    }

    public String getSearchPattern() {
        return searchPattern;
    }

    public void setSearchPattern(String searchPattern) {
        this.searchPattern = searchPattern;
        searchPatternDidChange();
    }

    /*
     * AbstractPanelController
     */
    @Override
    protected void fxomDocumentDidChange(FXOMDocument oldDocument) {
//        System.out.println("FXOM Document changed : " + getEditorController().getFxomDocument());
        if (isInspectorLoaded() && hasFxomDocument()) {
            selectionState.initialize();
            rebuild();
        }
    }

    @Override
    protected void sceneGraphRevisionDidChange() {
//        System.out.println("Scene graph changed.");
        if (!dragOnGoing) {
            updateInspector();
        }
    }

    @Override
    protected void cssRevisionDidChange() {
//        System.out.println("CSS changed.");
        Platform.runLater(() -> {
            if (!dragOnGoing) {
                updateInspector();
            }
        });
    }

    @Override
    protected void jobManagerRevisionDidChange() {
        // FXOMDocument has been modified by a job.
        // getEditorController().getJobManager().getLastJob()
        // is the job responsible of the change.
        // Since sceneGraphRevisionDidChange() will be called in this case, nothing to do here.
    }

    @Override
    protected void editorSelectionDidChange() {
//        System.out.println("Selection changed.");
        // DTL-6570 should be resolved before this assertion is back.
//        assert !editorController.isTextEditingSessionOnGoing();
        if (!dragOnGoing) {
            updateInspector();
        }
    }

    /*
     * AbstractFxmlPanelController
     */
    @Override
    protected void controllerDidLoadFxml() {

        // Sanity checks
        assert propertiesTitledPane != null;
        assert propertiesScroll != null;
        assert propertiesSection != null;
        assert layoutTitledPane != null;
        assert layoutScroll != null;
        assert layoutSection != null;
        assert codeTitledPane != null;
        assert codeScroll != null;
        assert codeSection != null;
        assert allTitledPane != null;
        assert allScroll != null;
        assert allContent != null;
        assert searchStackPane != null;
        assert searchContent != null;
        assert accordion != null;
        assert inspectorRoot != null;

        propertiesTitledPane.expandedProperty().addListener((ChangeListener<Boolean>) (ov, wasExpanded, expanded) -> handleTitledPane(wasExpanded, expanded, SectionId.PROPERTIES));
        layoutTitledPane.expandedProperty().addListener((ChangeListener<Boolean>) (ov, wasExpanded, expanded) -> handleTitledPane(wasExpanded, expanded, SectionId.LAYOUT));
        codeTitledPane.expandedProperty().addListener((ChangeListener<Boolean>) (ov, wasExpanded, expanded) -> handleTitledPane(wasExpanded, expanded, SectionId.CODE));

        // Clean the potential nodes added for design purpose in fxml
        clearSections();

        // Listen the drag property changes
        getEditorController().getDragController().dragSourceProperty().addListener((ChangeListener<AbstractDragSource>) (ov, oldVal, newVal) -> {
            if (newVal != null) {
//                    System.out.println("Drag started !");
                dragOnGoing = true;
            } else {
//                    System.out.println("Drag finished.");
                dragOnGoing = false;
                updateInspector();
            }
        });
        
        // Listen the Scene stylesheets changes
        getEditorController().sceneStyleSheetProperty().addListener((ChangeListener<ObservableList<File>>) (ov, t, t1) -> updateInspector());
        
        selectionState = new SelectionState(editorController);
        viewModeChanged(null, getViewMode());
        expandedSectionChanged();
        accordion.setPrefSize(300, 700);
        buildExpandedSection();
        updateClassNameInSectionTitles();
        searchResultDividerPosition = inspectorRoot.getDividerPositions()[0];
        searchPatternDidChange();
    }

    /*
     * Private
     */
    private void updateInspector() {
        if (isInspectorLoaded() && hasFxomDocument()) {
            SelectionState newSelectionState = new SelectionState(editorController);
            if (isInspectorStateChanged(newSelectionState) || isEditedMode()) {
                selectionState = newSelectionState;
                rebuild();
            } else {
                // we may have a property changed here.
                selectionState = newSelectionState;
                reset();
            }
        }
    }

    private boolean isInspectorStateChanged(SelectionState newSelectionState) {
        // Inspector state change if one of the following is true:
        // - selected classes change
        // - common parent change
        // - resolve state change
        return (!newSelectionState.getSelectedClasses().equals(selectionState.getSelectedClasses())
                || (newSelectionState.getCommonParentClass() != selectionState.getCommonParentClass())
                || (!newSelectionState.getUnresolvedInstances().equals(selectionState.getUnresolvedInstances())));
    }

    private void searchPatternDidChange() {
        if (isInspectorLoaded()) {
            // Collapse/Expand the search result panel
            if (hasSearchPattern()) {
                if (!inspectorRoot.getItems().contains(searchStackPane)) {
                    inspectorRoot.getItems().add(0, searchStackPane);
                    inspectorRoot.setDividerPositions(searchResultDividerPosition);
                }
            } else {
                // Save the divider position for next search
                searchResultDividerPosition = inspectorRoot.getDividerPositions()[0];
                if (inspectorRoot.getItems().contains(searchStackPane)) {
                    inspectorRoot.getItems().remove(searchStackPane);
                }
            }

            buildFlatContent(searchContent);
        }
    }

    private void rebuild() {
//        System.out.println("Inspector rebuild() called !");
        // The inspector structure has changed :
        // - selection changed
        // - parent changed
        // - search pattern changed
        // - SceneGraphObject resolved state changed
        // ==> the current section is to be fully rebuilt
        // TBD: we could optimize this by only refreshing values if 
        //      same element class + same container class + same search pattern.
        clearSections();
        if (getViewMode() == ViewMode.SECTION) {
            buildExpandedSection();
        } else {
            buildFlatContent(allContent);
        }
        updateClassNameInSectionTitles();
        if (hasSearchPattern()) {
            buildFlatContent(searchContent);
        }
    }

    private void reset() {
//        System.out.println("Inspector reset() called !");
        // A property has changed, a reference has changed (e.g. css file). 
        // or a selection of an identical node appears
        // ==> For all the editors currently in use:
        // - reset (state, suggested list, ...)
        // - reset the value
//        System.out.println("Refresh all the editors in use...");

        for (Editor editor : editorsInUse) {

            if (editor instanceof PropertyEditor) {
                if (editor == lastPropertyEditorValueChanged) {
                    // do not reset an editor that just changed its value and initiated the reset
                    lastPropertyEditorValueChanged = null;
                    continue;
                }
                resetPropertyEditor((PropertyEditor) editor);
//                System.out.println("reset " + ((PropertyEditor) editor).getPropertyNameText());
            }
            setEditorValueFromSelection(editor);
        }
    }

    private void buildExpandedSection() {
        buildSection(getExpandedSectionId());
    }

    private void buildSection(SectionId sectionId) {
        if (sectionId == SectionId.NONE) {
            return;
        }
//        System.out.println("\nBuilding section " + sectionId + " - Selection : " + selection.getEntries());
        GridPane gridPane = getSectionContent(sectionId);
        gridPane.getChildren().clear();
        if (handleSelectionMessage(gridPane)) {
            return;
        }

        // Get Metadata
        Set<ValuePropertyMetadata> propMetaAll = getValuePropertyMetadata();

        SortedMap<InspectorPath, ValuePropertyMetadata> propMetaSection = new TreeMap<>(Metadata.getMetadata().INSPECTOR_PATH_COMPARATOR);
        assert propMetaAll != null;
        for (ValuePropertyMetadata valuePropMeta : propMetaAll) {
            InspectorPath inspectorPath = valuePropMeta.getInspectorPath();
            // Check section
            if (!isSameSection(inspectorPath.getSectionTag(), sectionId)) {
                continue;
            }
            if (valuePropMeta.isStaticProperty() && !isStaticPropertyRelevant(valuePropMeta.getName())) {
                continue;
            }
            if (isEditedMode()) {
                if (!isPropertyEdited(valuePropMeta, propMetaAll)) {
                    continue;
                }
            }
            propMetaSection.put(valuePropMeta.getInspectorPath(), valuePropMeta);
        }

        if (propMetaSection.isEmpty()) {
            displayEmptyMessage(gridPane);
            return;
        }

        Iterator<Entry<InspectorPath, ValuePropertyMetadata>> iter = propMetaSection.entrySet().iterator();
        String currentSubSection = ""; //NOI18N
        int lineIndex = 0;
        if (sectionId == SectionId.CODE) {
            // add fx:id here, since it is not a property.
            // It has its own sub section title
            addSubSectionSeparator(gridPane, lineIndex, FXID_SUBSECTION_NAME);
            lineIndex++;
            currentSubSection = FXID_SUBSECTION_NAME;
            lineIndex = addFxIdEditor(gridPane, lineIndex);
        }
        Set<PropertyName> groupProperties = new HashSet<>();
        while (iter.hasNext()) {
            // Loop on properties
            Entry<InspectorPath, ValuePropertyMetadata> entry = iter.next();
            InspectorPath inspectorPath = entry.getKey();
            ValuePropertyMetadata propMeta = entry.getValue();
            String newSubSection = inspectorPath.getSubSectionTag();
//            System.out.println(inspectorPath.getSectionTag() + " - " + newSubSection + " - " + propMeta.getName());
            if (!currentSubSection.equalsIgnoreCase(newSubSection)) {
                addSubSectionSeparator(gridPane, lineIndex, newSubSection);
                lineIndex++;
                currentSubSection = newSubSection;
            }
            if (isGroupedProperty(propMeta.getName())) {
                // Several properties are grouped in a single editor (e.g. AnchorPane constraints)
                if (groupProperties.contains(propMeta.getName())) {
                    continue;
                }
                PropertiesEditor propertiesEditor
                        = getInitializedPropertiesEditor(propMeta.getName(), propMetaSection.values(), groupProperties);
                if (propertiesEditor == null) {
                    continue;
                }
                lineIndex = addInGridPane(gridPane, propertiesEditor, lineIndex);
            } else {
                lineIndex = addInGridPane(gridPane, getInitializedPropertyEditor(propMeta), lineIndex);
            }
        }
    }

    private void addSubSectionSeparator(GridPane gridPane, int lineIndex, String titleStr) {
        Node title = getSubSectionTitle(titleStr);
        gridPane.add(title, 0, lineIndex);
        GridPane.setColumnSpan(title, GridPane.REMAINING);
        RowConstraints rowConstraint = new RowConstraints();
        rowConstraint.setValignment(VPos.CENTER);
        gridPane.getRowConstraints().add(rowConstraint);
    }

    private PropertiesEditor getInitializedPropertiesEditor(PropertyName groupedPropName,
            Collection<ValuePropertyMetadata> propMetas, Set<PropertyName> groupProperties) {
        ValuePropertyMetadata[] propMetaGroup = getGroupedPropertiesMetadata(groupedPropName, propMetas, groupProperties);
        PropertiesEditor propertiesEditor = getPropertiesEditor(propMetaGroup);
        if (propertiesEditor == null) {
            return null;
        }
        for (PropertyEditor propertyEditor : propertiesEditor.getPropertyEditors()) {
            setEditorValueFromSelection(propertyEditor);
            handlePropertyEditorChanges(propertyEditor);
        }
        return propertiesEditor;
    }

    private PropertyEditor getInitializedPropertyEditor(ValuePropertyMetadata propMeta) {
        PropertyEditor propertyEditor = getPropertyEditor(propMeta);

        setEditorValueFromSelection(propertyEditor);
        handlePropertyEditorChanges(propertyEditor);
        return propertyEditor;
    }

    private int addFxIdEditor(GridPane gridPane, int lineIndex) {
        PropertyEditor propertyEditor = makePropertyEditor(FxIdEditor.class, null);
        setFxIdFromSelection(propertyEditor);
        handlePropertyEditorChanges(propertyEditor);
        return addInGridPane(gridPane, propertyEditor, lineIndex);
    }

    private void handlePropertyEditorChanges(PropertyEditor propertyEditor) {
        handleValueChange(propertyEditor);
        handleTransientValueChange(propertyEditor);
        handleEditingChange(propertyEditor);
        handleNavigateRequest(propertyEditor);
    }

    private boolean isGroupedProperty(PropertyName propName) {
        // AnchorPane anchors only for now
        return isAnchorConstraintsProp(propName);
    }

    private boolean isGroupEdited(Collection<ValuePropertyMetadata> propMetaAll) {
        // AnchorPane anchors only for now
        return isAnchorConstraintsEdited(propMetaAll);
    }

    private boolean isPropertyEdited(ValuePropertyMetadata valuePropMeta, Collection<ValuePropertyMetadata> propMetadatas) {
        PropertyName propName = valuePropMeta.getName();
        boolean groupedProperty = isGroupedProperty(propName);
        if (!groupedProperty && !isPropertyEdited(valuePropMeta)) {
            return false;
        }
        if (groupedProperty) {
            // We may have some properties edited in a group, some not.
            // In this case, we want to show all the goup properties.
            if (!isGroupEdited(new HashSet<>(propMetadatas))) {
                return false;
            }
        }
        return true;
    }

    private boolean isAnchorConstraintsProp(PropertyName propName) {
        String[] anchorPropNames = {Editor.topAnchorPropName, Editor.rightAnchorPropName,
            Editor.bottomAnchorPropName, Editor.leftAnchorPropName};
        return Arrays.asList(anchorPropNames).contains(propName.toString());
    }

    private boolean isAnchorConstraintsEdited(Collection<ValuePropertyMetadata> propMetaAll) {
        for (ValuePropertyMetadata valuePropMeta : propMetaAll) {
            if (isAnchorConstraintsProp(valuePropMeta.getName())) {
                if (isPropertyEdited(valuePropMeta)) {
                    return true;
                }
            }
        }
        return false;
    }

    private ValuePropertyMetadata[] getGroupedPropertiesMetadata(PropertyName groupedPropName,
            Collection<ValuePropertyMetadata> propMetas, Set<PropertyName> groupProperties) {
        // For now, the SB metadata does NOT include this grouping information.
        // Since we have for now only AnchorPane constraints properties in this case,
        // this is handled at the inspector level.
        // We may include this in the metadata in the future if we have a sigificant number
        // of properties in this case (i.e. if we plan to implement editors for rotateX/Y/Z,
        // min/max/prefWidth, etc...)

        //
        // AnchorPane anchors only for now
        //
        assert isAnchorConstraintsProp(groupedPropName);
        int anchorsNb = 4;
        ArrayList<ValuePropertyMetadata> propMetaGroup = new ArrayList<>();
        // Create an empty list, to be able to set the entries at the right index.
        for (int ii = 0; ii < anchorsNb; ii++) {
            propMetaGroup.add(null);
        }

        // Loop on properties to find anchors properties
        for (ValuePropertyMetadata propMeta : propMetas) {
            PropertyName propName = propMeta.getName();
            if (!isAnchorConstraintsProp(propName)) {
                continue;
            }
            groupProperties.add(propName);
            switch (propName.toString()) {
                case Editor.topAnchorPropName:
                    propMetaGroup.set(0, propMeta);
                    break;
                case Editor.rightAnchorPropName:
                    propMetaGroup.set(1, propMeta);
                    break;
                case Editor.bottomAnchorPropName:
                    propMetaGroup.set(2, propMeta);
                    break;
                case Editor.leftAnchorPropName:
                    propMetaGroup.set(3, propMeta);
                    break;
                default:
                    assert false;
            }
        }
        return propMetaGroup.toArray(new ValuePropertyMetadata[propMetaGroup.size()]);
    }

    private boolean isSameSection(String sectionStr, SectionId sectionId) {
        return sectionStr.equalsIgnoreCase(sectionId.toString());
    }

    private boolean isStaticPropertyRelevant(PropertyName propName) {
        // Check if the static property class is the common parent of the selection
        if (getCommonParent() == null) {
            return false;
        }
        return getCommonParent() == propName.getResidenceClass();
    }

    private boolean hasSelectedElement() {
        return hasFxomDocument() && (!selectionState.isSelectionEmpty());
    }

    private boolean hasSelectedElementNothingForInspector() {
        return hasFxomDocument() && getSelectedInstances().isEmpty();
    }

    private boolean hasMultipleSelection() {
        return getSelectedInstances().size() > 1;
    }

    private boolean hasUnresolvedInstance() {
        return getUnresolvedInstances().size() > 0;
    }

    private void buildFlatContent(GridPane gridPane) {
//        System.out.println("\nBuilding Flat panel" + " - Selection : " + selection.getEntries());
        gridPane.getChildren().clear();
        gridPane.getRowConstraints().clear();
        if (handleSelectionMessage(gridPane)) {
            return;
        }
        if (isSearch(gridPane) && !hasSearchPattern()) {
            addMessage(gridPane, I18N.getString("inspector.message.searchpattern.empty"));
            return;
        }
        boolean isOrderdByType = getViewMode() == ViewMode.PROPERTY_TYPE;

        // Get Metadata
        Set<ValuePropertyMetadata> propMetadatas = getValuePropertyMetadata();
        if (propMetadatas.isEmpty()) {
            addMessage(gridPane, I18N.getString("inspector.message.no.properties"));
            return;
        }
        List<ValuePropertyMetadata> propMetadataList = Arrays.asList(propMetadatas.toArray(new ValuePropertyMetadata[propMetadatas.size()]));
        if (isOrderdByType) {
            Collections.sort(propMetadataList, new ValuePropertyMetadataClassComparator());
        } else {
            Collections.sort(propMetadataList, new ValuePropertyMetadataNameComparator());
        }

        List<ValuePropertyMetadata> orderedPropMetadatas = new ArrayList<>();
        for (ValuePropertyMetadata valuePropMeta : propMetadataList) {
            if (isSearch(gridPane) && !isSearchPatternMatch(valuePropMeta)) {
                continue;
            }
            if (valuePropMeta.isStaticProperty() && !isStaticPropertyRelevant(valuePropMeta.getName())) {
                continue;
            }
            if (isEditedMode()) {
                if (!isPropertyEdited(valuePropMeta, propMetadataList)) {
                    continue;
                }
            }
            orderedPropMetadatas.add(valuePropMeta);
        }

        if (orderedPropMetadatas.isEmpty()) {
            displayEmptyMessage(gridPane);
            return;
        }

        int lineIndex = 0;
        Set<PropertyName> groupProperties = new HashSet<>();
        for (ValuePropertyMetadata propMeta : orderedPropMetadatas) {
            if (isGroupedProperty(propMeta.getName())) {
                if (groupProperties.contains(propMeta.getName())) {
                    continue;
                }
                // Several properties are grouped in a single editor (e.g. AnchorPane constraints)
                PropertiesEditor propertiesEditor
                        = getInitializedPropertiesEditor(propMeta.getName(), new HashSet<>(orderedPropMetadatas), groupProperties);
                if (propertiesEditor == null) {
                    continue;
                }
                lineIndex = addInGridPane(gridPane, propertiesEditor, lineIndex);
            } else {
                lineIndex = addInGridPane(gridPane, getInitializedPropertyEditor(propMeta), lineIndex);
            }
        }
    }

    private boolean handleSelectionMessage(GridPane gridPane) {
        if (!hasSelectedElement()) {
            addMessage(gridPane, I18N.getString("inspector.message.no.selected"));
            return true;
        }
        if (hasSelectedElementNothingForInspector()) {
            addMessage(gridPane, I18N.getString("inspector.message.no.thingforinspector"));
            return true;
        }
        if (hasUnresolvedInstance()) {
            addMessage(gridPane, I18N.getString("inspector.message.no.resolved"));
            return true;
        }
        return false;
    }

    private void displayEmptyMessage(GridPane gridPane) {
        String messKey;
        if (isSearch(gridPane)) {
            messKey = "label.search.noresults";
        } else if (isEditedMode()) {
            messKey = "inspector.message.no.propertiesedited";
        } else {
            messKey = "inspector.message.no.properties";
        }
        addMessage(gridPane, I18N.getString(messKey));
    }

    private boolean isSearchPatternMatch(ValuePropertyMetadata propMeta) {
        String propSimpleName = propMeta.getName().getName();
        // Check model name
        if (propSimpleName.toLowerCase(Locale.ENGLISH).contains(searchPattern.toLowerCase(Locale.ENGLISH))) {
            return true;
        }

        // Check display name
        return EditorUtils.toDisplayName(propSimpleName).toLowerCase(Locale.ENGLISH).contains(searchPattern.toLowerCase(Locale.ENGLISH));
    }

    private boolean hasSearchPattern() {
        return (searchPattern != null) && !searchPattern.isEmpty();
    }

    private boolean isSearch(GridPane gridPane) {
        return gridPane == searchContent;
    }

    private int addInGridPane(GridPane gridPane, Editor editor, int lineIndex) {
        RowConstraints row1Constraints = new RowConstraints();
        LayoutFormat editorLayout;
        HBox propNameNode;
        String propNameText;
        if (editor instanceof PropertyEditor) {
            propNameNode = ((PropertyEditor) editor).getPropNameNode();
            propNameText = ((PropertyEditor) editor).getPropertyNameText();
            editorLayout = ((PropertyEditor) editor).getLayoutFormat();
        } else {
            // PropertiesEditor
            propNameNode = ((PropertiesEditor) editor).getNameNode();
            propNameText = ((PropertiesEditor) editor).getPropertyNameText();
            if (getViewMode() == ViewMode.SECTION) {
                editorLayout = LayoutFormat.SIMPLE_LINE_NO_NAME;
            } else {
                editorLayout = LayoutFormat.DOUBLE_LINE;
            }
        }
        propNameNode.setFocusTraversable(false);
        MenuButton menu = editor.getMenu();
        // For SQE tests
        menu.setId(propNameText + " Menu"); //NOI18N
        Node valueEditor = editor.getValueEditor();
        // For SQE tests
        valueEditor.setId(propNameText + " Value"); //NOI18N

        if (editorLayout == LayoutFormat.DOUBLE_LINE) {
            // We have to wrap the property name and the value editor in a VBox
            row1Constraints.setValignment(VPos.TOP);
            gridPane.getRowConstraints().add(row1Constraints);
            VBox editorBox = new VBox();
            editorBox.getChildren().addAll(propNameNode, valueEditor);
            propNameNode.setAlignment(Pos.CENTER_LEFT);
            GridPane.setColumnSpan(editorBox, 2);
            gridPane.add(editorBox, 0, lineIndex);
        } else {
            // One row
            gridPane.getRowConstraints().add(lineIndex, row1Constraints);
            if (editorLayout != LayoutFormat.SIMPLE_LINE_NO_NAME) {
                gridPane.add(propNameNode, 0, lineIndex);
                if (editorLayout == LayoutFormat.SIMPLE_LINE_CENTERED) {
                    // Property name, valued editor and cog menu are aligned, centered.
                    propNameNode.setAlignment(Pos.CENTER_LEFT);
                } else if (editorLayout == LayoutFormat.SIMPLE_LINE_TOP) {
                    // Property name, valued editor and cog menu are aligned on top.
                    propNameNode.setAlignment(Pos.TOP_LEFT);
                    row1Constraints.setValignment(VPos.TOP);
                } else if (editorLayout == LayoutFormat.SIMPLE_LINE_BOTTOM) {
                    // Property name, valued editor and cog menu are aligned on the bottom.
                    propNameNode.setAlignment(Pos.BOTTOM_LEFT);
                    row1Constraints.setValignment(VPos.BOTTOM);
                }
                GridPane.setColumnSpan(propNameNode, 1);
                GridPane.setColumnSpan(valueEditor, 1);
                gridPane.add(valueEditor, 1, lineIndex);
            } else {
                // LayoutFormat.SIMPLE_LINE_NO_NAME
                row1Constraints.setValignment(VPos.CENTER);
                GridPane.setColumnSpan(valueEditor, 2);
                gridPane.add(valueEditor, 0, lineIndex);
            }
        }

        // Add cog menu
        gridPane.add(menu, 2, lineIndex);

        lineIndex++;
        return lineIndex;
    }

    private void handleValueChange(PropertyEditor propertyEditor) {
        // Handle the value change
        propertyEditor.addValueListener((ov, oldValue, newValue) -> {
//                System.out.println("Value change : " + newValue);
            if (!propertyEditor.isUpdateFromModel()) {
                lastPropertyEditorValueChanged = propertyEditor;
                updateValueInModel(propertyEditor, oldValue, newValue);
            }
            if (propertyEditor.isRuledByCss()) {
                editorController.getMessageLog().logWarningMessage(
                        "inspector.css.overridden", propertyEditor.getPropertyNameText());
            }
        });
    }

    private void handleTransientValueChange(PropertyEditor propertyEditor) {
        // Handle the transient value change (no job here, only the scene graph is updated)
        propertyEditor.addTransientValueListener((ov, oldValue, newValue) -> {
//                System.out.println("Transient value change : " + newValue);
            lastPropertyEditorValueChanged = propertyEditor;
            for (FXOMInstance fxomInstance : getSelectedInstances()) {
                propertyEditor.getPropertyMeta().setValueInSceneGraphObject(fxomInstance, newValue);
            }
        });
    }

    private void updateValueInModel(PropertyEditor propertyEditor, Object oldValue, Object newValue) {
        if (propertyEditor.isUpdateFromModel()) {
            return;
        }
//        System.out.println("Property " + propertyEditor.getPropertyName() + ": Value changed from \"" + oldValue + "\" to \"" + newValue + "\"");
        if (propertyEditor instanceof FxIdEditor) {
            assert (newValue instanceof String) || (newValue == null);
            setSelectedFXOMInstanceFxId(getSelectedInstance(), (String) newValue);
        } else if (propertyEditor instanceof ToggleGroupEditor) {
            assert (newValue instanceof String) || (newValue == null);
            setSelectionToggleGroup((String) newValue);
        } else {
            setSelectedFXOMInstances(propertyEditor.getPropertyMeta(), newValue);
        }
    }

    private void handleEditingChange(PropertyEditor propertyEditor) {
        // Handle the editing change
        propertyEditor.addEditingListener((ov, oldValue, newValue) -> {
            if (newValue) {
                // Editing session starting
//                    System.out.println("textEditingSessionDidBegin() called.");
                editorController.textEditingSessionDidBegin(p -> {
                    // requestSessionEnd
                    if (propertyEditor.getCommitListener() != null) {
                        propertyEditor.getCommitListener().handle(null);
                    }
                    boolean hasError = propertyEditor.isInvalidValue();
                    if (!hasError) {
//                                System.out.println("textEditingSessionDidEnd() called (from callback).");
                        if (editorController.isTextEditingSessionOnGoing()) {
                            editorController.textEditingSessionDidEnd();
                        }
                    }
//                            System.out.println("textEditingSessionDidBegin callback returns : " + !hasError);
                    return !hasError;
                });
            } else {
                // Editing session completed
                if (editorController.isTextEditingSessionOnGoing()) {
//                        System.out.println("textEditingSessionDidEnd() called.");
                    editorController.textEditingSessionDidEnd();
                    if (propertyEditor.getCommitListener() != null) {
                        propertyEditor.getCommitListener().handle(null);
                    }
                }
            }
        });

    }

    private void handleNavigateRequest(PropertyEditor propertyEditor) {
        // Handle a navigate request from an editor
        propertyEditor.addNavigateListener((ov, oldStr, newStr) -> {
            if (newStr != null) {
                setFocusToEditor(new PropertyName(newStr));
            }
        });
    }

    private void setSelectedFXOMInstances(ValuePropertyMetadata propMeta, Object value) {
        final BatchModifySelectionJob job = new BatchModifySelectionJob(propMeta, value, getEditorController());
//        System.out.println(job.getDescription());
        pushJob(job);
    }

    private void setSelectedFXOMInstanceFxId(FXOMObject fxomObject, String fxId) {
        final ModifyFxIdJob job = new ModifyFxIdJob(fxomObject, fxId, getEditorController());
        pushJob(job);
    }

    private void setSelectionToggleGroup(String tgId) {
        final ModifySelectionToggleGroupJob job = new ModifySelectionToggleGroupJob(tgId, getEditorController());
        pushJob(job);
    }

    private void pushJob(Job job) {
        if (job.isExecutable()) {
            getEditorController().getJobManager().push(job);
        } else {
            System.out.println("Modify job not executable (because no value change?)");
        }
    }

    // Check if a property is edited
    private boolean isPropertyEdited(ValuePropertyMetadata propMeta) {
        for (FXOMInstance instance : getSelectedInstances()) {
            if (!propMeta.isReadWrite()) {
                continue;
            }
            Object value = propMeta.getValueObject(instance);
            Object defaultValue = propMeta.getDefaultValueObject();
            if (!EditorUtils.areEqual(value, defaultValue)) {
                return true;
            }
        }
        return false;
    }

    // Set the editor value from selection
    private void setEditorValueFromSelection(Editor editor) {
        if (editor instanceof FxIdEditor) {
            setFxIdFromSelection(editor);
        } else if (isPropertyEditor(editor)) {
            setEditorValueFromSelection((PropertyEditor) editor);
        } else if (isPropertiesEditor(editor)) {
            for (PropertyEditor propertyEditor : ((PropertiesEditor) editor).getPropertyEditors()) {
                setEditorValueFromSelection(propertyEditor);
            }
        }
    }

    // Set the fx:id from selection
    private void setFxIdFromSelection(Editor editor) {
        assert editor instanceof FxIdEditor;
        FxIdEditor fxIdEditor = (FxIdEditor) editor;
        if (hasMultipleSelection()) {
            // multi-selection ==> indeterminate
            fxIdEditor.setIndeterminate(true);
            fxIdEditor.setDisable(true);
        } else {
            String instanceFxId = getSelectedInstance().getFxId();
            fxIdEditor.setDisable(false);
            fxIdEditor.setUpdateFromModel(true);
            fxIdEditor.reset(getSuggestedFxIds(getControllerClass()), getEditorController());
            fxIdEditor.setValue(instanceFxId);
            fxIdEditor.setUpdateFromModel(false);
        }
    }

    // Set the editor value from selection
    private void setEditorValueFromSelection(PropertyEditor propertyEditor) {

        // Determine the property value
        Object val = null;
        boolean isIndeterminate = false;
        boolean isReadWrite = true;
        boolean isRuledByCss = false;
        CssInternal.CssPropAuthorInfo cssInfo = null;
        PropertyName propName = propertyEditor.getPropertyName();

        // General case
        boolean first = true;
        for (FXOMInstance instance : getSelectedInstances()) {
            ValuePropertyMetadata propMeta = Metadata.getMetadata().queryValueProperty(instance, propName);
            assert propMeta != null;
            Object newVal = propMeta.getValueObject(instance);
//            System.out.println(propName + " value : " + newVal);
            if (!propMeta.isReadWrite()) {
                isReadWrite = false;
            }
            if (first) {
                val = newVal;
                first = false;
            } else if (!EditorUtils.areEqual(newVal, val)) {
                isIndeterminate = true;
            }

            cssInfo = CssInternal.getCssInfo(instance.getSceneGraphObject(), propMeta);
            if (cssInfo != null) {
                isRuledByCss = true;
            }
        }

        if (isRuledByCss) {
            propertyEditor.setRuledByCss(true);
            propertyEditor.setCssInfo(cssInfo);
        } else {
            propertyEditor.setRuledByCss(false);
            propertyEditor.setCssInfo(null);
        }
        if (isIndeterminate) {
            propertyEditor.setUpdateFromModel(true);
            propertyEditor.setIndeterminate(true);
            propertyEditor.setUpdateFromModel(false);
        } else {
            propertyEditor.setUpdateFromModel(true);
            propertyEditor.setValue(val);
            propertyEditor.setUpdateFromModel(false);
        }

        if (!(propertyEditor instanceof GenericEditor)) {
            if (!isReadWrite) {
                propertyEditor.setDisable(true);
            } else {
                propertyEditor.setDisable(false);
            }
        }
    }

    private PropertyEditor getPropertyEditor(ValuePropertyMetadata propMeta) {
        PropertyEditor propertyEditor;

        if (propMeta instanceof StringPropertyMetadata) {
            switch (propMeta.getName().getName()) {
                case "style": //NOI18N
                    propertyEditor = makePropertyEditor(StyleEditor.class, propMeta);
                    break;
                case "id": //NOI18N
                    propertyEditor = makePropertyEditor(StringEditor.class, propMeta);
                    break;
                default:
                    propertyEditor = makePropertyEditor(I18nStringEditor.class, propMeta);
                    break;
            }
        } else if (propMeta instanceof ListValuePropertyMetadata) {
            switch (propMeta.getName().getName()) {
                case "styleClass": //NOI18N
                    propertyEditor = makePropertyEditor(StyleClassEditor.class, propMeta);
                    break;
                case "stylesheets": //NOI18N
                    propertyEditor = makePropertyEditor(StylesheetEditor.class, propMeta);
                    break;
                case "dividerPositions": //NOI18N
                    propertyEditor = makePropertyEditor(DividerPositionsEditor.class, propMeta);
                    break;
                default:
                    // Generic editor
                    propertyEditor = makePropertyEditor(GenericEditor.class, propMeta);
                    break;
            }
        } else if (propMeta instanceof DoublePropertyMetadata) {
            // Double editors
            DoublePropertyMetadata doublePropMeta = (DoublePropertyMetadata) propMeta;
            DoubleKind kind = doublePropMeta.getKind();
            if ((kind == DoubleKind.OPACITY) || (kind == DoubleKind.PROGRESS) || isBoundedByProperties(propMeta)) {
                propertyEditor = makePropertyEditor(BoundedDoubleEditor.class, propMeta);
            } else if ((kind == DoubleKind.COORDINATE)
                    || (kind == DoubleKind.USE_COMPUTED_SIZE) || (kind == DoubleKind.USE_PREF_SIZE)
                    || (kind == DoubleKind.NULLABLE_COORDINATE)) {
                // We may have constants to add
                propertyEditor = makePropertyEditor(DoubleEditor.class, propMeta);
            } else if (kind == DoubleKind.ANGLE) {
                propertyEditor = makePropertyEditor(RotateEditor.class, propMeta);
            } else {
                // other kind to be added when editors available...
                // Use simple double editor for now
                propertyEditor = makePropertyEditor(DoubleEditor.class, propMeta);
            }
        } else if (propMeta instanceof IntegerPropertyMetadata) {
            // Integer editor
            propertyEditor = makePropertyEditor(IntegerEditor.class, propMeta);
        } else if (propMeta instanceof BooleanPropertyMetadata) {
            // Boolean editor
            propertyEditor = makePropertyEditor(BooleanEditor.class, propMeta);
        } else if (propMeta instanceof EnumerationPropertyMetadata) {
            switch (propMeta.getName().getName()) {
                case "textAlignment": //NOI18N
                    propertyEditor = makePropertyEditor(TextAlignmentEditor.class, propMeta);
                    break;
                default:
                    // Enum editor
                    propertyEditor = makePropertyEditor(EnumEditor.class, propMeta);
                    break;
            }
        } else if (propMeta instanceof InsetsPropertyMetadata) {
            // Insets editor
            propertyEditor = makePropertyEditor(InsetsEditor.class, propMeta);
        } else if (propMeta instanceof CursorPropertyMetadata) {
            // Cursor editor
            propertyEditor = makePropertyEditor(CursorEditor.class, propMeta);
        } else if (propMeta instanceof EventHandlerPropertyMetadata) {
            // EventHandler editor
            propertyEditor = makePropertyEditor(EventHandlerEditor.class, propMeta);
        } else if (propMeta instanceof EffectPropertyMetadata) {
            // Effect editor
            propertyEditor = makePropertyEditor(EffectPopupEditor.class, propMeta);
        } else if (propMeta instanceof FontPropertyMetadata) {
            // Font editor
            propertyEditor = makePropertyEditor(FontPopupEditor.class, propMeta);
        } else if (propMeta instanceof PaintPropertyMetadata) {
            // Paint editor
            propertyEditor = makePropertyEditor(PaintPopupEditor.class, propMeta);
        } else if (propMeta instanceof ImagePropertyMetadata) {
            // Image editor
            propertyEditor = makePropertyEditor(ImageEditor.class, propMeta);
        } else if (propMeta instanceof BoundsPropertyMetadata) {
            // Bounds editor
            propertyEditor = makePropertyEditor(BoundsPopupEditor.class, propMeta);
        } else if (propMeta instanceof Point3DPropertyMetadata) {
            // Point3D editor
            propertyEditor = makePropertyEditor(Point3DEditor.class, propMeta);
        } else if (propMeta instanceof KeyCombinationPropertyMetadata) {
            // KeyCombination editor
            propertyEditor = makePropertyEditor(KeyCombinationPopupEditor.class, propMeta);
        } else if ((propMeta instanceof TableViewResizePolicyPropertyMetadata)
                || (propMeta instanceof TreeTableViewResizePolicyPropertyMetadata)) {
            // ColumnResizePolicy editor
            propertyEditor = makePropertyEditor(ColumnResizePolicyEditor.class, propMeta);
        } else if (propMeta instanceof Rectangle2DPropertyMetadata) {
            // Rectangle2D editor
            propertyEditor = makePropertyEditor(Rectangle2DPopupEditor.class, propMeta);
        } else if (propMeta instanceof ToggleGroupPropertyMetadata) {
            // ToggleGroup editor
            propertyEditor = makePropertyEditor(ToggleGroupEditor.class, propMeta);
        } else {
            // Generic editor
            propertyEditor = makePropertyEditor(GenericEditor.class, propMeta);
        }

        // Set all the "Code" properties a double line layout
        if (isSameSection(propMeta.getInspectorPath().getSectionTag(), SectionId.CODE)) {
            propertyEditor.setLayoutFormat(LayoutFormat.DOUBLE_LINE);
        }
        return propertyEditor;
    }

    private PropertiesEditor getPropertiesEditor(ValuePropertyMetadata[] propMetas) {
        // AnchorPane only for now
        for (ValuePropertyMetadata propMeta : propMetas) {
            if (propMeta == null) {
                // may happen if search
                return null;
            }
            assert isAnchorConstraintsProp(propMeta.getName());
        }
        return makePropertiesEditor(AnchorPaneConstraintsEditor.class, propMetas);
    }

    private Map<String, Object> getConstants(DoublePropertyMetadata doublePropMeta) {
        Map<String, Object> constants = new TreeMap<>();
        String propNameStr = doublePropMeta.getName().getName();
        DoubleKind kind = doublePropMeta.getKind();
        if (propNameStr.contains("maxWidth") || propNameStr.contains("maxHeight")) { //NOI18N
            constants.put("MAX_VALUE", Double.MAX_VALUE); //NOI18N
        }
        if (kind == DoubleKind.USE_COMPUTED_SIZE) {
            constants.put(DoubleKind.USE_COMPUTED_SIZE.toString(), Region.USE_COMPUTED_SIZE);
        } else if (kind == DoubleKind.USE_PREF_SIZE) {
            constants.put(DoubleKind.USE_COMPUTED_SIZE.toString(), Region.USE_COMPUTED_SIZE);
            constants.put(DoubleKind.USE_PREF_SIZE.toString(), Region.USE_PREF_SIZE);
        } else if (kind == DoubleKind.NULLABLE_COORDINATE) {
            constants.put("NULL", null); //NOI18N
        } else if (kind == DoubleKind.PROGRESS) {
            constants.put("INDETERMINATE", ProgressIndicator.INDETERMINATE_PROGRESS);
        }
        return constants;
    }

    private Map<String, Object> getConstants(IntegerPropertyMetadata integerPropMeta) {
        Map<String, Object> constants = new TreeMap<>();
        String propNameStr = integerPropMeta.getName().getName();
        if (propNameStr.contains("columnSpan") || propNameStr.contains("rowSpan")) { //NOI18N
            constants.put("REMAINING", GridPane.REMAINING); //NOI18N
        } else if (propNameStr.contains("prefColumnCount")) {
            if (getSelectedClasses().size() == 1) {
                if (getSelectedClass() == TextField.class || getSelectedClass() == PasswordField.class) {
                    constants.put("DEFAULT_PREF_COLUMN_COUNT", TextField.DEFAULT_PREF_COLUMN_COUNT); //NOI18N
                } else if (getSelectedClass() == TextArea.class) {
                    constants.put("DEFAULT_PREF_COLUMN_COUNT", TextArea.DEFAULT_PREF_COLUMN_COUNT); //NOI18N
                }
            }
        } else if (propNameStr.contains("prefRowCount")) {
            assert getSelectedClass() == TextArea.class;
            constants.put("DEFAULT_PREF_ROW_COUNT", TextArea.DEFAULT_PREF_ROW_COUNT); //NOI18N
        }
        return constants;
    }

    private int getMax(IntegerPropertyMetadata integerPropMeta) {
        String propNameStr = integerPropMeta.getName().getName();
        if (propNameStr.contains("columnIndex") || propNameStr.contains("columnSpan")) { //NOI18N
            GridPane gridPane = getGridPane(propNameStr);
            if (gridPane == null) {
                // multi-selection from different GridPanes: not supported for now
                return getMin(integerPropMeta);
            }
            int nbColumns = Deprecation.getGridPaneColumnCount(gridPane);
            if (propNameStr.contains("columnIndex")) {//NOI18N
                // index start to 0
                return nbColumns - 1;
            }
            if (propNameStr.contains("columnSpan")) {//NOI18N
                int maxIndex = getSpanPropertyMaxIndex(propNameStr);
                return nbColumns - maxIndex;
            }
        }
        if (propNameStr.contains("rowIndex") || propNameStr.contains("rowSpan")) { //NOI18N
            GridPane gridPane = getGridPane(propNameStr);
            if (gridPane == null) {
                // multi-selection from different GridPanes: not supported for now
                return getMin(integerPropMeta);
            }
            int nbRow = Deprecation.getGridPaneRowCount(gridPane);
            if (propNameStr.contains("rowIndex")) {//NOI18N
                // index start to 0
                return nbRow - 1;
            }
            if (propNameStr.contains("rowSpan")) {//NOI18N
                int maxIndex = getSpanPropertyMaxIndex(propNameStr);
                return nbRow - maxIndex;
            }
        }
        return Integer.MAX_VALUE;
    }

    private int getMin(IntegerPropertyMetadata integerPropMeta) {
        String propNameStr = integerPropMeta.getName().getName();
        if (propNameStr.contains("columnSpan") || propNameStr.contains("rowSpan")) { //NOI18N
            return 1;
        }
        return 0;
    }
    
    private boolean isMultiLinesSupported(Set<Class<?>> selectedClasses, ValuePropertyMetadata propMeta) {
        String propertyNameStr = propMeta.getName().getName();
        if (selectedClasses.contains(TextField.class) || selectedClasses.contains(PasswordField.class)) {
            if (propertyNameStr.equalsIgnoreCase("text")) {
                return false;
            }
        }
        if (propertyNameStr.equalsIgnoreCase("promptText")) {
            return false;
        }

        if (propertyNameStr.equalsIgnoreCase("ellipsisString")) {
            return false;
        }
        return true;
    }

    private int getSpanPropertyMaxIndex(String propNameStr) {
        assert propNameStr.contains("columnSpan") || propNameStr.contains("rowSpan");
        int maxIndex = 0;
        for (FXOMInstance instance : getSelectedInstances()) {
            assert instance.getSceneGraphObject() instanceof Node;
            Integer index;
            Node node = (Node) instance.getSceneGraphObject();
            if (propNameStr.contains("columnSpan")) {//NOI18N
                index = GridPane.getColumnIndex(node);
            } else {
                index = GridPane.getRowIndex(node);
            }
            if (index == null) {
                index = 0;
            }
            if (index > maxIndex) {
                maxIndex = index;
            }
        }
        return maxIndex;
    }
    
    private GridPane getGridPane(String propNameStr) {
        assert propNameStr.contains("columnIndex") || propNameStr.contains("columnSpan") //NOI18N
                || propNameStr.contains("rowIndex") || propNameStr.contains("rowSpan");//NOI18N
            FXOMObject commonParent = selectionState.getCommonParentObject();
            if (commonParent == null) {
                return null;
            }
            Object parentObj = commonParent.getSceneGraphObject();
            assert parentObj instanceof GridPane;
            return (GridPane) parentObj;
    }

    private boolean isInspectorLoaded() {
        return accordion != null;
    }

    private boolean hasFxomDocument() {
        return getEditorController().getFxomDocument() != null;
    }

    private void addMessage(GridPane gridPane, String mess) {
        Label label = new Label(mess);
        label.getStyleClass().add("inspector-message");
        GridPane.setHalignment(label, HPos.LEFT);
        gridPane.add(label, 0, 0, 3, 1);
    }

    private Set<ValuePropertyMetadata> getValuePropertyMetadata() {
        return Metadata.getMetadata().queryValueProperties(getSelectedClasses());
    }

    private void clearSections() {
        // Put all the editors used in the editor pools
        for (Editor editor : editorsInUse) {

            Stack<Editor> editorPool = editorPools.get(editor.getClass());
            assert editorPool != null;
            editorPool.push(editor);
            // remove all editor listeners
            editor.removeAllListeners();
        }
        editorsInUse.clear();

        // Put all the subSectionTitles used in its pool
        for (SubSectionTitle subSectionTitle : subSectionTitlesInUse) {
            subSectionTitlePool.push(subSectionTitle);
        }
        subSectionTitlesInUse.clear();

        // Clear section content
        for (SectionId section : sections) {
            GridPane content = getSectionContent(section);
            if (content != null) {
                getSectionContent(section).getChildren().clear();
                getSectionContent(section).getRowConstraints().clear();
            }
        }
        allContent.getChildren().clear();
        allContent.getRowConstraints().clear();
        searchContent.getChildren().clear();
        searchContent.getRowConstraints().clear();

        // Set the scrollbars in upper position
//        propertiesScroll.setVvalue(0);
//        layoutScroll.setVvalue(0);
//        codeScroll.setVvalue(0);
//        allScroll.setVvalue(0);
//        searchScrollPane.setVvalue(0);
    }

    private GridPane getSectionContent(SectionId sectionId) {
        assert sectionId != SectionId.NONE;
        GridPane gp;
        switch (sectionId) {
            case PROPERTIES:
                gp = propertiesSection;
                break;
            case LAYOUT:
                gp = layoutSection;
                break;
            case CODE:
                gp = codeSection;
                break;
            default:
                throw new IllegalStateException("Unexpected section id " + sectionId); //NOI18N
        }
        return gp;
    }

    private void handleTitledPane(boolean wasExpanded, boolean expanded, SectionId sectionId) {
        if (!wasExpanded && expanded) {
            // TitledPane is expanded
            if (getSectionContent(sectionId).getChildren().isEmpty()) {
                buildSection(sectionId);
            }
        }
    }

    private Node getSubSectionTitle(String title) {
        SubSectionTitle subSectionTitle;
        if (subSectionTitlePool.isEmpty()) {
//            System.out.println("Creating NEW subsection title...");
            subSectionTitle = new SubSectionTitle(title);
        } else {
//            System.out.println("Getting subsection title from CACHE...");
            subSectionTitle = subSectionTitlePool.pop();
            subSectionTitle.setTitle(title);
        }
        subSectionTitlesInUse.add(subSectionTitle);
        return subSectionTitle.getNode();
    }

    private PropertyEditor makePropertyEditor(Class<? extends Editor> editorClass, ValuePropertyMetadata propMeta) {
        Editor editor;
        PropertyEditor propertyEditor = null;
        Stack<Editor> editorPool = editorPools.get(editorClass);
        if ((editorPool != null) && !editorPool.isEmpty()) {
            editor = editorPool.pop();
            assert isPropertyEditor(editor);
            propertyEditor = (PropertyEditor) editor;
        }

        propertyEditor = makeOrResetPropertyEditor(editorClass, propMeta, propertyEditor);

        editorsInUse.add(propertyEditor);
        return propertyEditor;
    }

    private void resetPropertyEditor(PropertyEditor propertyEditor) {
        assert propertyEditor != null;
        makeOrResetPropertyEditor(propertyEditor.getClass(), propertyEditor.getPropertyMeta(), propertyEditor);
    }

    private PropertyEditor makeOrResetPropertyEditor(
            Class<? extends Editor> editorClass, ValuePropertyMetadata propMeta, PropertyEditor propertyEditor) {

        if (propertyEditor != null) {
            propertyEditor.setUpdateFromModel(true);
        }
        Set<Class<?>> selectedClasses = getSelectedClasses();
        if (editorClass == I18nStringEditor.class) {
            if (propertyEditor != null) {
                ((I18nStringEditor) propertyEditor).reset(propMeta, selectedClasses, isMultiLinesSupported(selectedClasses, propMeta));
            } else {
                propertyEditor = new I18nStringEditor(propMeta, selectedClasses, isMultiLinesSupported(selectedClasses, propMeta));
            }
        } else if (editorClass == StringEditor.class) {
            if (propertyEditor != null) {
                ((StringEditor) propertyEditor).reset(propMeta, selectedClasses);
            } else {
                propertyEditor = new StringEditor(propMeta, selectedClasses);
            }
        } else if (editorClass == DoubleEditor.class) {
            assert propMeta instanceof DoublePropertyMetadata;
            DoublePropertyMetadata doublePropMeta = (DoublePropertyMetadata) propMeta;
            if (propertyEditor != null) {
                ((DoubleEditor) propertyEditor).reset(propMeta, selectedClasses, getConstants(doublePropMeta));
            } else {
                propertyEditor = new DoubleEditor(propMeta, selectedClasses, getConstants(doublePropMeta));
            }
        } else if (editorClass == IntegerEditor.class) {
            assert propMeta instanceof IntegerPropertyMetadata;
            IntegerPropertyMetadata integerPropMeta = (IntegerPropertyMetadata) propMeta;
            if (propertyEditor != null) {
                ((IntegerEditor) propertyEditor).reset(propMeta, selectedClasses,
                        getConstants(integerPropMeta), getMin(integerPropMeta), getMax(integerPropMeta));
            } else {
                propertyEditor = new IntegerEditor(propMeta, selectedClasses,
                        getConstants(integerPropMeta), getMin(integerPropMeta), getMax(integerPropMeta));
            }
        } else if (editorClass == BooleanEditor.class) {
            if (propertyEditor != null) {
                ((BooleanEditor) propertyEditor).reset(propMeta, selectedClasses);
            } else {
                propertyEditor = new BooleanEditor(propMeta, selectedClasses);
            }
        } else if (editorClass == EnumEditor.class) {
            if (propertyEditor != null) {
                ((EnumEditor) propertyEditor).reset(propMeta, selectedClasses);
            } else {
                propertyEditor = new EnumEditor(propMeta, selectedClasses);
            }
        } else if (editorClass == InsetsEditor.class) {
            if (propertyEditor != null) {
                ((InsetsEditor) propertyEditor).reset(propMeta, selectedClasses);
            } else {
                propertyEditor = new InsetsEditor(propMeta, selectedClasses);
            }
        } else if (editorClass == BoundedDoubleEditor.class) {
            assert propMeta instanceof DoublePropertyMetadata;
            DoublePropertyMetadata doublePropMeta = (DoublePropertyMetadata) propMeta;
            if (propertyEditor != null) {
                ((BoundedDoubleEditor) propertyEditor).reset(propMeta, selectedClasses, getSelectedInstances(), getConstants(doublePropMeta));
            } else {
                propertyEditor = new BoundedDoubleEditor(propMeta, selectedClasses, getSelectedInstances(), getConstants(doublePropMeta));
            }
        } else if (editorClass == RotateEditor.class) {
            if (propertyEditor != null) {
                ((RotateEditor) propertyEditor).reset(propMeta, selectedClasses);
            } else {
                propertyEditor = new RotateEditor(propMeta, selectedClasses);
            }
        } else if (editorClass == StyleEditor.class) {
            if (propertyEditor != null) {
                ((StyleEditor) propertyEditor).reset(propMeta, selectedClasses, getEditorController());
            } else {
                propertyEditor = new StyleEditor(propMeta, selectedClasses, getEditorController());
            }
        } else if (editorClass == StyleClassEditor.class) {
            if (propertyEditor != null) {
                ((StyleClassEditor) propertyEditor).reset(propMeta, selectedClasses, getSelectedInstances(), getEditorController());
            } else {
                propertyEditor = new StyleClassEditor(propMeta, selectedClasses, getSelectedInstances(), getEditorController());
            }
        } else if (editorClass == StylesheetEditor.class) {
            if (propertyEditor != null) {
                ((StylesheetEditor) propertyEditor).reset(propMeta, selectedClasses, getEditorController().getFxmlLocation());
            } else {
                propertyEditor = new StylesheetEditor(propMeta, selectedClasses, getEditorController().getFxmlLocation());
            }
        } else if (editorClass == FxIdEditor.class) {
            String controllerClass = getControllerClass();
            if (propertyEditor != null) {
                ((FxIdEditor) propertyEditor).reset(getSuggestedFxIds(controllerClass), getEditorController());
            } else {
                propertyEditor = new FxIdEditor(getSuggestedFxIds(controllerClass), getEditorController());
            }
        } else if (editorClass == CursorEditor.class) {
            if (propertyEditor != null) {
                ((CursorEditor) propertyEditor).reset(propMeta, selectedClasses);
            } else {
                propertyEditor = new CursorEditor(propMeta, selectedClasses);
            }
        } else if (editorClass == EventHandlerEditor.class) {
            if (propertyEditor != null) {
                ((EventHandlerEditor) propertyEditor).reset(propMeta, selectedClasses, getSuggestedEventHandlers(getControllerClass()));
            } else {
                propertyEditor = new EventHandlerEditor(propMeta, selectedClasses, getSuggestedEventHandlers(getControllerClass()));
            }
        } else if (editorClass == EffectPopupEditor.class) {
            if (propertyEditor != null) {
                ((EffectPopupEditor) propertyEditor).reset(propMeta, selectedClasses);
            } else {
                propertyEditor = new EffectPopupEditor(propMeta, selectedClasses, getEditorController());
            }
        } else if (editorClass == FontPopupEditor.class) {
            if (propertyEditor != null) {
                ((FontPopupEditor) propertyEditor).reset(propMeta, selectedClasses, getEditorController());
            } else {
                propertyEditor = new FontPopupEditor(propMeta, selectedClasses, getEditorController());
            }
        } else if (editorClass == PaintPopupEditor.class) {
            if (propertyEditor != null) {
                ((PaintPopupEditor) propertyEditor).reset(propMeta, selectedClasses);
            } else {
                propertyEditor = new PaintPopupEditor(propMeta, selectedClasses, getEditorController());
            }
        } else if (editorClass == ImageEditor.class) {
            if (propertyEditor != null) {
                ((ImageEditor) propertyEditor).reset(propMeta, selectedClasses, getEditorController().getFxmlLocation());
            } else {
                propertyEditor = new ImageEditor(propMeta, selectedClasses, getEditorController().getFxmlLocation());
            }
        } else if (editorClass == BoundsPopupEditor.class) {
            if (propertyEditor != null) {
                ((BoundsPopupEditor) propertyEditor).reset(propMeta, selectedClasses);
            } else {
                propertyEditor = new BoundsPopupEditor(propMeta, selectedClasses);
            }
        } else if (editorClass == Point3DEditor.class) {
            if (propertyEditor != null) {
                ((Point3DEditor) propertyEditor).reset(propMeta, selectedClasses);
            } else {
                propertyEditor = new Point3DEditor(propMeta, selectedClasses);
            }
        } else if (editorClass == DividerPositionsEditor.class) {
            if (propertyEditor != null) {
                ((DividerPositionsEditor) propertyEditor).reset(propMeta, selectedClasses);
            } else {
                propertyEditor = new DividerPositionsEditor(propMeta, selectedClasses);
            }
        } else if (editorClass == TextAlignmentEditor.class) {
            if (propertyEditor != null) {
                ((TextAlignmentEditor) propertyEditor).reset(propMeta, selectedClasses);
            } else {
                propertyEditor = new TextAlignmentEditor(propMeta, selectedClasses);
            }
        } else if (editorClass == KeyCombinationPopupEditor.class) {
            if (propertyEditor != null) {
                ((KeyCombinationPopupEditor) propertyEditor).reset(propMeta, selectedClasses, getEditorController());
            } else {
                propertyEditor = new KeyCombinationPopupEditor(propMeta, selectedClasses, getEditorController());
            }
        } else if (editorClass == ColumnResizePolicyEditor.class) {
            if (propertyEditor != null) {
                ((ColumnResizePolicyEditor) propertyEditor).reset(propMeta, selectedClasses);
            } else {
                propertyEditor = new ColumnResizePolicyEditor(propMeta, selectedClasses);
            }
        } else if (editorClass == Rectangle2DPopupEditor.class) {
            if (propertyEditor != null) {
                ((Rectangle2DPopupEditor) propertyEditor).reset(propMeta, selectedClasses);
            } else {
                propertyEditor = new Rectangle2DPopupEditor(propMeta, selectedClasses);
            }
        } else if (editorClass == ToggleGroupEditor.class) {
            if (propertyEditor != null) {
                ((ToggleGroupEditor) propertyEditor).reset(propMeta, selectedClasses, getSuggestedToggleGroups());
            } else {
                propertyEditor = new ToggleGroupEditor(propMeta, selectedClasses, getSuggestedToggleGroups());
            }
        } else {
            if (propertyEditor != null) {
                ((GenericEditor) propertyEditor).reset(propMeta, selectedClasses);
            } else {
                propertyEditor = new GenericEditor(propMeta, selectedClasses);
            }
        }
        propertyEditor.setUpdateFromModel(false);

        return propertyEditor;
    }

    private PropertiesEditor makePropertiesEditor(Class<? extends Editor> editorClass, ValuePropertyMetadata[] propMetas) {
        Editor editor = null;
        PropertiesEditor propertiesEditor;
        Stack<Editor> editorPool = editorPools.get(editorClass);
        if ((editorPool != null) && !editorPool.isEmpty()) {
            editor = editorPool.pop();
            assert isPropertiesEditor(editor);
        }

        // Only AnchorPane for now
        assert editorClass == AnchorPaneConstraintsEditor.class;

        if (editor != null) {
            assert editor instanceof AnchorPaneConstraintsEditor;
            ((AnchorPaneConstraintsEditor) editor).reset(
                    propMetas[0], propMetas[1], propMetas[2], propMetas[3], getSelectedInstances());
        } else {
            editor = new AnchorPaneConstraintsEditor("AnchorPane Constraints", propMetas[0], propMetas[1], propMetas[2], propMetas[3], getSelectedInstances());
        }
        propertiesEditor = (AnchorPaneConstraintsEditor) editor;

        editorsInUse.add(editor);
        return propertiesEditor;
    }

    private static class SubSectionTitle {

        @FXML
        private Label titleLb;

        private Parent root;

        public SubSectionTitle(String title) {
            initialize(title);
        }

        // Separate method to avoid FindBugs warning
        private void initialize(String title) {
//          System.out.println("Loading new SubSection.fxml...");
          URL fxmlURL = SubSectionTitle.class.getResource("SubSection.fxml");
          root = EditorUtils.loadFxml(fxmlURL, this);
          titleLb.setText(title);
        }

        public void setTitle(String title) {
            titleLb.setText(title);
        }

        public Node getNode() {
            return root;
        }
    }

    private void updateClassNameInSectionTitles() {
        String selClass = ""; //NOI18N
        if (getSelectedClasses().size() > 1) {
            selClass = I18N.getString("inspector.sectiontitle.multiple");
        } else if (getSelectedClasses().size() == 1) {
            selClass = getSelectedClass().getSimpleName();
        }

        for (TitledPane titledPane : accordion.getPanes()) {
            Node graphic = titledPane.getGraphic();
            assert graphic instanceof Label;
            if (titledPane == allTitledPane) {
                allTitledPane.setText(null);
            } else {
                if (!selClass.isEmpty() && !selClass.startsWith(" :")) { //NOI18N
                    selClass = " : " + selClass; //NOI18N
                }
            }
            ((Label) graphic).setText(selClass);
        }
    }

    private boolean isPropertyEditor(Editor editor) {
        return editor instanceof PropertyEditor;
    }

    private boolean isPropertiesEditor(Editor editor) {
        return editor instanceof PropertiesEditor;
    }

    private List<String> getSuggestedFxIds(String controllerClass) {
        // Is not needed if multiple selection.
        if (controllerClass == null || hasMultipleSelection()) {
            return Collections.emptyList();
        }
        Glossary glossary = getEditorController().getGlossary();
        URL location = null;
        if (getEditorController().getFxomDocument() != null) {
            location = getEditorController().getFxomDocument().getLocation();
        }
        List<String> fxIds = glossary.queryFxIds(location, controllerClass, getSelectedClass());
        // Remove the already used FxIds
        fxIds.removeAll(getFxIdsInUse());
        return fxIds;
    }

    private List<String> getFxIdsInUse() {
        FXOMFxIdIndex fxomIndex = new FXOMFxIdIndex(getEditorController().getFxomDocument());
        return new ArrayList<>(fxomIndex.getFxIds().keySet());
    }

    private List<String> getSuggestedEventHandlers(String controllerClass) {
        if (controllerClass == null) {
            return Collections.emptyList();
        }
        Glossary glossary = getEditorController().getGlossary();
        URL location = null;
        if (getEditorController().getFxomDocument() != null) {
            location = getEditorController().getFxomDocument().getLocation();
        }
        return glossary.queryEventHandlers(location, controllerClass);
    }

    private List<String> getSuggestedToggleGroups() {
        FXOMFxIdIndex fxomIndex = new FXOMFxIdIndex(getEditorController().getFxomDocument());
        List<FXOMInstance> tgs = fxomIndex.collectToggleGroups();
        ArrayList<String> tgNames = new ArrayList<>();
        for (FXOMInstance tg : tgs) {
            tgNames.add(tg.getFxId());
        }
        return tgNames;
    }

    private String getControllerClass() {
        return getEditorController().getFxomDocument().getFxomRoot().getFxController();
    }

    // 
    // Helper methods for SelectionState class
    //
    private Set<FXOMInstance> getSelectedInstances() {
        return selectionState.getSelectedInstances();
    }

    private FXOMInstance getSelectedInstance() {
        assert getSelectedInstances().size() == 1;
        return (FXOMInstance) getSelectedInstances().toArray()[0];
    }

    private Set<FXOMInstance> getUnresolvedInstances() {
        return selectionState.getUnresolvedInstances();
    }

    private Set<Class<?>> getSelectedClasses() {
        return selectionState.getSelectedClasses();
    }

    private Class<?> getSelectedClass() {
        assert getSelectedClasses().size() == 1;
        return (Class<?>) getSelectedClasses().toArray()[0];
    }

    private Class<?> getCommonParent() {
        return selectionState.getCommonParentClass();
    }

    private boolean isBoundedByProperties(ValuePropertyMetadata propMeta) {
        // Only ScrollPane.hValue and ScrollPane.vValue for now
        if (propMeta.getName().toString().equals(Editor.hValuePropName) 
                || propMeta.getName().toString().equals(Editor.vValuePropName)) {
            return true;
        }
        return false;
    }
    
    /*
     *   This class represents the selection state: 
     *   - the selected instances, 
     *   - the selected classes,
     *   - the common parent for the selected instances (if any),
     *   - the unresolved selected instances (if any), 
     *      in case of an instance is missing its corresponding object (for instance a png file)
     */
    private final class SelectionState {

        private final Selection selection;
        private final Set<FXOMInstance> selectedInstances = new HashSet<>();
        private final Set<Class<?>> selectedClasses = new HashSet<>();
        private Class<?> commonParentClass;
        private FXOMObject commonParentObject;
        private final Set<FXOMInstance> unresolvedInstances = new HashSet<>();

        public SelectionState(EditorController editorController) {
            this.selection = editorController.getSelection();
            initialize();
        }

        protected void initialize() {
            // New selection: initializePopupContent all the selection variables

            selectedInstances.clear();
            if (selection.getGroup() instanceof ObjectSelectionGroup) {
                final ObjectSelectionGroup osg = (ObjectSelectionGroup) selection.getGroup();
                for (FXOMObject obj : osg.getItems()) {
                    if (obj instanceof FXOMInstance) {
                        selectedInstances.add((FXOMInstance) obj);
                    }
                }
            } else if (selection.getGroup() instanceof GridSelectionGroup) {
                GridSelectionGroup gsg = (GridSelectionGroup) selection.getGroup();
                for (FXOMInstance inst : gsg.collectConstraintInstances()) {
                    selectedInstances.add(inst);
                    // Open the Layout section, since all the row/columns properties are there.
                    if (getExpandedSectionId() != SectionId.LAYOUT) {
                        setExpandedSection(SectionId.LAYOUT);
                    }
                }
            }

            selectedClasses.clear();
            for (FXOMInstance instance : selectedInstances) {
                if (instance.getDeclaredClass() != null) { // null means unresolved instance
                    selectedClasses.add(instance.getDeclaredClass());
                }
            }

            commonParentClass = null;
            for (FXOMInstance instance : selectedInstances) {
                if (commonParentClass == null) {
                    // first instance
                    commonParentClass = getParentClass(instance);
                } else {
                    if (getParentClass(instance) != commonParentClass) {
                        commonParentClass = null;
                        break;
                    }
                }
            }

            commonParentObject = null;
            for (FXOMInstance instance : selectedInstances) {
                if (commonParentObject == null) {
                    // first instance
                    commonParentObject = instance.getParentObject();
                } else {
                    if (instance.getParentObject() != commonParentObject) {
                        commonParentObject = null;
                        break;
                    }
                }
            }

            unresolvedInstances.clear();
            for (FXOMInstance instance : selectedInstances) {
                if (instance.getSceneGraphObject() == null) {
                    unresolvedInstances.add(instance);
                }
            }
        }

        private boolean isSelectionEmpty() {
            return selection.isEmpty();
        }

        private Set<Class<?>> getSelectedClasses() {
            return selectedClasses;
        }

        private Class<?> getCommonParentClass() {
            return commonParentClass;
        }

        private FXOMObject getCommonParentObject() {
            return commonParentObject;
        }

        private Set<FXOMInstance> getSelectedInstances() {
            return selectedInstances;
        }

        private Set<FXOMInstance> getUnresolvedInstances() {
            return unresolvedInstances;
        }

        private Class<?> getParentClass(FXOMInstance instance) {
            FXOMObject parent = instance.getParentObject();
            if (parent == null) {
                // root
                return null;
            }
            // A parent is always a FXOMInstance
            assert parent instanceof FXOMInstance;
            return ((FXOMInstance) parent).getDeclaredClass();
        }

    }

    /*
     * Set the focus to a given property value editor,
     * and move the scrolllbar so that it is visible.
     * Typically used by CSS analyzer.
     */
    public void setFocusToEditor(PropertyName propName) {
        // Retrieve the editor
        PropertyEditor editor = null;
        for (Editor ed : editorsInUse) {
            if (ed instanceof PropertyEditor) {
                if (propName.equals(((PropertyEditor) ed).getPropertyName())) {
                    editor = (PropertyEditor) ed;
                }
            }
        }
        if (editor == null) {
            // editor not found
            return;
        }

        final PropertyEditor editorToFocus = editor;

        final Node valueEditorNode = editorToFocus.getValueEditor();
        // Search the ScrollPane
        ScrollPane sp = null;
        Node node = valueEditorNode.getParent();
        while (node != null) {
            if (node instanceof ScrollPane) {
                sp = (ScrollPane) node;
                break;
            }
            node = node.getParent();
        }
        if (sp == null) {
            return;
        }

        // Position the scrollBar such as the editor is centered in the TitledPane (when possible)
        final ScrollPane scrollPane = sp;
        double editorHeight = valueEditorNode.getLayoutBounds().getHeight();
        final Point2D pt = scrollPane.getContent().sceneToLocal(valueEditorNode.localToScene(0, 0));
        // viewport height
        double vpHeight = scrollPane.getViewportBounds().getHeight();
        // Position of the editor in the scrollPane content
        double selY = pt.getY();
        // Height of the scrollPane content
        double contentHeight = scrollPane.getContent().getLayoutBounds().getHeight();
        // Position of the middle point of the scrollPane content
        double contentMiddle = contentHeight / 2;
        // Manage the editor height depending on its position
        if (selY > contentMiddle) {
            selY += editorHeight;
        } else {
            selY -= editorHeight;
        }
        // Compute the move to apply to position the editor on the middle of the scrollPane content
        double moveContent = selY - contentMiddle;
        // Size ratio between scrollPane content and viewport
        double vpRatio = contentHeight / vpHeight;
        // Move to apply to the editor to position it in the middle of the viewport
        double moveVp = moveContent / vpRatio;
        // Position of the editor in the viewport
        double selYVp = (vpHeight / 2) + moveVp;
        // Position in percent
        double scrollPos = selYVp / vpHeight;
        // Finally, set the scrollBar position
        scrollPane.setVvalue(scrollPos);

        // Set the focus to the editor
        editorToFocus.requestFocus();
    }

}
