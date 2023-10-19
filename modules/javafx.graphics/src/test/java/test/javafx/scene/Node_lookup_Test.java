/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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
    //                  Group & #root
    //                /      \        \
    //              #a      .b.c       .f:testPseudo
    //             /   \        \         /           \
    //           .d    #e        .d   .g:testPseudo1   .h.g:testPseudo1:testPseudo2
    private Group root, a, bc, d, e, d2, f, g, hg;

    @Before public void setup() {
        root = new Group();
        root.setId("root");
        a = new Group();
        a.setId("a");
        d = new Group();
        d.getStyleClass().add("d");
        e = new Group();
        e.setId("e");
        bc = new Group();
        bc.getStyleClass().addAll("b", "c");
        d2 = new Group();
        d2.getStyleClass().add("d");
        f = new Group();
        f.getStyleClass().add("f");
        f.pseudoClassStateChanged(PseudoClass.getPseudoClass("testPseudo"),true);
        g = new Group();
        g.getStyleClass().add("g");
        g.pseudoClassStateChanged(PseudoClass.getPseudoClass("testPseudo1"),true);
        hg = new Group();
        hg.getStyleClass().addAll("h", "g");
        hg.pseudoClassStateChanged(PseudoClass.getPseudoClass("testPseudo1"),true);
        hg.pseudoClassStateChanged(PseudoClass.getPseudoClass("testPseudo2"),true);
        ParentShim.getChildren(root).addAll(a, bc, f);
        ParentShim.getChildren(a).addAll(d, e);
        ParentShim.getChildren(bc).addAll(d2);
        ParentShim.getChildren(f).addAll(g, hg);
    }

    @Test public void quickTest() {
        Node found = root.lookup("Group");
        assertSame(root, found);

        found = root.lookup("#a");
        assertSame(a, found);

        found = root.lookup("#a > .d");
        assertSame(d, found);

        found = root.lookup("#e");
        assertSame(e, found);

        found = root.lookup(".b .d");
        assertSame(d2, found);

        found = root.lookup(".c .d");
        assertSame(d2, found);

        found = root.lookup(".b");
        assertSame(bc, found);
    }

    @Test public void lookupAllTest() {
        Set<Node> nodes = root.lookupAll("#a");
        assertEquals(1, nodes.size());
        assertTrue(nodes.contains(a));

        nodes = root.lookupAll(".d");
        assertEquals(2, nodes.size());
        assertTrue(nodes.contains(d));
        assertTrue(nodes.contains(d2));
    }

    @Test
    public void lookupPseudoTest(){
        Set<Node> nodes = root.lookupAll(".h:testPseudo2");
        assertEquals(1, nodes.size());
        assertTrue(nodes.contains(hg));

        Node found = root.lookup(".h:testPseudo2");
        assertSame(hg, found);

        found = root.lookup(":testPseudo2");
        assertSame(hg, found);

        found = root.lookup(".h:testPseudo1:testPseudo2");
        assertSame(hg, found);

        nodes = root.lookupAll(".g:testPseudo1");
        assertEquals(2, nodes.size());
        assertTrue(nodes.contains(g));
        assertTrue(nodes.contains(hg));

        nodes = root.lookupAll(":testPseudo1");
        assertEquals(2, nodes.size());
        assertTrue(nodes.contains(g));
        assertTrue(nodes.contains(hg));

        nodes = root.lookupAll(".f > .h:testPseudo1");
        assertEquals(1, nodes.size());
        assertTrue(nodes.contains(hg));

        nodes = root.lookupAll(".f:testPseudo > .h:testPseudo1");
        assertEquals(1, nodes.size());
        assertTrue(nodes.contains(hg));

        nodes = root.lookupAll(".f:randomPseudo");
        assertEquals(0, nodes.size());
    }
}
