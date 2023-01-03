/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test the Image to DataURL function
 */
public class SwingDependencyTest extends TestBase {

    private final PrintStream err = System.err;

    private static final String htmlContent = "\n"
        + "<!DOCTYPE html>\n"
        + "<html>\n"
        + "<body>\n"
        + "<canvas id=\"theCanvas\" width=\"200\" height=\"100\">\n"
        + "</canvas>\n"
        + "<p id = \"encodedText\">\n"
        + "</p>\n"
        + "<script>\n"
        + "var c = document.getElementById(\"theCanvas\");\n"
        + "var ctx = c.getContext(\"2d\");\n"
        + "var my_gradient=ctx.createLinearGradient(0,0,0,75);\n"
        + "my_gradient.addColorStop(0,\"red\");\n"
        + "my_gradient.addColorStop(0.5,\"green\");\n"
        + "my_gradient.addColorStop(1,\"blue\");\n"
        + "ctx.fillStyle=my_gradient;\n"
        + "ctx.fillRect(0,0,150,75);\n"
        + "var dataURL = c.toDataURL();\n"
        + "document.getElementById(\"encodedText\").innerHTML=dataURL;\n"
        + "</script>\n"
        + "</body>\n"
        + "</html>\n";

    @Test
    public void testSwingDependency() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        System.setErr(new PrintStream(bytes));

        loadContent(htmlContent);

        System.setErr(err);

        Assert.assertFalse("ClassNotFoundException found",
                            bytes.toString().contains("ClassNotFoundException"));
    }

    @After
    public void resetSystemErr() {
        System.setErr(err);
    }
}
