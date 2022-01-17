/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.skin.MenuButtonSkinBase;
import javafx.scene.input.Mnemonic;
import javafx.stage.Stage;

public class MenuButtonSkinBaseTest {

    private MenuButton menubutton;
    private MenuItem menuItem;

    @Before
    public void setup() {
        menubutton = new MenuButton();
        menuItem = new MenuItem("Menu Item");
        menubutton.getItems().add(menuItem);
        menubutton.setSkin(new MenuButtonSkinBase<>(menubutton));
    }

    @Test
    public void testNoNullPointerOnRemovingFromTheSceneWhilePopupIsShowing() {
        Thread.UncaughtExceptionHandler originalExceptionHandler = Thread.currentThread().getUncaughtExceptionHandler();

        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
            Assert.fail("No exception expected, but was a " + e);
            e.printStackTrace();
        });

        try {
            Scene scene = new Scene(menubutton);
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.show();

            menubutton.show();
            menuItem.setOnAction(e -> scene.setRoot(new MenuButton()));
            menuItem.fire();

        } finally {
            Thread.currentThread().setUncaughtExceptionHandler(originalExceptionHandler);
        }
    }

    @Test
    public void testMnemonicsRemovedOnRemovingFromTheSceneWhilePopupIsShowing() {
        menuItem.setText("_Menu Item");
        menuItem.setMnemonicParsing(true);

        ObjectProperty<Mnemonic> menuItemMnemonic = new SimpleObjectProperty<>();

        Scene scene = new Scene(menubutton) {
            @Override
            public void addMnemonic(Mnemonic m) {
                if (menuItemMnemonic.get() != null) {
                    // The test is designed for only one mnemonic.
                    Assert.fail("Test failure: More than one Mnemonic registered.");
                }
                menuItemMnemonic.set(m);
                super.addMnemonic(m);
            }

            @Override
            public void removeMnemonic(Mnemonic m) {
                if (m == menuItemMnemonic.get()) {
                    menuItemMnemonic.set(null);
                }
                super.removeMnemonic(m);
            }
        };
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        menubutton.show();
        menuItem.setOnAction(e -> scene.setRoot(new MenuButton()));
        menuItem.fire();

        Assert.assertNull("Mnemonic was not removed from the scene,", menuItemMnemonic.get());
    }
}
