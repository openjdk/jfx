/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.fxml;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;
import javafx.fxml.FXMLLoader;
import org.junit.Test;

import static org.junit.Assert.*;

public class RT_27529Test {

    @Test
    public void testListAndArrayWithResources() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("rt_27529_1.fxml"),
            ResourceBundle.getBundle("test/javafx/fxml/rt_27529"));

        Widget widget = (Widget)fxmlLoader.load();
        assertEquals(Arrays.asList(new String[]{"a", "b", "c"}), widget.getStyles());
        assertTrue(Arrays.equals(new String[]{"a", "b", "c"}, widget.getNames()));
        assertTrue(Arrays.equals(new float[] {1.0f, 2.0f, 3.0f}, widget.getRatios()));
    }

    @Test
    public void testListAndArrayWithEscapes() throws IOException {
        System.err.println("Below warnings about - deprecated escape sequence - are expected from this test.");
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("rt_27529_2.fxml"),
            ResourceBundle.getBundle("test/javafx/fxml/rt_27529"));
        fxmlLoader.load();

        Widget widget = (Widget)fxmlLoader.getNamespace().get("widget1");
        assertEquals(Arrays.asList(new String[]{"@a", "%b", "$c", "@c", "%d", "$e"}), widget.getStyles());
        assertTrue(Arrays.equals(  new String[]{"@a", "%b", "$c", "@c", "%d", "$e"}, widget.getNames()));
    }

    @Test
    public void testListAndArrayWithRelativePath() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("rt_27529_3.fxml"),
            ResourceBundle.getBundle("test/javafx/fxml/rt_27529"));

        Widget widget = (Widget)fxmlLoader.load();
        assertEquals(Arrays.asList(new String[]{
            new URL(fxmlLoader.getLocation(), "a").toString(),
            new URL(fxmlLoader.getLocation(), "b").toString(),
            new URL(fxmlLoader.getLocation(), "c").toString()}), widget.getStyles());
    }

    @Test
    public void testListAndArrayWithReference() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("rt_27529_4.fxml"),
            ResourceBundle.getBundle("test/javafx/fxml/rt_27529"));

        fxmlLoader.load();
        Widget widget = (Widget)fxmlLoader.getNamespace().get("widget1");
        assertEquals(Arrays.asList(new String[]{"ABC", "ABC"}), widget.getStyles());
        assertTrue(Arrays.equals(new String[]{"ABC", "ABC"}, widget.getNames()));
        assertTrue(Arrays.equals(new float[] {1.0f, 1.0f}, widget.getRatios()));
    }
}
