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

package region;

import javafx.scene.layout.Region;

/**
 *
 */
public class RegionBorderImageUITest extends RegionUITestBase {
    public static void main(String[] args) {
        launch(args);
    }

    public void button(Region region) {
        region.setStyle(
                "-fx-border-image-source: url('region/BlackButton.png');" +
                "-fx-border-image-slice: 25 95 25 95 fill;" +
                "-fx-border-image-width: 25 95 25 95;" +
                "-fx-border-image-repeat: stretch;");
    }

    public void button2(Region region) {
        region.setStyle(
                "-fx-border-image-source: url('region/WindowsButton.png');" +
                "-fx-border-image-slice: 24 5 24 5 fill;" +
                "-fx-border-image-width: 24 5 24 5;" +
                "-fx-border-image-repeat: stretch;");
    }

    public void test(Region region) {
        region.setStyle(
                "-fx-border-image-source: url('region/border.png');" +
                "-fx-border-image-slice: 27;" +
                "-fx-border-image-width: 27;" +
                "-fx-border-image-repeat: round;");
    }

    public void test2(Region region) {
        region.setStyle(
                "-fx-border-image-source: url('region/border.png');" +
                "-fx-border-image-slice: 27;" +
                "-fx-border-image-width: 27;" +
                "-fx-border-image-repeat: repeat;");
    }

    public void test3(Region region) {
        region.setStyle(
                "-fx-border-image-source: url('region/border.png');" +
                "-fx-border-image-slice: 27;" +
                "-fx-border-image-width: 27;" +
                "-fx-border-image-repeat: space;");
    }

    public void test4(Region region) {
        region.setStyle(
                "-fx-border-image-source: url('region/border.png');" +
                "-fx-border-image-slice: 27;" +
                "-fx-border-image-width: 27;" +
                "-fx-border-image-repeat: stretch;");
    }

    public void differentSliceAndWidths(Region region) {
        region.setStyle(
                "-fx-border-image-source: url('region/border.png');" +
                "-fx-border-color:green;" +
                "-fx-border-image-repeat: repeat;" +
                "-fx-border-image-slice: 27;" +
                "-fx-border-image-width: 10 15 20 25;");
    }

    public void withInsets(Region region) {
        region.setStyle(
                "-fx-border-image-source: url('region/popover-no-arrow-empty.png');" +
                "-fx-border-image-slice: 78 50 60 50;" +
                "-fx-border-image-width: 78 50 60 50;");
    }

    public void withInsets2(Region region) {
        region.setStyle(
                "-fx-border-image-source: url('region/popover-no-arrow-empty.png');" +
                "-fx-border-image-slice: 78 50 60 50;" +
                "-fx-border-image-width: 78 50 60 50;" +
                "-fx-border-image-insets: -27 -37 -47 -37;");
    }

    public void repeatX(Region region) {
        region.setStyle("-fx-border-image-source: url('region/border.png');" +
                        "-fx-border-image-repeat: repeat-x;" +
                        "-fx-border-image-slice: 28;" +
                        "-fx-border-image-width: 28;");
    }

    public void repeatY(Region region) {
        region.setStyle("-fx-border-image-source: url('region/border.png');" +
                        "-fx-border-image-repeat: repeat-y;" +
                        "-fx-border-image-slice: 28;" +
                        "-fx-border-image-width: 28;");
    }

    public void testWider(Region region) {
        region.setStyle(
                "-fx-border-image-source: url('region/border.png');" +
                "-fx-border-image-slice: 27;" +
                "-fx-border-image-width: 40;" +
                "-fx-border-image-repeat: round;");
    }

    public void testWider2(Region region) {
        region.setStyle(
                "-fx-border-image-source: url('region/border.png');" +
                "-fx-border-image-slice: 27;" +
                "-fx-border-image-width: 40;" +
                "-fx-border-image-repeat: repeat;");
    }

    public void testWider3(Region region) {
        region.setStyle(
                "-fx-border-image-source: url('region/border.png');" +
                "-fx-border-image-slice: 27;" +
                "-fx-border-image-width: 40;" +
                "-fx-border-image-repeat: space;");
    }

    public void testWider4(Region region) {
        region.setStyle(
                "-fx-border-image-source: url('region/border.png');" +
                "-fx-border-image-slice: 27;" +
                "-fx-border-image-width: 40;" +
                "-fx-border-image-repeat: stretch;");
    }

    public void repeatXWider(Region region) {
        region.setStyle("-fx-border-image-source: url('region/border.png');" +
                        "-fx-border-image-repeat: repeat-x;" +
                        "-fx-border-image-slice: 28;" +
                        "-fx-border-image-width: 40;");
    }

    public void repeatYWider(Region region) {
        region.setStyle("-fx-border-image-source: url('region/border.png');" +
                        "-fx-border-image-repeat: repeat-y;" +
                        "-fx-border-image-slice: 28;" +
                        "-fx-border-image-width: 40;");
    }
}
