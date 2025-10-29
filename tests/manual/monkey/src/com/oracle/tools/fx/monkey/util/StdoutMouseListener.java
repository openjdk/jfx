/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.fx.monkey.util;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.MouseEvent;

/**
 * MouseListener which prints to stdout.
 */
public class StdoutMouseListener {
    private static final EventHandler<MouseEvent> listener = StdoutMouseListener::handle;
    private static final Object KEY = new Object();

    private StdoutMouseListener() {
    }

    public static void attach(ContextMenu m, Node n) {
        if (n != null) {
            String name = "Mouse Listener (" + Utils.simpleName(n) + ")";
            FX.checkItem(m, name, StdoutMouseListener.isRegistered(n), (on) -> {
                update(n, on);
            });
        }
    }

    static boolean isRegistered(Node n) {
        return n.getProperties().containsKey(KEY);
    }

    static void update(Node n, boolean on) {
        if (on) {
            if (!isRegistered(n)) {
                n.addEventHandler(MouseEvent.ANY, listener);
                n.getProperties().put(KEY, Boolean.TRUE);
            }
        } else {
            n.removeEventHandler(MouseEvent.ANY, listener);
            n.getProperties().remove(KEY);
        }
    }

    static void handle(MouseEvent ev) {
        StringBuilder sb = new StringBuilder();
        sb.append(ev.getEventType());
        sb.append(" (");
        sb.append(Utils.f2(ev.getX())).append(", ").append(Utils.f2(ev.getY()));
        sb.append(") screen=(");
        sb.append(Utils.f2(ev.getScreenX())).append(", ").append(Utils.f2(ev.getScreenY()));
        sb.append(")");
        System.out.println(sb);
    }
}
