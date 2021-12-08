/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.control.Button;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.Mnemonic;
import javafx.collections.ObservableList;
import javafx.scene.input.KeyCombination;

import com.sun.javafx.scene.control.behavior.MnemonicInfo.MnemonicKeyCombination;
import javafx.scene.Node;
import javafx.scene.control.skin.ButtonSkin;
import javafx.scene.shape.Rectangle;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;

import java.lang.ref.WeakReference;

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
        Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException)throwable;
            } else {
                Thread.currentThread().getThreadGroup().uncaughtException(thread, throwable);
            }
        });
    }

    @After public void cleanup() {
        Thread.currentThread().setUncaughtExceptionHandler(null);
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

    class ButtonSkin1 extends ButtonSkin {
        ButtonSkin1(Button btn) {
            super(btn);
        }
    }

    class ButtonSkin2 extends ButtonSkin {
        ButtonSkin2(Button btn) {
            super(btn);
        }
    }

    @Test
    public void testOldSkinShouldGC() {
        Button button = new Button();
        Group root = new Group(button);
        Scene scene = new Scene(root);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        WeakReference<ButtonSkin> defSkinRef = new WeakReference<>((ButtonSkin)button.getSkin());
        ButtonSkin skin = new ButtonSkin1(button);
        WeakReference<ButtonSkin> oldSkinRef = new WeakReference<>(skin);
        button.setSkin(skin);
        skin = new ButtonSkin2(button);
        WeakReference<ButtonSkin> currSkinRef = new WeakReference<>(skin);
        button.setSkin(skin);
        skin = null;

        attemptGC(oldSkinRef);
        assertNull("Old ButtonSkin must be GCed.", oldSkinRef.get());
        assertNull("Default ButtonSkin must be GCed.", defSkinRef.get());
        assertNotNull("Current ButtonSkin must NOT be GCed.", currSkinRef.get());
    }

    @Test
    public void testUnusedSkinShouldGC() {
        Button button = new Button();
        Group root = new Group(button);
        Scene scene = new Scene(root);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        WeakReference<ButtonSkin> defSkinRef = new WeakReference<>((ButtonSkin)button.getSkin());
        ButtonSkin skin = new ButtonSkin1(button);
        WeakReference<ButtonSkin> skinRef1 = new WeakReference<>(skin);
        skin = new ButtonSkin2(button);
        WeakReference<ButtonSkin> skinRef2 = new WeakReference<>(skin);
        skin = null;

        attemptGC(skinRef1);
        assertNull("Unused ButtonSkin must be GCed.", skinRef1.get());
        assertNull("Unused ButtonSkin must be GCed.", skinRef2.get());
        assertNotNull("Default ButtonSkin must NOT be GCed.", defSkinRef.get());
    }

    @Test
    public void testButtonAndSkinShouldGC() {
        Button button = new Button();
        ButtonSkin skin = new ButtonSkin1(button);
        WeakReference<Button> buttonRef = new WeakReference<>(button);
        WeakReference<ButtonSkin> skinRef = new WeakReference<>(skin);
        button.setSkin(skin);
        button = null;
        skin = null;

        attemptGC(skinRef);
        assertNull("Button must be GCed.", buttonRef.get());
        assertNull("ButtonSkin must be GCed.", skinRef.get());
    }

    @Test
    public void testNPEOnSwitchSkinAndRemoveButton() {
        Button button = new Button();
        Group root = new Group(button);
        Scene scene = new Scene(root);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        button.setSkin(new ButtonSkin1(button));
        root.getChildren().remove(button);
    }

    @Test
    public void testDefaultButtonNullSkinReleased() {
        Button button = new Button();
        button.setDefaultButton(true);
        Group root = new Group(button);
        Scene scene = new Scene(root);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();
        WeakReference<ButtonSkin> defSkinRef = new WeakReference<>((ButtonSkin)button.getSkin());
        KeyCodeCombination key = new KeyCodeCombination(KeyCode.ENTER);
        assertNotNull(scene.getAccelerators().get(key));
        button.setSkin(null);
        assertNull(scene.getAccelerators().get(key));

        attemptGC(defSkinRef);
        assertNull("ButtonSkin must be GCed", defSkinRef.get());
    }

    @Test
    public void testCancelButtonNullSkinReleased() {
        Button button = new Button();
        button.setCancelButton(true);
        Group root = new Group(button);
        Scene scene = new Scene(root);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();
        WeakReference<ButtonSkin> defSkinRef = new WeakReference<>((ButtonSkin)button.getSkin());
        KeyCodeCombination key = new KeyCodeCombination(KeyCode.ESCAPE);
        assertNotNull(scene.getAccelerators().get(key));
        button.setSkin(null);
        assertNull(scene.getAccelerators().get(key));

        attemptGC(defSkinRef);
        assertNull("ButtonSkin must be GCed", defSkinRef.get());
    }

    private void attemptGC(WeakReference<ButtonSkin> weakRef) {
        for (int i = 0; i < 10; i++) {
            System.gc();
            System.runFinalization();

            if (weakRef.get() == null) {
                break;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                fail("InterruptedException occurred during Thread.sleep()");
            }
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
