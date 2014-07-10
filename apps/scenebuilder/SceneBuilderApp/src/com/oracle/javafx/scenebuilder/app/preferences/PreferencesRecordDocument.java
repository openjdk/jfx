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
package com.oracle.javafx.scenebuilder.app.preferences;

import com.oracle.javafx.scenebuilder.app.DocumentWindowController;
import com.oracle.javafx.scenebuilder.app.SplitController;

import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.BOTTOM_DIVIDER_VPOS;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.BOTTOM_VISIBLE;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.DOCUMENT_VISIBLE;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.I18N_RESOURCE;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.STAGE_HEIGHT;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.INSPECTOR_SECTION_ID;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.LEFT_VISIBLE;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.LEFT_DIVIDER_HPOS;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.LEFT_DIVIDER_VPOS;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.LIBRARY_VISIBLE;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.PATH;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.RIGHT_DIVIDER_HPOS;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.RIGHT_VISIBLE;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.SCENE_STYLE_SHEETS;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.STAGE_WIDTH;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.X_POS;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.Y_POS;

import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.InspectorPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.inspector.InspectorPanelController.SectionId;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Accordion;
import javafx.scene.control.TitledPane;
import javafx.stage.Stage;

/**
 * Defines preferences specific to a document.
 */
public class PreferencesRecordDocument {

    private static final double UNDEFINED_POS = -1.0;

    // Default values
    private static final double DEFAULT_X_POS = UNDEFINED_POS;
    private static final double DEFAULT_Y_POS = UNDEFINED_POS;
    private static final double DEFAULT_STAGE_HEIGHT = 800;
    private static final double DEFAULT_STAGE_WIDTH = 1240;
    private static final boolean DEFAULT_BOTTOM_VISIBLE = false;
    private static final boolean DEFAULT_LEFT_VISIBLE = true;
    private static final boolean DEFAULT_RIGHT_VISIBLE = true;
    private static final boolean DEFAULT_LIBRARY_VISIBLE = true;
    private static final boolean DEFAULT_DOCUMENT_VISIBLE = true;
    private static final SectionId DEFAULT_INSPECTOR_SECTION_ID = SectionId.PROPERTIES;
    private static final double DEFAULT_LEFT_DIVIDER_HPOS = UNDEFINED_POS;
    private static final double DEFAULT_RIGHT_DIVIDER_HPOS = UNDEFINED_POS;
    private static final double DEFAULT_BOTTOM_DIVIDER_VPOS = UNDEFINED_POS;
    private static final double DEFAULT_LEFT_DIVIDER_VPOS = UNDEFINED_POS;

    // Document preferences
    private String path = null;
    private double xPos = DEFAULT_X_POS;
    private double yPos = DEFAULT_Y_POS;
    private double stageHeight = DEFAULT_STAGE_HEIGHT;
    private double stageWidth = DEFAULT_STAGE_WIDTH;
    private boolean bottomVisible = DEFAULT_BOTTOM_VISIBLE;
    private boolean leftVisible = DEFAULT_LEFT_VISIBLE;
    private boolean rightVisible = DEFAULT_RIGHT_VISIBLE;
    private boolean libraryVisible = DEFAULT_LIBRARY_VISIBLE;
    private boolean documentVisible = DEFAULT_DOCUMENT_VISIBLE;
    private SectionId inspectorSectionId = DEFAULT_INSPECTOR_SECTION_ID;
    private double leftDividerHPos = DEFAULT_LEFT_DIVIDER_HPOS;
    private double rightDividerHPos = DEFAULT_RIGHT_DIVIDER_HPOS;
    private double bottomDividerVPos = DEFAULT_BOTTOM_DIVIDER_VPOS;
    private double leftDividerVPos = DEFAULT_LEFT_DIVIDER_VPOS;
    private final List<String> sceneStyleSheets = new ArrayList<>();
    private String I18NResource = null;

    private Preferences documentPreferences;
    private final Preferences documentsRootPreferences; // preference root node for all documents records
    private final DocumentWindowController documentWindowController;

    private final ChangeListener<Number> leftDividerHListener = (ov, t, t1) -> setLeftDividerHPos(t1.doubleValue());
    private final ChangeListener<Number> rightDividerHListener = (ov, t, t1) -> setRightDividerHPos(t1.doubleValue());
    private final ChangeListener<Number> bottomDividerVListener = (ov, t, t1) -> setBottomDividerVPos(t1.doubleValue());
    private final ChangeListener<Number> leftDividerVListener = (ov, t, t1) -> setLeftDividerVPos(t1.doubleValue());
    private final ChangeListener<ObservableList<File>> sceneStyleSheetsListener = (ov, t, t1) -> setSceneStyleSheets(t1);

