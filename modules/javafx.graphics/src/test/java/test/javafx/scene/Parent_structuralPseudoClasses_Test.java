/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene;

import java.util.ArrayList;
import java.util.List;
import javafx.css.PseudoClass;
import javafx.scene.Group;
import javafx.scene.Node;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class Parent_structuralPseudoClasses_Test {

    static final PseudoClass FIRST_CHILD = PseudoClass.getPseudoClass("first-child");
    static final PseudoClass LAST_CHILD = PseudoClass.getPseudoClass("last-child");
    static final PseudoClass ONLY_CHILD = PseudoClass.getPseudoClass("only-child");
    static final PseudoClass NTH_EVEN_CHILD = PseudoClass.getPseudoClass("nth-child(even)");
    static final PseudoClass NTH_ODD_CHILD = PseudoClass.getPseudoClass("nth-child(odd)");
    static final PseudoClass[] EMPTY = new PseudoClass[0];
    static final PseudoClass[] ALL = new PseudoClass[] { FIRST_CHILD, LAST_CHILD, ONLY_CHILD, NTH_EVEN_CHILD, NTH_ODD_CHILD };

    @Test
    void multipleNodes_removeFromFront() {
        var group = new Group();
        var child1 = new Group();
        var child2 = new Group();
        var child3 = new Group();
        var child4 = new Group();

        // child1 = [first-child, nth-child(odd)]
        // child2 = [nth-child(even)]
        // child3 = [nth-child(odd)]
        // child4 = [last-child, nth-child(even)]
        group.getChildren().addAll(child1, child2, child3, child4);
        assertOnlyPseudoClasses(child1, FIRST_CHILD, NTH_ODD_CHILD);
        assertOnlyPseudoClasses(child2, NTH_EVEN_CHILD);
        assertOnlyPseudoClasses(child3, NTH_ODD_CHILD);
        assertOnlyPseudoClasses(child4, LAST_CHILD, NTH_EVEN_CHILD);

        // child1 = []
        // child2 = [first-child, last-child, nth-child(odd)]
        // child3 = [nth-child(even)]
        // child4 = [last-child, nth-child(odd)]
        group.getChildren().removeFirst();
        assertOnlyPseudoClasses(child1, EMPTY);
        assertOnlyPseudoClasses(child2, FIRST_CHILD, NTH_ODD_CHILD);
        assertOnlyPseudoClasses(child3, NTH_EVEN_CHILD);
        assertOnlyPseudoClasses(child4, LAST_CHILD, NTH_ODD_CHILD);

        // child1 = []
        // child2 = []
        // child3 = [first-child, nth-child(odd)]
        // child4 = [last-child, nth-child(even)]
        group.getChildren().removeFirst();
        assertOnlyPseudoClasses(child1, EMPTY);
        assertOnlyPseudoClasses(child2, EMPTY);
        assertOnlyPseudoClasses(child3, FIRST_CHILD, NTH_ODD_CHILD);
        assertOnlyPseudoClasses(child4, LAST_CHILD, NTH_EVEN_CHILD);

        // child1 = []
        // child2 = []
        // child3 = []
        // child4 = [only-child, first-child, last-child, nth-child(odd)]
        group.getChildren().removeFirst();
        assertOnlyPseudoClasses(child1, EMPTY);
        assertOnlyPseudoClasses(child2, EMPTY);
        assertOnlyPseudoClasses(child3, EMPTY);
        assertOnlyPseudoClasses(child4, ONLY_CHILD, FIRST_CHILD, LAST_CHILD, NTH_ODD_CHILD);
    }

    @Test
    void multipleNodes_removeFromBack() {
        var group = new Group();
        var child1 = new Group();
        var child2 = new Group();
        var child3 = new Group();
        var child4 = new Group();

        // child1 = [first-child, nth-child(odd)]
        // child2 = [nth-child(even)]
        // child3 = [nth-child(odd)]
        // child4 = [last-child, nth-child(even)]
        group.getChildren().addAll(child1, child2, child3, child4);
        assertOnlyPseudoClasses(child1, FIRST_CHILD, NTH_ODD_CHILD);
        assertOnlyPseudoClasses(child2, NTH_EVEN_CHILD);
        assertOnlyPseudoClasses(child3, NTH_ODD_CHILD);
        assertOnlyPseudoClasses(child4, LAST_CHILD, NTH_EVEN_CHILD);

        // child1 = [first-child, last-child, nth-child(odd)]
        // child2 = [nth-child(even)]
        // child3 = [last-child, nth-child(odd)]
        // child4 = []
        group.getChildren().removeLast();
        assertOnlyPseudoClasses(child1, FIRST_CHILD, NTH_ODD_CHILD);
        assertOnlyPseudoClasses(child2, NTH_EVEN_CHILD);
        assertOnlyPseudoClasses(child3, LAST_CHILD, NTH_ODD_CHILD);
        assertOnlyPseudoClasses(child4, EMPTY);

        // child1 = [first-child, nth-child(odd)]
        // child2 = [last-child, nth-child(even)]
        // child3 = []
        // child4 = []
        group.getChildren().removeLast();
        assertOnlyPseudoClasses(child1, FIRST_CHILD, NTH_ODD_CHILD);
        assertOnlyPseudoClasses(child2, LAST_CHILD, NTH_EVEN_CHILD);
        assertOnlyPseudoClasses(child3, EMPTY);
        assertOnlyPseudoClasses(child4, EMPTY);

        // child1 = [only-child, first-child, last-child, nth-child(odd)]
        // child2 = []
        // child3 = []
        // child4 = []
        group.getChildren().removeLast();
        assertOnlyPseudoClasses(child1, ONLY_CHILD, FIRST_CHILD, LAST_CHILD, NTH_ODD_CHILD);
        assertOnlyPseudoClasses(child2, EMPTY);
        assertOnlyPseudoClasses(child3, EMPTY);
        assertOnlyPseudoClasses(child4, EMPTY);
    }

    @Test
    void multipleNodes_removeInterior() {
        var group = new Group();
        var child1 = new Group();
        var child2 = new Group();
        var child3 = new Group();
        var child4 = new Group();

        // child1 = [first-child, nth-child(odd)]
        // child2 = [nth-child(even)]
        // child3 = [nth-child(odd)]
        // child4 = [last-child, nth-child(even)]
        group.getChildren().addAll(child1, child2, child3, child4);
        assertOnlyPseudoClasses(child1, FIRST_CHILD, NTH_ODD_CHILD);
        assertOnlyPseudoClasses(child2, NTH_EVEN_CHILD);
        assertOnlyPseudoClasses(child3, NTH_ODD_CHILD);
        assertOnlyPseudoClasses(child4, LAST_CHILD, NTH_EVEN_CHILD);

        // child1 = [first-child, nth-child(odd)]
        // child2 = []
        // child3 = [nth-child(even)]
        // child4 = [last-child, nth-child(odd)]
        group.getChildren().remove(1);
        assertOnlyPseudoClasses(child1, FIRST_CHILD, NTH_ODD_CHILD);
        assertOnlyPseudoClasses(child2, EMPTY);
        assertOnlyPseudoClasses(child3, NTH_EVEN_CHILD);
        assertOnlyPseudoClasses(child4, LAST_CHILD, NTH_ODD_CHILD);

        // child1 = [first-child, nth-child(odd)]
        // child2 = []
        // child3 = []
        // child4 = [last-child, nth-child(even)]
        group.getChildren().remove(1);
        assertOnlyPseudoClasses(child1, FIRST_CHILD, NTH_ODD_CHILD);
        assertOnlyPseudoClasses(child2, EMPTY);
        assertOnlyPseudoClasses(child3, EMPTY);
        assertOnlyPseudoClasses(child4, LAST_CHILD, NTH_EVEN_CHILD);
    }

    @Test
    void multipleNodes_removeInteriorRange() {
        var group = new Group();
        var child1 = new Group();
        var child2 = new Group();
        var child3 = new Group();
        var child4 = new Group();

        // child1 = [first-child, nth-child(odd)]
        // child2 = [nth-child(even)]
        // child3 = [nth-child(odd)]
        // child4 = [last-child, nth-child(even)]
        group.getChildren().addAll(child1, child2, child3, child4);
        assertOnlyPseudoClasses(child1, FIRST_CHILD, NTH_ODD_CHILD);
        assertOnlyPseudoClasses(child2, NTH_EVEN_CHILD);
        assertOnlyPseudoClasses(child3, NTH_ODD_CHILD);
        assertOnlyPseudoClasses(child4, LAST_CHILD, NTH_EVEN_CHILD);

        // child1 = [first-child, nth-child(odd)]
        // child2 = []
        // child3 = []
        // child4 = [last-child, nth-child(even)]
        group.getChildren().removeAll(child2, child3);
        assertOnlyPseudoClasses(child1, FIRST_CHILD, NTH_ODD_CHILD);
        assertOnlyPseudoClasses(child2, EMPTY);
        assertOnlyPseudoClasses(child3, EMPTY);
        assertOnlyPseudoClasses(child4, LAST_CHILD, NTH_EVEN_CHILD);
    }

    @Test
    @Disabled("JDK-8233179")
    void multipleNodes_permutation() {
        var group = new Group();
        var child1 = new Group();
        var child2 = new Group();
        var child3 = new Group();
        var child4 = new Group();

        // child1 = [first-child, nth-child(odd)]
        // child2 = [nth-child(even)]
        // child3 = [nth-child(odd)]
        // child4 = [last-child, nth-child(even)]
        group.getChildren().addAll(child1, child2, child3, child4);
        assertOnlyPseudoClasses(child1, FIRST_CHILD, NTH_ODD_CHILD);
        assertOnlyPseudoClasses(child2, NTH_EVEN_CHILD);
        assertOnlyPseudoClasses(child3, NTH_ODD_CHILD);
        assertOnlyPseudoClasses(child4, LAST_CHILD, NTH_EVEN_CHILD);

        // child1 = [first-child, nth-child(odd)]
        // child2 = [nth-child(even)]
        // child4 = [nth-child(odd)]
        // child3 = [last-child, nth-child(even)]
        group.getChildren().sort((o1, o2) -> { // swap child3 and child4
            if (o1 == child3) return o2 == child4 ? 1 : 0;
            if (o1 == child4) return o2 == child3 ? -1 : 0;
            return 0;
        });

        assertOnlyPseudoClasses(child1, FIRST_CHILD, NTH_ODD_CHILD);
        assertOnlyPseudoClasses(child2, NTH_EVEN_CHILD);
        assertOnlyPseudoClasses(child4, NTH_ODD_CHILD);
        assertOnlyPseudoClasses(child3, LAST_CHILD, NTH_EVEN_CHILD);
    }

    @Test
    void firstChildPseudoClass() {
        var group = new Group();
        var child1 = new Group();
        var child2 = new Group();
        assertNotPseudoClass(FIRST_CHILD, child1);
        assertNotPseudoClass(FIRST_CHILD, child2);
        group.getChildren().add(child1);
        assertPseudoClass(FIRST_CHILD, child1);
        group.getChildren().add(child2);
        assertPseudoClass(FIRST_CHILD, child1);
        assertNotPseudoClass(FIRST_CHILD, child2);
        group.getChildren().removeFirst();
        assertNotPseudoClass(FIRST_CHILD, child1);
        assertPseudoClass(FIRST_CHILD, child2);
    }

    @Test
    void lastChildPseudoClass() {
        var group = new Group();
        var child1 = new Group();
        var child2 = new Group();
        assertNotPseudoClass(LAST_CHILD, child1);
        assertNotPseudoClass(LAST_CHILD, child2);
        group.getChildren().add(child1);
        assertPseudoClass(LAST_CHILD, child1);
        group.getChildren().add(child2);
        assertNotPseudoClass(LAST_CHILD, child1);
        assertPseudoClass(LAST_CHILD, child2);
        group.getChildren().removeFirst();
        assertNotPseudoClass(LAST_CHILD, child1);
        assertPseudoClass(LAST_CHILD, child2);
        group.getChildren().removeFirst();
        assertNotPseudoClass(LAST_CHILD, child2);
    }

    @Test
    void nthChildEvenOddPseudoClass() {
        var group = new Group();
        var child1 = new Group();
        var child2 = new Group();
        var child3 = new Group();
        var child4 = new Group();

        // [child1, child2, child3, child4]
        group.getChildren().addAll(child1, child2, child3, child4);
        assertPseudoClass(NTH_EVEN_CHILD, child2, child4);
        assertNotPseudoClass(NTH_EVEN_CHILD, child1, child3);
        assertPseudoClass(NTH_ODD_CHILD, child1, child3);
        assertNotPseudoClass(NTH_ODD_CHILD, child2, child4);

        // [child1, child2, child2b, child3, child4]
        var child2b = new Group();
        group.getChildren().add(2, child2b);
        assertPseudoClass(NTH_EVEN_CHILD, child2, child3);
        assertNotPseudoClass(NTH_EVEN_CHILD, child1, child2b, child4);
        assertPseudoClass(NTH_ODD_CHILD, child1, child2b, child4);
        assertNotPseudoClass(NTH_ODD_CHILD, child2, child3);

        // [child1, child3, child4]
        group.getChildren().remove(1, 3);
        assertPseudoClass(NTH_EVEN_CHILD, child3);
        assertNotPseudoClass(NTH_EVEN_CHILD, child1, child2, child2b, child4);
        assertPseudoClass(NTH_ODD_CHILD, child1, child4);
        assertNotPseudoClass(NTH_ODD_CHILD, child2, child2b, child3);
    }

    private void assertOnlyPseudoClasses(Node node, PseudoClass... pseudoClass) {
        List<PseudoClass> remaining = new ArrayList<>(List.of(ALL));

        for (PseudoClass pc : pseudoClass) {
            assertTrue(node.getPseudoClassStates().contains(pc));
            remaining.remove(pc);
        }

        for (PseudoClass pc : remaining) {
            assertFalse(node.getPseudoClassStates().contains(pc));
        }
    }

    private void assertPseudoClass(PseudoClass pseudoClass, Node... nodes) {
        for (Node node : nodes) {
            assertTrue(node.getPseudoClassStates().contains(pseudoClass));
        }
    }

    private void assertNotPseudoClass(PseudoClass pseudoClass, Node... nodes) {
        for (Node node : nodes) {
            assertFalse(node.getPseudoClassStates().contains(pseudoClass));
        }
    }
}
