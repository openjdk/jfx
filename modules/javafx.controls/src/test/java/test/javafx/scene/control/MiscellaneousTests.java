/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.control;

import test.com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.Toolkit;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import junit.framework.Assert;
import org.junit.Test;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class MiscellaneousTests {

    @Test
    public void test_RT_31168() {
        //
        // Make sure that a control added and removed from the scene-graph before css is processed
        // gets css processed when it is added back in.

        Button button = new Button("RT-31168");
        Rectangle rectangle = new Rectangle(50,50);

        Group container = new Group();
        container.getChildren().add(rectangle);

        Scene scene = new Scene(new Group(container, new Button("button")));

        //
        // Gotta put this in a window for the pulse listener to get hooked up (see Scene#initPeer().
        // Need the pulse listener since we want to enter root via Scene#doCSSPass()
        //
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        //
        // Has to be a pulse since we want to enter from Scene.doCSSPass()
        //
        ((StubToolkit)Toolkit.getToolkit()).fireTestPulse();

        container.getChildren().set(0, button);
        container.getChildren().set(0, rectangle);

        //
        // Has to be a pulse since we want to enter from Scene.doCSSPass()
        //
        ((StubToolkit)Toolkit.getToolkit()).fireTestPulse();

        // Should be null since the button was added and removed before the pulse processed css
        assertNull(button.getBackground());

        container.getChildren().set(0, button);

        ((StubToolkit)Toolkit.getToolkit()).fireTestPulse();

        // Should no longer be null
        assertNotNull(button.getBackground());

    }

    @Test public void test_RT_33103() {

        HBox box = new HBox();

        TextField field = new TextField();
        Label badLabel = new Label("Field:", field);

        box.getChildren().addAll(badLabel, field);

        Scene scene = new Scene(box);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        ((StubToolkit)Toolkit.getToolkit()).fireTestPulse();

        assertSame(badLabel, field.getParent());

    }

    @Test public void test_RT_33080() {

        // Rough approximation of sample code from the bug and steps to reproduce

        final HBox root = new HBox(10);

        final RadioButton rb1 = new RadioButton("RB1");
        final RadioButton rb2 = new RadioButton("RB2");

        root.getChildren().addAll(rb1, rb2);

        Scene scene = new Scene(root);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        ((StubToolkit)Toolkit.getToolkit()).fireTestPulse();

        // click on rb1
        rb1.setSelected(true);

        // pulse
        ((StubToolkit)Toolkit.getToolkit()).fireTestPulse();

        // change font
        rb1.setFont(new Font("system", 22));
        rb2.setFont(new Font("system", 22));

        // pulse
        ((StubToolkit) Toolkit.getToolkit()).fireTestPulse();

        // click on rb1 again
        rb1.setSelected(false);

        // pulse
        ((StubToolkit) Toolkit.getToolkit()).fireTestPulse();

        // At this point, if the bug is present, the width and height of the buttons will be different.
        Bounds b1 = rb1.getLayoutBounds();
        Bounds b2 = rb2.getLayoutBounds();

        Assert.assertEquals(rb1.getWidth(), rb2.getWidth(), 0.00001);
    }

}
