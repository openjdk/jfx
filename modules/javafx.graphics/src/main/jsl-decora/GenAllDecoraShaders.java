/*
 * Copyright (c) 2021, 2026, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Arrays;

/**
 * This class is used as a common entry point for generating Decora shaders.
 * <p>
 * The build passes the JSLC options every shader shares (input and output
 * directories, package, hardware shader backends) followed by the two placeholder
 * arguments {@code -all} and {@code GenAllDecoraShaders}. The placeholders are
 * checked and dropped, and each shader is compiled with the shared options, its
 * own output types and its JSL name.
 * <p>
 * The output types are {@code -java}, which selects {@code JSLC.OUT_JAVA} (the
 * Java software peer), and {@code -hw}, which selects the peer set
 * {@code JSLC.OUT_HW_PEERS}. That set is defined as {@code OUT_PRISM} alone
 * (the Prism hardware peer), both in this JSL compiler and in the ones built
 * before the SSE backend was deleted. {@code -all} and {@code -sw} are not
 * used as output types: they select {@code OUT_ALL_PEERS} and
 * {@code OUT_SW_PEERS}, which held the SSE software peers in those older
 * compilers, so the generated files would depend on which JSL compiler is on
 * the classpath.
 */

public class GenAllDecoraShaders {

    /** The generated Java software peer and the Prism hardware peer. */
    private static final String[] JAVA_AND_HW = {"-java", "-hw"};

    /** The Prism hardware peer only: these kernels have hand-written Java peers. */
    private static final String[] HW = {"-hw"};

    /** The arguments the build passes after the shared options; they are checked and dropped. */
    private static final String[] PLACEHOLDERS = {"-all", "GenAllDecoraShaders"};

    private record Shader(String compiler, String[] outputTypes, String name) {
    }

    private static final Shader[] SHADERS = {
            new Shader("CompileJSL", JAVA_AND_HW, "ColorAdjust"),
            new Shader("CompileJSL", JAVA_AND_HW, "Brightpass"),
            new Shader("CompileJSL", JAVA_AND_HW, "SepiaTone"),
            new Shader("CompileJSL", JAVA_AND_HW, "PerspectiveTransform"),
            new Shader("CompileJSL", JAVA_AND_HW, "DisplacementMap"),
            new Shader("CompileJSL", JAVA_AND_HW, "InvertMask"),
            new Shader("CompileBlend", JAVA_AND_HW, "Blend"),
            new Shader("CompilePhong", JAVA_AND_HW, "PhongLighting"),
            new Shader("CompileLinearConvolve", HW, "LinearConvolve"),
            new Shader("CompileLinearConvolve", HW, "LinearConvolveShadow")
    };

    public static void main(String[] args) throws Exception {
        int sharedCount = args.length - PLACEHOLDERS.length;
        if (sharedCount < 0
                || !Arrays.equals(args, sharedCount, args.length, PLACEHOLDERS, 0, PLACEHOLDERS.length)) {
            throw new IllegalArgumentException("expected the shared JSLC options followed by the placeholder"
                    + " arguments " + Arrays.toString(PLACEHOLDERS) + ", got " + Arrays.toString(args));
        }
        String[] shared = Arrays.copyOf(args, sharedCount);
        for (Shader shader : SHADERS) {
            String[] types = shader.outputTypes();
            String[] shaderArgs = Arrays.copyOf(shared, shared.length + types.length + 1);
            System.arraycopy(types, 0, shaderArgs, shared.length, types.length);
            shaderArgs[shaderArgs.length - 1] = shader.name();
            Class<?> cls = Class.forName(shader.compiler());
            Method meth = cls.getMethod("main", String[].class);
            meth.invoke(null, (Object) shaderArgs);
        }
    }
}
