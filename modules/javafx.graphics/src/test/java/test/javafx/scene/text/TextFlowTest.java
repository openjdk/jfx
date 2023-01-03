/*
 * Copyright (c) 2019, 2022, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.text;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.stage.Stage;
import test.com.sun.javafx.pgstub.StubToolkit;

import com.sun.javafx.tk.Toolkit;

import org.junit.Test;

public class TextFlowTest {

    @Test public void testTabSize() {
        Toolkit tk = Toolkit.getToolkit();

        assertTrue(tk instanceof StubToolkit);  // Ensure it's StubToolkit

        VBox root = new VBox();
        Scene scene = new Scene(root);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.setWidth(300);
        stage.setHeight(200);

        try {
            Text text1 = new Text("\tfirst");
            Text text2 = new Text("\tsecond");
            TextFlow textFlow = new TextFlow(text1, text2);
            textFlow.setPrefWidth(TextFlow.USE_COMPUTED_SIZE);
            textFlow.setMaxWidth(TextFlow.USE_PREF_SIZE);
            root.getChildren().addAll(textFlow);
            stage.show();
            tk.firePulse();
            assertEquals(8, textFlow.getTabSize());
            // initial width with default 8-space tab
            double widthT8 = textFlow.getBoundsInLocal().getWidth();
            text1.setTabSize(4);
            text2.setTabSize(3);
            // StubToolkit is reusing a StubTextLayout ?
            tk.getTextLayoutFactory().disposeLayout(tk.getTextLayoutFactory().getLayout());
            // Tab size of contained text nodes should not have any effect.
            tk.firePulse();
            assertEquals(widthT8, textFlow.getBoundsInLocal().getWidth(), 0.0);

            textFlow.setTabSize(1);
            tk.firePulse();
            // width with tab at 1 spaces
            double widthT1 = textFlow.getBoundsInLocal().getWidth();
            assertTrue(widthT1 < widthT8);

            textFlow.setTabSize(20);
            tk.firePulse();
            double widthT20 = textFlow.getBoundsInLocal().getWidth();
            assertTrue(widthT20 > widthT8);

            assertEquals(20, textFlow.getTabSize());
            assertEquals(20, textFlow.tabSizeProperty().get());

            textFlow.tabSizeProperty().set(10);
            tk.firePulse();
            double widthT10 = textFlow.getBoundsInLocal().getWidth();
            assertTrue(widthT10 > widthT8);
            assertTrue(widthT10 < widthT20);

            assertEquals(10, textFlow.getTabSize());
            assertEquals(10, textFlow.tabSizeProperty().get());

            // tab size of contained text nodes isn't modified by TextFlow
            assertEquals(4, text1.getTabSize());
            assertEquals(3, text2.getTabSize());

            // Test clamping
            textFlow.tabSizeProperty().set(0);
            assertEquals(0, textFlow.tabSizeProperty().get());
            assertEquals(0, textFlow.getTabSize());
            tk.firePulse();
            double widthT0Clamp = textFlow.getBoundsInLocal().getWidth();
            // values < 1 are treated as 1
            assertEquals(widthT1, widthT0Clamp, 0.5);
        } finally {
            stage.hide();
        }

    }
}