    public PreferencesRecordDocument(Preferences documentsRootPreferences, DocumentWindowController dwc) {
        this.documentWindowController = dwc;
        this.documentsRootPreferences = documentsRootPreferences;

        // Add stage X and Y listeners
        final Stage stage = documentWindowController.getStage();
        assert stage != null;
        stage.xProperty().addListener((ChangeListener<Number>) (ov, t, t1) -> setXPos(t1.doubleValue()));
        stage.yProperty().addListener((ChangeListener<Number>) (ov, t, t1) -> setYPos(t1.doubleValue()));

        // Add stage height and width listeners
        stage.heightProperty().addListener((ChangeListener<Number>) (ov, t, t1) -> setStageHeight(t1.doubleValue()));
        stage.widthProperty().addListener((ChangeListener<Number>) (ov, t, t1) -> setStageWidth(t1.doubleValue()));

        // Add inspector accordion expanded pane listener
        final InspectorPanelController ipc = documentWindowController.getInspectorPanelController();
        assert ipc != null;
        final Accordion accordion = ipc.getAccordion();
        assert accordion != null;
        accordion.expandedPaneProperty().addListener((ChangeListener<TitledPane>) (ov, t, t1) -> setInspectorSectionId(ipc.getExpandedSectionId()));

        // Add dividers position listeners
        final SplitController lhsc = documentWindowController.getLeftSplitController();
        lhsc.position().addListener(leftDividerHListener);
        final SplitController rhsc = documentWindowController.getRightSplitController();
        rhsc.position().addListener(rightDividerHListener);
        final SplitController bvsc = documentWindowController.getBottomSplitController();
        bvsc.position().addListener(bottomDividerVListener);
        final SplitController lvsc = documentWindowController.getLibrarySplitController();
        lvsc.position().addListener(leftDividerVListener);

        // Add scene style sheets listener
        final EditorController ec = documentWindowController.getEditorController();
        ec.sceneStyleSheetProperty().addListener(sceneStyleSheetsListener);
    }
    
    public void resetDocumentPreferences() {
        this.documentPreferences = null;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String value) {
        path = value;
    }

    public double getXPos() {
        return xPos;
    }

    public void setXPos(double value) {
        xPos = value;
    }

    public double getYPos() {
        return yPos;
    }

    public void setYPos(double value) {
        yPos = value;
    }

    public double getStageHeight() {
        return stageHeight;
    }

    public void setStageHeight(double value) {
        stageHeight = value;
    }

    public double getStageWidth() {
        return stageWidth;
    }

    public void setStageWidth(double value) {
        stageWidth = value;
    }

    public boolean getBottomVisible() {
        return bottomVisible;
    }

    public void setBottomVisible(boolean value) {
        bottomVisible = value;
    }

    public boolean getLeftVisible() {
        return leftVisible;
    }

    public void setLeftVisible(boolean value) {
        leftVisible = value;
    }

    public boolean getRightVisible() {
        return rightVisible;
    }

    public void setRightVisible(boolean value) {
        rightVisible = value;
    }

    public boolean getLibraryVisible() {
        return libraryVisible;
    }

    public void setLibraryVisible(boolean value) {
        libraryVisible = value;
    }

    public boolean getDocumentVisible() {
        return documentVisible;
    }

    public void setDocumentVisible(boolean value) {
        documentVisible = value;
    }

    public SectionId getInspectorSectionId() {
        return inspectorSectionId;
    }

    public void setInspectorSectionId(SectionId value) {
        inspectorSectionId = value;
    }

    public double getLeftDividerHPos() {
        return leftDividerHPos;
    }

    public void setLeftDividerHPos(double value) {
        leftDividerHPos = value;
    }

    public double getRightDividerHPos() {
        return rightDividerHPos;
    }

    public void setRightDividerHPos(double value) {
        rightDividerHPos = value;
    }

    public double getBottomDividerVPos() {
        return bottomDividerVPos;
    }

    public void setBottomDividerVPos(double value) {
        bottomDividerVPos = value;
    }

    public double getLeftividerVPos() {
        return leftDividerVPos;
    }

    public void setLeftDividerVPos(double value) {
        leftDividerVPos = value;
    }

    public List<String> getSceneStyleSheets() {
        return sceneStyleSheets;
    }

    public void setSceneStyleSheets(ObservableList<File> files) {
        sceneStyleSheets.clear();
        for (File file : files) {
            final String filePath = file.getPath();
            sceneStyleSheets.add(filePath);
        }
    }

