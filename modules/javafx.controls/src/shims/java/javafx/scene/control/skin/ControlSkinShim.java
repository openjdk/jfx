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

package javafx.scene.control.skin;

import java.lang.reflect.Field;

import com.sun.javafx.scene.control.behavior.BehaviorBase;

import javafx.scene.control.Skin;

public class ControlSkinShim {

    /**
     * Reflectively accesses and returns the value of the skin's behavior field.
     *
     * @param skin the skin to get the behavior from
     * @return the value of the skin's behavior field
     * @throws RuntimeException wrapped around the exception thrown by the reflective access
     */
    public static BehaviorBase<?> getBehavior(Skin<?> skin) {
        try {
            Field field = skin.getClass().getDeclaredField("behavior");
            field.setAccessible(true);
            return (BehaviorBase<?>) field.get(skin);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException("failed access to behavior in " + skin.getClass(), e);
        }
    }


    private ControlSkinShim() {}
}
