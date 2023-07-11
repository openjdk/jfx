/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.tools.demo.rich;

import java.util.Random;

public class UnevenStyledTextModel extends SimpleReadOnlyStyledModel {
    private Random r = new Random();

    public UnevenStyledTextModel(int lineCount) {
        float longLineProbability = 0.1f;
        for (int i = 0; i < lineCount; i++) {
            boolean large = (r.nextFloat() < longLineProbability);
            addSegment((large ? "L." : "S.") + (i + 1));

            if (large) {
                add(1000);
            } else {
                add(10);
            }
            nl();
        }
    }

    private void add(int count) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < count; i++) {
            int len = r.nextInt(10) + 1;
            sb.append(' ');
            sb.append(i);
            sb.append('.');
            
            for (int j = 0; j < len; j++) {
                sb.append('*');
            }
        }

        addSegment(sb.toString());
    }
}
