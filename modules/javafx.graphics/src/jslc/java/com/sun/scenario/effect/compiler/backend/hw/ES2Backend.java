/*
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.scenario.effect.compiler.backend.hw;

import java.util.Map;
import com.sun.scenario.effect.compiler.JSLParser;
import com.sun.scenario.effect.compiler.model.Precision;
import com.sun.scenario.effect.compiler.tree.FuncDef;
import com.sun.scenario.effect.compiler.tree.JSLVisitor;

/**
 */
public class ES2Backend extends GLSLBackend {

    public ES2Backend(JSLParser parser, JSLVisitor visitor) {
        super(parser, visitor);
    }

    // GLSL v1.10 no longer has gl_TexCoord*; these are now passed in
    // from vertex shader as texCoord0/1
    private static final Map<String, String> VAR_MAP = Map.of(
        "pos0",            "texCoord0",
        "pos1",            "texCoord1",
        "color",           "gl_FragColor",
        "jsl_vertexColor", "perVertexColor");

    private static final Map<String, String> FUNC_MAP = Map.of(
        "sample",  "texture2D",
        "ddx",     "dFdx",
        "ddy",     "dFdy",
        "intcast", "int");

    @Override
    protected String getVar(String v) {
        String s = VAR_MAP.get(v);
        return (s != null) ? s : v;
    }

    @Override
    protected String getFuncName(String f) {
        String s = FUNC_MAP.get(f);
        return (s != null) ? s : f;
    }

    @Override
    protected String getPrecision(Precision p) {
        return p.name();
    }

    @Override
    public void visitFuncDef(FuncDef d) {
        // this is a hack to help force the return value of certain Prism
        // shader functions to have lower precision
        String name = d.getFunction().getName();
        if ("mask".equals(name) || "paint".equals(name)) {
            output("lowp ");
        }
        super.visitFuncDef(d);
    }

    @Override
    protected String getHeader() {
        StringBuilder sb = new StringBuilder();
        // For the time being we are attempting to generate fragment programs
        // that will run on the desktop and on OpenGL ES 2.0 devices.
        // For OpenGL ES 2.0, fragment programs are required to specify the
        // precision for all variables.  Also for ES 2.0, the default GLSL
        // version is 1.00, so implicitly we are using "#version 100" for
        // that case.  We are not yet taking advantage of language features
        // above (desktop GLSL) version 1.10 so we can get away with not
        // including the #version directive here (it will implicitly be
        // "#version 110" for the desktop case).  It appears that the
        // desktop and ES versions of the GLSL spec may continue to be
        // developed independently (see section 10.23 in the GLSL ES spec),
        // so if we ever need to use a higher version for one case or the
        // other, it will get awkward since the #version string has to be
        // the first thing in the file (i.e., you can't put it inside the
        // "#ifdef GL_ES" section).
        // TODO: We are currently using highp across the board if it is
        // supported just to be safe, but there are likely many variables
        // that could live with mediump or lowp; should experiment with
        // using lower precision by default...
        sb.append("#ifdef GL_ES\n");
        sb.append("#extension GL_OES_standard_derivatives : enable\n");
        sb.append("#ifdef GL_FRAGMENT_PRECISION_HIGH\n");
        sb.append("precision highp float;\n");
        sb.append("precision highp int;\n");
        sb.append("#else\n");
        sb.append("precision mediump float;\n");
        sb.append("precision mediump int;\n");
        sb.append("#endif\n");
        sb.append("#else\n");
        sb.append("#define highp\n");
        sb.append("#define mediump\n");
        sb.append("#define lowp\n");
        sb.append("#endif\n");

        // output varying value declarations (passed from the vertex shader)
        if (maxTexCoordIndex >= 0) {
            sb.append("varying vec2 texCoord0;\n");
        }
        if (maxTexCoordIndex >= 1) {
            sb.append("varying vec2 texCoord1;\n");
        }
        if (isVertexColorReferenced) {
            sb.append("varying lowp vec4 perVertexColor;\n");
        }

        // output special pixcoord offset uniform variable declaration
        // at the top of the program, if needed
        if (isPixcoordReferenced) {
            sb.append("uniform vec4 jsl_pixCoordOffset;\n");
        }

        return sb.toString();
    }
}
