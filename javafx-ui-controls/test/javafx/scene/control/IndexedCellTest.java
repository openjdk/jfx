/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package javafx.scene.control;

import javafx.scene.control.IndexedCell;
import org.junit.Before;
import org.junit.Test;

import static javafx.scene.control.ControlTestUtils.*;
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
