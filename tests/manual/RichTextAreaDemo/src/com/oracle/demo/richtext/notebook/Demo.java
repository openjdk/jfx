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

package com.oracle.demo.richtext.notebook;

import com.oracle.demo.richtext.notebook.data.CellInfo;
import com.oracle.demo.richtext.notebook.data.Notebook;

public class Demo {
    public static Notebook createNotebookExample() {
        Notebook b = new Notebook();
        {
            CellInfo c = new CellInfo(CellType.TEXT);
            c.setSource(
                """
                Notebook Interface

                A notebook interface or computational notebook is a virtual notebook environment used for literate programming, a method of writing computer programs.  Some notebooks are WYSIWYG environments including executable calculations embedded in formatted documents; others separate calculations and text into separate sections. Notebooks share some goals and features with spreadsheets and word processors but go beyond their limited data models.

                Modular notebooks may connect to a variety of computational back ends, called "kernels". Notebook interfaces are widely used for statistics, data science, machine learning, and computer algebra.

                https://en.wikipedia.org/wiki/Notebook_interface""");
            b.add(c);
        }
        {
            CellInfo c = new CellInfo(CellType.CODE);
            c.setSource(
                """
                /**
                 * This code cell generates a multi-line text result.
                 */
                int x = 5;
                String text = "text";
                print(x);""");
            b.add(c);
        }
        {
            CellInfo c = new CellInfo(CellType.CODE);
            c.setSource(
                """
                //
                // This code cell generates a general failure (exception)
                //
                double sin(double x) {
                    return Math.sin(x);
                }
                print(sin(x) + 5.0);""");
            b.add(c);
        }
        {
            CellInfo c = new CellInfo(CellType.CODE);
            c.setSource(
                """
                //
                // This code cell generates an image output
                //
                display(image);""");
            b.add(c);
        }
        {
            CellInfo c = new CellInfo(CellType.CODE);
            c.setSource(
                """
                // And finally, this code cell generates a Node output.
                // This way any complex result can be rendered: a chart, a table or a spreadsheet, a complex input form...
                //
                var node = new ListView(data);
                render(node);""");
            b.add(c);
        }
        {
            CellInfo c = new CellInfo(CellType.CODE);
            c.setSource(
                """
                // This example simulates a JSON output backed by an external source, such as
                // database or remote API call.
                json = generateJsonOutput();""");
            b.add(c);
        }
        return b;
    }

    public static Notebook createSingleTextCell() {
        Notebook b = new Notebook();
        {
            CellInfo c = new CellInfo(CellType.TEXT);
            c.setSource(
                """
                This is a text cell.
                Right now it is a plain text cell, but we can make it a rich text cell.
                The only problem is that the user can change the cell type - and changing it from rich text to
                code or any other plain text based types will remove the styles.
                We could, of course, save the rich text until the user modifies the text, or may be even preserve
                the style information by simply rendering the plain text paragraphs, but then what would happen if
                the user switches back to rich text after editing?  Worth the try.""");
            b.add(c);
        }
        return b;
    }

    public static Notebook createSingleCodeCell() {
        Notebook b = new Notebook();
        {
            CellInfo c = new CellInfo(CellType.CODE);
            b.add(c);
        }
        return b;
    }
}
