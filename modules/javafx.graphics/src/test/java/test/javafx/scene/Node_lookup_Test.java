/*
 * Copyright (c) 2011, 2015, 2023, Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import javafx.css.PseudoClass;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.ParentShim;

import org.junit.Before;
import org.junit.Test;

public class Node_lookup_Test {
    //                   Group & #root
    //                    /        \
    //                 #a.c       .b.c:testPseudo
    //                /    \         \
    //    .d:testPseudo1    #e     .d:testPseudo1:testPseudo2
    private Group root, ac, bc, d, e, d2;

    @Before public void setup() {
        root = new Group();
        root.setId("root");
        ac = new Group();
        ac.setId("a");
        ac.getStyleClass().addAll("c");
        d = new Group();
        d.getStyleClass().add("d");
        d.pseudoClassStateChanged(PseudoClass.getPseudoClass("testPseudo1"),true);
        e = new Group();
        e.setId("e");
        bc = new Group();
        bc.getStyleClass().addAll("b", "c");
        bc.pseudoClassStateChanged(PseudoClass.getPseudoClass("testPseudo"),true);
        d2 = new Group();
        d2.getStyleClass().add("d");
        d2.pseudoClassStateChanged(PseudoClass.getPseudoClass("testPseudo1"),true);
        d2.pseudoClassStateChanged(PseudoClass.getPseudoClass("testPseudo2"),true);
        ParentShim.getChildren(root).addAll(ac, bc);
        ParentShim.getChildren(ac).addAll(d, e);
        ParentShim.getChildren(bc).addAll(d2);
    }

    @Test public void quickTest() {
        Node found = root.lookup("Group");
        assertSame(root, found);

        found = root.lookup("#a");
        assertSame(ac, found);

        found = root.lookup("#a > .d");
        assertSame(d, found);

        found = root.lookup("#e");
        assertSame(e, found);

        found = root.lookup(".b .d");
        assertSame(d2, found);

        found = root.lookup(".c .d");
        assertSame(d, found);

        found = root.lookup(".b");
        assertSame(bc, found);
    }

    @Test public void lookupAllTest() {
        Set<Node> nodes = root.lookupAll("#a");
        assertEquals(1, nodes.size());
        assertTrue(nodes.contains(ac));

        nodes = root.lookupAll(".d");
        assertEquals(2, nodes.size());
        assertTrue(nodes.contains(d));
        assertTrue(nodes.contains(d2));
    }

    @Test
    public void lookupPsuedoTest(){
        Set<Node> nodes = root.lookupAll(".d:testPseudo2");
        assertEquals(1, nodes.size());
        assertTrue(nodes.contains(d2));

        Node found = root.lookup(".d:testPseudo2");
        assertSame(d2, found);

        found = root.lookup(".d:testPseudo1:testPseudo2");
        assertSame(d2, found);

        nodes = root.lookupAll(".d:testPseudo1");
        assertEquals(2, nodes.size());
        assertTrue(nodes.contains(d));
        assertTrue(nodes.contains(d2));

        nodes = root.lookupAll("#a > .d:testPseudo1");
        assertEquals(1, nodes.size());
        assertTrue(nodes.contains(d));

        nodes = root.lookupAll(".c:testPseudo > .d:testPseudo1");
        assertEquals(1, nodes.size());
        assertTrue(nodes.contains(d2));

        nodes = root.lookupAll(".d:randomPseudo");
        assertEquals(0, nodes.size());
    }
}
