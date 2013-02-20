/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.scenario.effect.impl.state.LinearConvolveKernel;
import java.io.File;

/**
 * This class is only used at build time to generate EffectPeer
 * implementations from the LinearConvolve JSL file, and should
 * not be included in the resulting runtime jar file.
 */
public class CompileLinearConvolve {
    /*
     * The basic idea here is to create a few different versions of the
     * LinearConvolve hardware effect peers, based on the kernel size.
     * The LinearConvolveKernel state class contains the algorithm that
     * determines how many peers should be generated and at which optimized
     * sizes.
     */
    private static void compileLinearConvolve(JSLCInfo jslcinfo, String name)
        throws Exception
    {
        int outTypes = jslcinfo.outTypes;
        jslcinfo.shaderName = "Effect";
        File baseFile = jslcinfo.getJSLFile(name);
        String base = CompileJSL.readFile(baseFile);
        long basetime = baseFile.lastModified();

        // output one hardware shader for each unrolled size (as determined
        // by the LinearConvolveKernel quantization algorithm)
        jslcinfo.outTypes = (outTypes & JSLC.OUT_HW_SHADERS);
        int lastpeersize = -1;
        for (int i = 1; i < LinearConvolveKernel.MAX_KERNEL_SIZE; i += 4) {
            int peersize = LinearConvolveKernel.getPeerSize(i);
            if (peersize != lastpeersize) {
                String source = String.format(base, peersize/4, peersize/4);
                jslcinfo.peerName = name + "_" + peersize;
                JSLC.compile(jslcinfo, source, basetime);
                lastpeersize = peersize;
            }
        }

        // output a single hardware peer class (can be instantiated for
        // each of the shaders generated above)
        jslcinfo.outTypes = (outTypes & JSLC.OUT_HW_PEERS);
        jslcinfo.peerName = name;
        jslcinfo.interfaceName = "LinearConvolvePeer";
        int peersize = LinearConvolveKernel.MAX_KERNEL_SIZE / 4;
        String genericbase = String.format(base, peersize, 0);
        JSLC.compile(jslcinfo, genericbase, basetime);

        // output a single version of the software peer (there's
        // no loop unrolling in this case)
        jslcinfo.outTypes = (outTypes & JSLC.OUT_SW_PEERS);
        JSLC.compile(jslcinfo, genericbase, basetime);
    }

    public static void main(String[] args) throws Exception {
        JSLCInfo jslcinfo = new JSLCInfo("LinearConvolve[Shadow]");
        int index = jslcinfo.parseArgs(args);
        if (index != args.length - 1) {
            jslcinfo.usage(System.err);
        }
        String arg = args[index];
        if (arg.equals("LinearConvolve") ||
            arg.equals("LinearConvolveShadow"))
        {
            compileLinearConvolve(jslcinfo, arg);
        } else {
            jslcinfo.error("Unrecognized argument: "+arg);
        }
    }
}
