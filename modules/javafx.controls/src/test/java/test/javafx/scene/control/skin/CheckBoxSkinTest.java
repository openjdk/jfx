/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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
import javafx.scene.control.CheckBox;
import javafx.scene.control.skin.CheckBoxSkin;
import com.sun.javafx.tk.Toolkit;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Test;

/**
 */
public class CheckBoxSkinTest {
    private CheckBox checkbox;
    private CheckBoxSkinMock skin;
    private static Toolkit tk;
    private Scene scene;
    private Stage stage;

    @BeforeClass public static void initToolKit() {
        tk = Toolkit.getToolkit();
    }

    @Before public void setup() {
        checkbox = new CheckBox("Test");
        skin = new CheckBoxSkinMock(checkbox);
        // Set some padding so that any places where padding was being
        // computed but wasn't expected will be caught.
        checkbox.setPadding(new Insets(10, 10, 10, 10));
        checkbox.setSkin(skin);

        scene = new Scene(new Group(checkbox));
        stage = new Stage();
        stage.setScene(scene);
        stage.show();
        tk.firePulse();
    }

    @Test public void maxWidthTracksPreferred() {
        checkbox.setPrefWidth(500);
        assertEquals(500, checkbox.maxWidth(-1), 0);
    }

    @Test public void maxHeightTracksPreferred() {
        checkbox.setPrefHeight(500);
        assertEquals(500, checkbox.maxHeight(-1), 0);
    }

    @Test public void testPadding() {
        checkbox.setPadding(new Insets(10, 20, 30, 40));

        tk.firePulse();

        double expectedArea = checkbox.getHeight() * checkbox.getWidth();
        double actualArea = checkbox.getSkin().getNode().computeAreaInScreen();

        assertEquals(expectedArea, actualArea, 0.001);
    }

    public static final class CheckBoxSkinMock extends CheckBoxSkin {
        boolean propertyChanged = false;
        int propertyChangeCount = 0;
        public CheckBoxSkinMock(CheckBox checkbox) {
            super(checkbox);
        }

        public void addWatchedProperty(ObservableValue<?> p) {
            p.addListener(o -> {
                propertyChanged = true;
                propertyChangeCount++;
            });
        }
    }
}
