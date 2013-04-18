/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.scene.control;

import static com.sun.javafx.scene.control.infrastructure.ControlTestUtils.*;

import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.Toolkit;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;
import static org.junit.Assert.*;


import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author srikalyc
 */
public class SeparatorMenuItemTest {
    private SeparatorMenuItem separatorMenuItem, smi;
    private Node node;
    private Toolkit tk;

    @Before public void setup() {
        tk = (StubToolkit)Toolkit.getToolkit();//This step is not needed (Just to make sure StubToolkit is loaded into VM)
        node = new Rectangle();
        separatorMenuItem = smi = new SeparatorMenuItem();
    }
    
   
   
    /*********************************************************************
     * Tests for default values                                         *
     ********************************************************************/
    
    @Test public void defaultConstructorShouldSetStyleClassTo_separatormenuitem() {
        assertStyleClassContains(separatorMenuItem, "separator-menu-item");
    }

    @Test public void defaultSeparatorNotNullAndHorizontal() {
        assertNotNull(separatorMenuItem.getContent());
        assertTrue(separatorMenuItem.getContent() instanceof Separator);
        assertSame(((Separator)(separatorMenuItem.getContent())).getOrientation(), Orientation.HORIZONTAL);
    }

    @Test public void defaultHideOnClickFalse() {
        assertFalse(separatorMenuItem.isHideOnClick());
    }

    @Test public void defaultConstructorShouldHaveNotNullContent() {
        assertNotNull(smi.getContent());
    }

    @Test public void defaultConstructorShouldBeSeparator() {
        assertTrue(smi.getContent() instanceof Separator);
    }

    @Test public void defaultConstructorShouldBeHorizontalSeparator() {
        Separator sep = (Separator)(smi.getContent());
        assertEquals(Orientation.HORIZONTAL, sep.getOrientation());
    }

    @Test public void defaultConstructorCanChangeSeparatorOrientation() {
        Separator sep = (Separator)(smi.getContent());
        sep.setOrientation(Orientation.VERTICAL);
        assertEquals(Orientation.VERTICAL, sep.getOrientation());
    }

    @Test public void defaultConstructorShouldHaveFalseHideClick() {
        assertFalse(smi.isHideOnClick());
    }

    @Test public void defaultConstructorShouldHaveNullGraphic() {
        assertNull(smi.getGraphic());
    }

    @Test public void defaultConstructorShouldHaveNullText() {
        assertNull(smi.getText());
    }
}
