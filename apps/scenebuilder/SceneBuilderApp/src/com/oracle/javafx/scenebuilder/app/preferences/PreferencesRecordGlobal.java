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
package com.oracle.javafx.scenebuilder.app.preferences;

import com.oracle.javafx.scenebuilder.app.DocumentWindowController;
import com.oracle.javafx.scenebuilder.app.SceneBuilderApp;
import com.oracle.javafx.scenebuilder.app.i18n.I18N;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.ALIGNMENT_GUIDES_COLOR;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.BACKGROUND_IMAGE;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.CSS_ANALYZER_COLUMN_ORDER;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.DOCUMENT_HEIGHT;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.DOCUMENT_WIDTH;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.HIERARCHY_DISPLAY_OPTION;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.PARENT_RING_COLOR;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.RECENT_ITEMS;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.RECENT_ITEMS_SIZE;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.ContentPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.AbstractHierarchyPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.AbstractHierarchyPanelController.DisplayOption;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.paint.Color;

/**
 * Defines preferences global to the application.
 */
public class PreferencesRecordGlobal {

    // Default values
    private static final double DEFAULT_DOCUMENT_HEIGHT = 400;
    private static final double DEFAULT_DOCUMENT_WIDTH = 600;

    private static final BackgroundImage DEFAULT_BACKGROUND_IMAGE
            = BackgroundImage.BACKGROUND_01;
    private static final Color DEFAULT_ALIGNMENT_GUIDES_COLOR = Color.RED;
    private static final Color DEFAULT_PARENT_RING_COLOR = Color.rgb(238, 168, 47);

    private static final DisplayOption DEFAULT_HIERARCHY_DISPLAY_OPTION
            = DisplayOption.INFO;
    private static final CSSAnalyzerColumnsOrder DEFAULT_CSS_ANALYZER_COLUMN_ORDER
            = CSSAnalyzerColumnsOrder.DEFAULTS_FIRST;

    private static final int DEFAULT_RECENT_ITEMS_SIZE = 15;

    // Global preferences
    private double documentHeight = DEFAULT_DOCUMENT_HEIGHT;
    private double documentWidth = DEFAULT_DOCUMENT_WIDTH;
    private BackgroundImage backgroundImage = DEFAULT_BACKGROUND_IMAGE;
    private Color alignmentGuidesColor = DEFAULT_ALIGNMENT_GUIDES_COLOR;
    private Color parentRingColor = DEFAULT_PARENT_RING_COLOR;
    private DisplayOption hierarchyDisplayOption = DEFAULT_HIERARCHY_DISPLAY_OPTION;
    private CSSAnalyzerColumnsOrder cssAnalyzerColumnOrder = DEFAULT_CSS_ANALYZER_COLUMN_ORDER;
    private int recentItemsSize = DEFAULT_RECENT_ITEMS_SIZE;
    private final List<String> recentItems = new ArrayList<>();

    private final Preferences applicationRootPreferences;

    final static Integer[] recentItemsSizes = {5, 10, 15, 20};

    public enum BackgroundImage {

        BACKGROUND_01 {

                    @Override
                    public String toString() {
                        return I18N.getString("prefs.background.value1");
                    }
                },
        BACKGROUND_02 {

                    @Override
                    public String toString() {
                        return I18N.getString("prefs.background.value2");
                    }
                },
        BACKGROUND_03 {

                    @Override
                    public String toString() {
                        return I18N.getString("prefs.background.value3");
                    }
                }
    }

    public enum CSSAnalyzerColumnsOrder {

        DEFAULTS_FIRST {

                    @Override
                    public String toString() {
                        return I18N.getString("prefs.cssanalyzer.columns.defaults.first");
                    }
                },
        DEFAULTS_LAST {

                    @Override
                    public String toString() {
                        return I18N.getString("prefs.cssanalyzer.columns.defaults.last");
                    }
                }
    }

