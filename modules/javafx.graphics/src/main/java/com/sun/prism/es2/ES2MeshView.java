/*
 * Copyright (c) 2013, 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.es2;

import com.sun.prism.Graphics;
import com.sun.prism.Material;
import com.sun.prism.impl.BaseMeshView;
import com.sun.prism.impl.Disposer;

/**
 * TODO: 3D - Need documentation
 */
class ES2MeshView extends BaseMeshView {

    static int count = 0;
    private final ES2Context context;
    private float ambientLightRed = 0;
    private float ambientLightBlue = 0;
    private float ambientLightGreen = 0;

    // The cull and fill state the folded-away native MeshViewInfo used to carry;
    // es2_mesh_render now receives these directly. Initialised to the values
    // nCreateES2MeshView used to set (cullEnable = GL_TRUE, cullMode = GL_BACK,
    // fillMode = GL_FILL) so behaviour is unchanged by the FFM migration.
    private int cullEnable = 1;
    private int cullModeGL = ES2Native.GL_BACK;
    private int fillModeGL = ES2Native.GL_FILL;

    // NOTE: We only support up to 3 point lights at the present
    private ES2Light[] lights = new ES2Light[3];

    // TODO: 3D - Need a mechanism to "decRefCount" Mesh and Material
    //            if we need to do eager clean up
    final private ES2Mesh mesh;
    private ES2PhongMaterial material;

    private ES2MeshView(ES2Context context, ES2Mesh mesh,
            Disposer.Record disposerRecord) {
        super(disposerRecord);
        this.context = context;
        this.mesh = mesh;
        count++;
    }

    static ES2MeshView create(ES2Context context, ES2Mesh mesh) {
        return new ES2MeshView(context, mesh, new ES2MeshViewDisposerRecord());
    }

    @Override
    public void setCullingMode(int cullingMode) {
        // Faithful port of the JNI nSetCullingMode switch (GLContext.c). NGShape3D
        // passes CullFace.ordinal() i.e. MeshView.CULL_* (0/1/2), which never matched
        // these GLContext.GL_BACK / GL_FRONT / GL_NONE cases (110/111/112), so the
        // native MeshViewInfo always kept its nCreateES2MeshView init defaults
        // (cullEnable = GL_TRUE, cullMode = GL_BACK). Preserved verbatim so the FFM
        // migration is behaviour-neutral (see ES2 migration notes).
        switch (cullingMode) {
            case GLContext.GL_BACK:
                cullEnable = 1;
                cullModeGL = ES2Native.GL_BACK;
                break;
            case GLContext.GL_FRONT:
                cullEnable = 1;
                cullModeGL = ES2Native.GL_FRONT;
                break;
            case GLContext.GL_NONE:
                cullEnable = 0;
                cullModeGL = ES2Native.GL_BACK;
                break;
            default:
                // No-op, exactly as the JNI switch (which had no default clause) did
                // for the CullFace-ordinal values actually supplied at runtime.
                break;
        }
    }

    @Override
    public void setMaterial(Material material) {
        this.material = (ES2PhongMaterial) material;
    }

    @Override
    public void setWireframe(boolean wireframe) {
        // nSetWireframe stored GL_LINE / GL_FILL into MeshViewInfo.fillMode.
        fillModeGL = wireframe ? ES2Native.GL_LINE : ES2Native.GL_FILL;
    }

    @Override
    public void setAmbientLight(float r, float g, float b) {
        // The native nSetAmbientLight store was dead (never read); ES2PhongShader
        // reads these Java fields directly.
        ambientLightRed = r;
        ambientLightGreen = g;
        ambientLightBlue = b;
    }

    float getAmbientLightRed() {
        return ambientLightRed;
    }

    float getAmbientLightGreen() {
        return ambientLightGreen;
    }

    float getAmbientLightBlue() {
        return ambientLightBlue;
    }

    @Override
    public void setLight(int index, float x, float y, float z, float r, float g, float b, float w,
            float ca, float la, float qa, float isAttenuated, float maxRange, float dirX, float dirY, float dirZ,
            float innerAngle, float outerAngle, float falloff) {
        // NOTE: We only support up to 3 point lights at the present
        if (index >= 0 && index <= 2) {
            // The native nSetLight store was dead (never read); ES2PhongShader reads
            // this lights[] array directly.
            lights[index] = new ES2Light(x, y, z, r, g, b, w, ca, la, qa, isAttenuated,
                    maxRange, dirX, dirY, dirZ, innerAngle, outerAngle, falloff);
        }
    }

    ES2Light[] getLights() {
        return lights;
    }

    ES2Mesh getMesh() {
        return mesh;
    }

    int getCullEnable() {
        return cullEnable;
    }

    int getCullModeGL() {
        return cullModeGL;
    }

    int getFillModeGL() {
        return fillModeGL;
    }

    @Override
    public void render(Graphics g) {
        // nRenderMeshView early-returned when phongMaterialInfo was NULL; es2_mesh_render
        // no longer tracks the material, so reproduce that gate here.
        if (material == null) {
            return;
        }
        material.lockTextureMaps();
        context.renderMeshView(g, this);
        material.unlockTextureMaps();
    }

    ES2PhongMaterial getMaterial() {
        return material;
    }

    @Override
    public void dispose() {
        // TODO: 3D - Need a mechanism to "decRefCount" Mesh and Material
        material = null;
        lights = null;
        disposerRecord.dispose();
        count--;
    }

    public int getCount() {
        return count;
    }

    /**
     * ES2MeshView no longer owns a native MeshViewInfo: nCreateES2MeshView /
     * nReleaseES2MeshView were folded away and es2_mesh_render now takes the mesh
     * handle plus the cull / fill state directly. This record therefore has nothing
     * to release; it exists only so BaseGraphicsResource has a Disposer.Record to
     * register.
     */
    static class ES2MeshViewDisposerRecord implements Disposer.Record {

        @Override
        public void dispose() { }
    }
}
