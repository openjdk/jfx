/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.control.skin;

import static org.junit.Assert.assertEquals;

import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.control.Button;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.input.Mnemonic;
import javafx.collections.ObservableList;
import javafx.scene.input.KeyCombination;

import com.sun.javafx.scene.control.behavior.TextBinding.MnemonicKeyCombination;
import javafx.scene.Node;
import javafx.scene.control.skin.ButtonSkin;
import javafx.scene.shape.Rectangle;

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

    private long countMnemonicNodes(Scene scene, KeyCombination mnemonicKeyCombo, Node node) {
        ObservableList<Mnemonic> mnemonicsList = scene.getMnemonics().get(mnemonicKeyCombo);
        if (mnemonicsList != null) {
            return mnemonicsList
                    .stream()
                    .filter(m -> m.getNode() == node)
                    .count();
        }
        return 0;
    }

    @Test
    public void testMnemonicAutoParseAddition() {
        if(!com.sun.javafx.PlatformUtil.isMac()) {
            Stage stage = new Stage();
            Scene scene = new Scene(new Group(), 500, 500);
            stage.setScene(scene);

            button.setMnemonicParsing(true);
            button.setText("_Mnemonic");

            ((Group)scene.getRoot()).getChildren().add(button);

            stage.show();

            KeyCombination mnemonicKeyCombo = new MnemonicKeyCombination("M");
            assertEquals(1, countMnemonicNodes(scene, mnemonicKeyCombo, button));
        }
    }


    @Test
    public void testMnemonicAutoParseAdditionRemovalOnParentChange() {
        if(!com.sun.javafx.PlatformUtil.isMac()) {
            Stage stage = new Stage();
            Scene scene = new Scene(new Group(), 500, 500);
            stage.setScene(scene);

            button.setMnemonicParsing(true);
            button.setText("_AnotherMnemonic");

            ((Group)scene.getRoot()).getChildren().add(button);

            stage.show();

            KeyCombination mnemonicKeyCombo = new MnemonicKeyCombination("A");
            assertEquals(1, countMnemonicNodes(scene, mnemonicKeyCombo, button));

            ((Group)scene.getRoot()).getChildren().remove(button);
            assertEquals(0, countMnemonicNodes(scene, mnemonicKeyCombo, button));
        }
    }

    @Test
    public void testMnemonicDoesntDuplicateOnGraphicsChange() {
        if(!com.sun.javafx.PlatformUtil.isMac()) {
            Stage stage = new Stage();
            Scene scene = new Scene(new Group(), 500, 500);
            stage.setScene(scene);

            button.setMnemonicParsing(true);
            button.setText("_Mnemonic");
            Rectangle graphic = new Rectangle(10, 10);
            button.setGraphic(graphic);

            ((Group)scene.getRoot()).getChildren().add(button);

            stage.show();

            KeyCombination mnemonicKeyCombo = new MnemonicKeyCombination("M");

            assertEquals(1, countMnemonicNodes(scene, mnemonicKeyCombo, button));

            graphic.setWidth(20); // force graphic layoutBounds invalidation
            button.layout();

            assertEquals(1, countMnemonicNodes(scene, mnemonicKeyCombo, button));
        }
    }

    public static final class ButtonSkinMock extends ButtonSkin {
        boolean propertyChanged = false;
        int propertyChangeCount = 0;
        public ButtonSkinMock(Button button) {
            super(button);
        }

        public void addWatchedProperty(ObservableValue<?> p) {
            p.addListener(o -> {
                propertyChanged = true;
                propertyChangeCount++;
            });
        }
    }
}
