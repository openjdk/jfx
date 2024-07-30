/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

package jfx.incubator.scene.control.input;

import com.sun.javafx.ModuleUtil;

/**
 * A function tag is a public identifier of a method that can be mapped to a key binding by the
 * control's {@link InputMap}.
 * <h2>Example</h2>
 * The following example is taken from the {@code TabPane} class:
 * <pre>    public class TabPane extends Control {
 *      // Identifiers for methods available for customization via the InputMap.
 *      public static final class Tag {
 *          // Selects the first tab.
 *          public static final FunctionTag SELECT_FIRST_TAB = new FunctionTag();
 *          // Selects the last tab.
 *          public static final FunctionTag SELECT_LAST_TAB = new FunctionTag();
 *          // Selects the left tab: previous in LTR mode, next in RTL mode.
 *          public static final FunctionTag SELECT_LEFT_TAB = new FunctionTag();
 *          ...
 * </pre>
 *
 * @since 999 TODO
 */
public final class FunctionTag {
    /** Constructs the function tag. */
    public FunctionTag() {
    }

    static { ModuleUtil.incubatorWarning(); }
}
