/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.tools.fx.monkey.tools;

import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.input.PickResult;
import com.oracle.tools.fx.monkey.util.Utils;

public class AccessibilityPropertyViewer {
    private final PickResult pick;
    private Node node;
    private StringBuilder sb;

    public AccessibilityPropertyViewer(PickResult pick) {
        this.pick = pick;
    }

    public static void open(PickResult pick) {
        Node parent = pick.getIntersectedNode();
        String text = new AccessibilityPropertyViewer(pick).generate();
        Utils.showTextDialog(parent, "Accessibility", "Accessibility Properties", text);
    }

    public String generate() {
        node = pick.getIntersectedNode();
        Point3D p3 = pick.getIntersectedPoint();
        Point2D point = node.localToScreen(p3);

        Integer offset = parseInteger(node.queryAccessibleAttribute(AccessibleAttribute.OFFSET_AT_POINT, point));
        Integer line = offset == null ? null : parseInteger(node.queryAccessibleAttribute(AccessibleAttribute.LINE_FOR_OFFSET, offset));

        sb = new StringBuilder();

        sb.append("*** Node: ").append(node.getClass().getSimpleName()).append("\n");

        if (offset != null) {
            query(AccessibleAttribute.LINE_FOR_OFFSET, offset);
        }
        if (line != null) {
            query(AccessibleAttribute.LINE_START, line);
            query(AccessibleAttribute.LINE_END, line);
        }
        query(AccessibleAttribute.OFFSET_AT_POINT, point);

        return sb.toString();
    }

    private void query(AccessibleAttribute a, Object... params) {
        Object v = node.queryAccessibleAttribute(a, params);
        if (v == null) {
            return;
        }

        sb.append(a).append(": ").append(v).append("\n");
    }

    private static Integer parseInteger(Object x) {
        return (x instanceof Integer n) ? n : null;
    }
}
