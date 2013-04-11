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

package com.sun.javafx.sg.prism;

import com.sun.javafx.geom.Vec3d;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.sg.PGPhongMaterial;
import com.sun.javafx.sg.PGShape3D;
import com.sun.javafx.sg.PGTriangleMesh;
import com.sun.prism.Graphics;
import com.sun.prism.Material;
import com.sun.prism.MeshView;
import com.sun.prism.ResourceFactory;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;

/**
 * TODO: 3D - Need documentation
 */
public abstract class NGShape3D extends NGNode implements PGShape3D {
    protected NGPhongMaterial material;
    protected DrawMode drawMode;
    protected CullFace cullFace;
    protected boolean materialDirty = false;
    protected boolean drawModeDirty = false;
    protected boolean cullFaceDirty = false;
    private NGTriangleMesh mesh;
    private MeshView meshView;

    public void setMaterial(PGPhongMaterial material) {
        this.material = (NGPhongMaterial) material;
        materialDirty = true;
        visualsChanged();
    }
    public void setDrawMode(Object drawMode) {
        this.drawMode = (DrawMode) drawMode;
        drawModeDirty = true;
        visualsChanged();
    }

    public void setCullFace(Object cullFace) {
        this.cullFace = (CullFace) cullFace;
        cullFaceDirty = true;
        visualsChanged();
    }

    protected void invalidate() {
        setMesh(null);
    }

    protected void renderMeshView(Graphics g) {

        //validate state
        g.setup3DRendering();

        ResourceFactory rf = g.getResourceFactory();

        if (meshView == null && mesh != null) {
            meshView = rf.createMeshView(mesh.createMesh(rf));
            setMaterial(material);
            setDrawMode(drawMode);
            setCullFace(cullFace);
        }

        if (meshView == null) {
            return;
        }

        mesh.validate();

        Material mtl =  material.createMaterial(rf);
        if (materialDirty) {
            meshView.setMaterial(mtl);
            materialDirty = false;
        }

        if (cullFaceDirty) {
            meshView.setCullingMode(cullFace.ordinal());
            cullFaceDirty = false;
        }

        if (drawModeDirty) {
            meshView.setWireframe(drawMode == DrawMode.LINE);
            drawModeDirty = false;
        }

        // Setup lights
        int pointLightIdx = 0;
        if (g.getLights() == null || g.getLights()[0] == null) {
            // If no lights are in scene apply default light. Default light
            // is a single point white point light at camera eye position.
            meshView.setAmbientLight(0.0f, 0.0f, 0.0f);
            Vec3d cameraPos = g.getCameraNoClone().getPositionInWorld(null);
            meshView.setPointLight(pointLightIdx++,
                                   (float)cameraPos.x,
                                   (float)cameraPos.y,
                                   (float)cameraPos.z,
                                   1.0f, 1.0f, 1.0f, 1.0f);
        } else {
            float ambientRed = 0.0f;
            float ambientBlue = 0.0f;
            float ambientGreen = 0.0f;

            for (int i = 0; i < g.getLights().length; i++) {
                NGLightBase lightBase = (NGLightBase)g.getLights()[i];
                if (lightBase == null) {
                    // The array of lights can have nulls
                    break;
                } else if (lightBase.affects(this)) {
                    if (lightBase instanceof NGPointLight) {
                        NGPointLight light = (NGPointLight) lightBase;
                        float intensity = light.getColor().getAlpha();
                        if (intensity == 0.0f) {
                            continue;
                        }
                        Affine3D lightWT = light.getWorldTransform();
                        meshView.setPointLight(pointLightIdx++,
                                (float)lightWT.getMxt(),
                                (float)lightWT.getMyt(),
                                (float)lightWT.getMzt(),
                                light.getColor().getRed(),
                                light.getColor().getGreen(),
                                light.getColor().getBlue(),
                                intensity);
                    } else if (lightBase instanceof NGAmbientLight) {
                        // Accumulate ambient lights
                        ambientRed   += lightBase.getColor().getRedPremult();
                        ambientGreen += lightBase.getColor().getGreenPremult();
                        ambientBlue  += lightBase.getColor().getBluePremult();
                    } else {
                        // Unknown light type
                    }
                }
            }
            ambientRed = saturate(ambientRed);
            ambientGreen = saturate(ambientGreen);
            ambientBlue = saturate(ambientBlue);
            meshView.setAmbientLight(ambientRed, ambientGreen, ambientBlue);
        }
        // TODO: 3D Required for D3D implementation of lights, which is limited to 3
        while (pointLightIdx < 3) {
            // Reset any previously set lights
            meshView.setPointLight(pointLightIdx++, 0, 0, 0, 0, 0, 0, 0);
        }

        meshView.render(g);
    }

    // Clamp between [0, 1]
    private static float saturate(float value) {
        return value < 1.0f ? ((value < 0.0f) ? 0.0f : value) : 1.0f;
    }

    protected void setMesh(PGTriangleMesh triangleMesh) {
        this.mesh = (NGTriangleMesh)triangleMesh;
        meshView = null;
        visualsChanged();
    }

    protected NGTriangleMesh getMesh() {
        return mesh;
    }

    protected void renderContent(Graphics g) {

        if (material == null) {
            return;
        }

        renderMeshView(g);
    }

    // This node requires 3D graphics state for rendering
    @Override
    boolean isShape3D() {
        return true;
    }

    @Override
    protected boolean hasOverlappingContents() {
        return false;
    }

    @Override
    public void release() {
        // TODO: 3D - Need to release native resources
        // material, mesh and meshview have native backing that need clean up.
    }
}
