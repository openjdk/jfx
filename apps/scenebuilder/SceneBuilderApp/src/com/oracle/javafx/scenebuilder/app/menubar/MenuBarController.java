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
package com.oracle.javafx.scenebuilder.app.menubar;

import com.oracle.javafx.scenebuilder.app.DocumentWindowController;
import com.oracle.javafx.scenebuilder.app.DocumentWindowController.DocumentControlAction;
import com.oracle.javafx.scenebuilder.app.SceneBuilderApp;
import com.oracle.javafx.scenebuilder.app.SceneBuilderApp.ApplicationControlAction;
import com.oracle.javafx.scenebuilder.app.i18n.I18N;
import com.oracle.javafx.scenebuilder.app.preferences.PreferencesController;
import com.oracle.javafx.scenebuilder.app.preferences.PreferencesRecordGlobal;
import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.EditorController.ControlAction;
import com.oracle.javafx.scenebuilder.kit.editor.EditorController.EditAction;
import com.oracle.javafx.scenebuilder.kit.editor.EditorPlatform;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.ContentPanelController;
import com.oracle.javafx.scenebuilder.kit.library.BuiltinSectionComparator;
import com.oracle.javafx.scenebuilder.kit.library.Library;
import com.oracle.javafx.scenebuilder.kit.library.LibraryItem;
import com.oracle.javafx.scenebuilder.kit.library.LibraryItemNameComparator;
import com.oracle.javafx.scenebuilder.kit.util.MathUtils;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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

    private final DebugMenuController debugMenuController;
    private final DocumentWindowController documentWindowController;
    // This member is null when this MenuBarController is used for
    // managing the menu bar passed to MenuBarSkin.setDefaultSystemMenu().

    @FXML
    private MenuBar menuBar;
    @FXML
    private Menu insertMenu;
    @FXML
    private Menu addEffectMenu;
    @FXML
    private Menu fileMenu; // Useless as soon as Preferences menu item is implemented
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
    private MenuItem showPreferencesMenuItem;
    @FXML
    private MenuItem separatorAbovePreferencesMenuItem; // Useless as soon as Preferences menu item is implemented
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
    private MenuItem selectNextRowMenuItem;
    @FXML
    private MenuItem selectNextColumnMenuItem;
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
    private MenuItem editIncludedFileMenuItem;
    @FXML
    private MenuItem revealIncludedFileMenuItem;

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
    private MenuItem showPreviewMenuItem;
    @FXML
    private MenuItem modenaThemeMenuItem;
    @FXML
    private MenuItem caspianThemeMenuItem;
    @FXML
    private MenuItem chooseBackgroundColorMenuItem;
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

    // Window
    // Help
    @FXML
    private MenuItem helpMenuItem;
    @FXML
    private MenuItem aboutMenuItem;

    private static final KeyCombination.Modifier modifier;

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
        debugMenuController = new DebugMenuController(documentWindowController);
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
                menuBar.getMenus().add(debugMenuController.getMenu());
            } else {
                menuBar.getMenus().remove(debugMenuController.getMenu());
            }
        }
    }

    public boolean isDebugMenuVisible() {
        return menuBar.getMenus().contains(debugMenuController.getMenu());
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
        assert separatorAbovePreferencesMenuItem != null;
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
        assert selectNextRowMenuItem != null;
        assert selectNextColumnMenuItem != null;
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
        assert editIncludedFileMenuItem != null;
        assert revealIncludedFileMenuItem != null;

        assert bringToFrontMenuItem != null;
        assert sendToBackMenuItem != null;
        assert bringForwardMenuItem != null;
        assert sendBackwardMenuItem != null;
        assert wrapInAnchorPaneMenuItem != null;
        assert wrapInGridPaneMenuItem != null;
        assert wrapInHBoxMenuItem != null;
        assert wrapInPaneMenuItem != null;
        assert wrapInScrollPaneMenuItem != null;
        assert wrapInSplitPaneMenuItem != null;
        assert wrapInStackPaneMenuItem != null;
        assert wrapInTabPaneMenuItem != null;
        assert wrapInTitledPaneMenuItem != null;
        assert wrapInToolBarMenuItem != null;
        assert wrapInVBoxMenuItem != null;
        assert wrapInGroupMenuItem != null;
        assert unwrapMenuItem != null;

        assert showPreviewMenuItem != null;
        assert modenaThemeMenuItem != null;
        assert caspianThemeMenuItem != null;
        assert chooseBackgroundColorMenuItem != null;
        assert addSceneStyleSheetMenuItem != null;
        assert removeSceneStyleSheetMenu != null;
        assert openSceneStyleSheetMenu != null;
        assert setResourceMenuItem != null;
        assert removeResourceMenuItem != null;
        assert revealResourceMenuItem != null;

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
            revealMenuKey = "menu.title.reveal.win";
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
        openRecentMenu.setOnShowing(new EventHandler<Event>() {
            @Override
            public void handle(Event t) {
                updateOpenRecentMenuItems();
            }
        });
        saveMenuItem.setUserData(new DocumentControlActionController(DocumentControlAction.SAVE_FILE));
        saveMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.S, modifier));
        saveAsMenuItem.setUserData(new DocumentControlActionController(DocumentControlAction.SAVE_AS_FILE));
        saveAsMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHIFT_DOWN, modifier));
        revertMenuItem.setUserData(new DocumentControlActionController(DocumentControlAction.REVERT_FILE));
        revealMenuItem.setUserData(new DocumentControlActionController(DocumentControlAction.REVEAL_FILE));
        closeMenuItem.setUserData(new ApplicationControlActionController(ApplicationControlAction.CLOSE_FRONT_WINDOW));
        closeMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.W, modifier));
        showPreferencesMenuItem.setUserData(new ApplicationControlActionController(ApplicationControlAction.SHOW_PREFERENCES));
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
        copyMenuItem.setUserData(new ControlActionController(ControlAction.COPY));
        copyMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.C, modifier));
        cutMenuItem.setUserData(new EditActionController(EditAction.CUT));
        cutMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.X, modifier));
        pasteMenuItem.setUserData(new EditActionController(EditAction.PASTE));
        pasteMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.V, modifier));
        pasteIntoMenuItem.setUserData(new EditActionController(EditAction.PASTE_INTO));
        pasteIntoMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.V, KeyCombination.SHIFT_DOWN, modifier));
        duplicateMenuItem.setUserData(new EditActionController(EditAction.DUPLICATE));
        duplicateMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.D, modifier));
        deleteMenuItem.setUserData(new EditActionController(EditAction.DELETE));
        deleteMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.DELETE));
        selectAllMenuItem.setUserData(new ControlActionController(ControlAction.SELECT_ALL));
        selectAllMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.A, modifier));
        selectNoneMenuItem.setUserData(new ControlActionController(ControlAction.SELECT_NONE));
        selectNoneMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.A, KeyCombination.SHIFT_DOWN, modifier));
        selectParentMenuItem.setUserData(new ControlActionController(ControlAction.SELECT_PARENT));
        selectParentMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.L, modifier));
        selectNextMenuItem.setUserData(new ControlActionController(ControlAction.SELECT_NEXT));
        selectPreviousMenuItem.setUserData(new ControlActionController(ControlAction.SELECT_PREVIOUS));
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
        toggleHierarchyPanelMenuItem.setUserData(new DocumentControlActionController(DocumentControlAction.TOGGLE_HIERARCHY_PANEL) {
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
//        toggleOutlinesMenuItem.setUserData(new ControlActionController(ControlAction.));
        toggleOutlinesMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.E, modifier));