    public PreferencesRecordGlobal(Preferences applicationRootPreferences) {
        this.applicationRootPreferences = applicationRootPreferences;
    }

    public double getDocumentHeight() {
        return documentHeight;
    }

    public void setDocumentHeight(double value) {
        documentHeight = value;
    }

    public double getDocumentWidth() {
        return documentWidth;
    }

    public void setDocumentWidth(double value) {
        documentWidth = value;
    }

    public BackgroundImage getBackgroundImage() {
        return backgroundImage;
    }

    public void setBackgroundImage(BackgroundImage value) {
        backgroundImage = value;
    }

    public Color getAlignmentGuidesColor() {
        return alignmentGuidesColor;
    }

    public void setAlignmentGuidesColor(Color value) {
        alignmentGuidesColor = value;
    }

    public Color getParentRingColor() {
        return parentRingColor;
    }

    public void setParentRingColor(Color value) {
        parentRingColor = value;
    }

    public DisplayOption getHierarchyDisplayOption() {
        return hierarchyDisplayOption;
    }

    public void setHierarchyDisplayOption(DisplayOption value) {
        hierarchyDisplayOption = value;
    }

    public CSSAnalyzerColumnsOrder getCSSAnalyzerColumnsOrder() {
        return cssAnalyzerColumnOrder;
    }

    public void setCSSAnalyzerColumnsOrder(CSSAnalyzerColumnsOrder value) {
        cssAnalyzerColumnOrder = value;
    }

    public int getRecentItemsSize() {
        return recentItemsSize;
    }

    public void setRecentItemsSize(int value) {
        recentItemsSize = value;
        // Remove last items depending on the size
        while (recentItems.size() > recentItemsSize) {
            recentItems.remove(recentItems.size() - 1);
        }
    }

    public List<String> getRecentItems() {
        return recentItems;
    }

    public void addRecentItem(File file) {
        // Add the specified file to the recent items at first position
        final String path = file.getAbsolutePath();
        if (recentItems.contains(path)) {
            recentItems.remove(path);
        }
        recentItems.add(0, path);
        // Remove last items depending on the size
        while (recentItems.size() > recentItemsSize) {
            recentItems.remove(recentItems.size() - 1);
        }
        writeToJavaPreferences(RECENT_ITEMS);
    }

    public void addRecentItems(List<File> files) {
        // Add the specified files to the recent items at first position
        for (File file : files) {
            final String path = file.getAbsolutePath();
            if (recentItems.contains(path)) {
                recentItems.remove(path);
            }
            recentItems.add(0, path);
        }
        // Remove last items depending on the size
        while (recentItems.size() > recentItemsSize) {
            recentItems.remove(recentItems.size() - 1);
        }
        writeToJavaPreferences(RECENT_ITEMS);
    }

    public void removeRecentItems(List<File> files) {
        // Remove the specified files from the recent items
        for (File file : files) {
            final String path = file.getAbsolutePath();
            recentItems.remove(path);
        }
        writeToJavaPreferences(RECENT_ITEMS);
    }

    public void clearRecentItems() {
        recentItems.clear();
        writeToJavaPreferences(RECENT_ITEMS);
    }

    public void refreshAlignmentGuidesColor(DocumentWindowController dwc) {
        final ContentPanelController cpc = dwc.getContentPanelController();
        cpc.setGuidesColor(alignmentGuidesColor);
    }

    public void refreshBackgroundImage(DocumentWindowController dwc) {
        // Background images
        final Image img1 = getImage(backgroundImage);
        final javafx.scene.layout.BackgroundImage bgi1
                = new javafx.scene.layout.BackgroundImage(img1,
                        BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                        BackgroundPosition.CENTER, BackgroundSize.DEFAULT);
        // Does the shadow image render something different ?
//        final Image img2 = getShadowImage();
//        final javafx.scene.layout.BackgroundImage bgi2
//                = new javafx.scene.layout.BackgroundImage(img2,
//                        BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
//                        BackgroundPosition.CENTER, BackgroundSize.DEFAULT);
//        final Background bg = new Background(bgi1, bgi2);
        final Background bg = new Background(bgi1);
        final ContentPanelController cpc = dwc.getContentPanelController();
        cpc.getWorkspacePane().setBackground(bg);
    }

