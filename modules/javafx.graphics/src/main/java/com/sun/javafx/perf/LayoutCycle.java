/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.perf;

public class LayoutCycle {

    public enum Type {
        MANUAL,
        SCENE,
        PULSE
    }

    private final LayoutFrame root;
    private final Type type;

    LayoutCycle(LayoutFrame root, Type type) {
        this.root = root;
        this.type = type;
    }

    public LayoutFrame getRoot() {
        return root;
    }

    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("Laying out ");
        builder.append(root.getNode().getClass().getSimpleName()).append(' ');

        switch (type) {
            case MANUAL:
                builder.append("(triggered manually)");
                break;
            case SCENE:
                builder.append("(triggered by scene, out of pulse)");
                break;
            case PULSE:
                builder.append("(triggered by scene pulse)");
                break;
        }

        builder.append(System.lineSeparator()).append("--> ");

        printLayoutTree(builder, root, "    ", false);

        return builder.toString();
    }

    private void printLayoutTree(StringBuilder text, LayoutFrame frame, String prefix, boolean skin) {
        String className = frame.getNode().getClass().getSimpleName();
        if (className.isEmpty()) {
            className = frame.getNode().getClass().getSuperclass().getSimpleName();
        }

        text.append(className);

        String id = frame.getNode().getId();
        boolean bracket = id != null && !id.isEmpty() || frame.isLayoutRoot() || skin;

        if (bracket) {
            text.append("[");
            boolean comma = false;

            if (id != null && !id.isEmpty()) {
                text.append("id=").append(id);
                comma = true;
            }

            if (frame.isLayoutRoot()) {
                text.append(comma ? ", root" : "root");
                comma = true;
            }

            if (skin) {
                text.append(comma ? ", skin" : "skin");
            }

            text.append("]");
        }

        text.append(": ")
            .append(frame.getPasses())
            .append(System.lineSeparator());

        skin = isControl(frame.getNode().getClass());

        for (int i = 0; i < frame.getChildren().size(); ++i) {
            text.append(prefix);
            String appendix;

            if (i == frame.getChildren().size() - 1) {
                text.append("\\--- ");
                appendix = "     ";
            } else {
                text.append("+--- ");
                appendix = "|    ";
            }

            printLayoutTree(text, frame.getChildren().get(i), prefix + appendix, skin);
        }
    }

    private boolean isControl(Class<?> clazz) {
        return clazz.getName().equals("javafx.scene.control.Control") ||
            clazz.getSuperclass() != null && isControl(clazz.getSuperclass());
    }

}
