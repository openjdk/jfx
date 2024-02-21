/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

/**
<p>Provides classes that represent {@code RichTextArea} Control.</p>
<h2>Examples</h2>
<p>Creating a RichTextArea with a simple editable rich text model:</p>
<pre>
<code>
    RichTextArea textArea = new RichTextArea();
</code>
</pre>
<p>Creating a read-only RichTextArea with rich text content:</p>
<pre>
<code>
    SimpleReadOnlyStyledModel m = new SimpleReadOnlyStyledModel();
    // add text segment using CSS style name (requires a style sheet)
    m.addSegment("RichTextArea ", null, "HEADER");
    // add text segment using direct style
    m.addSegment("Demo", "-fx-font-size:200%;", null);
    // newline
    m.nl();

    RichTextArea t = new RichTextArea(m);
</code>
</pre>
 * <BR><b><a href="https://openjdk.org/jeps/11">Incubating Feature.</a>
 * Will be removed in a future release.</b>
 */
package javafx.incubator.scene.control.rich;
