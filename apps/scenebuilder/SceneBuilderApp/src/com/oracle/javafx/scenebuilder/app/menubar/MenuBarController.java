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
package com.oracle.javafx.scenebuilder.app.menubar;

import com.oracle.javafx.scenebuilder.app.DocumentWindowController;
import com.oracle.javafx.scenebuilder.app.DocumentWindowController.DocumentControlAction;
import com.oracle.javafx.scenebuilder.app.DocumentWindowController.DocumentEditAction;
import com.oracle.javafx.scenebuilder.app.SceneBuilderApp;
import com.oracle.javafx.scenebuilder.app.SceneBuilderApp.ApplicationControlAction;
import com.oracle.javafx.scenebuilder.app.i18n.I18N;
import com.oracle.javafx.scenebuilder.app.preferences.PreferencesController;
import com.oracle.javafx.scenebuilder.app.preferences.PreferencesRecordGlobal;
import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.EditorController.ControlAction;
import com.oracle.javafx.scenebuilder.kit.editor.EditorController.EditAction;
import com.oracle.javafx.scenebuilder.kit.editor.EditorController.Size;
import com.oracle.javafx.scenebuilder.kit.editor.EditorPlatform;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.ContentPanelController;
import com.oracle.javafx.scenebuilder.kit.library.BuiltinLibrary;
import com.oracle.javafx.scenebuilder.kit.library.BuiltinSectionComparator;
import com.oracle.javafx.scenebuilder.kit.library.LibraryItem;
import com.oracle.javafx.scenebuilder.kit.library.LibraryItemNameComparator;
import com.oracle.javafx.scenebuilder.kit.library.user.UserLibrary;
import com.oracle.javafx.scenebuilder.kit.util.MathUtils;
import com.oracle.javafx.scenebuilder.kit.util.control.effectpicker.EffectPicker;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.effect.Effect;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.StackPane;

/**
 *
 */
public class MenuBarController {
    
    private static MenuBarController systemMenuBarController; // For Mac only

    private Menu insertCustomMenu;
    private final DocumentWindowController documentWindowController;
    // This member is null when this MenuBarController is used for
    // managing the menu bar passed to MenuBarSkin.setDefaultSystemMenu().

    private DebugMenuController debugMenuController; // Initialized lazily

    @FXML
    private MenuBar menuBar;
    @FXML
    private Menu insertMenu;
    @FXML
    private Menu addEffectMenu;
    @FXML
    private Menu fileMenu; // Useless as soon as Preferences menu item is implemented
    @FXML
    private Menu previewMenu;
    @FXML
    private Menu windowMenu;

    // File
    @FXML
    private MenuItem newMenuItem;
    @FXML
    private MenuItem newAlertDialogMenuItem;
    @FXML
    private MenuItem newAlertDialogCssMenuItem;
    @FXML
    private MenuItem newAlertDialogI18nMenuItem;
    @FXML
    private MenuItem newBasicAppMenuItem;
    @FXML
    private MenuItem newBasicAppCssMenuItem;
    @FXML
    private MenuItem newBasicAppI18nMenuItem;
    @FXML
    private MenuItem newComplexAppMenuItem;
    @FXML
    private MenuItem newComplexAppCssMenuItem;
    @FXML
    private MenuItem newComplexAppI18nMenuItem;
    @FXML
    private MenuItem openMenuItem;
    @FXML
    private Menu openRecentMenu;
    @FXML
    private MenuItem saveMenuItem;
    @FXML
    private MenuItem saveAsMenuItem;
    @FXML
    private MenuItem revertMenuItem;
    @FXML
    private MenuItem closeMenuItem;
    @FXML
    private MenuItem revealMenuItem;
    @FXML
    private MenuItem importFxmlMenuItem;
    @FXML
    private MenuItem importMediaMenuItem;
    @FXML
    private MenuItem includeFileMenuItem;
    @FXML
    private MenuItem editIncludedFileMenuItem;
    @FXML
    private MenuItem revealIncludedFileMenuItem;
    @FXML
    private MenuItem showPreferencesMenuItem;
    @FXML
    private MenuItem exitMenuItem;

    // Edit
    @FXML
    private MenuItem undoMenuItem;
    @FXML
    private MenuItem redoMenuItem;
    @FXML
    private MenuItem copyMenuItem;
    @FXML
    private MenuItem cutMenuItem;
    @FXML
    private MenuItem pasteMenuItem;
    @FXML
    private MenuItem pasteIntoMenuItem;
    @FXML
    private MenuItem duplicateMenuItem;
    @FXML
    private MenuItem deleteMenuItem;
    @FXML
    private MenuItem selectAllMenuItem;
    @FXML
    private MenuItem selectNoneMenuItem;
    @FXML
    private MenuItem selectParentMenuItem;
    @FXML
    private MenuItem selectNextMenuItem;
    @FXML
    private MenuItem selectPreviousMenuItem;
    @FXML
    private MenuItem trimMenuItem;

    // View
    @FXML
    private MenuItem gotoContentMenuItem;
    @FXML
    private MenuItem gotoPropertiesMenuItem;
    @FXML
    private MenuItem gotoLayoutMenuItem;
    @FXML
    private MenuItem gotoCodeMenuItem;
    @FXML
    private MenuItem toggleLibraryPanelMenuItem;
    @FXML
    private MenuItem toggleHierarchyPanelMenuItem;
    @FXML
    private MenuItem toggleCSSPanelMenuItem;
    @FXML
    private MenuItem toggleLeftPanelMenuItem;
    @FXML
    private MenuItem toggleRightPanelMenuItem;
    @FXML
    private MenuItem toggleOutlinesMenuItem;
    @FXML
    private MenuItem toggleSampleDataMenuItem;
    @FXML
    private MenuItem toggleAlignmentGuidesMenuItem;
    @FXML
    private MenuItem showSampleControllerMenuItem;
    @FXML
    private Menu zoomMenu;

    // Modify
    @FXML
    private MenuItem fitToParentMenuItem;
    @FXML
    private MenuItem useComputedSizesMenuItem;
    @FXML
    private MenuItem addContextMenuMenuItem;
    @FXML
    private MenuItem addTooltipMenuItem;
    @FXML
    private MenuItem moveRowAboveMenuItem;
    @FXML
    private MenuItem moveRowBelowMenuItem;
    @FXML
    private MenuItem moveColumnBeforeMenuItem;
    @FXML
    private MenuItem moveColumnAfterMenuItem;
    @FXML
    private MenuItem addRowAboveMenuItem;
    @FXML
    private MenuItem addRowBelowMenuItem;
    @FXML
    private MenuItem addColumnBeforeMenuItem;
    @FXML
    private MenuItem addColumnAfterMenuItem;
    @FXML
    private MenuItem increaseRowSpanMenuItem;
    @FXML
    private MenuItem decreaseRowSpanMenuItem;
    @FXML
    private MenuItem increaseColumnSpanMenuItem;
    @FXML
    private MenuItem decreaseColumnSpanMenuItem;
    @FXML
    private RadioMenuItem qvgaSetSizeMenuItem;
    @FXML
    private RadioMenuItem vgaSetSizeMenuItem;
    @FXML
    private RadioMenuItem touchSetSizeMenuItem;
    @FXML
    private RadioMenuItem hdSetSizeMenuItem;

    // Arrange
    @FXML
    private MenuItem bringToFrontMenuItem;
    @FXML
    private MenuItem sendToBackMenuItem;
    @FXML
    private MenuItem bringForwardMenuItem;
    @FXML
    private MenuItem sendBackwardMenuItem;
    @FXML
    private MenuItem wrapInAnchorPaneMenuItem;
    @FXML
    private MenuItem wrapInBorderPaneMenuItem;
    @FXML
    private MenuItem wrapInDialogPaneMenuItem;
    @FXML
    private MenuItem wrapInFlowPaneMenuItem;
    @FXML
    private MenuItem wrapInGridPaneMenuItem;
    @FXML
    private MenuItem wrapInHBoxMenuItem;
    @FXML
    private MenuItem wrapInPaneMenuItem;
    @FXML
    private MenuItem wrapInScrollPaneMenuItem;
    @FXML
    private MenuItem wrapInSplitPaneMenuItem;
    @FXML
    private MenuItem wrapInStackPaneMenuItem;
    @FXML
    private MenuItem wrapInTabPaneMenuItem;
    @FXML
    private MenuItem wrapInTextFlowMenuItem;
    @FXML
    private MenuItem wrapInTilePaneMenuItem;
    @FXML
    private MenuItem wrapInTitledPaneMenuItem;
    @FXML
    private MenuItem wrapInToolBarMenuItem;
    @FXML
    private MenuItem wrapInVBoxMenuItem;
    @FXML
    private MenuItem wrapInGroupMenuItem;
    @FXML
    private MenuItem unwrapMenuItem;

    // Preview
    @FXML
    private MenuItem showPreviewInWindowMenuItem;
    @FXML
    private MenuItem showPreviewInDialogMenuItem;
    @FXML
    private RadioMenuItem modenaThemeMenuItem;
    @FXML
    private RadioMenuItem modenaTouchThemeMenuItem;
    @FXML
    private RadioMenuItem modenaHighContrastBlackonwhiteThemeMenuItem;
    @FXML
    private RadioMenuItem modenaHighContrastWhiteonblackThemeMenuItem;
    @FXML
    private RadioMenuItem modenaHighContrastYellowonblackThemeMenuItem;
    @FXML
    private RadioMenuItem caspianThemeMenuItem;
    @FXML
    private CheckMenuItem caspianHighContrastThemeMenuItem;
    @FXML
    private RadioMenuItem caspianEmbeddedThemeMenuItem;
    @FXML
    private RadioMenuItem caspianEmbeddedQVGAThemeMenuItem;
    @FXML
    private MenuItem addSceneStyleSheetMenuItem;
    @FXML
    private Menu removeSceneStyleSheetMenu;
    @FXML
    private Menu openSceneStyleSheetMenu;
    @FXML
    private MenuItem setResourceMenuItem;
    @FXML
    private MenuItem removeResourceMenuItem;
    @FXML
    private MenuItem revealResourceMenuItem;
    @FXML
    private RadioMenuItem qvgaPreviewSizeMenuItem;
    @FXML
    private RadioMenuItem vgaPreviewSizeMenuItem;
    @FXML
    private RadioMenuItem touchPreviewSizeMenuItem;
    @FXML
    private RadioMenuItem hdPreviewSizeMenuItem;
    @FXML
    private RadioMenuItem preferredPreviewSizeMenuItem;

