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

package com.oracle.javafx.jmx;

import com.oracle.javafx.jmx.json.JSONDocument;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class SGMXBean_sceneGraph_Test {

    private final SGMXBean mxBean = new SGMXBeanImpl();

    private static Stage stage1;
    private static Stage stage2;

    private static int nodesCount;

    @BeforeClass
    public static void setUp() {
        stage1 = new Stage();
        Scene scene1 = new Scene(new Group(
                new Rectangle(100, 100),
                new Circle(30),
                new Group(
                    new Rectangle(30, 30),
                    new Line(0, 0, 100, 30)
                )));
        stage1.setScene(scene1);
        stage1.show();

        stage2 = new Stage();
        Scene scene2 = new Scene(new Group(
                new Circle(100),
                new Line(30, 30, 20, 100)));
        stage2.setScene(scene2);
        stage2.show();

        final Iterator<Window> it = Window.impl_getWindows();
        nodesCount = 0;
        while (it.hasNext()) {
            final Window w = it.next();
            nodesCount += countNodes(w.getScene().getRoot());

        }
    }

    private static int countNodes(Parent p) {
        ObservableList<Node> children = p.getChildrenUnmodifiable();
        int res = 1; // the node itself
        for (int i = 0; i < children.size(); i++) {
            final Node n = children.get(i);
            if (n instanceof Parent) {
                res += countNodes((Parent)n);
            } else {
                res++;
            }
        }
        return res;
    }

    @Test
    public void sceneGraphsStructureTest() {
        mxBean.pause();
        JSONDocument jwindows = TestUtils.getJSONDocument(mxBean.getWindows());
        int[] ids = TestUtils.getWindowIDs(jwindows);
        for (int i = 0; i < ids.length; i++) {
            JSONDocument sg = TestUtils.getJSONDocument(mxBean.getSGTree(ids[i]));
            checkSGStructure(sg);
        }
    }

    private static void checkSGStructure(JSONDocument d) {
        assertEquals(JSONDocument.Type.OBJECT, d.type());
        JSONDocument jchildren = d.get("children");
        if (jchildren.equals(JSONDocument.EMPTY_OBJECT)) {
            return;
        }
        assertEquals(JSONDocument.Type.ARRAY, jchildren.type());
        for (int i = 0; i < jchildren.array().size(); i++) {
            checkSGStructure(jchildren.get(i));
        }
    }

    @Test
    public void nodesCountTest() {
        mxBean.pause();
        JSONDocument jwindows = TestUtils.getJSONDocument(mxBean.getWindows());
        int[] ids = TestUtils.getWindowIDs(jwindows);
        int jNodeCount = 0;
        for (int i = 0; i < ids.length; i++) {
            JSONDocument sg = TestUtils.getJSONDocument(mxBean.getSGTree(ids[i]));
            jNodeCount += countNodes(sg);
        }
        assertEquals(nodesCount, jNodeCount);
    }

    private static int countNodes(JSONDocument d) {
        JSONDocument jchildren = d.get("children");
        if (jchildren.equals(JSONDocument.EMPTY_OBJECT)) {
            return 1;
        }
        int res = 1; // the node (container) itself
        for (int i = 0; i < jchildren.array().size(); i++) {
            res += countNodes(jchildren.get(i));
        }
        return res;
    }

    @Test
    public void nodesHaveIDsTest() {
        mxBean.pause();
        JSONDocument jwindows = TestUtils.getJSONDocument(mxBean.getWindows());
        int[] ids = TestUtils.getWindowIDs(jwindows);
        for (int i = 0; i < ids.length; i++) {
            JSONDocument sg = TestUtils.getJSONDocument(mxBean.getSGTree(ids[i]));
            checkNodeHasID(sg);
        }
    }

    private static void checkNodeHasID(JSONDocument d) {
        Number id = d.getNumber("id");
        assertNotNull(id);
        JSONDocument jchildren = d.get("children");
        if (jchildren.equals(JSONDocument.EMPTY_OBJECT)) {
            return;
        }
        for (int i = 0; i < jchildren.array().size(); i++) {
            checkNodeHasID(jchildren.get(i));
        }
    }

    @Test
    public void nodesHaveUniqueIDsTest() {
        Set<Number> ids = new HashSet<Number>();
        mxBean.pause();
        JSONDocument jwindows = TestUtils.getJSONDocument(mxBean.getWindows());
        int[] windowIDs = TestUtils.getWindowIDs(jwindows);
        for (int i = 0; i < windowIDs.length; i++) {
            JSONDocument sg = TestUtils.getJSONDocument(mxBean.getSGTree(windowIDs[i]));
            checkNodeHasUniqueID(sg, ids);
        }
    }

    private static void checkNodeHasUniqueID(JSONDocument d, Set<Number> ids) {
        Number id = d.getNumber("id");
        assertFalse(ids.contains(id));
        ids.add(id);
        JSONDocument jchildren = d.get("children");
        if (jchildren.equals(JSONDocument.EMPTY_OBJECT)) {
            return;
        }
        for (int i = 0; i < jchildren.array().size(); i++) {
            checkNodeHasUniqueID(jchildren.get(i), ids);
        }
    }
}
