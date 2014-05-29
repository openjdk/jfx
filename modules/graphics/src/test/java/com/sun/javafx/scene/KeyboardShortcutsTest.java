/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.javafx.scene;

import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.Mnemonic;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;


/*
** Test keyboard shortcuts.
*/
public final class KeyboardShortcutsTest {

    private Stage stage;
    private Scene scene;

    @Before
    public void setUp() {
        stage = new Stage();
        scene = new Scene(new Group(), 500, 500);
        stage.setScene(scene);

        stage.show();
    }

    @After
    public void tearDown() {
        stage = null;
        scene = null;
    }


    /*
    ** Add a mnemonic
    ** Make sure that it's there.
    */
    @Test
    public void addMnemonicTest() {
        boolean nodeFound = false;
        final Text node = new Text("text");

        ((Group)scene.getRoot()).getChildren().add(node);

        KeyCodeCombination mnemonicKeyCombo =
            new KeyCodeCombination(KeyCode.Q,KeyCombination.ALT_DOWN);

        Mnemonic myMnemonic = new Mnemonic(node, mnemonicKeyCombo);
        scene.addMnemonic(myMnemonic);

        ObservableList<Mnemonic> mnemonicsList = scene.getMnemonics().get(mnemonicKeyCombo);
        if (mnemonicsList != null) {
            for (int i = 0 ; i < mnemonicsList.size() ; i++) {
                if (mnemonicsList.get(i).getNode() == node) {
                    nodeFound = true;
                }
            }
        }
        assertTrue(nodeFound);
    }


    /*
    ** Add a mnemonic, then remove it.
    ** Make sure that it's gone.
    */
    @Test
    public void addAndRemoveMnemonicTest() {
        boolean nodeFound = false;
        final Text node = new Text("text");

        ((Group)scene.getRoot()).getChildren().add(node);

        KeyCodeCombination mnemonicKeyCombo =
            new KeyCodeCombination(KeyCode.Q,KeyCombination.ALT_DOWN);

        Mnemonic myMnemonic = new Mnemonic(node, mnemonicKeyCombo);
        scene.addMnemonic(myMnemonic);

        /*
        ** remove it.....
        */
        scene.removeMnemonic(myMnemonic);

        ObservableList<Mnemonic> mnemonicsList = scene.getMnemonics().get(mnemonicKeyCombo);
        if (mnemonicsList != null) {
            for (int i = 0 ; i < mnemonicsList.size() ; i++) {
                if (mnemonicsList.get(i).getNode() == node) {
                    nodeFound = true;
                }
            }
        }
        assertTrue(!nodeFound);
    }

    @Test
    public void mnemonicRemovedWithNodeTest() {
        final Text node = new Text("text");
        ((Group)scene.getRoot()).getChildren().add(node);

        KeyCodeCombination mnemonicKeyCombo =
                new KeyCodeCombination(KeyCode.Q,KeyCombination.ALT_DOWN);

        Mnemonic myMnemonic = new Mnemonic(node, mnemonicKeyCombo);
        scene.addMnemonic(myMnemonic);

        ObservableList<Mnemonic> mnemonicsList = scene.getMnemonics().get(mnemonicKeyCombo);

        assertTrue(mnemonicsList.contains(myMnemonic));

        scene.setRoot(new Group());

        assertFalse(mnemonicsList.contains(myMnemonic));

    }
}