    public void removeSceneStyleSheet(String filePath) {
        sceneStyleSheets.remove(filePath);
    }

    public void removeSceneStyleSheet(List<String> filePaths) {
        for (String filePath : filePaths) {
            removeSceneStyleSheet(filePath);
        }
    }

    public String getI18NResource() {
        return I18NResource;
    }

    public void setI18NResource(String value) {
        I18NResource = value;
    }

    public void setI18NResourceFile(File file) {
        if (file != null) {
            I18NResource = file.getPath();
        }
    }

    public void refreshXPos() {
        if (xPos != UNDEFINED_POS) {
            documentWindowController.getStage().setX(xPos);
        }
    }

    public void refreshYPos() {
        if (yPos != UNDEFINED_POS) {
            documentWindowController.getStage().setY(yPos);
        }
    }

    public void refreshStageHeight() {
        documentWindowController.getStage().setHeight(stageHeight);
    }

    public void refreshStageWidth() {
        documentWindowController.getStage().setWidth(stageWidth);
    }

    public void refreshInspectorSectionId() {
        final InspectorPanelController ipc = documentWindowController.getInspectorPanelController();
        ipc.setExpandedSection(inspectorSectionId);
    }

    public void refreshBottomVisible() {
        final SplitController sc = documentWindowController.getBottomSplitController();
        if (bottomVisible) {
            // CSS panel is built lazely : initialize the CSS panel first
            documentWindowController.initializeCssPanel();
        }
        sc.setTargetVisible(bottomVisible);
    }

    public void refreshLeftVisible() {
        final SplitController sc = documentWindowController.getLeftSplitController();
        sc.setTargetVisible(leftVisible);
    }

    public void refreshRightVisible() {
        final SplitController sc = documentWindowController.getRightSplitController();
        sc.setTargetVisible(rightVisible);
    }

    public void refreshLibraryVisible() {
        final SplitController sc = documentWindowController.getLibrarySplitController();
        sc.setTargetVisible(libraryVisible);
    }

    public void refreshDocumentVisible() {
        final SplitController sc = documentWindowController.getDocumentSplitController();
        sc.setTargetVisible(documentVisible);
    }

    public void refreshLeftDividerHPos() {
        final SplitController sc = documentWindowController.getLeftSplitController();
        if (leftDividerHPos != UNDEFINED_POS) {
            sc.setPosition(leftDividerHPos);
        }
    }

    public void refreshRightDividerHPos() {
        final SplitController sc = documentWindowController.getRightSplitController();
        if (rightDividerHPos != UNDEFINED_POS) {
            sc.setPosition(rightDividerHPos);
        }
    }

    public void refreshBottomDividerVPos() {
        final SplitController sc = documentWindowController.getBottomSplitController();
        if (bottomDividerVPos != UNDEFINED_POS) {
            sc.setPosition(bottomDividerVPos);
        }
    }

    public void refreshLeftDividerVPos() {
        final SplitController sc = documentWindowController.getLibrarySplitController();
        if (leftDividerVPos != UNDEFINED_POS) {
            sc.setPosition(leftDividerVPos);
        }
    }

    public void refreshSceneStyleSheets() {
        if (sceneStyleSheets.isEmpty() == false) {
            final ObservableList<File> files = FXCollections.observableArrayList();
            final List<String> filePathsToRemove = new ArrayList<>();
            for (String sceneStyleSheet : sceneStyleSheets) {
                final File file = new File(sceneStyleSheet);
                if (file.exists()) {
                    files.add(file);
                } else {
                    // File is still in preferences DB but has been removed from disk
                    filePathsToRemove.add(sceneStyleSheet);
                }
            }
            final EditorController ec = documentWindowController.getEditorController();
            ec.setSceneStyleSheets(files);
            // Cleanup style sheets preferences if needed
            if (filePathsToRemove.isEmpty() == false) {
                removeSceneStyleSheet(filePathsToRemove);
            }
        }
    }

    public void refreshI18NResource() {
        if (I18NResource != null) {
            final File file = new File(I18NResource);
            if (file.exists()) {
                documentWindowController.setResourceFile(file);
            } else {
                // File is still in preferences DB but has been removed from disk
                setI18NResource(null);
            }
        }
    }

    public void refresh() {
        refreshXPos();
        refreshYPos();
        refreshStageHeight();
        refreshStageWidth();
        refreshInspectorSectionId();
        refreshBottomVisible();
        refreshLeftVisible();
        refreshRightVisible();
        refreshLibraryVisible();
        refreshDocumentVisible();
        refreshLeftDividerHPos();
        refreshRightDividerHPos();
        refreshBottomDividerVPos();
        refreshLeftDividerVPos();
        refreshSceneStyleSheets();
        refreshI18NResource();
    }

