/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.event.EventUtil;
import com.sun.javafx.pgstub.StubScene;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ToggleGroupTest {
    private ToggleButton b1, b2, b3, b4;
    private ToggleGroup g1, g2;

    private ToggleButton make(String s) {
        final ToggleButton btn = new ToggleButton(s);
        btn.setId(s);
        return btn;
    }

    private boolean isMember(ToggleButton b, ToggleGroup g) {
        for (Toggle t : g.getToggles()) {
            if (t == b) return true;
        }
        return false;
    }

    private boolean isNotMember(ToggleButton b, ToggleGroup g) {
        return !isMember(b, g);
    }

    @Before public void setup() {
        b1 = make("one");
        b2 = make("two");
        b3 = make("three");
        b4 = make("four");
        g1 = new ToggleGroup();
        g2 = new ToggleGroup();
    }

    // really a test of ToggleButton, not ToggleGroup
    @Test public void testButtonInit() {
        assertFalse(b1.isSelected());
        assertNull(b1.getToggleGroup());
    }

    @Test public void testGroupInit() {
        assertTrue(g1.getToggles().isEmpty());
        assertNull(g1.getSelectedToggle());
    }

    @Test public void testGroupMembership() {
        b1.setToggleGroup(g1);
        b2.setToggleGroup(g2);
        b3.setToggleGroup(g1);
        b4.setToggleGroup(g2);

        assertTrue(isMember(b1, g1));
        assertTrue(isMember(b2, g2));
        assertTrue(isMember(b3, g1));
        assertTrue(isMember(b4, g2));

        b3.setToggleGroup(null);
        b4.setToggleGroup(g1);

        assertTrue(isNotMember(b3, g1));
        assertTrue(isMember(b4, g1));
        assertTrue(isNotMember(b4, g2));
    }

    @Test public void testSelectAfterAdding() {
        b1.setToggleGroup(g1);
        b2.setToggleGroup(g1);
        b3.setToggleGroup(g1);

        b1.setSelected(true);
        assertTrue(b1.isSelected());
        assertEquals(b1, g1.getSelectedToggle());
        b2.setSelected(true);
        assertFalse(b1.isSelected());
        assertTrue(b2.isSelected());
        assertEquals(b2, g1.getSelectedToggle());
    }

    @Test public void testSelectBeforeAdding1() {
        b2.setSelected(true);
        b1.setToggleGroup(g1);
        b2.setToggleGroup(g1);
        b3.setToggleGroup(g1);

        assertFalse(b1.isSelected());
        assertTrue(b2.isSelected());
        assertFalse(b3.isSelected());
        assertEquals(b2, g1.getSelectedToggle());
    }

    @Test public void testSelectBeforeAdding2() {
        b2.setSelected(true);
        b3.setSelected(true);
        b1.setToggleGroup(g1);
        b2.setToggleGroup(g1);
        b3.setToggleGroup(g1);

        assertFalse(b1.isSelected());
        assertFalse(b2.isSelected());
        assertTrue(b3.isSelected());
        assertEquals(b3, g1.getSelectedToggle());
    }

    @Test public void testDeselect() {
        b2.setSelected(true);
        b1.setToggleGroup(g1);
        b2.setToggleGroup(g1);
        b3.setToggleGroup(g1);
        b2.setSelected(false);

        assertFalse(b1.isSelected());
        assertFalse(b2.isSelected());
        assertFalse(b3.isSelected());
        assertNull(g1.getSelectedToggle());
    }

    @Test public void testSetSelected() {
        b1.setToggleGroup(g1);
        b2.setToggleGroup(g1);
        b3.setToggleGroup(g1);

        g1.selectToggle(b1);
        assertEquals(b1, g1.getSelectedToggle());
        assertTrue(b1.isSelected());
        assertFalse(b2.isSelected());
        assertFalse(b3.isSelected());

        g1.selectToggle(b2);
        assertEquals(b2, g1.getSelectedToggle());
        assertFalse(b1.isSelected());
        assertTrue(b2.isSelected());
        assertFalse(b3.isSelected());

        g1.selectToggle(null);
        assertNull(g1.getSelectedToggle());
        assertFalse(b1.isSelected());
        assertFalse(b2.isSelected());
        assertFalse(b3.isSelected());
    }

    @Test public void testSetSelectedWrongGroup() {
        b1.setToggleGroup(g1);
        b2.setToggleGroup(g1);
        b3.setToggleGroup(g2);
        b4.setToggleGroup(g2);
        g1.selectToggle(b1);
        g2.selectToggle(b3);

        assertTrue(b1.isSelected());
        assertFalse(b2.isSelected());
        assertTrue(b3.isSelected());
        assertFalse(b4.isSelected());
        assertEquals(b1, g1.getSelectedToggle());
        assertEquals(b3, g2.getSelectedToggle());

        // this is illegal, nothing should change
        g2.selectToggle(b2);

        assertTrue(b1.isSelected());
        assertFalse(b2.isSelected());
        assertTrue(b3.isSelected());
        assertFalse(b4.isSelected());
        assertEquals(b1, g1.getSelectedToggle());
        assertEquals(b3, g2.getSelectedToggle());
    }

    @Test public void testSetSelectedWrongGroup2() {
        b1.setToggleGroup(g1);
        b2.setToggleGroup(g1);
        b3.setToggleGroup(g2);
        b4.setToggleGroup(g2);
        g1.selectToggle(b1);
        g2.selectToggle(b3);

        assertTrue(b1.isSelected());
        assertFalse(b2.isSelected());
        assertTrue(b3.isSelected());
        assertFalse(b4.isSelected());
        assertEquals(b1, g1.getSelectedToggle());
        assertEquals(b3, g2.getSelectedToggle());

        // this is illegal, nothing should change
        g2.selectToggle(b1);

        assertTrue(b1.isSelected());
        assertFalse(b2.isSelected());
        assertTrue(b3.isSelected());
        assertFalse(b4.isSelected());
        assertEquals(b1, g1.getSelectedToggle());
        assertEquals(b3, g2.getSelectedToggle());
    }

    // RT-3977
    @Test public void testAddMultipleSelected() {
        b1.setSelected(true);
        b2.setSelected(true);
        b3.setSelected(true);
        b1.setToggleGroup(g1);
        b2.setToggleGroup(g1);
        b3.setToggleGroup(g1);

        assertFalse(b1.isSelected());
        assertFalse(b2.isSelected());
        assertTrue(b3.isSelected());
        assertEquals(b3, g1.getSelectedToggle());
    }

    // remove an unselected button
    // remove selected button
    @Test public void testRemoveSelected() {
        b1.setToggleGroup(g1);
        b2.setToggleGroup(g1);
        b3.setToggleGroup(g1);
        b2.setSelected(true);

        b2.setToggleGroup(g2);
        assertFalse(b1.isSelected());
        assertTrue(b2.isSelected());
        assertFalse(b3.isSelected());
        assertNull(g1.getSelectedToggle());
        assertEquals(b2, g2.getSelectedToggle());
    }

    @Test public void testRemoveUnselected() {
        b1.setToggleGroup(g1);
        b2.setToggleGroup(g1);
        b3.setToggleGroup(g1);
        b2.setSelected(true);

        b3.setToggleGroup(g2);
        assertFalse(b1.isSelected());
        assertTrue(b2.isSelected());
        assertFalse(b3.isSelected());
        assertEquals(b2, g1.getSelectedToggle());
    }

    @Test public void selectButtonInToggleGroupThenUnselectIt_ToggleGroup_getSelectedToggle_ShouldReturnNull() {
        b1.setToggleGroup(g1);
        b2.setToggleGroup(g1);
        assertFalse(b1.isSelected());
        assertFalse(b2.isSelected());
        assertNull(g1.getSelectedToggle());

        b1.setSelected(true);
        assertTrue(b1.isSelected());
        assertSame(b1, g1.getSelectedToggle());

        b1.setSelected(false);
        assertFalse(b1.isSelected());
        assertFalse(b2.isSelected());
        assertNull(g1.getSelectedToggle());
    }

    @Test public void settingSelectedToggleAndThenCreatingAModelAndReadingTheValueStillWorks() {
        b1.setToggleGroup(g1);
        b2.setToggleGroup(g1);
        b3.setToggleGroup(g1);
        g1.selectToggle(b1);
        assertSame(b1, g1.selectedToggleProperty().get());
        assertTrue(b1.isSelected());
    }

//    if (old == null || old.getToggleGroup() == ToggleGroup.this || !old.isSelected())
//    @Test public void foo() {
//        // old != null
//        // old.getToggleGroup() != this
//        // old.isSelected (or is it, old is not selected?)
//        b1.setToggleGroup(g1);
//        b2.setToggleGroup(g1);
//        b1.setSelected(true);
//        b1.setToggleGroup(g2);
//        assertNull(g1.getSelectedToggle());
//        assertTrue(b1.isSelected());
//        assertFalse(b2.isSelected());
//    }

    // I just removed support for binding the "selectedToggle" property of the ToggleGroup.
    // This was done because, as the following tests were beginning to demonstrate, the
    // ToggleGroup simply begins to fail in its function as a mechanism for enforcing
    // mutual exclusivity when you allow it to be bound. Likewise, the ability to
    // bind the "selected" state of the individual Toggles is also suspect, for the same
    // reason. Although since a Toggle (such as a RadioButton) doesn't have to be placed
    // into a ToggleGroup, it does make sense to allow that to be bound. In which case,
    // if two bound toggles are in the same ToggleGroup, then while both of the Toggles
    // may claim to be selected, the ToggleGroup will only allow one of them (and it
    // might be arbitrary as to which it is) to be bound.

    // I just want to make sure, really, that there is no exception here in this case and
    // that one of the "selected" toggles is actually selected
    @Test public void bindingTwoTogglesSelectedBothToTrueResultsInOnlyOneBeingSelectedOnTheToggleGroup() {
        BooleanProperty selected = new SimpleBooleanProperty(true);
        b1.setToggleGroup(g1);
        b2.setToggleGroup(g1);
        b3.setToggleGroup(g1);
        b1.selectedProperty().bind(selected);
        b2.selectedProperty().bind(selected);
        assertTrue(b1.isSelected());
        assertTrue(b2.isSelected());
        assertTrue(g1.getSelectedToggle() == b1 || g1.getSelectedToggle() == b2);
    }

    // Basically, if you manually bind the "selected" state of an external toggle, it doesn't really
    // impact what the selected item is in the toggle group. It is simply, the last toggle to be set
    // true, wins, as far as the toggle group is concerned.
    @Test public void bindingTwoTogglesSelectedBothToTrueAndManuallySelectingAThirdResultsInTheThirdBeingSelected() {
        BooleanProperty selected = new SimpleBooleanProperty(true);
        b1.setToggleGroup(g1);
        b2.setToggleGroup(g1);
        b3.setToggleGroup(g1);
        b1.selectedProperty().bind(selected);
        b2.selectedProperty().bind(selected);
        g1.selectToggle(b3);
        assertTrue(b1.isSelected());
        assertTrue(b2.isSelected());
        assertTrue(b3.isSelected());
        assertSame(b3, g1.getSelectedToggle());
    }

    @Test public void settingTheSelectedToggleAndThenBindingAnotherToTrueResultsInTheBoundToggleBeingSelected() {
        BooleanProperty selected = new SimpleBooleanProperty(true);
        b1.setToggleGroup(g1);
        b2.setToggleGroup(g1);
        b3.setToggleGroup(g1);
        g1.selectToggle(b3);
        b1.selectedProperty().bind(selected);
        assertTrue(b1.isSelected());
        assertFalse(b3.isSelected());
        assertEquals(b1, g1.getSelectedToggle());
    }

    @Test public void addingTogglesUsingToggleGroupGetToggles() {
        g1.getToggles().add(b1);
        g1.getToggles().add(b2);
        assertTrue(isMember(b1, g1));
        assertTrue(isMember(b2, g1));
    }

    @Test public void addingTogglesUsingToggleGroupGetTogglesAndToggleSetToggleGroup() {
        g1.getToggles().add(b1);
        g1.getToggles().add(b2);
        b3.setToggleGroup(g1);
        assertTrue(isMember(b1, g1));
        assertTrue(isMember(b2, g1));
        assertTrue(isMember(b3, g1));
    }

    @Test public void removingTogglesUsingToggleGroupGetToggles() {
        g1.getToggles().add(b1);
        g1.getToggles().remove(b2);
        assertTrue(isMember(b1, g1));
        assertTrue(isNotMember(b2, g1));
    }

    @Test public void removingTogglesUsingToggleGroupGetTogglesAndToggleSetToggleGroup() {
        g1.getToggles().add(b1);
        g1.getToggles().remove(b2);
        b3.setToggleGroup(g1);
        assertTrue(isMember(b1, g1));
        assertTrue(isNotMember(b2, g1));
        assertTrue(isMember(b3, g1));
    }

    @Test public void addingTogglesUsingToggleGroupGetTogglesSetAll() {
        g1.getToggles().setAll(b1, b2, b3);
        assertTrue(isMember(b1, g1));
        assertTrue(isMember(b2, g1));
        assertTrue(isMember(b3, g1));
    }

    @Test public void addingTogglesUsingToggleGroupGetTogglesSetAllAndSet() {
        g1.getToggles().setAll(b1, b2, b3);
        g1.getToggles().set(0, b4);
        assertTrue(isNotMember(b1, g1));
        assertTrue(isMember(b2, g1));
        assertTrue(isMember(b3, g1));
        assertTrue(isMember(b4, g1));
    }

    @Test public void addingTogglesUsingToggleGroupGetTogglesSetAllTwice() {
        g1.getToggles().setAll(b1, b2);
        g1.getToggles().setAll(b3, b4);
        assertTrue(isNotMember(b1, g1));
        assertTrue(isNotMember(b2, g1));
        assertTrue(isMember(b3, g1));
        assertTrue(isMember(b4, g1));
    }

    @Test public void addingTogglesUsingToggleGroupGetTogglesSetAllAndAddAll() {
        g1.getToggles().setAll(b1, b2);
        g1.getToggles().addAll(b3, b4);
        assertTrue(isMember(b1, g1));
        assertTrue(isMember(b2, g1));
        assertTrue(isMember(b3, g1));
        assertTrue(isMember(b4, g1));
    }

    @Test public void addingDuplicateTogglesUsingToggleGroupGetTogglesAddAllAndSetToggleGroup() {
        b1.setToggleGroup(g1);
        try {
            g1.getToggles().addAll(b1, b2);
            fail("Duplicates are not allowed");
        } catch(IllegalArgumentException iae) {            
            assertNotNull(iae);
        }        
    }

    @Test public void addingDuplicateTogglesUsingToggleGroupGetTogglesSetAllTwice() {
        g1.getToggles().setAll(b1, b2);
        g1.getToggles().setAll(b1, b3, b4);
        assertTrue(isMember(b1, g1));
        assertTrue(isNotMember(b2, g1));
        assertTrue(isMember(b3, g1));
        assertTrue(isMember(b4, g1));
    }

    @Test public void addingFromOneTogglesUsingToggleGroupGetTogglesToAnother() {
        g1.getToggles().setAll(b1, b2);
        g2.getToggles().setAll(b1, b3);
        assertTrue(isNotMember(b1, g1));
        assertTrue(isMember(b1, g2));
        assertTrue(isMember(b2, g1));
        assertTrue(isMember(b3, g2));
    }

    @Test public void arrowKeysSelectTogglesTogetherWithFocus() {

        Group root = new Group();

        Scene scene = new Scene(root);
        root.getChildren().addAll(b1, b2, b3);
        root.applyCss(); // Initilize the skin

        g1.getToggles().setAll(b1, b2, b3);

        g1.selectToggle(b1);

        assertTrue(b1.isSelected());
        assertFalse(b2.isSelected());
        assertFalse(b3.isSelected());

        EventUtil.fireEvent(new KeyEvent(null, null, KeyEvent.KEY_PRESSED, "", "", KeyCode.RIGHT, false, false, false, false),
                b1);

        assertFalse(b1.isSelected());
        assertTrue(b2.isSelected());
        assertFalse(b3.isSelected());

        EventUtil.fireEvent(new KeyEvent(null, null, KeyEvent.KEY_PRESSED, "", "", KeyCode.DOWN, false, false, false, false),
                b2);

        assertFalse(b1.isSelected());
        assertFalse(b2.isSelected());
        assertTrue(b3.isSelected());

        EventUtil.fireEvent(new KeyEvent(null, null, KeyEvent.KEY_PRESSED, "", "", KeyCode.UP, false, false, false, false),
                b3);

        assertFalse(b1.isSelected());
        assertTrue(b2.isSelected());
        assertFalse(b3.isSelected());

        EventUtil.fireEvent(new KeyEvent(null, null, KeyEvent.KEY_PRESSED, "", "", KeyCode.LEFT, false, false, false, false),
                b2);

        assertTrue(b1.isSelected());
        assertFalse(b2.isSelected());
        assertFalse(b3.isSelected());
    }

    @Test public void testSelectingTwiceIsNop() {
        b1.setToggleGroup(g1);
        b2.setToggleGroup(g1);

        assertFalse(b1.isSelected());
        assertFalse(b2.isSelected());

        g1.selectToggle(b1);
        assertTrue(b1.isSelected());
        assertFalse(b2.isSelected());
        assertEquals(g1.getSelectedToggle(), b1);

        g1.selectToggle(b1);
        assertTrue(b1.isSelected());
        assertFalse(b2.isSelected());
        assertEquals(g1.getSelectedToggle(), b1);
    }
}