    // Window
    // Help
    @FXML
    private MenuItem helpMenuItem;
    @FXML
    private MenuItem aboutMenuItem;

    private static final KeyCombination.Modifier modifier;
    private final Map<KeyCombination, MenuItem> keyToMenu = new HashMap<>();

    static {
        if (EditorPlatform.IS_MAC) {
            modifier = KeyCombination.META_DOWN;
        } else {
            // Should cover Windows, Solaris, Linux
            modifier = KeyCombination.CONTROL_DOWN;
        }
    }

    public MenuBarController(DocumentWindowController documentWindowController) {
        this.documentWindowController = documentWindowController;
    }

    public MenuBar getMenuBar() {

        if (menuBar == null) {
            final URL fxmlURL = MenuBarController.class.getResource("MenuBar.fxml"); //NOI18N
            final FXMLLoader loader = new FXMLLoader();

            loader.setController(this);
            loader.setLocation(fxmlURL);
            loader.setResources(I18N.getBundle());
            try {
                loader.load();
                controllerDidLoadFxml();
            } catch (RuntimeException | IOException x) {
                System.out.println("loader.getController()=" + loader.getController()); //NOI18N
                System.out.println("loader.getLocation()=" + loader.getLocation()); //NOI18N
                throw new RuntimeException("Failed to load " + fxmlURL.getFile(), x); //NOI18N
            }
        }

        return menuBar;
    }

    public void setDebugMenuVisible(boolean visible) {
        if (isDebugMenuVisible() != visible) {
            if (visible) {
                if (debugMenuController == null) {
                    debugMenuController = new DebugMenuController(documentWindowController);
                }
                menuBar.getMenus().add(debugMenuController.getMenu());
            } else {
                menuBar.getMenus().remove(debugMenuController.getMenu());
            }
        }
    }

    public boolean isDebugMenuVisible() {
        final boolean result;
        if (debugMenuController == null) {
            result = false;
        } else {
            result = menuBar.getMenus().contains(debugMenuController.getMenu());
        }
        return result;
    }
    
    
    public static synchronized MenuBarController getSystemMenuBarController() {
        if (systemMenuBarController == null) {
            systemMenuBarController = new MenuBarController(null);
        }
        return systemMenuBarController;
    }
    
