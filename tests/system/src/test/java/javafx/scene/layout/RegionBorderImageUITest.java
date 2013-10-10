/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package javafx.scene.layout;

import org.junit.Test;

/**
 */
public class RegionBorderImageUITest extends RegionUITestBase {
    @Test public void dummy() {

    }
//    @Test(timeout=20000)
//    public void test() {
//        setStyle(
//                "-fx-border-image-source: url('javafx/scene/layout/border.png');" +
//                "-fx-border-image-slice: 10;" +
//                "-fx-border-image-width: 10;" +
//                "-fx-border-image-repeat: round;");
//
//        System.out.println("WHAT");
//    }
//
//    @Test(timeout=20000)
//    public void test2() {
//        setStyle(
//                "-fx-border-image-source: url('javafx/scene/layout/border.png');" +
//                "-fx-border-image-slice: 10;" +
//                "-fx-border-image-width: 10;" +
//                "-fx-border-image-repeat: repeat;");
//
//        System.out.println("WHAT");
//    }
//
//    @Test(timeout=20000)
//    public void test3() {
//        setStyle(
//                "-fx-border-image-source: url('javafx/scene/layout/border-uneven.png');" +
//                "-fx-border-image-slice: 11;" +
//                "-fx-border-image-width: 11;" +
//                "-fx-border-image-repeat: space;");
//
//        System.out.println("WHAT");
//    }
//
    @Test(timeout=20000)
    public void test4() {
        setStyle(
                "-fx-border-image-source: url('javafx/scene/layout/border-stretch.png');" +
                "-fx-border-image-slice: 14;" +
                "-fx-border-image-width: 14;" +
                "-fx-border-image-repeat: stretch;");

        System.out.println("WHAT");
    }

//    @Test(timeout=20000)
//    public void differentSliceAndWidths() {
//        setStyle(
//                "-fx-border-image-source: url('javafx/scene/layout/border.png');" +
//                "-fx-border-color:green;" +
//                "-fx-border-image-repeat: repeat;" +
//                "-fx-border-image-slice: 27;" +
//                "-fx-border-image-width: 10 15 20 25;");
//    }
//
//    @Test(timeout=20000)
//    public void repeatX() {
//        setStyle("-fx-border-image-source: url('javafx/scene/layout/border.png');" +
//                        "-fx-border-image-repeat: repeat-x;" +
//                        "-fx-border-image-slice: 28;" +
//                        "-fx-border-image-width: 28;");
//    }
//
//    @Test(timeout=20000)
//    public void repeatY() {
//        setStyle("-fx-border-image-source: url('javafx/scene/layout/border.png');" +
//                        "-fx-border-image-repeat: repeat-y;" +
//                        "-fx-border-image-slice: 28;" +
//                        "-fx-border-image-width: 28;");
//    }
//
//    @Test(timeout=20000)
//    public void testWider() {
//        setStyle(
//                "-fx-border-image-source: url('javafx/scene/layout/border.png');" +
//                "-fx-border-image-slice: 27;" +
//                "-fx-border-image-width: 40;" +
//                "-fx-border-image-repeat: round;");
//    }
//
//    @Test(timeout=20000)
//    public void testWider2() {
//        setStyle(
//                "-fx-border-image-source: url('javafx/scene/layout/border.png');" +
//                "-fx-border-image-slice: 27;" +
//                "-fx-border-image-width: 40;" +
//                "-fx-border-image-repeat: repeat;");
//    }
//
//    @Test(timeout=20000)
//    public void testWider3() {
//        setStyle(
//                "-fx-border-image-source: url('javafx/scene/layout/border.png');" +
//                "-fx-border-image-slice: 27;" +
//                "-fx-border-image-width: 40;" +
//                "-fx-border-image-repeat: space;");
//    }
//
//    @Test(timeout=20000)
//    public void testWider4() {
//        setStyle(
//                "-fx-border-image-source: url('javafx/scene/layout/border.png');" +
//                "-fx-border-image-slice: 27;" +
//                "-fx-border-image-width: 40;" +
//                "-fx-border-image-repeat: stretch;");
//    }
//
//    @Test(timeout=20000)
//    public void repeatXWider() {
//        setStyle("-fx-border-image-source: url('javafx/scene/layout/border.png');" +
//                        "-fx-border-image-repeat: repeat-x;" +
//                        "-fx-border-image-slice: 28;" +
//                        "-fx-border-image-width: 40;");
//    }
//
//    @Test(timeout=20000)
//    public void repeatYWider() {
//        setStyle("-fx-border-image-source: url('javafx/scene/layout/border.png');" +
//                        "-fx-border-image-repeat: repeat-y;" +
//                        "-fx-border-image-slice: 28;" +
//                        "-fx-border-image-width: 40;");
//    }

//    @Test(timeout=20000)
//    public void withInsets() {
//        setStyle(
//                "-fx-border-image-source: url('javafx/scene/layout/popover-no-arrow-empty.png');" +
//                "-fx-border-image-slice: 78 50 60 50;" +
//                "-fx-border-image-width: 78 50 60 50;");
//    }
//
//    @Test(timeout=20000)
//    public void withInsets2() {
//        setStyle(
//                "-fx-border-image-source: url('javafx/scene/layout/popover-no-arrow-empty.png');" +
//                "-fx-border-image-slice: 78 50 60 50;" +
//                "-fx-border-image-width: 78 50 60 50;" +
//                "-fx-border-image-insets: -27 -37 -47 -37;");
//    }
//
//    @Test(timeout=20000)
//    public void button() {
//        setStyle(
//                "-fx-border-image-source: url('javafx/scene/layout/BlackButton.png');" +
//                "-fx-border-image-slice: 25 95 25 95 fill;" +
//                "-fx-border-image-width: 25 95 25 95;" +
//                "-fx-border-image-repeat: stretch;");
//    }
}
