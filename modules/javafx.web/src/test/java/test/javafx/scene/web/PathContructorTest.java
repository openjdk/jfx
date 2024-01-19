/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.web;

import org.junit.Test;
import static org.junit.Assert.*;


public class PathContructorTest extends TestBase {

    @Test public void testCanvasPathConstructor() {
        final String htmlCanvasContent = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<body style='margin: 0px 0px;'>\n" +
                "<canvas id=\"myCanvas\" width=\"200\" height=\"100\" style=\"border:1px solid grey;\"></canvas>\n" +
                "<script>\n" +
                "const canvas = document.getElementById(\"myCanvas\");\n" +
                "const ctx = canvas.getContext(\"2d\");\n" +
                "p1 = new Path2D();\n" +
                "p1.rect(0,0,200,200);\n" +
                "ctx.fillStyle = 'yellow';\n" +
                "ctx.fill(p1);\n" +
                "p2 = new Path2D(p1);\n" +
                "ctx.fillStyle = 'green';\n" +
                "ctx.fill(p2);\n" +
                "</script>\n" +
                "</body>\n" +
                "</html>";

        loadContent(htmlCanvasContent);

        submit(() -> {
            int greenColor = 128;
            assertEquals("First rect center", greenColor, (int) getEngine().executeScript(
                    "document.getElementById('myCanvas').getContext('2d').getImageData(21, 21, 1, 1).data[1]"));
        });
    }
}
