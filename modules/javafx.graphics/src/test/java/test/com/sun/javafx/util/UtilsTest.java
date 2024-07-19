/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.util;

import com.sun.javafx.util.Utils;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import javafx.geometry.HPos;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.junit.Test;

public class UtilsTest {
    @Test
    public void testSplit() {
        // normal use case
        String s = "VK_ENTER";
        String[] split = Utils.split(s, "_");
        assertEquals("Array content: " + Arrays.toString(split),2, split.length);
        assertEquals("VK", split[0]);
        assertEquals("ENTER", split[1]);

        // normal use case
        s = "VK_LEFT_ARROW";
        split = Utils.split(s, "_");
        assertEquals("Array content: " + Arrays.toString(split),3, split.length);
        assertEquals("VK", split[0]);
        assertEquals("LEFT", split[1]);
        assertEquals("ARROW", split[2]);

        // split with same string as separator - expect empty array
        s = "VK_LEFT_ARROW";
        split = Utils.split(s, "VK_LEFT_ARROW");
        assertEquals("Array content: " + Arrays.toString(split),0, split.length);

        // split with longer string as separator - expect empty array
        s = "VK_LEFT_ARROW";
        split = Utils.split(s, "VK_LEFT_ARROW_EXT");
        assertEquals("Array content: " + Arrays.toString(split),0, split.length);
    }

    @Test
    public void testConvertUnicode() {
        String s = "";
        String r = Utils.convertUnicode(s);
        assertEquals("", r);

        /*String*/ s = "test";
        /*String*/ r = Utils.convertUnicode(s);
        assertEquals("test", r);

        /*String*/ s = "hi\\u1234";
        /*String*/ r = Utils.convertUnicode(s);
        assertEquals("hi\u1234", r);

        /*String*/ s = "\\u5678";
        /*String*/ r = Utils.convertUnicode(s);
        assertEquals("\u5678", r);

        /*String*/ s = "hi\\u1234there\\u432112";
        /*String*/ r = Utils.convertUnicode(s);
        assertEquals("hi\u1234there\u432112", r);

        /*String*/ s = "Hello\u5678There";
        /*String*/ r = Utils.convertUnicode(s);
        assertEquals("Hello\u5678There", r);

        /*String*/ s = "\\this\\is\\a\\windows\\path";
        /*String*/ r = Utils.convertUnicode(s);
        assertEquals("\\this\\is\\a\\windows\\path", r);

        /*String*/ s = "\\this\\is\\a\\12\\windows\\path";
        /*String*/ r = Utils.convertUnicode(s);
        assertEquals("\\this\\is\\a\\12\\windows\\path", r);

        /*String*/ s = "u12u12";
        /*String*/ r = Utils.convertUnicode(s);
        assertEquals("u12u12", r);

        /*String*/ s = "hello\nu1234\n";
        /*String*/ r = Utils.convertUnicode(s);
        assertEquals("hello\nu1234\n", r);
    }

    @Test
    public void testConvertUnicodeFail2_2() {

        //Error case - null
        //String s = null;
        //String r = Utils.convertUnicode(s);
        //assertEquals("", r);

        //String s = "\\";
        //String r = Utils.convertUnicode(s);
        //assertEquals("\\", r);

        //Error case - no length
        //*String*/ s = "hi\\u";
        //*String*/ r = Utils.convertUnicode(s);
        //assertEquals("hi\\u", r);
    }

    @Test
    public void testConvertUnicodeWrong2_2() {

        //Error case - short length
        String s = "hi\\u12";
        String r = Utils.convertUnicode(s);
        //assertEquals("hi\\u12", r);

        /*String*/ s = "\\this\\is\\a\\umm\\windows\\path";
        /*String*/ r = Utils.convertUnicode(s);
        //assertEquals("\\this\\is\\a\\umm\\windows\\path", r);
    }

    @Test
    public void testPointRelativeTo() {
        VBox root = new VBox();
        final Rectangle rectangle = new Rectangle(50, 50, 100, 100);
        root.getChildren().add(rectangle);
        Scene scene = new Scene(root,800,600);
        Stage stage = new Stage();
        stage.setX(0);
        stage.setY(0);
        stage.setScene(scene);
        final Point2D res = Utils.pointRelativeTo(rectangle, 0, 0, HPos.CENTER, VPos.CENTER, 0, 0, false);
        assertEquals(50, res.getX(), 1e-1);
        assertEquals(50, res.getY(), 1e-1);

    }

    @Test
    public void testPointRelativeTo_InSubScene() {
        Group root = new Group();
        Scene scene = new Scene(root,800,600);
        VBox subRoot = new VBox();
        SubScene subScene = new SubScene(subRoot, 100, 100);
        subScene.setLayoutX(20);
        subScene.setLayoutY(20);
        root.getChildren().addAll(subScene);
        final Rectangle rectangle = new Rectangle(50, 50, 100, 100);
        subRoot.getChildren().add(rectangle);
        Stage stage = new Stage();
        stage.setX(0);
        stage.setY(0);
        stage.setScene(scene);
        final Point2D res = Utils.pointRelativeTo(rectangle, 0, 0, HPos.CENTER, VPos.CENTER, 0, 0, false);
        assertEquals(70, res.getX(), 1e-1);
        assertEquals(70, res.getY(), 1e-1);

    }
}
