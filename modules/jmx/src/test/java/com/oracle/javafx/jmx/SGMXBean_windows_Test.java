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
import com.oracle.javafx.jmx.json.JSONFactory;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

import java.io.StringReader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class SGMXBean_windows_Test {

    private final SGMXBean mxBean = new SGMXBeanImpl();

    private static Stage stage1;
    private static Stage stage2;

    private static int windowsCount;

    @BeforeClass
    public static void setUp() {
        stage1 = new Stage();
        Rectangle r1 = new Rectangle(100, 100);
        Scene scene1 = new Scene(new Group(r1));
        stage1.setScene(scene1);
        stage1.show();

        stage2 = new Stage();
        Circle c1 = new Circle(100);
        Scene scene2 = new Scene(new Group(c1));
        stage2.setScene(scene2);
        stage2.show();

        final Iterator<Window> it = Window.impl_getWindows();
        windowsCount = 0;
        while (it.hasNext()) {
            windowsCount++;
            it.next();
        }
    }

    private static JSONDocument getJSONDocument(String source) {
        return JSONFactory.instance().makeReader(new StringReader(source)).build();
    }

    @Test
    public void windowsStructureTest() {
        mxBean.pause();
        JSONDocument d = getJSONDocument(mxBean.getWindows());
        assertEquals(JSONDocument.Type.ARRAY, d.type());
        int count = d.array().size();
        for (int i = 0; i < count; i++) {
            JSONDocument jwindow = d.get(i);
            assertEquals(JSONDocument.Type.OBJECT, jwindow.type());
        }
    }

    @Test
    public void windowsCountTest() {
        mxBean.pause();
        JSONDocument d = getJSONDocument(mxBean.getWindows());
        final int count = d.array().size();
        assertEquals(windowsCount, count);
    }

    @Test
    public void windowsHaveIDsTest() {
        mxBean.pause();
        JSONDocument d = getJSONDocument(mxBean.getWindows());
        int count = d.array().size();
        for (int i = 0; i < count; i++) {
            JSONDocument jwindow = d.get(i);
            Number id = jwindow.getNumber("id");
            assertNotNull(id);
        }
    }

    @Test
    public void windowsHaveUniqueIDsTest() {
        Set<Number> ids = new HashSet<Number>();
        mxBean.pause();
        JSONDocument d = getJSONDocument(mxBean.getWindows());
        int count = d.array().size();
        for (int i = 0; i < count; i++) {
            JSONDocument jwindow = d.get(i);
            Number id = jwindow.getNumber("id");
            assertFalse(ids.contains(id));
            ids.add(id);
        }
    }

}