    public void refreshCSSAnalyzerColumnsOrder(DocumentWindowController dwc) {

    }

    public void refreshHierarchyDisplayOption(DocumentWindowController dwc) {
        dwc.refreshHierarchyDisplayOption(hierarchyDisplayOption);
    }

    public void refreshParentRingColor(DocumentWindowController dwc) {
        final ContentPanelController cpc = dwc.getContentPanelController();
        cpc.setPringColor(parentRingColor);
        final AbstractHierarchyPanelController hpc = dwc.getHierarchyPanelController();
        hpc.setParentRingColor(parentRingColor);
    }

    public void refreshAlignmentGuidesColor() {
        final SceneBuilderApp app = SceneBuilderApp.getSingleton();
        for (DocumentWindowController dwc : app.getDocumentWindowControllers()) {
            refreshAlignmentGuidesColor(dwc);
        }
    }

    public void refreshBackgroundImage() {
        final SceneBuilderApp app = SceneBuilderApp.getSingleton();
        for (DocumentWindowController dwc : app.getDocumentWindowControllers()) {
            refreshBackgroundImage(dwc);
        }
    }

    public void refreshCSSAnalyzerColumnsOrder() {
        final SceneBuilderApp app = SceneBuilderApp.getSingleton();
        for (DocumentWindowController dwc : app.getDocumentWindowControllers()) {
            refreshCSSAnalyzerColumnsOrder(dwc);
        }
    }

    public void refreshHierarchyDisplayOption() {
        final SceneBuilderApp app = SceneBuilderApp.getSingleton();
        for (DocumentWindowController dwc : app.getDocumentWindowControllers()) {
            refreshHierarchyDisplayOption(dwc);
        }
    }

    public void refreshParentRingColor() {
        final SceneBuilderApp app = SceneBuilderApp.getSingleton();
        for (DocumentWindowController dwc : app.getDocumentWindowControllers()) {
            refreshParentRingColor(dwc);
        }
    }

    public void refresh(DocumentWindowController dwc) {
        refreshAlignmentGuidesColor(dwc);
        refreshBackgroundImage(dwc);
        refreshCSSAnalyzerColumnsOrder(dwc);
        refreshHierarchyDisplayOption(dwc);
        refreshParentRingColor(dwc);
    }

    /**
     * Read data from the java preferences DB and initialize properties.
     */
    public void readFromJavaPreferences() {

        assert applicationRootPreferences != null;

        // Document size
        final double height = applicationRootPreferences.getDouble(DOCUMENT_HEIGHT,
                DEFAULT_DOCUMENT_HEIGHT);
        setDocumentHeight(height);
        final double width = applicationRootPreferences.getDouble(DOCUMENT_WIDTH,
                DEFAULT_DOCUMENT_WIDTH);
        setDocumentWidth(width);

        // Background image
        final String image = applicationRootPreferences.get(BACKGROUND_IMAGE,
                DEFAULT_BACKGROUND_IMAGE.name());
        setBackgroundImage(BackgroundImage.valueOf(image));

        // Alignment guides color
        final String agColor = applicationRootPreferences.get(ALIGNMENT_GUIDES_COLOR,
                DEFAULT_ALIGNMENT_GUIDES_COLOR.toString());
        setAlignmentGuidesColor(Color.valueOf(agColor));

        // Parent ring color
        final String prColor = applicationRootPreferences.get(PARENT_RING_COLOR,
                DEFAULT_PARENT_RING_COLOR.toString());
        setParentRingColor(Color.valueOf(prColor));

        // Hierarchy display option
        final String displayOption = applicationRootPreferences.get(HIERARCHY_DISPLAY_OPTION,
                DEFAULT_HIERARCHY_DISPLAY_OPTION.name());
        setHierarchyDisplayOption(DisplayOption.valueOf(displayOption));

        // CSS analyzer column order
        final String order = applicationRootPreferences.get(CSS_ANALYZER_COLUMN_ORDER,
                DEFAULT_CSS_ANALYZER_COLUMN_ORDER.name());
        setCSSAnalyzerColumnsOrder(CSSAnalyzerColumnsOrder.valueOf(order));

        // Recent items size
        final int size = applicationRootPreferences.getInt(
                RECENT_ITEMS_SIZE, DEFAULT_RECENT_ITEMS_SIZE);
        setRecentItemsSize(size);

        // Recent items list
        final String items = applicationRootPreferences.get(RECENT_ITEMS, ""); //NOI18N
        assert recentItems.isEmpty();
        if (items.isEmpty() == false) {
            final String[] itemsArray = items.split("\\" + File.pathSeparator); //NOI18N
            assert itemsArray.length <= recentItemsSize;
            recentItems.addAll(Arrays.asList(itemsArray));
        }
    }

