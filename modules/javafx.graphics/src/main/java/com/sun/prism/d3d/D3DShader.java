/*
 * Copyright (c) 2008, 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.d3d;

import com.sun.prism.impl.BufferUtil;
import com.sun.prism.ps.Shader;
import java.lang.foreign.MemorySegment;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Map;

final class D3DShader extends D3DResource implements Shader {

    private static IntBuffer itmp;
    private static FloatBuffer ftmp;
    private final Map<String, Integer> registers;
    private boolean valid;

    /** @param pData the {@code D3DPixelShaderResource*} from {@code D3DNative.shaderCreate}, or NULL */
    D3DShader(D3DContext context, MemorySegment pData, Map<String, Integer> registers) {
        super(new D3DRecord(context, pData));
        this.valid = (pData.address() != 0L);
        this.registers = registers;
    }

    @Override
    public void enable() {
        // res >= 0 is equivalent to D3D's SUCCEEDED(res) macro
        int res = D3DNative.shaderEnable(d3dResRecord.getContext().getContextHandle(),
                                         d3dResRecord.getResource());
        valid &= res >= 0;
        D3DContext.validate(res);
    }

    @Override
    public void disable() {
        int res = D3DNative.shaderDisable(d3dResRecord.getContext().getContextHandle());
        valid &= res >= 0;
        D3DContext.validate(res);
    }

    private static void checkTmpIntBuf() {
        if (itmp == null) {
            itmp = BufferUtil.newIntBuffer(4);
        }
        itmp.clear();
    }

    @Override
    public void setConstant(String name, int i0) {
        // NOTE: see HLSLBackend for an explanation of why we're using
        // floats here instead of ints...
        /*
        checkTmpIntBuf();
        itmp.put(i0);
        setConstants(name, itmp, 0, 1);
         */
        setConstant(name, (float)i0);
    }

    @Override
    public void setConstant(String name, int i0, int i1) {
        /*
        checkTmpIntBuf();
        itmp.put(i0);
        itmp.put(i1);
        setConstants(name, itmp, 0, 1);
         */
        setConstant(name, (float)i0, (float)i1);
    }

    @Override
    public void setConstant(String name, int i0, int i1, int i2) {
        /*
        checkTmpIntBuf();
        itmp.put(i0);
        itmp.put(i1);
        itmp.put(i2);
        setConstants(name, itmp, 0, 1);
         */
        setConstant(name, (float)i0, (float)i1, (float)i2);
    }

    @Override
    public void setConstant(String name, int i0, int i1, int i2, int i3) {
        /*
        checkTmpIntBuf();
        itmp.put(i0);
        itmp.put(i1);
        itmp.put(i2);
        itmp.put(i3);
        setConstants(name, itmp, 0, 1);
         */
        setConstant(name, (float)i0, (float)i1, (float)i2, (float)i3);
    }

    @Override
    public void setConstants(String name, IntBuffer buf, int off, int count) {
        // NOTE: see HLSLBackend for an explanation of why we need to use
        // floats instead of ints; for now this codepath is disabled...
        //setConstantsI(pData, getRegister(name), buf, off, count);
        throw new InternalError("Not yet implemented");
    }

    private static void checkTmpFloatBuf() {
        if (ftmp == null) {
            ftmp = BufferUtil.newFloatBuffer(4);
        }
        ftmp.clear();
    }

    @Override
    public void setConstant(String name, float f0) {
        checkTmpFloatBuf();
        ftmp.put(f0);
        setConstants(name, ftmp, 0, 1);
    }

    @Override
    public void setConstant(String name, float f0, float f1) {
        checkTmpFloatBuf();
        ftmp.put(f0);
        ftmp.put(f1);
        setConstants(name, ftmp, 0, 1);
    }

    @Override
    public void setConstant(String name, float f0, float f1, float f2) {
        checkTmpFloatBuf();
        ftmp.put(f0);
        ftmp.put(f1);
        ftmp.put(f2);
        setConstants(name, ftmp, 0, 1);
    }

    @Override
    public void setConstant(String name, float f0, float f1, float f2, float f3) {
        checkTmpFloatBuf();
        ftmp.put(f0);
        ftmp.put(f1);
        ftmp.put(f2);
        ftmp.put(f3);
        setConstants(name, ftmp, 0, 1);
    }

    @Override
    public void setConstants(String name, FloatBuffer buf, int off, int count) {
            int res = D3DNative.shaderSetConstantsF(d3dResRecord.getContext().getContextHandle(),
                                                    getRegister(name), buf, off, count);
            valid &= res >= 0;
            D3DContext.validate(res);
    }

    private int getRegister(String name) {
        Integer reg = registers.get(name);
        if (reg == null) {
            // The register map is the only source of registers: the native lookup the JNI version
            // fell back to here (nGetRegister) answered -1 for every name, so a name missing from
            // the map has always been this error.
            throw new IllegalArgumentException("Register not found for: " +
                                               name);
        }
        return reg;
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    @Override
    public void dispose() {
        super.dispose();
        valid = false;
    }
}