    /*
     * Private
     */
    private void controllerDidLoadFxml() {

        assert menuBar != null;
        assert menuBar.getParent() instanceof StackPane;
        assert insertMenu != null;
        assert addEffectMenu != null;
        assert fileMenu != null;
        assert windowMenu != null;

        assert newMenuItem != null;
        assert newAlertDialogMenuItem != null;
        assert newAlertDialogCssMenuItem != null;
        assert newAlertDialogI18nMenuItem != null;
        assert newBasicAppMenuItem != null;
        assert newBasicAppCssMenuItem != null;
        assert newBasicAppI18nMenuItem != null;
        assert newComplexAppMenuItem != null;
        assert newComplexAppCssMenuItem != null;
        assert newComplexAppI18nMenuItem != null;
        assert openMenuItem != null;
        assert openRecentMenu != null;
        assert saveMenuItem != null;
        assert saveAsMenuItem != null;
        assert revertMenuItem != null;
        assert closeMenuItem != null;
        assert revealMenuItem != null;
        assert importFxmlMenuItem != null;
        assert importMediaMenuItem != null;
        assert includeFileMenuItem != null;
        assert editIncludedFileMenuItem != null;
        assert revealIncludedFileMenuItem != null;
        assert showPreferencesMenuItem != null;
        assert exitMenuItem != null;

        assert undoMenuItem != null;
        assert redoMenuItem != null;
        assert copyMenuItem != null;
        assert cutMenuItem != null;
        assert pasteMenuItem != null;
        assert pasteIntoMenuItem != null;
        assert duplicateMenuItem != null;
        assert deleteMenuItem != null;
        assert selectAllMenuItem != null;
        assert selectNoneMenuItem != null;
        assert selectParentMenuItem != null;
        assert selectNextMenuItem != null;
        assert selectPreviousMenuItem != null;
        assert trimMenuItem != null;

        assert gotoContentMenuItem != null;
        assert gotoPropertiesMenuItem != null;
        assert gotoLayoutMenuItem != null;
        assert gotoCodeMenuItem != null;
        assert toggleLibraryPanelMenuItem != null;
        assert toggleHierarchyPanelMenuItem != null;
        assert toggleCSSPanelMenuItem != null;
        assert toggleLeftPanelMenuItem != null;
        assert toggleRightPanelMenuItem != null;
        assert toggleOutlinesMenuItem != null;
        assert toggleSampleDataMenuItem != null;
        assert toggleAlignmentGuidesMenuItem != null;
        assert showSampleControllerMenuItem != null;
        assert zoomMenu != null;
        assert zoomMenu.getItems().isEmpty();

        assert fitToParentMenuItem != null;
        assert useComputedSizesMenuItem != null;
        assert addContextMenuMenuItem != null;
        assert addTooltipMenuItem != null;
        assert moveRowAboveMenuItem != null;
        assert moveRowBelowMenuItem != null;
        assert moveColumnBeforeMenuItem != null;
        assert moveColumnAfterMenuItem != null;
        assert addRowAboveMenuItem != null;
        assert addRowBelowMenuItem != null;
        assert addColumnBeforeMenuItem != null;
        assert addColumnAfterMenuItem != null;
        assert increaseRowSpanMenuItem != null;
        assert decreaseRowSpanMenuItem != null;
        assert increaseColumnSpanMenuItem != null;
        assert decreaseColumnSpanMenuItem != null;
        assert qvgaSetSizeMenuItem != null;
        assert vgaSetSizeMenuItem != null;
        assert touchSetSizeMenuItem != null;
        assert hdSetSizeMenuItem != null;

        assert bringToFrontMenuItem != null;
        assert sendToBackMenuItem != null;
        assert bringForwardMenuItem != null;
        assert sendBackwardMenuItem != null;
        assert wrapInAnchorPaneMenuItem != null;
        assert wrapInBorderPaneMenuItem != null;
        assert wrapInDialogPaneMenuItem != null;
        assert wrapInFlowPaneMenuItem != null;
        assert wrapInGridPaneMenuItem != null;
        assert wrapInHBoxMenuItem != null;
        assert wrapInPaneMenuItem != null;
        assert wrapInScrollPaneMenuItem != null;
        assert wrapInSplitPaneMenuItem != null;
        assert wrapInStackPaneMenuItem != null;
        assert wrapInTabPaneMenuItem != null;
        assert wrapInTextFlowMenuItem != null;
        assert wrapInTilePaneMenuItem != null;
        assert wrapInTitledPaneMenuItem != null;
        assert wrapInToolBarMenuItem != null;
        assert wrapInVBoxMenuItem != null;
        assert wrapInGroupMenuItem != null;
        assert unwrapMenuItem != null;

        assert showPreviewInWindowMenuItem != null;
        assert showPreviewInDialogMenuItem != null;
        assert modenaThemeMenuItem != null;
        assert modenaTouchThemeMenuItem != null;
        assert modenaHighContrastBlackonwhiteThemeMenuItem != null;
        assert modenaHighContrastWhiteonblackThemeMenuItem != null;
        assert modenaHighContrastYellowonblackThemeMenuItem != null;
        assert caspianThemeMenuItem != null;
        assert caspianHighContrastThemeMenuItem != null;
        assert caspianEmbeddedThemeMenuItem != null;
        assert caspianEmbeddedQVGAThemeMenuItem != null;
        assert addSceneStyleSheetMenuItem != null;
        assert removeSceneStyleSheetMenu != null;
        assert openSceneStyleSheetMenu != null;
        assert setResourceMenuItem != null;
        assert removeResourceMenuItem != null;
        assert revealResourceMenuItem != null;
        assert qvgaPreviewSizeMenuItem != null;
        assert vgaPreviewSizeMenuItem != null;
        assert touchPreviewSizeMenuItem != null;
        assert hdPreviewSizeMenuItem != null;
        assert preferredPreviewSizeMenuItem != null;

        assert helpMenuItem != null;
        assert aboutMenuItem != null;

        /* 
         * To make MenuBar.fxml editable with SB 1.1, the menu bar is enclosed
         * in a StackPane. This stack pane is useless now.
         * So we unwrap the menu bar and make it the panel root.
         */
        final StackPane rootStackPane = (StackPane) menuBar.getParent();
        rootStackPane.getChildren().remove(menuBar);

        /*
         * On Mac, move the menu bar on the desktop and remove the Quit item
         * from the File menu
         */
        if (EditorPlatform.IS_MAC) {
            menuBar.setUseSystemMenuBar(true);
            exitMenuItem.getParentMenu().getItems().remove(exitMenuItem);
        }

        /*
         * Setup title of the Reveal menu item according the underlying o/s.
         */
        final String revealMenuKey;
        if (EditorPlatform.IS_MAC) {
            revealMenuKey = "menu.title.reveal.mac";
        } else if (EditorPlatform.IS_WINDOWS) {
            revealMenuKey = "menu.title.reveal.win.mnemonic";
        } else {
            assert EditorPlatform.IS_LINUX;
            revealMenuKey = "menu.title.reveal.linux";
        }
        revealMenuItem.setText(I18N.getString(revealMenuKey));

        /*
         * File menu
         */
        newMenuItem.setUserData(new ApplicationControlActionController(ApplicationControlAction.NEW_FILE));
        newMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.N, modifier));
        newAlertDialogMenuItem.setUserData(new ApplicationControlActionController(ApplicationControlAction.NEW_ALERT_DIALOG));
        newAlertDialogCssMenuItem.setUserData(new ApplicationControlActionController(ApplicationControlAction.NEW_ALERT_DIALOG_CSS));
        newAlertDialogI18nMenuItem.setUserData(new ApplicationControlActionController(ApplicationControlAction.NEW_ALERT_DIALOG_I18N));
        newBasicAppMenuItem.setUserData(new ApplicationControlActionController(ApplicationControlAction.NEW_BASIC_APPLICATION));
        newBasicAppCssMenuItem.setUserData(new ApplicationControlActionController(ApplicationControlAction.NEW_BASIC_APPLICATION_CSS));
        newBasicAppI18nMenuItem.setUserData(new ApplicationControlActionController(ApplicationControlAction.NEW_BASIC_APPLICATION_I18N));
        newComplexAppMenuItem.setUserData(new ApplicationControlActionController(ApplicationControlAction.NEW_COMPLEX_APPLICATION));
        newComplexAppCssMenuItem.setUserData(new ApplicationControlActionController(ApplicationControlAction.NEW_COMPLEX_APPLICATION_CSS));
        newComplexAppI18nMenuItem.setUserData(new ApplicationControlActionController(ApplicationControlAction.NEW_COMPLEX_APPLICATION_I18N));
        openMenuItem.setUserData(new ApplicationControlActionController(ApplicationControlAction.OPEN_FILE));
        openMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.O, modifier));
        openRecentMenu.setOnShowing(t -> updateOpenRecentMenuItems());
        saveMenuItem.setUserData(new DocumentControlActionController(DocumentControlAction.SAVE_FILE));
        saveMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.S, modifier));
        saveAsMenuItem.setUserData(new DocumentControlActionController(DocumentControlAction.SAVE_AS_FILE));
        saveAsMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHIFT_DOWN, modifier));
        revertMenuItem.setUserData(new DocumentControlActionController(DocumentControlAction.REVERT_FILE));
        revealMenuItem.setUserData(new DocumentControlActionController(DocumentControlAction.REVEAL_FILE));
        importFxmlMenuItem.setUserData(new DocumentEditActionController(DocumentEditAction.IMPORT_FXML));
        importMediaMenuItem.setUserData(new DocumentEditActionController(DocumentEditAction.IMPORT_MEDIA));
        includeFileMenuItem.setUserData(new DocumentEditActionController(DocumentEditAction.INCLUDE_FXML));
        editIncludedFileMenuItem.setUserData(new ControlActionController(ControlAction.EDIT_INCLUDED_FILE) {

            @Override
            public String getTitle() {
                String title = I18N.getString("menu.title.edit.included.default");
                if (documentWindowController != null) {
                    final File file = documentWindowController.getEditorController().getIncludedFile();
                    if (file != null) {
                        title = I18N.getString("menu.title.edit.included", file.getName());
                    }
                }
                return title;
            }
        });
        revealIncludedFileMenuItem.setUserData(new ControlActionController(ControlAction.REVEAL_INCLUDED_FILE) {

            @Override
            public String getTitle() {
                String title = I18N.getString("menu.title.reveal.included.default");
                if (documentWindowController != null) {
                    final File file = documentWindowController.getEditorController().getIncludedFile();
                    if (file != null) {
                        if (EditorPlatform.IS_MAC) {
                            title = I18N.getString("menu.title.reveal.included.finder", file.getName());
                        } else {
                            title = I18N.getString("menu.title.reveal.included.explorer", file.getName());
                        }
                    }
                }
                return title;
            }
        });
        closeMenuItem.setUserData(new ApplicationControlActionController(ApplicationControlAction.CLOSE_FRONT_WINDOW));
        closeMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.W, modifier));
        showPreferencesMenuItem.setUserData(new ApplicationControlActionController(ApplicationControlAction.SHOW_PREFERENCES));
        showPreferencesMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.COMMA, modifier));
        exitMenuItem.setUserData(new ApplicationControlActionController(ApplicationControlAction.EXIT));

        /*
         * Edit menu
         */
        undoMenuItem.setUserData(new UndoActionController());
        undoMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Z, modifier));
        redoMenuItem.setUserData(new RedoActionController());
        if (EditorPlatform.IS_MAC) {
            // Mac platforms.
            redoMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.SHIFT_DOWN, modifier));
        } else {
            // Windows and Linux platforms.
            // http://windows.microsoft.com/en-US/windows7/Keyboard-shortcuts
            redoMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Y, modifier));
        }
        copyMenuItem.setUserData(new DocumentControlActionController(DocumentControlAction.COPY));
        copyMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.C, modifier));
        cutMenuItem.setUserData(new DocumentEditActionController(DocumentEditAction.CUT));
        cutMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.X, modifier));
        pasteMenuItem.setUserData(new DocumentEditActionController(DocumentEditAction.PASTE));
        pasteMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.V, modifier));
        pasteIntoMenuItem.setUserData(new EditActionController(EditAction.PASTE_INTO));
        pasteIntoMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.V, KeyCombination.SHIFT_DOWN, modifier));
        duplicateMenuItem.setUserData(new EditActionController(EditAction.DUPLICATE));
        duplicateMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.D, modifier));
        deleteMenuItem.setUserData(new DocumentEditActionController(DocumentEditAction.DELETE));
        deleteMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.DELETE));
        selectAllMenuItem.setUserData(new DocumentControlActionController(DocumentControlAction.SELECT_ALL));
        selectAllMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.A, modifier));
        selectNoneMenuItem.setUserData(new DocumentControlActionController(DocumentControlAction.SELECT_NONE));
        selectNoneMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.A, KeyCombination.SHIFT_DOWN, modifier));
        selectParentMenuItem.setUserData(new ControlActionController(ControlAction.SELECT_PARENT));
        selectParentMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.UP, modifier));
        selectNextMenuItem.setUserData(new ControlActionController(ControlAction.SELECT_NEXT));
        selectNextMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.RIGHT, modifier));
        selectPreviousMenuItem.setUserData(new ControlActionController(ControlAction.SELECT_PREVIOUS));
        selectPreviousMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.LEFT, modifier));
        trimMenuItem.setUserData(new EditActionController(EditAction.TRIM));

        /*
         * View menu
         */
        gotoContentMenuItem.setUserData(new DocumentControlActionController(DocumentControlAction.GOTO_CONTENT));
        gotoContentMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.DIGIT0, modifier));
        gotoPropertiesMenuItem.setUserData(new DocumentControlActionController(DocumentControlAction.GOTO_PROPERTIES));
        gotoPropertiesMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.DIGIT1, modifier));
        gotoLayoutMenuItem.setUserData(new DocumentControlActionController(DocumentControlAction.GOTO_LAYOUT));
        gotoLayoutMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.DIGIT2, modifier));
        gotoCodeMenuItem.setUserData(new DocumentControlActionController(DocumentControlAction.GOTO_CODE));
        gotoCodeMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.DIGIT3, modifier));

        toggleLibraryPanelMenuItem.setUserData(
                new DocumentControlActionController(DocumentControlAction.TOGGLE_LIBRARY_PANEL) {
                    @Override
                    public String getTitle() {
                        final String titleKey;
                        if (documentWindowController == null) {
                            titleKey = "menu.title.hide.library.panel";
                        } else if (documentWindowController.isLibraryPanelVisible()) {
                            titleKey = "menu.title.hide.library.panel";
                        } else {
                            titleKey = "menu.title.show.library.panel";
                        }
                        return I18N.getString(titleKey);
                    }
                });
        toggleLibraryPanelMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.DIGIT4, modifier));
        toggleHierarchyPanelMenuItem.setUserData(new DocumentControlActionController(DocumentControlAction.TOGGLE_DOCUMENT_PANEL) {
            @Override
            public String getTitle() {
                final String titleKey;
                if (documentWindowController == null) {
                    titleKey = "menu.title.hide.document.panel";
                } else if (documentWindowController.isHierarchyPanelVisible()) {
                    titleKey = "menu.title.hide.document.panel";
                } else {
                    titleKey = "menu.title.show.document.panel";
                }
                return I18N.getString(titleKey);
            }
        });
        toggleHierarchyPanelMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.DIGIT5, modifier));
        toggleCSSPanelMenuItem.setUserData(new DocumentControlActionController(DocumentControlAction.TOGGLE_CSS_PANEL) {
            @Override
            public String getTitle() {
                final String titleKey;
                if (documentWindowController == null) {
                    titleKey = "menu.title.hide.bottom.panel";
                } else if (documentWindowController.isBottomPanelVisible()) {
                    titleKey = "menu.title.hide.bottom.panel";
                } else {
                    titleKey = "menu.title.show.bottom.panel";
                }
                return I18N.getString(titleKey);
            }
        });
        toggleCSSPanelMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.DIGIT6, modifier));
        toggleLeftPanelMenuItem.setUserData(new DocumentControlActionController(DocumentControlAction.TOGGLE_LEFT_PANEL) {
            @Override
            public String getTitle() {
                final String titleKey;
                if (documentWindowController == null) {
                    titleKey = "menu.title.hide.left.panel";
                } else if (documentWindowController.isLeftPanelVisible()) {
                    titleKey = "menu.title.hide.left.panel";
                } else {
                    titleKey = "menu.title.show.left.panel";
                }
                return I18N.getString(titleKey);
            }
        });
        toggleLeftPanelMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.DIGIT7, modifier));
        toggleRightPanelMenuItem.setUserData(new DocumentControlActionController(DocumentControlAction.TOGGLE_RIGHT_PANEL) {
            @Override
            public String getTitle() {
                final String titleKey;
                if (documentWindowController == null) {
                    titleKey = "menu.title.hide.right.panel";
                } else if (documentWindowController.isRightPanelVisible()) {
                    titleKey = "menu.title.hide.right.panel";
                } else {
                    titleKey = "menu.title.show.right.panel";
                }
                return I18N.getString(titleKey);
            }
        });
        toggleRightPanelMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.DIGIT8, modifier));
        toggleOutlinesMenuItem.setUserData(new DocumentControlActionController(DocumentControlAction.TOGGLE_OUTLINES_VISIBILITY) {
            @Override
            public String getTitle() {
                final String titleKey;
                if (documentWindowController == null) {
                    titleKey = "menu.title.hide.outlines";
                } else if (documentWindowController.getContentPanelController().isOutlinesVisible()) {
                    titleKey = "menu.title.hide.outlines";
                } else {
                    titleKey = "menu.title.show.outlines";
                }
                return I18N.getString(titleKey);
            }
        });
        toggleOutlinesMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.E, modifier));
        toggleSampleDataMenuItem.setUserData(new ControlActionController(ControlAction.TOGGLE_SAMPLE_DATA) {
            @Override
            public String getTitle() {
                final String titleKey;
                if (documentWindowController == null) {
                    titleKey = "menu.title.hide.sample.data";
                } else if (documentWindowController.getEditorController().isSampleDataEnabled()) {
                    titleKey = "menu.title.hide.sample.data";
                } else {
                    titleKey = "menu.title.show.sample.data";
                }
                return I18N.getString(titleKey);
            }
        });
        toggleAlignmentGuidesMenuItem.setUserData(new DocumentControlActionController(DocumentControlAction.TOGGLE_GUIDES_VISIBILITY) {
            @Override
            public String getTitle() {
                final String titleKey;
                if (documentWindowController == null) {
                    titleKey = "menu.title.disable.guides";
                } else {
                    final ContentPanelController contentPanelController
                            = documentWindowController.getContentPanelController();
                    if (contentPanelController.isGuidesVisible()) {
                        titleKey = "menu.title.disable.guides";
                    } else {
                        titleKey = "menu.title.enable.guides";
                    }
                }
                return I18N.getString(titleKey);
            }
        });
        showSampleControllerMenuItem.setUserData(new DocumentControlActionController(DocumentControlAction.SHOW_SAMPLE_CONTROLLER));
        updateZoomMenu();

        /*
         * Insert menu: it uses specific handlers, which means we initialize it
         * later to avoid interfering with other menus.
         */

        /*
         * Modify menu
         */
        fitToParentMenuItem.setUserData(new EditActionController(EditAction.FIT_TO_PARENT));
        fitToParentMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.K, modifier));
        useComputedSizesMenuItem.setUserData(new EditActionController(EditAction.USE_COMPUTED_SIZES));
        useComputedSizesMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.K, KeyCombination.SHIFT_DOWN, modifier));
        addContextMenuMenuItem.setUserData(new EditActionController(EditAction.ADD_CONTEXT_MENU));
        addTooltipMenuItem.setUserData(new EditActionController(EditAction.ADD_TOOLTIP));
        moveRowAboveMenuItem.setUserData(new EditActionController(EditAction.MOVE_ROW_ABOVE));
        moveRowBelowMenuItem.setUserData(new EditActionController(EditAction.MOVE_ROW_BELOW));
        moveColumnBeforeMenuItem.setUserData(new EditActionController(EditAction.MOVE_COLUMN_BEFORE));
        moveColumnAfterMenuItem.setUserData(new EditActionController(EditAction.MOVE_COLUMN_AFTER));
        addRowAboveMenuItem.setUserData(new EditActionController(EditAction.ADD_ROW_ABOVE));
        addRowBelowMenuItem.setUserData(new EditActionController(EditAction.ADD_ROW_BELOW));
        addColumnBeforeMenuItem.setUserData(new EditActionController(EditAction.ADD_COLUMN_BEFORE));
        addColumnAfterMenuItem.setUserData(new EditActionController(EditAction.ADD_COLUMN_AFTER));
        increaseRowSpanMenuItem.setUserData(new EditActionController(EditAction.INCREASE_ROW_SPAN));
        decreaseRowSpanMenuItem.setUserData(new EditActionController(EditAction.DECREASE_ROW_SPAN));
        increaseColumnSpanMenuItem.setUserData(new EditActionController(EditAction.INCREASE_COLUMN_SPAN));
        decreaseColumnSpanMenuItem.setUserData(new EditActionController(EditAction.DECREASE_COLUMN_SPAN));
        qvgaSetSizeMenuItem.setUserData(new EditActionController(EditAction.SET_SIZE_320x240) {
            @Override
            public void perform() {
                super.perform();
                updatePreviewWindowSize(Size.SIZE_320x240);
            }
        });
        vgaSetSizeMenuItem.setUserData(new EditActionController(EditAction.SET_SIZE_640x480) {
            @Override
            public void perform() {
                super.perform();
                updatePreviewWindowSize(Size.SIZE_640x480);
            }
        });
        touchSetSizeMenuItem.setUserData(new EditActionController(EditAction.SET_SIZE_1280x800) {
            @Override
            public void perform() {
                super.perform();
                updatePreviewWindowSize(Size.SIZE_1280x800);
            }
        });
        hdSetSizeMenuItem.setUserData(new EditActionController(EditAction.SET_SIZE_1920x1080) {
            @Override
            public void perform() {
                super.perform();
                updatePreviewWindowSize(Size.SIZE_1920x1080);
            }
        });

        // Add Effect submenu
        updateAddEffectMenu();

        /*
         * Arrange menu
         */
        bringToFrontMenuItem.setUserData(new EditActionController(EditAction.BRING_TO_FRONT));
        bringToFrontMenuItem.setAccelerator(new KeyCharacterCombination("]", //NOI18N
                KeyCombination.SHIFT_DOWN, modifier));
        sendToBackMenuItem.setUserData(new EditActionController(EditAction.SEND_TO_BACK));
        sendToBackMenuItem.setAccelerator(new KeyCharacterCombination("[", //NOI18N
                KeyCombination.SHIFT_DOWN, modifier));
        bringForwardMenuItem.setUserData(new EditActionController(EditAction.BRING_FORWARD));
        bringForwardMenuItem.setAccelerator(
                new KeyCharacterCombination("]", modifier)); //NOI18N
        sendBackwardMenuItem.setUserData(new EditActionController(EditAction.SEND_BACKWARD));
        sendBackwardMenuItem.setAccelerator(
                new KeyCharacterCombination("[", modifier)); //NOI18N
        wrapInAnchorPaneMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_ANCHOR_PANE));
        wrapInBorderPaneMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_BORDER_PANE));
        wrapInDialogPaneMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_DIALOG_PANE));
        wrapInFlowPaneMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_FLOW_PANE));
        wrapInGroupMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_GROUP));
        wrapInGridPaneMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_GRID_PANE));
        wrapInHBoxMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_HBOX));
        wrapInPaneMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_PANE));
        wrapInScrollPaneMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_SCROLL_PANE));
        wrapInSplitPaneMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_SPLIT_PANE));
        wrapInStackPaneMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_STACK_PANE));
        wrapInTabPaneMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_TAB_PANE));
        wrapInTextFlowMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_TEXT_FLOW));
        wrapInTilePaneMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_TILE_PANE));
        wrapInTitledPaneMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_TITLED_PANE));
        wrapInToolBarMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_TOOL_BAR));
        wrapInVBoxMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_VBOX));
        wrapInGroupMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_GROUP));
        unwrapMenuItem.setUserData(new EditActionController(EditAction.UNWRAP));
        unwrapMenuItem.setAccelerator(
                new KeyCodeCombination(KeyCode.U, modifier));

        /*
         * Preview menu
         */
        showPreviewInWindowMenuItem.setUserData(new DocumentControlActionController(DocumentControlAction.SHOW_PREVIEW_WINDOW));
        showPreviewInWindowMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.P, modifier));
        showPreviewInDialogMenuItem.setUserData(new DocumentControlActionController(DocumentControlAction.SHOW_PREVIEW_DIALOG));
        caspianHighContrastThemeMenuItem.setUserData(new SetThemeActionController(EditorPlatform.Theme.CASPIAN_HIGH_CONTRAST));
        caspianThemeMenuItem.setUserData(new SetThemeActionController(EditorPlatform.Theme.CASPIAN));
        caspianEmbeddedThemeMenuItem.setUserData(new SetThemeActionController(EditorPlatform.Theme.CASPIAN_EMBEDDED));
        caspianEmbeddedQVGAThemeMenuItem.setUserData(new SetThemeActionController(EditorPlatform.Theme.CASPIAN_EMBEDDED_QVGA));
        modenaThemeMenuItem.setUserData(new SetThemeActionController(EditorPlatform.Theme.MODENA));
        modenaTouchThemeMenuItem.setUserData(new SetThemeActionController(EditorPlatform.Theme.MODENA_TOUCH));
        modenaHighContrastBlackonwhiteThemeMenuItem.setUserData(new SetThemeActionController(EditorPlatform.Theme.MODENA_HIGH_CONTRAST_BLACK_ON_WHITE));
        modenaHighContrastWhiteonblackThemeMenuItem.setUserData(new SetThemeActionController(EditorPlatform.Theme.MODENA_HIGH_CONTRAST_WHITE_ON_BLACK));
        modenaHighContrastYellowonblackThemeMenuItem.setUserData(new SetThemeActionController(EditorPlatform.Theme.MODENA_HIGH_CONTRAST_YELLOW_ON_BLACK));

        addSceneStyleSheetMenuItem.setUserData(new DocumentControlActionController(DocumentControlAction.ADD_SCENE_STYLE_SHEET));
        updateOpenAndRemoveSceneStyleSheetMenus();
        if (documentWindowController != null) {
            this.documentWindowController.getEditorController().sceneStyleSheetProperty().addListener((ChangeListener<ObservableList<File>>) (ov, t, t1) -> {
                if (t1 != null) {
                    updateOpenAndRemoveSceneStyleSheetMenus();
                    setupMenuItemHandlers(removeSceneStyleSheetMenu);
                    setupMenuItemHandlers(openSceneStyleSheetMenu);
                }
            });
        }

        setResourceMenuItem.setUserData(new DocumentControlActionController(DocumentControlAction.SET_RESOURCE));
        removeResourceMenuItem.setUserData(new DocumentControlActionController(DocumentControlAction.REMOVE_RESOURCE) {
            @Override
            public String getTitle() {
                String title = I18N.getString("menu.title.remove.resource");
                if (documentWindowController != null
                        && documentWindowController.getResourceFile() != null) {
                    title = I18N.getString("menu.title.remove.resource.with.file",
                            documentWindowController.getResourceFile().getName());
                }

                return title;
            }
        });
        revealResourceMenuItem.setUserData(new DocumentControlActionController(DocumentControlAction.REVEAL_RESOURCE) {

            @Override
            public String getTitle() {
                String title = I18N.getString("menu.title.reveal.resource");
                if (documentWindowController != null
                        && documentWindowController.getResourceFile() != null) {
                    title = I18N.getString("menu.title.reveal.resource.with.file",
                            documentWindowController.getResourceFile().getName());
                }

                return title;
            }
        });
        qvgaPreviewSizeMenuItem.setUserData(new SetSizeActionController(EditorController.Size.SIZE_320x240));
        vgaPreviewSizeMenuItem.setUserData(new SetSizeActionController(EditorController.Size.SIZE_640x480));
        touchPreviewSizeMenuItem.setUserData(new SetSizeActionController(EditorController.Size.SIZE_1280x800));
        hdPreviewSizeMenuItem.setUserData(new SetSizeActionController(EditorController.Size.SIZE_1920x1080));
        preferredPreviewSizeMenuItem.setUserData(new SetSizeActionController(EditorController.Size.SIZE_PREFERRED));

        /*
         * Window menu : it is setup after the other menus
         */
        
        /*
         * Help menu
         */
        aboutMenuItem.setUserData(new ApplicationControlActionController(ApplicationControlAction.ABOUT));
        helpMenuItem.setUserData(new DocumentControlActionController(DocumentControlAction.HELP));
        helpMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.F1));

        /*
         * Put some generic handlers on each Menu and MenuItem.
         * For Insert and Window menu, we override with specific handlers.
         */
        for (Menu m : menuBar.getMenus()) {
            setupMenuItemHandlers(m);
        }
        
        /*
         * Insert menu: we set what is statically known.
         */
        constructBuiltinPartOfInsertMenu();
        constructCustomPartOfInsertMenu();
        
        // The handler for Insert menu deals only with Custom sub-menu.
        insertMenu.setOnMenuValidation(onCustomPartOfInsertMenuValidationHandler);
        
        windowMenu.setOnMenuValidation(onWindowMenuValidationHandler);
    }

    /*
     * Generic menu and item handlers
     */
    private void setupMenuItemHandlers(MenuItem i) {
        if (i instanceof Menu) {
            final Menu m = (Menu) i;
            m.setOnMenuValidation(onMenuValidationEventHandler);
            for (MenuItem child : m.getItems()) {
                setupMenuItemHandlers(child);
            }
        } else {
            i.setOnAction(onActionEventHandler);
            if (i.getAccelerator() != null) {
                keyToMenu.put(i.getAccelerator(), i);
            }
        }
    }

    private final EventHandler<Event> onMenuValidationEventHandler
            = t -> {
        assert t.getSource() instanceof Menu;
        handleOnMenuValidation((Menu) t.getSource());
    };

    private void handleOnMenuValidation(Menu menu) {
        for (MenuItem i : menu.getItems()) {
            final boolean disable, selected;
            final String title;
            if (i.getUserData() instanceof MenuItemController) {
                final MenuItemController c = (MenuItemController) i.getUserData();
                boolean canPerform;
                try {
                    canPerform = c.canPerform();
                } catch(RuntimeException x) {
                    // This catch is protection against a bug in canPerform().
                    // It avoids to block all the items in the menu in case
                    // of crash in canPerform() (see DTL-6164).
                    canPerform = false;
                    final Exception xx 
                            = new Exception(c.getClass().getSimpleName() 
                            + ".canPerform() did break for menu item " + i, x); //NOI18N
                    xx.printStackTrace();
                }
                disable = !canPerform;
                title = c.getTitle();
                selected = c.isSelected();
            } else {
                if (i instanceof Menu) {
                    disable = false;
                    selected = false;
                    title = null;
                } else {
                    disable = true;
                    selected = false;
                    title = null;
                }
            }
            i.setDisable(disable);
            if (title != null) {
                i.setText(title);
            }
            if (i instanceof RadioMenuItem) {
                final RadioMenuItem ri = (RadioMenuItem) i;
                ri.setSelected(selected);
            }
        }
    }

    private final EventHandler<ActionEvent> onActionEventHandler
            = t -> {
        assert t.getSource() instanceof MenuItem;
        handleOnActionMenu((MenuItem) t.getSource());
    };

    private void handleOnActionMenu(MenuItem i) {
        assert i.getUserData() instanceof MenuItemController;
        final MenuItemController c = (MenuItemController) i.getUserData();
        c.perform();
    }
    
    /*
     * Private (zoom menu)
     */
    
    final static double[] scalingTable = {0.25, 0.50, 0.75, 1.00, 1.50, 2.0, 4.0};
    
    private void updateZoomMenu() {
        final double[] scalingTable = {0.25, 0.50, 0.75, 1.00, 1.50, 2.0, 4.0};

        final MenuItem zoomInMenuItem = new MenuItem(I18N.getString("menu.title.zoom.in"));
        zoomInMenuItem.setUserData(new ZoomInActionController());
        zoomInMenuItem.setAccelerator(new KeyCharacterCombination("+", modifier)); //NOI18N
        zoomMenu.getItems().add(zoomInMenuItem);
        
        final MenuItem zoomOutMenuItem = new MenuItem(I18N.getString("menu.title.zoom.out"));
        zoomOutMenuItem.setUserData(new ZoomOutActionController());
        zoomOutMenuItem.setAccelerator(new KeyCharacterCombination("+",  //NOI18N
                KeyCombination.SHIFT_DOWN, modifier));
        zoomMenu.getItems().add(zoomOutMenuItem);
        
        zoomMenu.getItems().add(new SeparatorMenuItem());
        
        for (int i = 0; i < scalingTable.length; i++) {
            final double scaling = scalingTable[i];
            final String title = String.format("%.0f%%", scaling * 100); //NOI18N
            final RadioMenuItem mi = new RadioMenuItem(title);
            mi.setUserData(new SetZoomActionController(scaling));
            zoomMenu.getItems().add(mi);
        }
    }

    
    private static int findZoomScaleIndex(double zoomScale) {
        int result = -1;
        
        for (int i = 0; i < scalingTable.length; i++) {
            if (MathUtils.equals(zoomScale, scalingTable[i])) {
                result = i;
                break;
            }
        }
        
        return result;
    }
    
    private void updateOpenRecentMenuItems() {

        final List<MenuItem> menuItems = new ArrayList<>();

        final PreferencesController pc = PreferencesController.getSingleton();
        final PreferencesRecordGlobal recordGlobal = pc.getRecordGlobal();
        final List<String> recentItems = recordGlobal.getRecentItems();

        final MenuItem clearMenuItem = new MenuItem(I18N.getString("menu.title.open.recent.clear"));
        clearMenuItem.setOnAction(new ClearOpenRecentHandler());

        if (recentItems.isEmpty()) {
            clearMenuItem.setDisable(true);
            menuItems.add(clearMenuItem);
        } else {
            clearMenuItem.setDisable(false);

            final Map<String, Integer> recentItemsNames = new HashMap<>();
            final List<String> recentItemsToRemove = new ArrayList<>();

            // First pass to build recentItemsNames and recentItemsToRemove
            for (String recentItem : recentItems) {
                final File recentItemFile = new File(recentItem);
                if (recentItemFile.exists()) {
                    final String name = recentItemFile.getName();
                    if (recentItemsNames.containsKey(name)) {
                        recentItemsNames.replace(name, recentItemsNames.get(name) + 1);
                    } else {
                        recentItemsNames.put(name, 1);
                    }
                } else {
                    // recent item file is still in preferences DB but has been removed from disk
                    recentItemsToRemove.add(recentItem);
                }
            }
            // Second pass to build MenuItems
            for (String recentItem : recentItems) {
                final File recentItemFile = new File(recentItem);
                if (recentItemFile.exists()) {
                    final String name = recentItemFile.getName();
                    assert recentItemsNames.keySet().contains(name);
                    final MenuItem mi;
                    if (recentItemsNames.get(name) > 1) {
                        // Several files with same name : display full path
                        mi = new MenuItem(recentItem);
                    } else {
                        // Single file with this name : display file name only
                        assert recentItemsNames.get(name) == 1;
                        mi = new MenuItem(name);
                    }
                    mi.setOnAction(t -> {
                        final File file = new File(recentItem);
                        SceneBuilderApp.getSingleton().performOpenRecent(documentWindowController, file);
                    });
                    mi.setMnemonicParsing(false);
                    menuItems.add(mi);
                }
            }
            
            // Cleanup recent items preferences if needed
            if (recentItemsToRemove.isEmpty() == false) {
                recordGlobal.removeRecentItems(recentItemsToRemove);
            }

            menuItems.add(new SeparatorMenuItem());
            menuItems.add(clearMenuItem);
        }

        openRecentMenu.getItems().setAll(menuItems);
    }

    private void updateOpenAndRemoveSceneStyleSheetMenus() {
        assert removeSceneStyleSheetMenu != null;

        if (documentWindowController != null) {
            ObservableList<File> sceneStyleSheets = documentWindowController.getEditorController().getSceneStyleSheets();

            if (sceneStyleSheets != null) {
                removeSceneStyleSheetMenu.getItems().clear();
                openSceneStyleSheetMenu.getItems().clear();

                if (sceneStyleSheets.size() == 0) {
                    MenuItem mi = new MenuItem(I18N.getString("scenestylesheet.none"));
                    mi.setDisable(true);
                    removeSceneStyleSheetMenu.getItems().add(mi);
                    MenuItem mi2 = new MenuItem(I18N.getString("scenestylesheet.none"));
                    mi2.setDisable(true);
                    openSceneStyleSheetMenu.getItems().add(mi2);
                } else {
                    for (File f : sceneStyleSheets) {
                        MenuItem mi = new MenuItem(f.getName());
                        mi.setUserData(new RemoveSceneStyleSheetActionController(f));
                        removeSceneStyleSheetMenu.getItems().add(mi);
                        MenuItem mi2 = new MenuItem(f.getName());
                        mi2.setUserData(new OpenSceneStyleSheetActionController(f));
                        openSceneStyleSheetMenu.getItems().add(mi2);
                    }
                }
            }
        }
    }

    /*
     * Private (insert menu)
     */
    private final EventHandler<Event> onCustomPartOfInsertMenuValidationHandler
            = t -> {
        assert t.getSource() == insertMenu;
        updateCustomPartOfInsertMenu();
    };
    
    private void updateCustomPartOfInsertMenu() {
        assert insertMenu != null;
        assert insertCustomMenu != null;        

        if (documentWindowController != null) {
            final EditorController editorController = documentWindowController.getEditorController();
            assert editorController.getLibrary() != null;

            Set<LibraryItem> sectionItems = new TreeSet<>(new LibraryItemNameComparator());
            
            // Collect custom items
            for (LibraryItem li : editorController.getLibrary().getItems()) {
                if (li.getSection().equals(UserLibrary.TAG_USER_DEFINED)) {
                    sectionItems.add(li);
                }
            }

            // Make custom items visible and accessible via custom menu.
            if (sectionItems.size() > 0) {
                insertCustomMenu.getItems().clear();
                
                for (LibraryItem li : sectionItems) {
                    insertCustomMenu.getItems().add(makeMenuItemForLibraryItem(li));
                }
                
                insertCustomMenu.setVisible(true);
            } else {
                insertCustomMenu.setVisible(false);
            }
        }
    }
    
    // At constructing time we dunno if we've custom items then we keep it hidden.
    private void constructCustomPartOfInsertMenu() {
        assert insertMenu != null;
        insertCustomMenu = makeMenuForLibrarySection(UserLibrary.TAG_USER_DEFINED);
        insertMenu.getItems().add(0, insertCustomMenu);
        insertCustomMenu.setVisible(false);
    }
    
    // We consider the content of built-in library is static: it cannot change
    // unless its implementation is modified.
    private void constructBuiltinPartOfInsertMenu() {
        assert insertMenu != null;
        insertMenu.getItems().clear();

        final Map<String, Set<LibraryItem>> sectionMap
                = new TreeMap<>(new BuiltinSectionComparator());

        for (LibraryItem li : BuiltinLibrary.getLibrary().getItems()) {
            Set<LibraryItem> sectionItems = sectionMap.get(li.getSection());
            if (sectionItems == null) {
                sectionItems = new TreeSet<>(new LibraryItemNameComparator());
                sectionMap.put(li.getSection(), sectionItems);
            }
            sectionItems.add(li);
        }

        for (Map.Entry<String, Set<LibraryItem>> e : sectionMap.entrySet()) {
            final Menu sectionMenu = makeMenuForLibrarySection(e.getKey());
            insertMenu.getItems().add(sectionMenu);
            for (LibraryItem li : e.getValue()) {
                sectionMenu.getItems().add(makeMenuItemForLibraryItem(li));
            }
        }
    }

    private Menu makeMenuForLibrarySection(String section) {
        final Menu result = new Menu();
        result.setText(section);
        result.setOnShowing(t -> updateInsertMenuState(result));
        return result;
    }

    private MenuItem makeMenuItemForLibraryItem(final LibraryItem li) {
        final MenuItem result = new MenuItem();

        result.setText(li.getName());
        result.setUserData(li);
        result.setOnAction(t -> handleInsertMenuAction(li));
        return result;
    }

    private void updateInsertMenuState(Menu sectionMenu) {
        if (documentWindowController != null && documentWindowController.getStage().isFocused()) {
            final EditorController editorController = documentWindowController.getEditorController();
            for (MenuItem menuItem : sectionMenu.getItems()) {
                assert menuItem.getUserData() instanceof LibraryItem;
                final LibraryItem li = (LibraryItem) menuItem.getUserData();
                final boolean enabled = editorController.canPerformInsert(li);
                menuItem.setDisable(!enabled);
            }
        } else {
            // See DTL-6017 and DTL-6554.
            // This case is relevant on Mac only; on Win and Linux the top menu
            // bar is part of the document window then even if some other non-modal
            // window is opened (Preferences, Skeleton, Preview) one has to give
            // focus to the document window to become able to open the Insert menu.
            for (MenuItem menuItem : sectionMenu.getItems()) {
                assert menuItem.getUserData() instanceof LibraryItem;
                menuItem.setDisable(true);
            }
        }
    }

    private void handleInsertMenuAction(LibraryItem li) {
        if (documentWindowController != null) {
            final EditorController editorController = documentWindowController.getEditorController();
            editorController.performInsert(li);
        }
    }

    /*
     * Private (Add Effect menu)
     */
    private void updateAddEffectMenu() {
        addEffectMenu.getItems().clear();
        for (Class<? extends Effect> c : EffectPicker.getEffectClasses()) {
            addEffectMenu.getItems().add(makeMenuItemForEffect(c));
        }
    }

    private MenuItem makeMenuItemForEffect(Class<? extends Effect> effectClass) {
        final MenuItem result = new MenuItem();
        result.setText(effectClass.getSimpleName());
        result.setUserData(new AddEffectActionController(effectClass));
        return result;
    }

    /*
     * Private (window menu)
     */
    private final EventHandler<Event> onWindowMenuValidationHandler
            = t -> {
        assert t.getSource() == windowMenu;
        handleOnWindowMenuValidation();
    };

    private void handleOnWindowMenuValidation() {
        windowMenu.getItems().clear();

        final List<DocumentWindowController> documentWindowControllers
                = SceneBuilderApp.getSingleton().getDocumentWindowControllers();
        if (documentWindowControllers.isEmpty()) {
            // Adds the "No window" menu item
            windowMenu.getItems().add(makeWindowMenuItem(null));
        } else {
            final List<DocumentWindowController> sortedControllers
                    = new ArrayList<>(documentWindowControllers);
            Collections.sort(sortedControllers, new DocumentWindowController.TitleComparator());

            for (DocumentWindowController dwc : sortedControllers) {
                windowMenu.getItems().add(makeWindowMenuItem(dwc));
            }
        }
    }

    private MenuItem makeWindowMenuItem(final DocumentWindowController dwc) {
        final RadioMenuItem result = new RadioMenuItem();
        if (dwc != null) {
            result.setText(dwc.getStage().getTitle());
            result.setDisable(false);
            result.setSelected(dwc.getStage().isFocused());
            result.setOnAction(new WindowMenuEventHandler(dwc));
        } else {
            result.setText(I18N.getString("menu.title.no.window"));
            result.setDisable(true);
            result.setSelected(false);
        }

        return result;
    }

    private static class WindowMenuEventHandler implements EventHandler<ActionEvent> {

        private final DocumentWindowController dwc;

        public WindowMenuEventHandler(DocumentWindowController dwc) {
            this.dwc = dwc;
        }

        @Override
        public void handle(ActionEvent t) {
            dwc.getStage().toFront();
        }
    }

    /*
     * Private (MenuItemController)
     */
    abstract class MenuItemController {

        public abstract boolean canPerform();

        public abstract void perform();

        public String getTitle() {
            return null;
        }

        public boolean isSelected() {
            return false;
        }
    }

    class UndoActionController extends MenuItemController {

        @Override
        public boolean canPerform() {
            boolean result;
            if (documentWindowController == null
                    || documentWindowController.getStage().isFocused() == false) {
                result = false;
            } else {
                result = documentWindowController.getEditorController().canUndo();
            }
            return result;
        }

        @Override
        public void perform() {
            assert canPerform();
            documentWindowController.getEditorController().undo();
        }

        @Override
        public String getTitle() {
            final StringBuilder result = new StringBuilder();
            result.append(I18N.getString("menu.title.undo"));
            if (canPerform()) {
                result.append(" "); //NOI18N
                result.append(documentWindowController.getEditorController().getUndoDescription());
            }
            return result.toString();
        }
    }

    class RedoActionController extends MenuItemController {

        @Override
        public boolean canPerform() {
            boolean result;
            if (documentWindowController == null
                    || documentWindowController.getStage().isFocused() == false) {
                result = false;
            } else {
                result = documentWindowController.getEditorController().canRedo();
            }
            return result;
        }

        @Override
        public void perform() {
            assert canPerform();
            documentWindowController.getEditorController().redo();
        }

        @Override
        public String getTitle() {
            final StringBuilder result = new StringBuilder();
            result.append(I18N.getString("menu.title.redo"));
            if (canPerform()) {
                result.append(" "); //NOI18N
                result.append(documentWindowController.getEditorController().getRedoDescription());
            }
            return result.toString();
        }
    }

    class EditActionController extends MenuItemController {

        private final EditAction editAction;

        public EditActionController(EditAction editAction) {
            this.editAction = editAction;
        }

        @Override
        public boolean canPerform() {
            boolean result;
            if (documentWindowController == null
                    || documentWindowController.getStage().isFocused() == false) {
                result = false;
            } else {
                result = documentWindowController.getEditorController().canPerformEditAction(editAction);
            }
            return result;
        }

        @Override
        public void perform() {
            assert canPerform() : "editAction=" + editAction;
            documentWindowController.getEditorController().performEditAction(editAction);
        }

    }

    class ControlActionController extends MenuItemController {

        private final ControlAction controlAction;

        public ControlActionController(ControlAction controlAction) {
            this.controlAction = controlAction;
        }

        @Override
        public boolean canPerform() {
            boolean result;
            if (documentWindowController == null) {
                result = false;
            } else {
                result = documentWindowController.getEditorController().canPerformControlAction(controlAction);
            }
            return result;
        }

        @Override
        public void perform() {
            assert canPerform() : "controlAction=" + controlAction;
            documentWindowController.getEditorController().performControlAction(controlAction);
        }

    }

    class DocumentEditActionController extends MenuItemController {

        private final DocumentEditAction editAction;

        public DocumentEditActionController(DocumentEditAction editAction) {
            this.editAction = editAction;
        }

        @Override
        public boolean canPerform() {
            boolean result;
            if (documentWindowController == null
                    || documentWindowController.getStage().isFocused() == false) {
                result = false;
            } else {
                result = documentWindowController.canPerformEditAction(editAction);
            }
            return result;
        }

        @Override
        public void perform() {
            assert canPerform() : "editAction=" + editAction;
            documentWindowController.performEditAction(editAction);
        }

    }

    class DocumentControlActionController extends MenuItemController {

        private final DocumentControlAction controlAction;

        public DocumentControlActionController(DocumentControlAction controlAction) {
            this.controlAction = controlAction;
        }

        @Override
        public boolean canPerform() {
            boolean result;
            if (documentWindowController == null) {
                result = false;
            } else {
                result = documentWindowController.canPerformControlAction(controlAction);
            }
            return result;
        }

        @Override
        public void perform() {
            assert canPerform() : "controlAction=" + controlAction;
            documentWindowController.performControlAction(controlAction);
        }

    }

    class ApplicationControlActionController extends MenuItemController {

        private final ApplicationControlAction controlAction;

        public ApplicationControlActionController(ApplicationControlAction controlAction) {
            this.controlAction = controlAction;
        }

        @Override
        public boolean canPerform() {
            return SceneBuilderApp.getSingleton().canPerformControlAction(controlAction,
                    documentWindowController);
        }

        @Override
        public void perform() {
            SceneBuilderApp.getSingleton().performControlAction(controlAction,
                    documentWindowController);
        }

    }

    class AddEffectActionController extends MenuItemController {

        private final Class<? extends Effect> effectClass;

        public AddEffectActionController(Class<? extends Effect> effectClass) {
            this.effectClass = effectClass;
        }

        @Override
        public boolean canPerform() {
            boolean result;
            if (documentWindowController == null
                    || documentWindowController.getStage().isFocused() == false) {
                result = false;
            } else {
                result = documentWindowController.getEditorController().canPerformSetEffect();
            }
            return result;
        }

        @Override
        public void perform() {
            documentWindowController.getEditorController().performSetEffect(effectClass);
        }
    }

    class SetZoomActionController extends MenuItemController {

        private final double scaling;

        public SetZoomActionController(double scaling) {
            this.scaling = scaling;
        }

        @Override
        public boolean canPerform() {
            return (documentWindowController != null);
        }

        @Override
        public void perform() {
            final ContentPanelController contentPanelController
                    = documentWindowController.getContentPanelController();
            final double currentScaling
                    = contentPanelController.getScaling();
            if (MathUtils.equals(currentScaling, scaling) == false) {
                contentPanelController.setScaling(scaling);
            }
        }

        @Override
        public boolean isSelected() {
            boolean result;

            if (documentWindowController == null) {
                result = false;
            } else {
                final double currentScaling
                        = documentWindowController.getContentPanelController().getScaling();
                result = MathUtils.equals(currentScaling, scaling);
            }

            return result;
        }

    }
    
    class ZoomInActionController extends MenuItemController {

        @Override
        public boolean canPerform() {
            boolean result;
            if (documentWindowController == null) {
                result = false;
            } else {
                final ContentPanelController contentPanelController
                        = documentWindowController.getContentPanelController();
                final int currentScalingIndex
                        = findZoomScaleIndex(contentPanelController.getScaling());
                result = currentScalingIndex+1 < scalingTable.length;
            }
            return result;
        }

        @Override
        public void perform() {
            final ContentPanelController contentPanelController
                    = documentWindowController.getContentPanelController();
            final int currentScalingIndex
                    = findZoomScaleIndex(contentPanelController.getScaling());
            final double newScaling
                    = scalingTable[currentScalingIndex+1];
            contentPanelController.setScaling(newScaling);
        }

    }
    

    class ZoomOutActionController extends MenuItemController {

        @Override
        public boolean canPerform() {
            boolean result;
            if (documentWindowController == null) {
                result = false;
            } else {
                final ContentPanelController contentPanelController
                        = documentWindowController.getContentPanelController();
                final int currentScalingIndex
                        = findZoomScaleIndex(contentPanelController.getScaling());
                result = 0 <= currentScalingIndex-1;
            }
            return result;
        }

        @Override
        public void perform() {
            final ContentPanelController contentPanelController
                    = documentWindowController.getContentPanelController();
            final int currentScalingIndex
                    = findZoomScaleIndex(contentPanelController.getScaling());
            final double newScaling
                    = scalingTable[currentScalingIndex-1];
            contentPanelController.setScaling(newScaling);
        }

    }
    
    private void updatePreviewWindowSize(Size size) {
        if (documentWindowController != null
                && documentWindowController.getPreviewWindowController() != null
                && documentWindowController.getPreviewWindowController().getStage().isShowing()) {
            documentWindowController.getPreviewWindowController().setSize(size);
        }
    }

    class SetSizeActionController extends MenuItemController {

        private final EditorController.Size size;

        public SetSizeActionController(EditorController.Size size) {
            this.size = size;
        }

        @Override
        public boolean canPerform() {
            boolean res = (documentWindowController != null)
                    && (documentWindowController.getPreviewWindowController() != null)
                    && documentWindowController.getPreviewWindowController().getStage().isShowing()
                    && ! documentWindowController.getEditorController().is3D()
                    && documentWindowController.getEditorController().isNode()
                    && documentWindowController.getPreviewWindowController().sizeDoesFit(size);
            return res;
        }

        @Override
        public void perform() {
            assert documentWindowController != null;
            assert documentWindowController.getPreviewWindowController() != null;
            documentWindowController.getPreviewWindowController().setSize(size);
        }

        @Override
        public boolean isSelected() {
            boolean res;

            if (documentWindowController == null || documentWindowController.getPreviewWindowController() == null) {
                res = false;
            } else {
                Size currentSize = documentWindowController.getPreviewWindowController().getSize();
                res = (size == currentSize)
                        && documentWindowController.getPreviewWindowController().getStage().isShowing()
                        && ! documentWindowController.getPreviewWindowController().userResizedPreviewWindow()
                        && ! documentWindowController.getEditorController().is3D()
                        && documentWindowController.getEditorController().isNode();
            }
            
            return res;
        }
        
        @Override
        public String getTitle() {
            if (documentWindowController == null) {
                return null;
            }
            
            if (size == EditorController.Size.SIZE_PREFERRED) {
                String title = I18N.getString("menu.title.size.preferred");
                
                if (documentWindowController.getPreviewWindowController() != null
                        && documentWindowController.getPreviewWindowController().getStage().isShowing()
                        && ! documentWindowController.getEditorController().is3D()
                        && documentWindowController.getEditorController().isNode()) {
                        title = I18N.getString("menu.title.size.preferred.with.value",
                                getStringFromDouble(documentWindowController.getPreviewWindowController().getRoot().prefWidth(-1)),
                                getStringFromDouble(documentWindowController.getPreviewWindowController().getRoot().prefHeight(-1)));
                }
                
                return title;
            } else {
                return null;
            }
        }
    }

    class SetThemeActionController extends MenuItemController {

        private final EditorPlatform.Theme theme;

        public SetThemeActionController(EditorPlatform.Theme theme) {
            this.theme = theme;
        }

        @Override
        public boolean canPerform() {
            boolean res = documentWindowController != null;
            if (res) {
                final EditorPlatform.Theme currentTheme
                        = documentWindowController.getEditorController().getTheme();
                // CASPIAN_HIGH_CONTRAST can be selected only if another CASPIAN
                // theme is active.
                // MODENA_HIGH_CONTRAST_<*> can be selected only if another MODENA
                // theme is active.
                if (theme == EditorPlatform.Theme.CASPIAN_HIGH_CONTRAST
                        && EditorPlatform.isModena(currentTheme)) {
                    res = false;
                    caspianHighContrastThemeMenuItem.setSelected(false);
                } else if (theme == EditorPlatform.Theme.MODENA_HIGH_CONTRAST_BLACK_ON_WHITE
                        && EditorPlatform.isCaspian(currentTheme)) {
                    res = false;
                    modenaHighContrastBlackonwhiteThemeMenuItem.setSelected(false);
                } else if (theme == EditorPlatform.Theme.MODENA_HIGH_CONTRAST_WHITE_ON_BLACK
                        && EditorPlatform.isCaspian(currentTheme)) {
                    res = false;
                    modenaHighContrastWhiteonblackThemeMenuItem.setSelected(false);
                } else if (theme == EditorPlatform.Theme.MODENA_HIGH_CONTRAST_YELLOW_ON_BLACK
                        && EditorPlatform.isCaspian(currentTheme)) {
                    res = false;
                    modenaHighContrastYellowonblackThemeMenuItem.setSelected(false);
                }
            }
            
            return res;
        }

        @Override
        public void perform() {
            assert documentWindowController != null;
            EditorPlatform.Theme currentTheme
                            = documentWindowController.getEditorController().getTheme();
            EditorPlatform.Theme overiddingTheme = theme;

            switch (theme) {
                case CASPIAN:
                    if (caspianHighContrastThemeMenuItem.isSelected()) {
                        overiddingTheme = EditorPlatform.Theme.CASPIAN_HIGH_CONTRAST;
                    }
                    break;
                case CASPIAN_EMBEDDED:
                    if (caspianHighContrastThemeMenuItem.isSelected()) {
                        overiddingTheme = EditorPlatform.Theme.CASPIAN_EMBEDDED_HIGH_CONTRAST;
                    }
                    break;
                case CASPIAN_EMBEDDED_QVGA:
                    if (caspianHighContrastThemeMenuItem.isSelected()) {
                        overiddingTheme = EditorPlatform.Theme.CASPIAN_EMBEDDED_QVGA_HIGH_CONTRAST;
                    }
                    break;
                case CASPIAN_HIGH_CONTRAST:
                    switch (currentTheme) {
                        case CASPIAN:
                            if (caspianHighContrastThemeMenuItem.isSelected()) {
                                overiddingTheme = EditorPlatform.Theme.CASPIAN_HIGH_CONTRAST;
                            }
                            break;
                        case CASPIAN_EMBEDDED:
                            if (caspianHighContrastThemeMenuItem.isSelected()) {
                                overiddingTheme = EditorPlatform.Theme.CASPIAN_EMBEDDED_HIGH_CONTRAST;
                            }
                            break;
                        case CASPIAN_EMBEDDED_QVGA:
                            if (caspianHighContrastThemeMenuItem.isSelected()) {
                                overiddingTheme = EditorPlatform.Theme.CASPIAN_EMBEDDED_QVGA_HIGH_CONTRAST;
                            }
                            break;
                        case CASPIAN_HIGH_CONTRAST:
                            if (!caspianHighContrastThemeMenuItem.isSelected()) {
                                overiddingTheme = EditorPlatform.Theme.CASPIAN;
                            }
                            break;
                        case CASPIAN_EMBEDDED_HIGH_CONTRAST:
                            if (!caspianHighContrastThemeMenuItem.isSelected()) {
                                overiddingTheme = EditorPlatform.Theme.CASPIAN_EMBEDDED;
                            }
                            break;
                        case CASPIAN_EMBEDDED_QVGA_HIGH_CONTRAST:
                            if (!caspianHighContrastThemeMenuItem.isSelected()) {
                                overiddingTheme = EditorPlatform.Theme.CASPIAN_EMBEDDED_QVGA;
                            }
                            break;
                        default:
                            // All known 6 Caspian cases are handled above.
                            assert false;
                            break;
                    }
                    break;
                case MODENA:
                    if (modenaHighContrastBlackonwhiteThemeMenuItem.isSelected()) {
                        overiddingTheme = EditorPlatform.Theme.MODENA_HIGH_CONTRAST_BLACK_ON_WHITE;
                    } else if (modenaHighContrastWhiteonblackThemeMenuItem.isSelected()) {
                        overiddingTheme = EditorPlatform.Theme.MODENA_HIGH_CONTRAST_WHITE_ON_BLACK;
                    } else if (modenaHighContrastYellowonblackThemeMenuItem.isSelected()) {
                        overiddingTheme = EditorPlatform.Theme.MODENA_HIGH_CONTRAST_YELLOW_ON_BLACK;
                    }
                    break;
                case MODENA_TOUCH:
                    if (modenaHighContrastBlackonwhiteThemeMenuItem.isSelected()) {
                        overiddingTheme = EditorPlatform.Theme.MODENA_TOUCH_HIGH_CONTRAST_BLACK_ON_WHITE;
                    } else if (modenaHighContrastWhiteonblackThemeMenuItem.isSelected()) {
                        overiddingTheme = EditorPlatform.Theme.MODENA_TOUCH_HIGH_CONTRAST_WHITE_ON_BLACK;
                    } else if (modenaHighContrastYellowonblackThemeMenuItem.isSelected()) {
                        overiddingTheme = EditorPlatform.Theme.MODENA_TOUCH_HIGH_CONTRAST_YELLOW_ON_BLACK;
                    }
                    break;
                case MODENA_HIGH_CONTRAST_BLACK_ON_WHITE:
                    switch (currentTheme) {
                        case MODENA:
                            if (modenaHighContrastBlackonwhiteThemeMenuItem.isSelected()) {
                                overiddingTheme = EditorPlatform.Theme.MODENA_HIGH_CONTRAST_BLACK_ON_WHITE;
                            }
                            break;
                        case MODENA_TOUCH:
                            if (modenaHighContrastBlackonwhiteThemeMenuItem.isSelected()) {
                                overiddingTheme = EditorPlatform.Theme.MODENA_TOUCH_HIGH_CONTRAST_BLACK_ON_WHITE;
                            }
                            break;
                        case MODENA_HIGH_CONTRAST_BLACK_ON_WHITE:
                            if (modenaHighContrastBlackonwhiteThemeMenuItem.isSelected()) {
                                overiddingTheme = EditorPlatform.Theme.MODENA;
                            }
                            break;
                        case MODENA_HIGH_CONTRAST_WHITE_ON_BLACK:
                            break;
                        case MODENA_HIGH_CONTRAST_YELLOW_ON_BLACK:
                            break;
                        case MODENA_TOUCH_HIGH_CONTRAST_BLACK_ON_WHITE:
                            if (modenaHighContrastBlackonwhiteThemeMenuItem.isSelected()) {
                                overiddingTheme = EditorPlatform.Theme.MODENA_TOUCH;
                            }
                            break;
                        case MODENA_TOUCH_HIGH_CONTRAST_WHITE_ON_BLACK:
                            if (modenaHighContrastBlackonwhiteThemeMenuItem.isSelected()) {
                                overiddingTheme = EditorPlatform.Theme.MODENA_TOUCH_HIGH_CONTRAST_BLACK_ON_WHITE;
                            }
                            break;
                        case MODENA_TOUCH_HIGH_CONTRAST_YELLOW_ON_BLACK:
                            if (modenaHighContrastBlackonwhiteThemeMenuItem.isSelected()) {
                                overiddingTheme = EditorPlatform.Theme.MODENA_TOUCH_HIGH_CONTRAST_BLACK_ON_WHITE;
                            }
                            break;
                        default:
                            break;
                    }
                break;
                case MODENA_HIGH_CONTRAST_WHITE_ON_BLACK:
                    switch (currentTheme) {
                        case MODENA:
                                if (modenaHighContrastWhiteonblackThemeMenuItem.isSelected()) {
                                overiddingTheme = EditorPlatform.Theme.MODENA_HIGH_CONTRAST_WHITE_ON_BLACK;
                            }
                            break;
                        case MODENA_TOUCH:
                                if (modenaHighContrastWhiteonblackThemeMenuItem.isSelected()) {
                                overiddingTheme = EditorPlatform.Theme.MODENA_TOUCH_HIGH_CONTRAST_WHITE_ON_BLACK;
                            }
                            break;
                        case MODENA_HIGH_CONTRAST_BLACK_ON_WHITE:
                            break;
                        case MODENA_HIGH_CONTRAST_WHITE_ON_BLACK:
                            if (modenaHighContrastWhiteonblackThemeMenuItem.isSelected()) {
                                overiddingTheme = EditorPlatform.Theme.MODENA;
                            }
                            break;
                        case MODENA_HIGH_CONTRAST_YELLOW_ON_BLACK:
                            break;
                        case MODENA_TOUCH_HIGH_CONTRAST_BLACK_ON_WHITE:
                            if (modenaHighContrastWhiteonblackThemeMenuItem.isSelected()) {
                                overiddingTheme = EditorPlatform.Theme.MODENA_TOUCH_HIGH_CONTRAST_WHITE_ON_BLACK;
                            }
                            break;
                        case MODENA_TOUCH_HIGH_CONTRAST_WHITE_ON_BLACK:
                            if (modenaHighContrastWhiteonblackThemeMenuItem.isSelected()) {
                                overiddingTheme = EditorPlatform.Theme.MODENA_TOUCH;
                            }
                            break;
                        case MODENA_TOUCH_HIGH_CONTRAST_YELLOW_ON_BLACK:
                            if (modenaHighContrastWhiteonblackThemeMenuItem.isSelected()) {
                                overiddingTheme = EditorPlatform.Theme.MODENA_TOUCH_HIGH_CONTRAST_WHITE_ON_BLACK;
                            }
                            break;
                        default:
                            break;
                    }
                break;
                case MODENA_HIGH_CONTRAST_YELLOW_ON_BLACK:
                    switch (currentTheme) {
                        case MODENA:
                            if (modenaHighContrastYellowonblackThemeMenuItem.isSelected()) {
                                overiddingTheme = EditorPlatform.Theme.MODENA_HIGH_CONTRAST_YELLOW_ON_BLACK;
                            }
                            break;
                        case MODENA_TOUCH:
                            if (modenaHighContrastYellowonblackThemeMenuItem.isSelected()) {
                                overiddingTheme = EditorPlatform.Theme.MODENA_TOUCH_HIGH_CONTRAST_YELLOW_ON_BLACK;
                            }
                            break;
                        case MODENA_HIGH_CONTRAST_BLACK_ON_WHITE:
                            break;
                        case MODENA_HIGH_CONTRAST_WHITE_ON_BLACK:
                            break;
                        case MODENA_HIGH_CONTRAST_YELLOW_ON_BLACK:
                            if (modenaHighContrastYellowonblackThemeMenuItem.isSelected()) {
                                overiddingTheme = EditorPlatform.Theme.MODENA;
                            }
                            break;
                        case MODENA_TOUCH_HIGH_CONTRAST_BLACK_ON_WHITE:
                            if (modenaHighContrastYellowonblackThemeMenuItem.isSelected()) {
                                overiddingTheme = EditorPlatform.Theme.MODENA_TOUCH_HIGH_CONTRAST_YELLOW_ON_BLACK;
                            }
                            break;
                        case MODENA_TOUCH_HIGH_CONTRAST_WHITE_ON_BLACK:
                            if (modenaHighContrastYellowonblackThemeMenuItem.isSelected()) {
                                overiddingTheme = EditorPlatform.Theme.MODENA_TOUCH_HIGH_CONTRAST_YELLOW_ON_BLACK;
                            }
                            break;
                        case MODENA_TOUCH_HIGH_CONTRAST_YELLOW_ON_BLACK:
                            if (modenaHighContrastYellowonblackThemeMenuItem.isSelected()) {
                                overiddingTheme = EditorPlatform.Theme.MODENA_TOUCH;
                            }
                            break;
                        default:
                            break;
                    }
                break;
                default:
                    assert false;
                    break;
            }

            documentWindowController.getEditorController().setTheme(overiddingTheme);
        }

        @Override
        public boolean isSelected() {
            boolean res;

            if (documentWindowController == null) {
                res = false;
            } else {
                final EditorPlatform.Theme currentTheme
                        = documentWindowController.getEditorController().getTheme();

                switch (theme) {
                    // CASPIAN_HIGH_CONTRAST can be selected only if another CASPIAN
                    // theme is active.
                    case CASPIAN_HIGH_CONTRAST:
                        res = EditorPlatform.isCaspian(currentTheme);
                        break;
                    case CASPIAN:
                        res = (currentTheme == theme || currentTheme == EditorPlatform.Theme.CASPIAN_HIGH_CONTRAST);
                        break;
                    case CASPIAN_EMBEDDED:
                        res = (currentTheme == theme || currentTheme == EditorPlatform.Theme.CASPIAN_EMBEDDED_HIGH_CONTRAST);
                        break;
                    case CASPIAN_EMBEDDED_QVGA:
                        res = (currentTheme == theme || currentTheme == EditorPlatform.Theme.CASPIAN_EMBEDDED_QVGA_HIGH_CONTRAST);
                        break;
                    case MODENA_HIGH_CONTRAST_BLACK_ON_WHITE:
                        res = EditorPlatform.isModenaBlackonwhite(currentTheme)
                                && EditorPlatform.isModenaHighContrast(currentTheme);
                        break;
                    case MODENA_HIGH_CONTRAST_WHITE_ON_BLACK:
                        res = EditorPlatform.isModenaWhiteonblack(currentTheme)
                                && EditorPlatform.isModenaHighContrast(currentTheme);
                        break;
                    case MODENA_HIGH_CONTRAST_YELLOW_ON_BLACK:
                        res = EditorPlatform.isModenaYellowonblack(currentTheme)
                                && EditorPlatform.isModenaHighContrast(currentTheme);
                        break;
                    case MODENA:
                        res = (currentTheme == theme
                                || (EditorPlatform.isModenaHighContrast(currentTheme)
                                    && !EditorPlatform.isModenaTouch(currentTheme)));
                        break;
                    case MODENA_TOUCH:
                        res = (currentTheme == theme || EditorPlatform.isModenaTouchHighContrast(currentTheme));
                        break;
                    default:
                        assert false;
                        res = false;
                        break;
                }
            }

            return res;
        }
    }

    class RemoveSceneStyleSheetActionController extends MenuItemController {

        private final File styleSheet;

        public RemoveSceneStyleSheetActionController(File file) {
            this.styleSheet = file;
        }

        @Override
        public boolean canPerform() {
            return (documentWindowController != null && styleSheet.exists());
        }

        @Override
        public void perform() {
            assert documentWindowController != null;
            documentWindowController.getSceneStyleSheetMenuController().performRemoveSceneStyleSheet(styleSheet);
        }

        @Override
        public String getTitle() {
            return styleSheet.getName();
        }
    }

    class OpenSceneStyleSheetActionController extends MenuItemController {

        private final File styleSheet;

        public OpenSceneStyleSheetActionController(File file) {
            this.styleSheet = file;
        }

        @Override
        public boolean canPerform() {
            return (documentWindowController != null && styleSheet.exists());
        }

        @Override
        public void perform() {
            assert documentWindowController != null;
            documentWindowController.getSceneStyleSheetMenuController().performOpenSceneStyleSheet(styleSheet);
        }

        @Override
        public String getTitle() {
            return styleSheet.getName();
        }
    }

    public MenuItem getMenuItem(KeyCombination key) {
        return keyToMenu.get(key);
    }

    public Set<KeyCombination> getAccelerators() {
        return keyToMenu.keySet();
    }
    
    // Returns a String with no trailing zero; if decimal part is non zero then
    // it is kept.
    private String getStringFromDouble(double value) {
        String res = Double.toString(value);
        if(res.endsWith(".0")) { //NOI18N
            res = Integer.toString((int)value);
        }
        return res;
    }

    /**
     * *************************************************************************
     * Static inner class
     * *************************************************************************
     */
    private static class ClearOpenRecentHandler implements EventHandler<ActionEvent> {

        @Override
        public void handle(ActionEvent t) {
            final PreferencesController pc = PreferencesController.getSingleton();
            final PreferencesRecordGlobal recordGlobal = pc.getRecordGlobal();
            recordGlobal.clearRecentItems();
        }
    }
}
