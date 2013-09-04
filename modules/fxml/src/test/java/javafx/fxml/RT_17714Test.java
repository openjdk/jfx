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

package javafx.fxml;

import javafx.util.Callback;
import org.junit.Test;

import static org.junit.Assert.*;

public class RT_17714Test {
    @Test
    public void testListEvents() throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("rt_17714.fxml"));
        Widget widget = (Widget)fxmlLoader.load();
        RT_17714Controller controller = (RT_17714Controller)fxmlLoader.getController();

        assertFalse(controller.listWithParamCalled);
        assertFalse(controller.listNoParamCalled);
        // Test
        widget.getChildren().add(new Widget("Widget 4"));
        assertTrue(controller.listWithParamCalled);
        assertFalse(controller.listNoParamCalled);
    }

    @Test
    public void testMapEvents() throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("rt_17714.fxml"));
        Widget widget = (Widget)fxmlLoader.load();
        RT_17714Controller controller = (RT_17714Controller)fxmlLoader.getController();

        assertFalse(controller.mapWithParamCalled);
        assertFalse(controller.mapNoParamCalled);
        // Test
        widget.getProperties().put("d", 1000);

        assertTrue(controller.mapWithParamCalled);
        assertFalse(controller.mapNoParamCalled);
    }

    @Test
    public void testSetEvents() throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("rt_17714.fxml"));
        Widget widget = (Widget)fxmlLoader.load();
        RT_17714Controller controller = (RT_17714Controller)fxmlLoader.getController();

        assertFalse(controller.setWithParamCalled);
        assertFalse(controller.setNoParamCalled);
        // Test
        widget.getSet().add("x");

        assertTrue(controller.setWithParamCalled);
        assertFalse(controller.setNoParamCalled);
    }

    @Test
    public void testListEvents_NoParam() throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("rt_17714.fxml"), null, null, new Callback<Class<?>, Object>() {
            @Override
            public Object call(Class<?> param) {
                return new RT_17714Controller2();
            }
        });
        Widget widget = (Widget)fxmlLoader.load();
        RT_17714Controller2 controller = (RT_17714Controller2)fxmlLoader.getController();

        assertFalse(controller.listNoParamCalled);
        // Test
        widget.getChildren().add(new Widget("Widget 4"));
        assertTrue(controller.listNoParamCalled);
    }

    @Test
    public void testMapEvents_NoParam() throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("rt_17714.fxml"), null, null, new Callback<Class<?>, Object>() {
            @Override
            public Object call(Class<?> param) {
                return new RT_17714Controller2();
            }
        });
        Widget widget = (Widget)fxmlLoader.load();
        RT_17714Controller2 controller = (RT_17714Controller2)fxmlLoader.getController();

        assertFalse(controller.mapNoParamCalled);
        // Test
        widget.getProperties().put("d", 1000);

        assertTrue(controller.mapNoParamCalled);
    }

    @Test
    public void testSetEvents_NoParam() throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("rt_17714.fxml"), null, null, new Callback<Class<?>, Object>() {
            @Override
            public Object call(Class<?> param) {
                return new RT_17714Controller2();
            }
        });
        Widget widget = (Widget)fxmlLoader.load();
        RT_17714Controller2 controller = (RT_17714Controller2)fxmlLoader.getController();

        assertFalse(controller.setNoParamCalled);
        // Test
        widget.getSet().add("x");

        assertTrue(controller.setNoParamCalled);
    }
}
