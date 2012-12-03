/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

package javafx.fxml;

import org.junit.Test;

import static org.junit.Assert.*;

public class RT_17714Test {
    @Test
    public void testListEvents() throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("rt_17714.fxml"));
        Widget widget = (Widget)fxmlLoader.load();
        RT_17714Controller controller = (RT_17714Controller)fxmlLoader.getController();

        assertEquals(widget.getChildren(), controller.getChildren());

        // Test add
        widget.getChildren().add(new Widget("Widget 4"));
        widget.getChildren().add(new Widget("Widget 5"));

        assertEquals(widget.getChildren(), controller.getChildren());

        // Test update
        widget.getChildren().set(0, new Widget("Widget 1a"));
        widget.getChildren().set(2, new Widget("Widget 3a"));

        assertEquals(widget.getChildren(), controller.getChildren());

        // Test remove
        widget.getChildren().remove(1);

        assertEquals(widget.getChildren(), controller.getChildren());
    }

    @Test
    public void testMapEvents() throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("rt_17714.fxml"));
        Widget widget = (Widget)fxmlLoader.load();
        RT_17714Controller controller = (RT_17714Controller)fxmlLoader.getController();

        assertEquals(widget.getProperties(), controller.getProperties());

        // Test add
        widget.getProperties().put("d", 1000);
        widget.getProperties().put("e", 10000);

        assertEquals(widget.getProperties(), controller.getProperties());

        // Test update
        widget.getProperties().put("a", 2);
        widget.getProperties().put("c", 200);

        assertEquals(widget.getProperties(), controller.getProperties());

        // Test remove
        widget.getProperties().remove("b");

        assertEquals(widget.getProperties(), controller.getProperties());
    }
}