    /**
     * Read data from the java preferences DB and initialize properties.
     */
    public void readFromJavaPreferences() {

        assert documentPreferences == null;

        final URL fxmlLocation = documentWindowController.getEditorController().getFxmlLocation();
        if (fxmlLocation == null) {
            // Document has not been saved yet => nothing to read
            return;
        }

        // Check if there is some preferences for this document
        try {
            final File fxmlFile = new File(fxmlLocation.toURI());
            final String filePath = fxmlFile.getPath();
            final String[] childrenNames = documentsRootPreferences.childrenNames();
            for (String child : childrenNames) {
                final Preferences pref = documentsRootPreferences.node(child);
                final String nodePath = pref.get(PATH, null);
                assert nodePath != null && nodePath.isEmpty() == false; // Each document node defines a path
                if (filePath.equals(nodePath)) {
                    documentPreferences = pref;
                    break;
                }
            }
        } catch (BackingStoreException ex) {
            Logger.getLogger(PreferencesRecordDocument.class.getName()).log(Level.SEVERE, null, ex);
            return;
        } catch (URISyntaxException ex) {
            Logger.getLogger(PreferencesRecordDocument.class.getName()).log(Level.SEVERE, null, ex);
        }

        // There is no preferences for this document in the Java preferences DB
        // => nothing to read
        if (documentPreferences == null) {
            return;
        }

        // Window position
        double xpos = documentPreferences.getDouble(X_POS, DEFAULT_X_POS);
        if (xpos < 0) {
            xpos = DEFAULT_X_POS;
        }
        setXPos(xpos);
        double ypos = documentPreferences.getDouble(Y_POS, DEFAULT_Y_POS);
        if (ypos < 0) {
            ypos = DEFAULT_Y_POS;
        }
        setYPos(ypos);

        // Window size
        double h = documentPreferences.getDouble(STAGE_HEIGHT, DEFAULT_STAGE_HEIGHT);
        if (h < 0) {
            h = DEFAULT_STAGE_HEIGHT;
        }
        setStageHeight(h);
        double w = documentPreferences.getDouble(STAGE_WIDTH, DEFAULT_STAGE_WIDTH);
        if (w < 0) {
            w = DEFAULT_STAGE_WIDTH;
        }
        setStageWidth(w);

        // Panel visibility
        final boolean bv = documentPreferences.getBoolean(BOTTOM_VISIBLE,
                DEFAULT_BOTTOM_VISIBLE);
        setBottomVisible(bv);
        final boolean lv = documentPreferences.getBoolean(LEFT_VISIBLE,
                DEFAULT_LEFT_VISIBLE);
        setLeftVisible(lv);
        final boolean rv = documentPreferences.getBoolean(RIGHT_VISIBLE,
                DEFAULT_RIGHT_VISIBLE);
        setRightVisible(rv);
        final boolean libv = documentPreferences.getBoolean(LIBRARY_VISIBLE,
                DEFAULT_LIBRARY_VISIBLE);
        // Since SB 2.0 b11, the visibility of Library and Document was handled
        // independently from the Left visibility.
        // Starting from SB 2.0 b12, the visibility of Library and Document is
        // linked to the Left visibility. 
        // We need to handle new preferences as well as old ones :
        // hence the value set for Library and Document visible property.
        setLibraryVisible(lv && libv);
        final boolean docv = documentPreferences.getBoolean(DOCUMENT_VISIBLE,
                DEFAULT_DOCUMENT_VISIBLE);
        setDocumentVisible(lv && docv);

        // Inspector expanded TitledPane
        final String sectionId = documentPreferences.get(INSPECTOR_SECTION_ID,
                DEFAULT_INSPECTOR_SECTION_ID.name());
        setInspectorSectionId(SectionId.valueOf(sectionId));

        // Dividers position
        final double ldhp = documentPreferences.getDouble(LEFT_DIVIDER_HPOS,
                DEFAULT_LEFT_DIVIDER_HPOS);
        setLeftDividerHPos(ldhp);
        final double rdhp = documentPreferences.getDouble(RIGHT_DIVIDER_HPOS,
                DEFAULT_RIGHT_DIVIDER_HPOS);
        setRightDividerHPos(rdhp);
        final double bdvp = documentPreferences.getDouble(BOTTOM_DIVIDER_VPOS,
                DEFAULT_BOTTOM_DIVIDER_VPOS);
        setBottomDividerVPos(bdvp);
        final double ldvp = documentPreferences.getDouble(LEFT_DIVIDER_VPOS,
                DEFAULT_LEFT_DIVIDER_VPOS);
        setLeftDividerVPos(ldvp);

        // Scene style sheets
        final String items = documentPreferences.get(SCENE_STYLE_SHEETS, null);
        if (items != null) {
            final String[] itemsArray = items.split("\\" + File.pathSeparator); //NOI18N
            sceneStyleSheets.addAll(Arrays.asList(itemsArray));
        }

        // I18NResource
        final String resource = documentPreferences.get(I18N_RESOURCE, null); //NOI18N
        setI18NResource(resource);
    }

