/*
 * Copyright (c) 2024, 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jfx.incubator.scene.control.input;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import com.sun.javafx.util.Utils;
import jfx.incubator.scene.control.input.FunctionTag;
import jfx.incubator.scene.control.input.InputMap;
import jfx.incubator.scene.control.input.SkinInputMap;

/**
 * Hides execute() methods in InputMap from the public.
 */
public class InputMapHelper {
    public interface Accessor {
        public void execute(Object source, InputMap inputMap, FunctionTag tag);
        public void executeDefault(Object source, InputMap inputMap, FunctionTag tag);
        public void setSkinInputMap(InputMap m, SkinInputMap sm);
    }

    static {
        Utils.forceInit(InputMap.class);
    }

    private static Accessor accessor;

    public static void setAccessor(Accessor a) {
        if (accessor != null) {
            throw new IllegalStateException();
        }
        accessor = a;
    }

    public static void execute(Object source, InputMap inputMap, FunctionTag tag) {
        accessor.execute(source, inputMap, tag);
    }

    public static void executeDefault(Object source, InputMap inputMap, FunctionTag tag) {
        accessor.executeDefault(source, inputMap, tag);
    }

    /// Called using reflection from Control:981
    /// skin can be null
    public static void setSkinInputMap(Control c, Skin<?> skin) {
        InputMap m = getInputMap(c);
        if (m != null) {
            SkinInputMap sm = getSkinInputMap(skin);
            accessor.setSkinInputMap(m, sm);
        }
    }

    // will be replaced by Control.getInputMap() JDK-8314968
    private static InputMap getInputMap(Control c) {
        try {
            Method m = c.getClass().getMethod("getInputMap");
            var x = m.invoke(c);
            if (x instanceof InputMap im) {
                return im;
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            // ignore all errors
        }
        return null;
    }

    // will be replaced by Skin.getSkinInputMap() JDK-8314968
    private static SkinInputMap getSkinInputMap(Skin<?> skin) {
        if (skin != null) {
            try {
                Method m = skin.getClass().getMethod("getSkinInputMap");
                var x = m.invoke(skin);
                if (x instanceof SkinInputMap sm) {
                    return sm;
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                // ignore all errors
            }
        }
        return null;
    }
}
