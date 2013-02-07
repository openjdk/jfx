/*
 * Copyright (c) 2008, 2013 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.scenario.effect.compiler.JSLC;
import com.sun.scenario.effect.compiler.JSLC.JSLCInfo;
import com.sun.scenario.effect.light.Light;
import java.io.File;

/**
 * This class is only used at build time to generate EffectPeer
 * implementations from PhongLighting.jsl, and shouldn't be included in the
 * resulting runtime jar file.
 */
public class CompilePhong {

    // light position is constant for distant lights
    private static final String declPosConst =
        "param float3 normalizedLightPosition;";
    private static final String funcPosConst =
        "float3 Lxyz = normalizedLightPosition;";

    // light position depends on fragment location for point/spot lights
    private static final String declPosVar =
        "param float surfaceScale;\n" +
        "param float3 lightPosition;";
    private static final String funcPosVar =
        "float bumpA = sample(bumpImg, pos0).a;\n" +
        "float3 tmp = float3(pixcoord.x, pixcoord.y, surfaceScale*bumpA);\n" +
        "float3 Lxyz = normalize(lightPosition - tmp);";

    // light color is constant for distant/point lights
    private static final String declRgbConst = "";
    private static final String funcRgbConst =
        "float3 Lrgb = lightColor;";

    // light color depends on fragment location for spot lights
    private static final String declRgbVar =
        "param float3 normalizedLightDirection;\n" +
        "param float lightSpecularExponent;";
    private static final String funcRgbVar =
        "float LdotS = dot(Lxyz, normalizedLightDirection);\n" +
        "LdotS = min(LdotS, 0.0);\n" +
        "float3 Lrgb = lightColor * pow(-LdotS, lightSpecularExponent);";

    public static void main(String[] args) throws Exception {
        JSLCInfo jslcinfo = new JSLCInfo("PhongLighting");
        jslcinfo.shaderName = "PhongLighting";
        int index = jslcinfo.parseArgs(args);
        if (index != args.length - 1) {
            jslcinfo.usage(System.err);
        }
        String arg = args[index];
        if (!arg.equals(jslcinfo.shaderName)) {
            jslcinfo.error("Unrecognized argument: "+arg);
        }

        File baseFile = jslcinfo.getJSLFile();
        String base = CompileJSL.readFile(baseFile);
        long basetime = baseFile.lastModified();
        for (Light.Type type : Light.Type.values()) {
            String posDecl, posFunc, rgbFunc, rgbDecl;
            switch (type) {
            case DISTANT:
                posDecl = declPosConst;
                posFunc = funcPosConst;
                rgbDecl = declRgbConst;
                rgbFunc = funcRgbConst;
                break;
            case POINT:
                posDecl = declPosVar;
                posFunc = funcPosVar;
                rgbDecl = declRgbConst;
                rgbFunc = funcRgbConst;
                break;
            case SPOT:
                posDecl = declPosVar;
                posFunc = funcPosVar;
                rgbDecl = declRgbVar;
                rgbFunc = funcRgbVar;
                break;
            default:
                throw new InternalError();
            }
            String decls = posDecl + "\n" + rgbDecl;
            String funcs = posFunc + "\n" + rgbFunc;
            String source = String.format(base, decls, funcs);
            jslcinfo.peerName = jslcinfo.shaderName + "_" + type.name();
            JSLC.compile(jslcinfo, source, basetime);
        }
    }
}