    /**
     * Write the properties data to the java preferences DB.
     */
    public void writeToJavaPreferences() {

        final URL fxmlLocation = documentWindowController.getEditorController().getFxmlLocation();
        if (fxmlLocation == null) {
            // Document has not been saved => nothing to write
            // This is the case with initial empty document 
            return;
        }

        // There is no preferences for this document in the Java preferences DB
        // => create a new preference node
        if (documentPreferences == null) {
            try {
                final File fxmlFile = new File(fxmlLocation.toURI());
                final String filePath = fxmlFile.getPath();
                final String key = generateKey(fxmlFile.getName());
                assert documentsRootPreferences.nodeExists(key) == false;
                // Create a new document preference node under the document root node
                documentPreferences = documentsRootPreferences.node(key);
                // Document path
                documentPreferences.put(PATH, filePath);
            } catch (BackingStoreException ex) {
                Logger.getLogger(PreferencesRecordDocument.class.getName()).log(Level.SEVERE, null, ex);
                return;
            } catch (URISyntaxException ex) {
                Logger.getLogger(PreferencesRecordDocument.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        assert documentPreferences != null;

        // Window position
        documentPreferences.putDouble(X_POS, getXPos());
        documentPreferences.putDouble(Y_POS, getYPos());

        // Window size
        documentPreferences.putDouble(STAGE_HEIGHT, getStageHeight());
        documentPreferences.putDouble(STAGE_WIDTH, getStageWidth());

        // Panel visibility
        documentPreferences.putBoolean(BOTTOM_VISIBLE, getBottomVisible());
        documentPreferences.putBoolean(LEFT_VISIBLE, getLeftVisible());
        documentPreferences.putBoolean(RIGHT_VISIBLE, getRightVisible());
        documentPreferences.putBoolean(LIBRARY_VISIBLE, getLibraryVisible());
        documentPreferences.putBoolean(DOCUMENT_VISIBLE, getDocumentVisible());

        // Inspector expanded TitledPane
        if (getInspectorSectionId() != null) { // Section ID is null when View Mode differs from section
            documentPreferences.put(INSPECTOR_SECTION_ID, getInspectorSectionId().name());
        }

        // Dividers position
        documentPreferences.putDouble(LEFT_DIVIDER_HPOS, getLeftDividerHPos());
        documentPreferences.putDouble(RIGHT_DIVIDER_HPOS, getRightDividerHPos());
        documentPreferences.putDouble(BOTTOM_DIVIDER_VPOS, getBottomDividerVPos());
        documentPreferences.putDouble(LEFT_DIVIDER_VPOS, getLeftividerVPos());

        // Scene style sheets
        final StringBuilder sb = new StringBuilder();
        for (String sceneStyleSheet : getSceneStyleSheets()) {
            sb.append(sceneStyleSheet);
            sb.append(File.pathSeparator);
        }
        documentPreferences.put(SCENE_STYLE_SHEETS, sb.toString());

        // I18NResource
        final String resource = getI18NResource();
        if (resource != null) {
            documentPreferences.put(I18N_RESOURCE, resource);
        } else {
            documentPreferences.remove(I18N_RESOURCE);
        }
    }

    /**
     * Generates a document node key for the specified document file name.
     * Preferences keys are limited to 80 chars so we cannot use the document path.
     *
     * If there is no document key matching the specified document file name,
     * we use it as this document key.
     * Otherwise, we prepend the document file name with an index.
     *
     * @param name The document file name
     * @return
     */
    private String generateKey(String name) throws BackingStoreException {

        String key = name;
        if (key.length() > Preferences.MAX_KEY_LENGTH) {
            key = name.substring(0, Preferences.MAX_KEY_LENGTH);
        }

        int prefix = 1, max = 20; // Allow up to 20 files with same name
        while (documentsRootPreferences.nodeExists(key) && prefix < max) {
            key = prefix++ + "_" + name; //NOI18N
            if (key.length() > Preferences.MAX_KEY_LENGTH) {
                key = key.substring(0, Preferences.MAX_KEY_LENGTH);
            }
        }
        return key;
    }
}
