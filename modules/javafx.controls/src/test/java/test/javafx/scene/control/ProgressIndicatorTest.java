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

package test.javafx.scene.control;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


import javafx.scene.control.ProgressIndicator;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import com.sun.javafx.tk.Toolkit;

import org.junit.Before;
import org.junit.Test;

/**
 */
public class ProgressIndicatorTest {
    Toolkit tk;

    @Before public void setup() {
        tk = Toolkit.getToolkit();
    }

    @Test public void progressIndicatorHeightTest() {
        ProgressIndicator pi = new ProgressIndicator(0.5);

        HBox hb = new HBox();
        hb.setAlignment(Pos.CENTER);
        hb.getChildren().addAll(pi);

        Scene scene = new Scene(hb, 400, 400);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();
        tk.firePulse();

        // Test the ProgressIndicator height is non zero and
        // does not occupy entire height of the scene.
        int x = Double.compare(0.0, pi.getHeight());
        assertTrue(x != 0);

        x = Double.compare(400.0, pi.getHeight());
        assertTrue(x != 0);
    }

    @Test public void progressIndicatorWidthTest() {
        ProgressIndicator pi = new ProgressIndicator(0.5);

        VBox vb = new VBox();
        vb.setAlignment(Pos.CENTER);
        vb.getChildren().addAll(pi);

        Scene scene = new Scene(vb, 400, 400);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();

        tk.firePulse();

        // Test the ProgressIndicator width is non zero and
        // does not occupy entire width of the scene.
        int x = Double.compare(0.0, pi.getWidth());
        assertTrue(x != 0);

        x = Double.compare(400.0, pi.getWidth());
        assertTrue(x != 0);
    }
}
