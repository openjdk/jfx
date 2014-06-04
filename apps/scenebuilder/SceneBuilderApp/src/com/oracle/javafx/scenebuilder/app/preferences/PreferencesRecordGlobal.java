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
import com.oracle.javafx.scenebuilder.app.SceneBuilderApp;
import com.oracle.javafx.scenebuilder.app.SceneBuilderApp.ApplicationControlAction;
import com.oracle.javafx.scenebuilder.app.SceneBuilderApp.ToolTheme;
import com.oracle.javafx.scenebuilder.app.i18n.I18N;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.ALIGNMENT_GUIDES_COLOR;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.BACKGROUND_IMAGE;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.CSS_TABLE_COLUMNS_ORDERING_REVERSED;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.ROOT_CONTAINER_HEIGHT;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.ROOT_CONTAINER_WIDTH;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.HIERARCHY_DISPLAY_OPTION;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.LIBRARY_DISPLAY_OPTION;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.PARENT_RING_COLOR;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.RECENT_ITEMS;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.RECENT_ITEMS_SIZE;
import static com.oracle.javafx.scenebuilder.app.preferences.PreferencesController.TOOL_THEME;
import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.ContentPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.AbstractHierarchyPanelController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.hierarchy.AbstractHierarchyPanelController.DisplayOption;
import com.oracle.javafx.scenebuilder.kit.editor.panel.library.LibraryPanelController.DISPLAY_MODE;
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
    static final double DEFAULT_ROOT_CONTAINER_HEIGHT = 400;
    static final double DEFAULT_ROOT_CONTAINER_WIDTH = 600;

    static final BackgroundImage DEFAULT_BACKGROUND_IMAGE
            = BackgroundImage.BACKGROUND_03;
    static final Color DEFAULT_ALIGNMENT_GUIDES_COLOR = Color.RED;
    static final Color DEFAULT_PARENT_RING_COLOR = Color.rgb(238, 168, 47);

    static final ToolTheme DEFAULT_TOOL_THEME = ToolTheme.DEFAULT;
    static final DISPLAY_MODE DEFAULT_LIBRARY_DISPLAY_OPTION
            = DISPLAY_MODE.SECTIONS;
    static final DisplayOption DEFAULT_HIERARCHY_DISPLAY_OPTION
            = DisplayOption.INFO;
    static final boolean DEFAULT_CSS_TABLE_COLUMNS_ORDERING_REVERSED = false;

    static final int DEFAULT_RECENT_ITEMS_SIZE = 15;

    // Global preferences
    private double rootContainerHeight = DEFAULT_ROOT_CONTAINER_HEIGHT;
    private double rootContainerWidth = DEFAULT_ROOT_CONTAINER_WIDTH;
    private BackgroundImage backgroundImage = DEFAULT_BACKGROUND_IMAGE;
    private Color alignmentGuidesColor = DEFAULT_ALIGNMENT_GUIDES_COLOR;
    private Color parentRingColor = DEFAULT_PARENT_RING_COLOR;
    private ToolTheme toolTheme = DEFAULT_TOOL_THEME;
    private DISPLAY_MODE libraryDisplayOption = DEFAULT_LIBRARY_DISPLAY_OPTION;
    private DisplayOption hierarchyDisplayOption = DEFAULT_HIERARCHY_DISPLAY_OPTION;
    private boolean cssTableColumnsOrderingReversed = DEFAULT_CSS_TABLE_COLUMNS_ORDERING_REVERSED;
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

    public double getRootContainerHeight() {
        return rootContainerHeight;
    }

    public void setRootContainerHeight(double value) {
        rootContainerHeight = value;
    }

    public double getRootContainerWidth() {
        return rootContainerWidth;
    }

    public void setRootContainerWidth(double value) {
        rootContainerWidth = value;
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

    public ToolTheme getToolTheme() {
        return toolTheme;
    }
    
    public void setToolTheme(ToolTheme value) {
        toolTheme = value;
    }
    
    public DISPLAY_MODE getLibraryDisplayOption() {
        return libraryDisplayOption;
    }

    public void setLibraryDisplayOption(DISPLAY_MODE value) {
        libraryDisplayOption = value;
    }

    public DisplayOption getHierarchyDisplayOption() {
        return hierarchyDisplayOption;
    }

    public void setHierarchyDisplayOption(DisplayOption value) {
        hierarchyDisplayOption = value;
    }

    public CSSAnalyzerColumnsOrder getDefaultCSSAnalyzerColumnsOrder() {
        if (DEFAULT_CSS_TABLE_COLUMNS_ORDERING_REVERSED) {
            return CSSAnalyzerColumnsOrder.DEFAULTS_LAST;
        } else {
            return CSSAnalyzerColumnsOrder.DEFAULTS_FIRST;
        }
    }
    
    public CSSAnalyzerColumnsOrder getCSSAnalyzerColumnsOrder() {
        if (isCssTableColumnsOrderingReversed()) {
            return CSSAnalyzerColumnsOrder.DEFAULTS_LAST;
        } else {
            return CSSAnalyzerColumnsOrder.DEFAULTS_FIRST;
        }
    }

    public void setCSSAnalyzerColumnsOrder(CSSAnalyzerColumnsOrder value) {
        switch (value) {
            case DEFAULTS_FIRST:
                setCssTableColumnsOrderingReversed(false);
                break;
            case DEFAULTS_LAST:
                setCssTableColumnsOrderingReversed(true);
                break;
            default:
                assert false;
        }
    }

    public boolean isCssTableColumnsOrderingReversed() {
        return cssTableColumnsOrderingReversed;
    }

    public void setCssTableColumnsOrderingReversed(boolean value) {
        cssTableColumnsOrderingReversed = value;
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
        final List<File> files = new ArrayList<>();
        files.add(file);
        addRecentItems(files);
    }

    public void addRecentItems(List<File> files) {
        for (File file : files) {
            final String path = file.getPath();
            if (recentItems.contains(path)) {
                recentItems.remove(path);
            }
            // Add the specified file to the recent items at first position
            recentItems.add(0, path);
        }
        // Remove last items depending on the size
        while (recentItems.size() > recentItemsSize) {
            recentItems.remove(recentItems.size() - 1);
        }
        writeToJavaPreferences(RECENT_ITEMS);
    }

    public void removeRecentItems(List<String> filePaths) {
        // Remove the specified files from the recent items
        for (String filePath : filePaths) {
            recentItems.remove(filePath);
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
        dwc.refreshCssTableColumnsOrderingReversed(cssTableColumnsOrderingReversed);
    }

    public void refreshToolTheme(DocumentWindowController dwc) {
        final SceneBuilderApp app = SceneBuilderApp.getSingleton();
        final ApplicationControlAction aca;
        switch(toolTheme) {
            case DEFAULT:
                aca = ApplicationControlAction.USE_DEFAULT_THEME;
                break;
            case DARK:
                aca = ApplicationControlAction.USE_DARK_THEME;
                break;
            default:
                assert false;
                aca = null;
                break;
         }
        app.performControlAction(aca, dwc);
    }
    
    public void refreshLibraryDisplayOption(DocumentWindowController dwc) {
        dwc.refreshLibraryDisplayOption(libraryDisplayOption);
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

    public void refreshRootContainerHeight(DocumentWindowController dwc) {
        final EditorController ec = dwc.getEditorController();
        ec.setDefaultRootContainerHeight(rootContainerHeight);
    }

    public void refreshRootContainerWidth(DocumentWindowController dwc) {
        final EditorController ec = dwc.getEditorController();
        ec.setDefaultRootContainerWidth(rootContainerWidth);
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

    public void refreshToolTheme() {
        refreshToolTheme(null);
    }

    public void refreshLibraryDisplayOption() {
        final SceneBuilderApp app = SceneBuilderApp.getSingleton();
        for (DocumentWindowController dwc : app.getDocumentWindowControllers()) {
            refreshLibraryDisplayOption(dwc);
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

    public void refreshRootContainerHeight() {
        final SceneBuilderApp app = SceneBuilderApp.getSingleton();
        for (DocumentWindowController dwc : app.getDocumentWindowControllers()) {
            refreshRootContainerHeight(dwc);
        }
    }

    public void refreshRootContainerWidth() {
        final SceneBuilderApp app = SceneBuilderApp.getSingleton();
        for (DocumentWindowController dwc : app.getDocumentWindowControllers()) {
            refreshRootContainerWidth(dwc);
        }
    }

    public void refresh(DocumentWindowController dwc) {
        refreshAlignmentGuidesColor(dwc);
        refreshBackgroundImage(dwc);
        refreshCSSAnalyzerColumnsOrder(dwc);
        refreshToolTheme(dwc);
        refreshLibraryDisplayOption(dwc);
        refreshHierarchyDisplayOption(dwc);
        refreshParentRingColor(dwc);
        refreshRootContainerHeight(dwc);
        refreshRootContainerWidth(dwc);
    }
    
    /**
     * Read data from the java preferences DB and initialize properties.
     */
    public void readFromJavaPreferences() {

        assert applicationRootPreferences != null;

        // Document size
        final double height = applicationRootPreferences.getDouble(ROOT_CONTAINER_HEIGHT,
                DEFAULT_ROOT_CONTAINER_HEIGHT);
        setRootContainerHeight(height);
        final double width = applicationRootPreferences.getDouble(ROOT_CONTAINER_WIDTH,
                DEFAULT_ROOT_CONTAINER_WIDTH);
        setRootContainerWidth(width);

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

        // Tool Theme
        final String tool_theme = applicationRootPreferences.get(TOOL_THEME,
                DEFAULT_TOOL_THEME.name());
        setToolTheme(ToolTheme.valueOf(tool_theme));

        // Library display option
        final String library_DisplayOption = applicationRootPreferences.get(LIBRARY_DISPLAY_OPTION,
                DEFAULT_LIBRARY_DISPLAY_OPTION.name());
        setLibraryDisplayOption(DISPLAY_MODE.valueOf(library_DisplayOption));

        // Hierarchy display option
        final String hierarchy_DisplayOption = applicationRootPreferences.get(HIERARCHY_DISPLAY_OPTION,
                DEFAULT_HIERARCHY_DISPLAY_OPTION.name());
        setHierarchyDisplayOption(DisplayOption.valueOf(hierarchy_DisplayOption));

        // CSS analyzer column order
        final boolean reversed = applicationRootPreferences.getBoolean(
                CSS_TABLE_COLUMNS_ORDERING_REVERSED, DEFAULT_CSS_TABLE_COLUMNS_ORDERING_REVERSED);
        setCssTableColumnsOrderingReversed(reversed);

        // Recent items size
        final int size = applicationRootPreferences.getInt(
                RECENT_ITEMS_SIZE, DEFAULT_RECENT_ITEMS_SIZE);
        setRecentItemsSize(size);

        // Recent items list
        final String items = applicationRootPreferences.get(RECENT_ITEMS, null);
        assert recentItems.isEmpty();
        if (items != null) {
            final String[] itemsArray = items.split("\\" + File.pathSeparator); //NOI18N
            assert itemsArray.length <= recentItemsSize;
            recentItems.addAll(Arrays.asList(itemsArray));
        }
    }

    public void writeToJavaPreferences(String key) {

        assert applicationRootPreferences != null;
        assert key != null;
        switch (key) {
            case ROOT_CONTAINER_HEIGHT:
                applicationRootPreferences.putDouble(ROOT_CONTAINER_HEIGHT, getRootContainerHeight());
                break;
            case ROOT_CONTAINER_WIDTH:
                applicationRootPreferences.putDouble(ROOT_CONTAINER_WIDTH, getRootContainerWidth());
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
            case TOOL_THEME:
                applicationRootPreferences.put(TOOL_THEME, getToolTheme().name());
                break;
            case LIBRARY_DISPLAY_OPTION:
                applicationRootPreferences.put(LIBRARY_DISPLAY_OPTION, getLibraryDisplayOption().name());
                break;
            case HIERARCHY_DISPLAY_OPTION:
                applicationRootPreferences.put(HIERARCHY_DISPLAY_OPTION, getHierarchyDisplayOption().name());
                break;
            case CSS_TABLE_COLUMNS_ORDERING_REVERSED:
                applicationRootPreferences.putBoolean(CSS_TABLE_COLUMNS_ORDERING_REVERSED, isCssTableColumnsOrderingReversed());
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
