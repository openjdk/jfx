/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import com.sun.javafx.geom.Vec3d;
import com.sun.javafx.geom.transform.Affine3D;
import com.sun.prism.Graphics;
import com.sun.prism.Material;
import com.sun.prism.MeshView;
import com.sun.prism.ResourceFactory;

/**
 * TODO: 3D - Need documentation
 */
public abstract class NGShape3D extends NGNode {
    private NGPhongMaterial material;
    private DrawMode drawMode;
    private CullFace cullFace;
    private boolean materialDirty = false;
    private boolean drawModeDirty = false;
    NGTriangleMesh mesh;
    private MeshView meshView;

    public void setMaterial(NGPhongMaterial material) {
        this.material = material;
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
        visualsChanged();
    }

    void invalidate() {
        meshView = null;
        visualsChanged();
    }

    private void renderMeshView(Graphics g) {

        //validate state
        g.setup3DRendering();

        ResourceFactory rf = g.getResourceFactory();
        if (rf == null || rf.isDisposed()) {
            return;
        }

        // Check whether the meshView is valid; dispose and recreate if needed
        if (meshView != null && !meshView.isValid()) {
            meshView.dispose();
            meshView = null;
        }

        if (meshView == null && mesh != null) {
            meshView = rf.createMeshView(mesh.createMesh(rf));
            materialDirty = drawModeDirty = true;
        }

        if (meshView == null || !mesh.validate()) {
            return;
        }

        Material mtl =  material.createMaterial(rf);
        if (materialDirty) {
            meshView.setMaterial(mtl);
            materialDirty = false;
        }

        // NOTE: Always check determinant in case of mirror transform.
        int cullingMode = cullFace.ordinal();
        if (cullFace.ordinal() != MeshView.CULL_NONE
                && g.getTransformNoClone().getDeterminant() < 0) {
            cullingMode = cullingMode == MeshView.CULL_BACK
                    ? MeshView.CULL_FRONT : MeshView.CULL_BACK;
        }
        meshView.setCullingMode(cullingMode);

        if (drawModeDirty) {
            meshView.setWireframe(drawMode == DrawMode.LINE);
            drawModeDirty = false;
        }

        // Setup lights
        int pointLightIdx = 0;
        if (g.getLights() == null || g.getLights()[0] == null) {
            // If no lights are in scene apply default light. Default light
            // is a single white point light at camera eye position.
            meshView.setAmbientLight(0.0f, 0.0f, 0.0f);
            Vec3d cameraPos = g.getCameraNoClone().getPositionInWorld(null);
            meshView.setPointLight(pointLightIdx++,
                                   (float)cameraPos.x,
                                   (float)cameraPos.y,
                                   (float)cameraPos.z,
                                   1.0f, 1.0f, 1.0f, 1.0f,
                                   NGPointLight.getDefaultCa(),
                                   NGPointLight.getDefaultLa(),
                                   NGPointLight.getDefaultQa(),
                                   NGPointLight.getDefaultMaxRange());
        } else {
            float ambientRed = 0.0f;
            float ambientBlue = 0.0f;
            float ambientGreen = 0.0f;

            for (int i = 0; i < g.getLights().length; i++) {
                NGLightBase lightBase = g.getLights()[i];
                if (lightBase == null) {
                    // The array of lights can have nulls
                    break;
                } else if (lightBase.affects(this)) {
                    float rL = lightBase.getColor().getRed();
                    float gL = lightBase.getColor().getGreen();
                    float bL = lightBase.getColor().getBlue();
                    /* TODO: 3D
                     * There is a limit on the number of point lights that can affect
                     * a 3D shape. (Currently we simply select the first 3)
                     * Thus it is important to select the most relevant lights.
                     *
                     * One such way would be to sort lights according to
                     * intensity, which becomes especially relevant when lights
                     * are attenuated. Only the most intense set of lights
                     * would be set.
                     * The approximate intesity a light will have on a given
                     * shape, could be defined by:
                     */
//                    // Where d is distance from point light
//                    float attenuationFactor = 1/(c + cL * d + cQ * d * d);
//                    float intensity = rL * 0.299f + gL * 0.587f + bL * 0.114f;
//                    intensity *= attenuationFactor;
                    if (lightBase instanceof NGPointLight) {
                        NGPointLight light = (NGPointLight)lightBase;
                        if (rL != 0.0f || gL != 0.0f || bL != 0.0f) {
                            Affine3D lightWT = light.getWorldTransform();
                            meshView.setPointLight(pointLightIdx++,
                                    (float)lightWT.getMxt(),
                                    (float)lightWT.getMyt(),
                                    (float)lightWT.getMzt(),
                                    rL, gL, bL, 1.0f,
                                    light.getCa(),
                                    light.getLa(),
                                    light.getQa(),
                                    light.getMaxRange());
                        }
                    } else if (lightBase instanceof NGAmbientLight) {
                        // Accumulate ambient lights
                        ambientRed   += rL;
                        ambientGreen += gL;
                        ambientBlue  += bL;
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
                meshView.setPointLight(pointLightIdx++,
                        0, 0, 0, // x y z
                        0, 0, 0, 0, // r g b w
                        1, 0, 0, 0); // ca la qa maxRange
        }

        meshView.render(g);
    }

    // Clamp between [0, 1]
    private static float saturate(float value) {
        return value < 1.0f ? ((value < 0.0f) ? 0.0f : value) : 1.0f;
    }

    public void setMesh(NGTriangleMesh triangleMesh) {
        this.mesh = triangleMesh;
        meshView = null;
        visualsChanged();
    }

    @Override
    protected void renderContent(Graphics g) {
        if (!Platform.isSupported(ConditionalFeature.SCENE3D) ||
             material == null ||
             g instanceof com.sun.prism.PrinterGraphics)
        {
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
