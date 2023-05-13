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

import java.util.ArrayList;

public class DataFrame {
    private String[] columns;
    private final ArrayList<String[]> rows = new ArrayList();
    
    public DataFrame() {
    }

    public static DataFrame parse(String[] lines) {
        DataFrame f = new DataFrame();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String[] ss = line.split("\\|");
            if (i == 0) {
                f.setColumns(ss);
            } else {
                f.addValues(ss);
            }
        }
        return f;
    }

    public String[] getColumnNames() {
        return columns;
    }
    
    public void setColumns(String[] columns) {
        this.columns = columns;
    }
    
    public void addValues(String[] ss) {
        rows.add(ss);
    }
    
    public int getRowCount() {
        return rows.size();
    }
    
    public String[] getRow(int ix) {
        return rows.get(ix);
    }
}
