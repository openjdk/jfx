/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.css;

import com.sun.javafx.pgstub.StubToolkit;
import com.sun.javafx.tk.Toolkit;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Popup;
import javafx.stage.Stage;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author dgrieve
 */
public class StyleManagerTest {
    
    public StyleManagerTest() {
    }
    
    @Before
    public void setUp() {
//        rect = new Rectangle();
//        rect.setId("rectangle");
//
//        text = new Text();
//        text.setId("text");
//
//        Group group = new Group();
//        group.getChildren().addAll(rect, text);
//
//        scene = new Scene(group);/* {
//            TestWindow window;
//            {
//                window = new TestWindow();
//                window.setScene(HonorDeveloperSettingsTest.this.scene);
//                impl_setWindow(window);
//            }
//        };*/
//        
//        System.setProperty("binary.css", "false");
//        String url = getClass().getResource("HonorDeveloperSettingsTest_UA.css").toExternalForm();
//        StyleManager.getInstance().setDefaultUserAgentStylesheet(url);
//        
//        Stage stage = new Stage();
//        stage.setScene(scene);
//        stage.show();
    }
    
    @Test
    public void testMethod_getInstance() {
        Scene scene = new Scene(new Group());
        StyleManager sm = StyleManager.getInstance();
        assertNotNull(sm);
    }
        
}
