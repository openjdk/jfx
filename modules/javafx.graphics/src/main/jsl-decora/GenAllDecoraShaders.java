/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Method;

/**
 * This class is used as a common entry point for generating Decora shaders.
 */

public class GenAllDecoraShaders {

    private static final String [][] compileShaders = {
            {"CompileJSL", "-all", "ColorAdjust"},
            {"CompileJSL", "-all", "Brightpass"},
            {"CompileJSL", "-all", "SepiaTone"},
            {"CompileJSL", "-all", "PerspectiveTransform"},
            {"CompileJSL", "-all", "DisplacementMap"},
            {"CompileJSL", "-all", "InvertMask"},
            {"CompileBlend", "-all", "Blend"},
            {"CompilePhong", "-all", "PhongLighting"},
            {"CompileLinearConvolve", "-hw", "LinearConvolve"},
            {"CompileLinearConvolve", "-hw", "LinearConvolveShadow"}
    };

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < compileShaders.length; i++) {
            args[args.length - 2] = compileShaders[i][1]; // types of shaders to be generated
            args[args.length - 1] = compileShaders[i][2]; // jsl shader file name
            Class<?> cls = Class.forName(compileShaders[i][0]);
            Method meth = cls.getMethod("main", String[].class);
            meth.invoke(null, (Object) args);
        }
    }
}
