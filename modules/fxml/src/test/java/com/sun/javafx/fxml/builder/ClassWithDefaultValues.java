/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.fxml.builder;

import java.util.List;
import javafx.beans.NamedArg;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.StageStyle;

public class ClassWithDefaultValues {
    public double a;
    public double b;
    public List<Integer> list;
    public Color color;
    public Paint fill;
    public StageStyle stageStyle;

    public ClassWithDefaultValues(
            @NamedArg(value="a", defaultValue="1.0") double a,
            @NamedArg(value="b", defaultValue="2.0") double b,
            @NamedArg(value="color", defaultValue="red") Color color,
            @NamedArg(value="fill", defaultValue="GREEN") Paint fill,
            @NamedArg(value="stageStyle", defaultValue="DECORATED") StageStyle stageStyle            
            ) {
        this.a = a;
        this.b = b;
        this.color = color;
        this.fill = fill;
        this.stageStyle = stageStyle;
    }
}