    public void writeToJavaPreferences(String key) {

        assert applicationRootPreferences != null;
        assert key != null;
        switch (key) {
            case DOCUMENT_HEIGHT:
                applicationRootPreferences.putDouble(DOCUMENT_HEIGHT, getDocumentHeight());
                break;
            case DOCUMENT_WIDTH:
                applicationRootPreferences.putDouble(DOCUMENT_WIDTH, getDocumentWidth());
                break;
            case BACKGROUND_IMAGE:
                applicationRootPreferences.put(BACKGROUND_IMAGE, getBackgroundImage().name());
                break;
            case ALIGNMENT_GUIDES_COLOR:
                applicationRootPreferences.put(ALIGNMENT_GUIDES_COLOR, getAlignmentGuidesColor().toString());
                break;
            case PARENT_RING_COLOR:
                applicationRootPreferences.put(PARENT_RING_COLOR, getParentRingColor().toString());
                break;
            case HIERARCHY_DISPLAY_OPTION:
                applicationRootPreferences.put(HIERARCHY_DISPLAY_OPTION, getHierarchyDisplayOption().name());
                break;
            case CSS_ANALYZER_COLUMN_ORDER:
                applicationRootPreferences.put(CSS_ANALYZER_COLUMN_ORDER, getCSSAnalyzerColumnsOrder().name());
                break;
            case RECENT_ITEMS_SIZE:
                applicationRootPreferences.putInt(RECENT_ITEMS_SIZE, getRecentItemsSize());
                break;
            case RECENT_ITEMS:
                final StringBuilder sb = new StringBuilder();
                for (String recentItem : getRecentItems()) {
                    sb.append(recentItem);
                    sb.append(File.pathSeparator);
                }
                applicationRootPreferences.put(RECENT_ITEMS, sb.toString());
                break;
            default:
                assert false;
                break;
        }
    }

    private static Image getImage(BackgroundImage bgi) {
        final URL url;
        switch (bgi) {
            case BACKGROUND_01:
                url = PreferencesRecordGlobal.class.getResource("Background-Blue-Grid.png"); //NOI18N
                break;
            case BACKGROUND_02:
                url = PreferencesRecordGlobal.class.getResource("Background-Neutral-Grid.png"); //NOI18N
                break;
            case BACKGROUND_03:
                url = PreferencesRecordGlobal.class.getResource("Background-Neutral-Uniform.png"); //NOI18N
                break;
            default:
                url = null;
                assert false;
                break;
        }
        assert url != null;
        return new Image(url.toExternalForm());
    }

//    private static Image getShadowImage() {
//        final URL url = PreferencesRecordGlobal.class.getResource("background-shadow.png"); //NOI18N
//        return new Image(url.toExternalForm());
//    }
}
