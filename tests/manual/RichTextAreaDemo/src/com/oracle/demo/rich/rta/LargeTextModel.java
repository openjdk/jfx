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
package com.oracle.demo.rich.rta;

import java.util.Random;
import javafx.incubator.scene.control.rich.model.SimpleViewOnlyStyledModel;

public class LargeTextModel extends SimpleViewOnlyStyledModel {
    private final String STYLE = "-fx-font-size:500%";
    private final Random random = new Random();

    public LargeTextModel(int lineCount) {
        for (int i = 0; i < lineCount; i++) {
            addLine(i);
        }
    }

    private void addLine(int n) {
        StringBuilder sb = new StringBuilder();
        sb.append("L").append(n).append(' ');
        int ct;
        if (random.nextFloat() < 0.01f) {
            ct = 200;
        } else {
            ct = random.nextInt(10);
        }

        for (int i = 0; i < ct; i++) {
            sb.append(" ").append(i);
            int len = random.nextInt(10) + 1;
            for (int j = 0; j < len; j++) {
                sb.append('*');
            }
        }
        addSegment(sb.toString(), STYLE);
        nl();
    }
}
