/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit;

import java.lang.foreign.FunctionDescriptor;
import java.lang.invoke.MethodHandle;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * FFM facade for {@code wkj_context_menu_item_selected}, the one entry point
 * {@link ContextMenu} needs from the {@code jfxwebkit} C ABI.
 * <p>
 * The controller handle is the {@code WebCore::ContextMenuController*} that
 * {@code WKJHostTheme::context_menu_show} was given and that {@code ContextMenu.ShowContext} has
 * carried as {@code pdata} ever since. Dispatching an action can run script, so this is main thread
 * only - unchanged from the JNI form.
 *
 * @see com.sun.webkit.WebKitNative
 */
final class ContextMenuNative {

    private static final MethodHandle ITEM_SELECTED = WebKitNative.downcall(
            "wkj_context_menu_item_selected",
            FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_INT));

    private ContextMenuNative() {
    }

    /**
     * Dispatches the selected action.
     *
     * @param controller the {@code ContextMenuController} handle
     * @param action the WebCore {@code ContextMenuAction}
     */
    static void itemSelected(long controller, int action) {
        try {
            ITEM_SELECTED.invokeExact(controller, action);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }
}
