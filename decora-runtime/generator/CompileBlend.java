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
import com.sun.scenario.effect.Blend;
import java.io.File;
import java.util.Locale;

/**
 * This class is only used at build time to generate EffectPeer
 * implementations from Blend.jsl, and shouldn't be included in the
 * resulting runtime jar file.
 */
public class CompileBlend {

    public static void main(String[] args) throws Exception {
        JSLCInfo jslcinfo = new JSLCInfo();
        jslcinfo.shaderName = "Blend";
        jslcinfo.parseArgs(args);

        File mainFile = jslcinfo.getJSLFile();
        String main = CompileJSL.readFile(mainFile);
        long blendtime = mainFile.lastModified();
        for (Blend.Mode mode : Blend.Mode.values()) {
            String funcname = mode.name().toLowerCase(Locale.ENGLISH);
            String modename = jslcinfo.shaderName + "_" + mode.name();
            File funcFile = jslcinfo.getJSLFile(modename);
            String func = CompileJSL.readFile(funcFile);
            long modeTime = funcFile.lastModified();
            String source = String.format(main, func, funcname);
            long sourcetime = Math.max(blendtime, modeTime);
            jslcinfo.peerName = modename;
            JSLC.compile(jslcinfo, source, sourcetime);
        }
    }
}
