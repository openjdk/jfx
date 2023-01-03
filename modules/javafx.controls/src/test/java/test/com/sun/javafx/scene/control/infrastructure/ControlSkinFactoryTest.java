/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package test.com.sun.javafx.scene.control.infrastructure;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.*;
import static test.com.sun.javafx.scene.control.infrastructure.ControlSkinFactory.*;

import javafx.scene.control.Control;
import javafx.scene.control.ControlShim;
import javafx.scene.control.Skin;

/**
 * Tests on ControlSkinFactory.
 */
public class ControlSkinFactoryTest {

    @Test
    public void testConvertToArray() {
        List<Class<Control>> controls = getControlClasses();
        List<Object[]> asArray = asArrays(controls);
        for (int i = 0; i < controls.size(); i++) {
            assertEquals(1, asArray.get(i).length);
            assertSame(controls.get(i), asArray.get(i)[0]);
        }
    }

    @Test
    public void testControlClassesWithBehavior() {
        List<Class<Control>> controls = getControlClassesWithBehavior();
        assertEquals(controlClasses.length - withoutBehaviors.size(), controls.size());
        for (Class<Control> class1 : controls) {
            Control control = createControl(class1);
            ControlShim.installDefaultSkin(control);
            getBehavior(control.getSkin());
            createBehavior(control);
        }
    }

    @Test
    public void testGetControls() {
        List<Control> controls = getControls();
        assertEquals(controlClasses.length, controls.size());
        for (int i = 0; i < controlClasses.length; i++) {
            Class<Control> controlClass = (Class<Control>) controlClasses[i][0];
            assertSame(controlClass, controls.get(i).getClass());
        }
    }

    @Test
    public void testGetControlClasses() {
        List<Class<Control>> controls = getControlClasses();
        assertEquals(controlClasses.length, controls.size());
        for (int i = 0; i < controlClasses.length; i++) {
            Class<Control> controlClass = (Class<Control>) controlClasses[i][0];
            assertSame(controlClass, controls.get(i));
        }
    }

    @Test
    public void testAlternativeSkinAssignable() {
        for (int i = 0; i < controlClasses.length; i++) {
            Class<Control> controlClass = (Class<Control>) controlClasses[i][0];
            Control control = createControl(controlClass);
            Skin<?> old = replaceSkin(control);
            assertNotNull(control.getSkin());
            assertNotSame(old, control.getSkin());
        }
    }

    @Test
    public void testControlInstantiatable() {
        for (int i = 0; i < controlClasses.length; i++) {
            Class<Control> controlClass = (Class<Control>) controlClasses[i][0];
            Control control = createControl(controlClass);
            assertSame(controlClass, control.getClass());
        }
    }

    @Test
    public void testControlsAndSkin() {
        assertEquals(alternativeSkinClassMap.size(), controlClasses.length);
        // every control class has an entry
        for (int i = 0; i < controlClasses.length; i++) {
            Class<Control> controlClass = (Class<Control>) controlClasses[i][0];
            assertTrue(alternativeSkinClassMap.containsKey(controlClass));
        }

    }
}
