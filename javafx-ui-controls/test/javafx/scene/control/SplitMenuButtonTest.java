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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author lubermud
 */
public class SplitMenuButtonTest {
    private SplitMenuButton splitMenuButton;

    @Before public void setup() {
        splitMenuButton = new SplitMenuButton();
    }

    @Test public void defaultConstructorHasSizeZeroItems() {
        assertEquals(0, splitMenuButton.getItems().size());
    }

    @Test public void defaultConstructorHasTrueMnemonicParsing() {
        assertTrue(splitMenuButton.isMnemonicParsing());
    }

    @Test public void oneArgConstructorNullHasSizeZeroItems() {
        SplitMenuButton smb2 = new SplitMenuButton((MenuItem[])null);
        assertEquals(0, smb2.getItems().size());
    }

    @Test public void oneArgConstructorNullHasTrueMnemonicParsing() {
        SplitMenuButton smb2 = new SplitMenuButton((MenuItem[])null);
        assertTrue(smb2.isMnemonicParsing());
    }

    @Test public void oneArgConstructorSpecifiedMenuItemHasGreaterThanZeroItems() {
        SplitMenuButton smb2 = new SplitMenuButton(new MenuItem());
        assertTrue(smb2.getItems().size() > 0);
    }

    @Test public void oneArgConstructorSpecifiedMenuItemHasTrueMnemonicParsing() {
        SplitMenuButton smb2 = new SplitMenuButton(new MenuItem());
        assertTrue(smb2.isMnemonicParsing());
    }

    @Test public void oneArgConstructorSpecifiedMenuItemsHasGreaterThanZeroItems() {
        SplitMenuButton smb2 = new SplitMenuButton(new MenuItem("One"), new MenuItem("Two"));
        assertTrue(smb2.getItems().size() > 0);
    }

    @Test public void oneArgConstructorSpecifiedMenuItemsHasTrueMnemonicParsing() {
        SplitMenuButton smb2 = new SplitMenuButton(new MenuItem("One"), new MenuItem("Two"));
        assertTrue(smb2.isMnemonicParsing());
    }



    @Test public void splitMenuButtonIsFiredIsNoOp() {
        splitMenuButton.fire(); // should throw no exceptions, if it does, the test fails
    }
}
