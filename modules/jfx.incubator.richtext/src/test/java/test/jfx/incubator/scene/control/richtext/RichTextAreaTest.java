/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
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

package test.jfx.incubator.scene.control.richtext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.sun.jfx.incubator.scene.control.richtext.VFlow;
import jfx.incubator.scene.control.richtext.RichTextArea;
import jfx.incubator.scene.control.richtext.RichTextAreaShim;
import jfx.incubator.scene.control.richtext.SelectionSegment;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;
import jfx.incubator.scene.control.richtext.skin.RichTextAreaSkin;

/**
 * Tests RichTextArea control.
 */
public class RichTextAreaTest {
    private RichTextArea control;

    @BeforeEach
    public void beforeEach() {
        control = new RichTextArea();
        control.setSkin(new RichTextAreaSkin(control));
    }

    @AfterEach
    public void afterEach() {
    }

    @Test
    public void execute() {
        control.appendText("a");
        control.execute(RichTextArea.Tag.SELECT_ALL);

        SelectionSegment sel = control.getSelection();
        TextPos end = control.getDocumentEnd();
        assertNotNull(end);
        assertEquals(TextPos.ZERO, sel.getMin());
        assertEquals(end, sel.getMax());
    }

    @Test
    public void selectAll() {
        control.appendText("a");
        control.selectAll();

        SelectionSegment sel = control.getSelection();
        TextPos end = control.getDocumentEnd();
        assertNotNull(end);
        assertEquals(TextPos.ZERO, sel.getMin());
        assertEquals(end, sel.getMax());
    }

    /**
     * Tests the shim.
     */
    // TODO remove once a real test which needs the shim is added.
    @Test
    public void testShim() {
        RichTextArea t = new RichTextArea();
        VFlow f = RichTextAreaShim.vflow(t);
    }
}
