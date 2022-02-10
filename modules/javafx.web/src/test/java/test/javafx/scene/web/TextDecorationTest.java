/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertNotEquals;
import org.junit.Test;

public class TextDecorationTest extends TestBase {

    @Test public void testTokenHeight() throws Exception {
        loadContent("<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "<style>\n" +
                "\n" +
                "h1 { text-decoration: overline;}\n" +
                "\n" +
                "h2 { text-decoration: line-through;}\n" +
                "\n" +
                "h3 { text-decoration: underline;}\n" +
                "\n" +
                "</style>\n" +
                "</head>\n" +
                "<body>\n" +
                "\n" +
                "<h1>oveline test</h1>\n" +
                "<h2>line through test</h2>\n" +
                "<h3>Under line test</h3>\n" +
                "\n" +
                "</body>\n" +
                "</html>");
        /* The line through and underline css text property needs visual test, if line is straight
        so need to check only if style is applied or not for defect id JDK-8280020 */
        assertNotEquals("overline",executeScript("document.getElementsByTagName('h1')[0].style.textDecoration"));
        assertNotEquals("line-through",executeScript("document.getElementsByTagName('h2')[0].style.textDecoration"));
        assertNotEquals("underline",executeScript("document.getElementsByTagName('h3')[0].style.textDecoration"));

    }
}
