/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

import javafx.scene.control.IndexedCell;
import org.junit.Before;
import org.junit.Test;

import static com.sun.javafx.scene.control.infrastructure.ControlTestUtils.*;
import static org.junit.Assert.*;

/**
 */
public class IndexedCellTest {
    private IndexedCell<String> cell;

    @Before public void setup() {
        cell = new IndexedCell<String>();
    }

    @Test public void defaultStyleClassShouldBe_indexed_cell() {
        assertStyleClassContains(cell, "indexed-cell");
    }

    @Test public void indexIsNegativeOneByDefault() {
        assertEquals(-1, cell.getIndex());
        assertEquals(-1, cell.indexProperty().get());
    }

    @Test public void indexPropertyReferencesBean() {
        assertSame(cell, cell.indexProperty().getBean());
    }

    @Test public void indexPropertyHasCorrectName() {
        assertEquals("index", cell.indexProperty().getName());
    }

    @Test public void updateIndexWithNegativeNumber() {
        cell.updateIndex(-5);
        assertEquals(-5, cell.getIndex());
        assertEquals(-5, cell.indexProperty().get());
    }

    @Test public void updateIndexWithPositiveNumber() {
        cell.updateIndex(5);
        assertEquals(5, cell.getIndex());
        assertEquals(5, cell.indexProperty().get());
    }

    @Test public void updateIndexWithZero() {
        cell.updateIndex(0);
        assertEquals(0, cell.getIndex());
        assertEquals(0, cell.indexProperty().get());
    }

    @Test public void pseudoClassIsEvenWhenIndexIsEven() {
        cell.updateIndex(4);
        assertPseudoClassExists(cell, "even");
        assertPseudoClassDoesNotExist(cell, "odd");
    }

    @Test public void pseudoClassIsOddWhenIndexIsOdd() {
        cell.updateIndex(3);
        assertPseudoClassExists(cell, "odd");
        assertPseudoClassDoesNotExist(cell, "even");
    }

    @Test public void pseudoClassIsEventWhenIndexIsZero() {
        cell.updateIndex(0);
        assertPseudoClassExists(cell, "even");
        assertPseudoClassDoesNotExist(cell, "odd");
    }
}
