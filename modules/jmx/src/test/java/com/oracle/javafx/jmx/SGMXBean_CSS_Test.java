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
import javafx.scene.Group;
import javafx.scene.shape.Shape;
import javafx.scene.Scene;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class SGMXBean_CSS_Test {

    private final SGMXBean mxBean = new SGMXBeanImpl();

    private static Group root;

    @BeforeClass
    public static void setUp() {
        Stage stage = new Stage();
        root = new Group();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    public void cssTest() {
        Shape node = new Rectangle(10, 20, 100, 50);
        this.testNode(node);
    }


    private void testNode(Shape node) {
        root.getChildren().clear();
        root.getChildren().add(node);

        mxBean.pause();

        JSONDocument jwindows = TestUtils.getJSONDocument(mxBean.getWindows());
        int[] windowIDs = TestUtils.getWindowIDs(jwindows);
        assertTrue(windowIDs.length > 0);
        JSONDocument jsceneGraph = TestUtils.getJSONDocument(mxBean.getSGTree(windowIDs[0]));
        assertTrue(jsceneGraph.isObject());
        JSONDocument jchildren = jsceneGraph.get("children");
        assertTrue(jchildren.isArray());
        assertTrue(jchildren.array().size() > 0);
        JSONDocument jnode = (JSONDocument)jchildren.array().get(0);
        int nodeId = jnode.getNumber("id").intValue();
        JSONDocument jcss = TestUtils.getJSONDocument(mxBean.getCSSInfo(nodeId));


        assertEquals(node.getStrokeWidth(), Double.parseDouble(jcss.getString("-fx-stroke-width")), 0.001);
        assertEquals(node.isVisible(), Boolean.parseBoolean(jcss.getString("visibility")));
        assertEquals(node.getScaleX(), Double.parseDouble(jcss.getString("-fx-scale-x")), 0.001);



    }
}
