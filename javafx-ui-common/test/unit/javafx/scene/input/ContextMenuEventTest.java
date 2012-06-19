/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
package javafx.scene.input;

import com.sun.javafx.pgstub.StubScene;
import javafx.scene.Group;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.shape.Rectangle;
import javafx.event.EventHandler;
import javafx.stage.Stage;

import org.junit.Test;

public class ContextMenuEventTest {

    @Test public void mouseTriggerKeepsCoordinates() {
        Rectangle rect = new Rectangle(100, 100);
        rect.setTranslateX(100);
        rect.setTranslateY(100);
        Group root = new Group(rect);
        Scene scene = new Scene(root);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();
        rect.requestFocus();

        rect.setOnContextMenuRequested(new EventHandler<ContextMenuEvent>() {
            @Override public void handle(ContextMenuEvent event) {
                assertEquals(1.0, event.getX(), 0.0001);
                assertEquals(101, event.getSceneX(), 0.0001);
                assertEquals(201, event.getScreenX(), 0.0001);
                assertEquals(2.0, event.getY(), 0.0001);
                assertEquals(102, event.getSceneY(), 0.0001);
                assertEquals(202, event.getScreenY(), 0.0001);
                assertFalse(event.isKeyboardTrigger());
            }
        });

        ((StubScene) scene.impl_getPeer()).getListener().menuEvent(
                101, 102, 201, 202, false);
    }

    @Test public void keyTriggerSetsCoordinatesToFocusOwner() {
        Rectangle rect = new Rectangle(100, 100);
        rect.setTranslateX(100);
        rect.setTranslateY(100);
        Group root = new Group(rect);
        Scene scene = new Scene(root);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();
        rect.requestFocus();

        rect.setOnContextMenuRequested(new EventHandler<ContextMenuEvent>() {
            @Override public void handle(ContextMenuEvent event) {
                assertEquals(25.0, event.getX(), 0.0001);
                assertEquals(125, event.getSceneX(), 0.0001);
                assertEquals(225, event.getScreenX(), 0.0001);
                assertEquals(50.0, event.getY(), 0.0001);
                assertEquals(150, event.getSceneY(), 0.0001);
                assertEquals(250, event.getScreenY(), 0.0001);
                assertTrue(event.isKeyboardTrigger());
            }
        });

        ((StubScene) scene.impl_getPeer()).getListener().menuEvent(
                101, 102, 201, 202, true);
    }
}
