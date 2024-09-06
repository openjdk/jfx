/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.oracle.demo.richtext.notebook;

import com.oracle.demo.richtext.notebook.data.CellInfo;
import com.oracle.demo.richtext.notebook.data.Notebook;

/**
 * Canned notebooks.
 *
 * @author Andy Goryachev
 */
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
