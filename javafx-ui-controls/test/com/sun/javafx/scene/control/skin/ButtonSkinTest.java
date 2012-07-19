/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.javafx.scene.control.skin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import javafx.geometry.Insets;
import javafx.scene.control.Button;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.input.Mnemonic;
import javafx.collections.ObservableList;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import org.junit.Before;
import org.junit.Test;

/**
 */
public class ButtonSkinTest {
    private Button button;
    private ButtonSkinMock skin;

    @Before public void setup() {
        button = new Button("Test");
        skin = new ButtonSkinMock(button);
        // Set some padding so that any places where padding was being
        // computed but wasn't expected will be caught.
        button.setPadding(new Insets(10, 10, 10, 10));
        button.setSkin(skin);

    }

    @Test public void maxWidthTracksPreferred() {
        button.setPrefWidth(500);
        assertEquals(500, button.maxWidth(-1), 0);
    }

    @Test public void maxHeightTracksPreferred() {
        button.setPrefHeight(500);
        assertEquals(500, button.maxHeight(-1), 0);
    }

    @Test
    public void testMnemonicAutoParseAddition() {
        if(!com.sun.javafx.PlatformUtil.isMac()) {
            boolean nodeFound = false;
            Stage stage = new Stage();
            Scene scene = new Scene(new Group(), 500, 500);
            stage.setScene(scene);
            
            button.setMnemonicParsing(true);
            button.setText("_Mnemonic");
            
            ((Group)scene.getRoot()).getChildren().add(button);
        
            stage.show();
        
            KeyCodeCombination mnemonicKeyCombo =
                new KeyCodeCombination(KeyCode.M,KeyCombination.ALT_DOWN);

            ObservableList<Mnemonic> mnemonicsList = scene.getMnemonics().get(mnemonicKeyCombo);
            if (mnemonicsList != null) {
                for (int i = 0 ; i < mnemonicsList.size() ; i++) {
                    if (mnemonicsList.get(i).getNode() == button) {
                        nodeFound = true;
                    }
                }
            }
            assertTrue(nodeFound);
        }
    }

   
    @Test
    public void testMnemonicAutoParseAdditionRemovalOnParentChange() {
        if(!com.sun.javafx.PlatformUtil.isMac()) {
            boolean nodeFound = false;
            Stage stage = new Stage();
            Scene scene = new Scene(new Group(), 500, 500);
            stage.setScene(scene);
            
            button.setMnemonicParsing(true);
            button.setText("_AnotherMnemonic");
            
            ((Group)scene.getRoot()).getChildren().add(button);
        
            stage.show();
        
            KeyCodeCombination mnemonicKeyCombo =
                new KeyCodeCombination(KeyCode.A,KeyCombination.ALT_DOWN);

            ObservableList<Mnemonic> mnemonicsList = scene.getMnemonics().get(mnemonicKeyCombo);
            if (mnemonicsList != null) {
                for (int i = 0 ; i < mnemonicsList.size() ; i++) {
                    if (mnemonicsList.get(i).getNode() == button) {
                        nodeFound = true;
                    }
                }
            }
            assertTrue(nodeFound);

            nodeFound = false;

            ((Group)scene.getRoot()).getChildren().remove(button);
  
            mnemonicsList = scene.getMnemonics().get(mnemonicKeyCombo);
            if (mnemonicsList != null) {
                for (int i = 0 ; i < mnemonicsList.size() ; i++) {
                    if (mnemonicsList.get(i).getNode() == button) {
                        nodeFound = true;
                    }
                }
            }
            assertTrue(!nodeFound);
        }
    }

    public static final class ButtonSkinMock extends ButtonSkin {
        boolean propertyChanged = false;
        int propertyChangeCount = 0;
        public ButtonSkinMock(Button button) {
            super(button);
        }
        
        @Override protected void handleControlPropertyChanged(String p) {
            super.handleControlPropertyChanged(p);
            propertyChanged = true;
            propertyChangeCount++;
        }
    }
}
