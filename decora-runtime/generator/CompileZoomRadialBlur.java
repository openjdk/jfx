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

import java.io.File;
import com.sun.scenario.effect.compiler.JSLC;
import com.sun.scenario.effect.compiler.JSLC.JSLCInfo;

/**
 * This class is only used at build time to generate EffectPeer
 * implementations from Gaussian-based JSL files, and shouldn't be included
 * in the resulting runtime jar file.
 */
public class CompileZoomRadialBlur {

    /*
     * The basic idea here is to create a few different versions of the
     * Gaussian-based hardware effect peers, based on the kernel size.
     * The first one handles kernel sizes up to 10, the next one up to 20,
     * and so on.  This is better than unrolling a single version, since the
     * generated shader would be very inefficient for smaller kernel sizes.
     */
    private static void compileZoomRadialBlur(JSLCInfo jslcinfo)
        throws Exception
    {
        int outTypes = jslcinfo.outTypes;
        File baseFile = jslcinfo.getJSLFile();
        String base = CompileJSL.readFile(baseFile);
        long basetime = baseFile.lastModified();

        // output one hardware shader for each unrolled size
        jslcinfo.outTypes = (outTypes & JSLC.OUT_HW_SHADERS);
        for (int i = 4; i <= 68; i+=4) {
            String source = String.format(base, 2*i + 1);
            jslcinfo.peerName = jslcinfo.shaderName + "_" + i;
            JSLC.compile(jslcinfo, source, basetime);
        }

        // output a single hardware peer class (can be instantiated for
        // each of the shaders generated above)
        jslcinfo.outTypes = (outTypes & JSLC.OUT_HW_PEERS);
        jslcinfo.peerName = null;
        String genericbase = String.format(base, 0);
        JSLC.compile(jslcinfo, genericbase, basetime);

        // output a single version of the software peer (there's
        // no loop unrolling in this case)
        jslcinfo.outTypes = (outTypes & JSLC.OUT_SW_PEERS);
        JSLC.compile(jslcinfo, genericbase, basetime);
    }

    public static void main(String[] args) throws Exception {
        JSLCInfo jslcinfo = new JSLCInfo();
        jslcinfo.shaderName = "ZoomRadialBlur";
        jslcinfo.parseAllArgs(args);
        compileZoomRadialBlur(jslcinfo);
    }
}