//        toggleSampleDataMenuItem.setUserData(new DocumentControlActionController(DocumentControlAction.GOTO_CONTENT));
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
//        showSampleControllerMenuItem.setUserData(new DocumentControlActionController(DocumentControlAction.GOTO_CONTENT));
        updateZoomMenu();

        /*
         * Insert menu
         */
        updateInsertMenu();
        if (documentWindowController != null) {
            this.documentWindowController.getEditorController().libraryProperty().addListener(
                    new ChangeListener<Library>() {
                        @Override
                        public void changed(ObservableValue<? extends Library> ov, Library t, Library t1) {
                            updateInsertMenu();
                        }
                    });
        }

        /*
         * Modify menu
         */
        fitToParentMenuItem.setUserData(new EditActionController(EditAction.FIT_TO_PARENT));
        fitToParentMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.K, modifier));
        useComputedSizesMenuItem.setUserData(new EditActionController(EditAction.USE_COMPUTED_SIZES));
        useComputedSizesMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.K, KeyCombination.SHIFT_DOWN, modifier));
        selectNextRowMenuItem.setUserData(new ControlActionController(ControlAction.SELECT_NEXT_ROW));
        selectNextColumnMenuItem.setUserData(new ControlActionController(ControlAction.SELECT_NEXT_COLUMN));
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
        editIncludedFileMenuItem.setUserData(new DocumentControlActionController(DocumentControlAction.EDIT_INCLUDED_FILE));

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
        wrapInAnchorPaneMenuItem.setAccelerator(
                new KeyCodeCombination(KeyCode.A, KeyCombination.ALT_DOWN, modifier));
        wrapInGroupMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_GROUP));
        wrapInGridPaneMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_GRID_PANE));
        wrapInGridPaneMenuItem.setAccelerator(
                new KeyCodeCombination(KeyCode.G, KeyCombination.ALT_DOWN, modifier));
        wrapInHBoxMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_HBOX));
        wrapInHBoxMenuItem.setAccelerator(
                new KeyCodeCombination(KeyCode.H, KeyCombination.ALT_DOWN, modifier));
        wrapInPaneMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_PANE));
        wrapInScrollPaneMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_SCROLL_PANE));
        wrapInSplitPaneMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_SPLIT_PANE));
        wrapInSplitPaneMenuItem.setAccelerator(
                new KeyCodeCombination(KeyCode.S, KeyCombination.ALT_DOWN, modifier));
        wrapInStackPaneMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_STACK_PANE));
        wrapInTabPaneMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_TAB_PANE));
        wrapInTitledPaneMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_TITLED_PANE));
        wrapInTitledPaneMenuItem.setAccelerator(
                new KeyCodeCombination(KeyCode.T, KeyCombination.ALT_DOWN, modifier));
        wrapInToolBarMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_TOOL_BAR));
        wrapInVBoxMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_VBOX));
        wrapInVBoxMenuItem.setAccelerator(
                new KeyCodeCombination(KeyCode.V, KeyCombination.ALT_DOWN, modifier));
        wrapInGroupMenuItem.setUserData(new EditActionController(EditAction.WRAP_IN_GROUP));
        unwrapMenuItem.setUserData(new EditActionController(EditAction.UNWRAP));
        unwrapMenuItem.setAccelerator(
                new KeyCodeCombination(KeyCode.U, modifier));

        /*
         * Preview menu
         */
        showPreviewMenuItem.setUserData(new DocumentControlActionController(DocumentControlAction.SHOW_PREVIEW_WINDOW));
        showPreviewMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.P, modifier));
        chooseBackgroundColorMenuItem.setUserData(new DocumentControlActionController(DocumentControlAction.CHOOSE_BACKGROUND_COLOR));
        chooseBackgroundColorMenuItem.setDisable(true);
        caspianThemeMenuItem.setUserData(new SetThemeActionController(EditorPlatform.Theme.CASPIAN));
        modenaThemeMenuItem.setUserData(new SetThemeActionController(EditorPlatform.Theme.MODENA));

        addSceneStyleSheetMenuItem.setUserData(new DocumentControlActionController(DocumentControlAction.ADD_SCENE_STYLE_SHEET));
        updateOpenAndRemoveSceneStyleSheetMenus();
        if (documentWindowController != null) {
            this.documentWindowController.getEditorController().sceneStyleSheetProperty().addListener(new ChangeListener<ObservableList<File>>() {

                @Override
                public void changed(ObservableValue<? extends ObservableList<File>> ov, ObservableList<File> t, ObservableList<File> t1) {
                    if (t1 != null) {
                        updateOpenAndRemoveSceneStyleSheetMenus();
                        setupMenuItemHandlers(removeSceneStyleSheetMenu);
                        setupMenuItemHandlers(openSceneStyleSheetMenu);
                    }
                }
            });
        }

        setResourceMenuItem.setUserData(new DocumentControlActionController(DocumentControlAction.SET_RESOURCE));
        removeResourceMenuItem.setUserData(new DocumentControlActionController(DocumentControlAction.REMOVE_RESOURCE) {
            @Override
            public boolean canPerform() {
                if (documentWindowController == null
                        || documentWindowController.getEditorController().getResource() == null) {
                    return false;
                }

                return true;
            }

            @Override
            public String getTitle() {
                String title = I18N.getString("menu.title.remove.resource");
                if (documentWindowController != null
                        && documentWindowController.getEditorController().getResource() != null) {
                    title = I18N.getString("menu.title.remove.resource.with.file",
                            documentWindowController.getEditorController().getResource().getName());
                }

                return title;
            }
        });
        revealResourceMenuItem.setUserData(new DocumentControlActionController(DocumentControlAction.REVEAL_RESOURCE) {
            @Override
            public boolean canPerform() {
                if (documentWindowController == null
                        || documentWindowController.getEditorController().getResource() == null) {
                    return false;
                }

                return true;
            }

            @Override
            public String getTitle() {
                String title = I18N.getString("menu.title.reveal.resource");
                if (documentWindowController != null
                        && documentWindowController.getEditorController().getResource() != null) {
                    title = I18N.getString("menu.title.reveal.resource.with.file",
                            documentWindowController.getEditorController().getResource().getName());
                }

                return title;
            }
        });

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
         * For Window menu, they are overriden with specific handlers.
         */
        for (Menu m : menuBar.getMenus()) {
            setupMenuItemHandlers(m);
        }
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
        }
    }

    private final EventHandler<Event> onMenuValidationEventHandler
            = new EventHandler<Event>() {
                @Override
                public void handle(Event t) {
                    assert t.getSource() instanceof Menu;
                    handleOnMenuValidation((Menu) t.getSource());
                }
            };

    private void handleOnMenuValidation(Menu menu) {
        for (MenuItem i : menu.getItems()) {
            final boolean disable, selected;
            final String title;
            if (i.getUserData() instanceof MenuItemController) {
                final MenuItemController c = (MenuItemController) i.getUserData();
                disable = !c.canPerform();
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
            = new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent t) {
                    assert t.getSource() instanceof MenuItem;
                    handleOnActionMenu((MenuItem) t.getSource());
                }
            };

    private void handleOnActionMenu(MenuItem i) {
        assert i.getUserData() instanceof MenuItemController;
        final MenuItemController c = (MenuItemController) i.getUserData();
        c.perform();
    }

    /*
     * Private (zoom menu)
     */
    private void updateZoomMenu() {
        final double[] scalingTable = {0.25, 0.50, 0.75, 1.00, 1.50, 2.0, 4.0};

        for (int i = 0; i < scalingTable.length; i++) {
            final double scaling = scalingTable[i];
            final String title = "%" + (int) (scaling * 100); //NOI18N
            final RadioMenuItem mi = new RadioMenuItem(title);
            mi.setUserData(new SetZoomActionController(scaling));
            zoomMenu.getItems().add(mi);
        }
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
            for (String recentItem : recentItems) {
                final MenuItem mi = new MenuItem(recentItem);
                mi.setOnAction(new EventHandler<ActionEvent>() {

                    @Override
                    public void handle(ActionEvent t) {
                        final File file = new File(recentItem);
                        SceneBuilderApp.getSingleton().performOpenRecent(documentWindowController, file);
                    }
                });
                menuItems.add(mi);
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
    private void updateInsertMenu() {
        assert insertMenu != null;

        insertMenu.getItems().clear();

        if (documentWindowController == null) {
            insertMenu.getItems().add(new MenuItem(I18N.getString("menubar.no.lib.item")));
        } else {
            final EditorController editorController = documentWindowController.getEditorController();
            assert editorController.getLibrary() != null;

            final Map<String, Set<LibraryItem>> sectionMap
                    = new TreeMap<>(new BuiltinSectionComparator());
            for (LibraryItem li : editorController.getLibrary().getItems()) {
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
    }

    private Menu makeMenuForLibrarySection(String section) {
        final Menu result = new Menu();
        result.setText(section);
        result.setOnShowing(new EventHandler<Event>() {
            @Override
            public void handle(Event t) {
                updateInsertMenuState(result);
            }
        });
        return result;
    }

    private MenuItem makeMenuItemForLibraryItem(final LibraryItem li) {
        final MenuItem result = new MenuItem();

        result.setText(li.getName());
        result.setUserData(li);
        result.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                handleInsertMenuAction(li);
            }
        });
        return result;
    }

    private void updateInsertMenuState(Menu sectionMenu) {
        final EditorController editorController = documentWindowController.getEditorController();
        for (MenuItem menuItem : sectionMenu.getItems()) {
            assert menuItem.getUserData() instanceof LibraryItem;
            final LibraryItem li = (LibraryItem) menuItem.getUserData();
            final boolean enabled = editorController.canPerformInsert(li);
            menuItem.setDisable(!enabled);
        }
    }

    private void handleInsertMenuAction(LibraryItem li) {
        final EditorController editorController = documentWindowController.getEditorController();
        editorController.performInsert(li);
    }

    /*
     * Private (Add Effect menu)
     */
    private void updateAddEffectMenu() {
        addEffectMenu.getItems().clear();
        for (Class<? extends Effect> c : EditorController.getEffectsSupportingAddition()) {
            addEffectMenu.getItems().add(makeMenuItemForEffect(c));
        }
    }

    private MenuItem makeMenuItemForEffect(Class<? extends Effect> effectClass) {
        final Menu result = new Menu();
        result.setText(effectClass.getSimpleName());
        result.setUserData(new AddEffectActionController(effectClass));
        return result;
    }

    /*
     * Private (window menu)
     */
    private final EventHandler<Event> onWindowMenuValidationHandler
            = new EventHandler<Event>() {
                @Override
                public void handle(Event t) {
                    assert t.getSource() == windowMenu;
                    handleOnWindowMenuValidation();
                }
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
            if (documentWindowController == null) {
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
            if (documentWindowController == null) {
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
            if (documentWindowController == null) {
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
            // Right now, all document actions are always enabled
            return true;
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
            // TODO(elp) : to be implemented
            return false;
        }

        @Override
        public void perform() {
            throw new UnsupportedOperationException("Not supported yet: effectClass=" + effectClass);  //NOI18N
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

    class SetThemeActionController extends MenuItemController {

        private final EditorPlatform.Theme theme;

        public SetThemeActionController(EditorPlatform.Theme theme) {
            this.theme = theme;
        }

        @Override
        public boolean canPerform() {
            return (documentWindowController != null);
        }

        @Override
        public void perform() {
            assert documentWindowController != null;
            documentWindowController.getEditorController().setTheme(theme);
        }

        @Override
        public boolean isSelected() {
            boolean result;

            if (documentWindowController == null) {
                result = false;
            } else {
                final EditorPlatform.Theme currentTheme
                        = documentWindowController.getEditorController().getTheme();
                result = currentTheme == theme;
            }

            return result;
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
