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

import com.sun.javafx.geom.transform.Affine3D;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.sg.PGPhongMaterial;
import com.sun.javafx.sg.PGShape3D;
import com.sun.javafx.sg.PGTriangleMesh;
import com.sun.javafx.tk.Toolkit;
import com.sun.prism.Graphics;
import com.sun.prism.Material;
import com.sun.prism.MeshFactory;
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
    private NGTriangleMesh triangleMesh;
    private MeshView nativeObject;

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
    
    protected MeshView getNativeObject(ResourceFactory rf) {

        if (nativeObject == null && triangleMesh != null) {
            MeshFactory meshFactory = rf.get3DFactory();
            nativeObject = meshFactory.createMeshView(triangleMesh.getNativeObject(meshFactory));
            setMaterial(material);
            setDrawMode(drawMode);
            setCullFace(cullFace);
        }

        if (nativeObject == null) {
            return null;
        }

        triangleMesh.updateNativeIfNeeded();

        Material mtl =  material.getNativeMaterial(rf);
        if (materialDirty) {
            nativeObject.setMaterial(mtl);
            materialDirty = false;
        }

        if (cullFaceDirty) {
            nativeObject.setCullingMode(cullFace.ordinal());
            cullFaceDirty = false;
        }

        if (drawModeDirty) {
            // This need to change once the wire up is working
            nativeObject.setWireframe(drawMode == DrawMode.LINE);
            drawModeDirty = false;
        }

        Toolkit tk = Toolkit.getToolkit();
        for (int i = 0; i < tk.getLightsInScene().size(); i++) {
            NGLightBase lightBase = (NGLightBase) tk.getLightsInScene().get(i);
            if (lightBase instanceof NGPointLight) {
                NGPointLight light = (NGPointLight) lightBase;
                Affine3D lightWT = light.getWorldTransform();
                nativeObject.setLight(i,
                        (float) lightWT.getMxt(),
                        (float) lightWT.getMyt(),
                        (float) lightWT.getMzt(),
                        light.getColor().getRed(),
                        light.getColor().getGreen(),
                        light.getColor().getBlue(), 1.0f);
            } else if (lightBase instanceof NGAmbientLight) {
                NGAmbientLight light = (NGAmbientLight) lightBase;
                nativeObject.setAmbient(light.getColor().getRed(),
                        light.getColor().getGreen(),
                        light.getColor().getBlue());
            } else {
                // Unknown light type
            }
        }

        return nativeObject;
    }

    protected void setMesh(PGTriangleMesh triangleMesh) {
        this.triangleMesh = (NGTriangleMesh)triangleMesh;
        nativeObject = null;
        visualsChanged();
    }

    protected NGTriangleMesh getMesh() {
        return triangleMesh;
    }

    // for renderContent temp
    private static final float tempTx[] = new float[12];

    // TODO: 3D HACK - Remove this temporary method. 
    // DELETE THIS ONCE D3D native clean up is done.
    // This copy with tranpose operation should be done in the native d3d pipe.    
    private void setMatrixAs3x4TempTx(BaseTransform bTx) {
        assert tempTx.length == 12;
        tempTx[0] = (float)bTx.getMxx(); tempTx[3] = (float)bTx.getMxy(); tempTx[6] = (float)bTx.getMxz();
        tempTx[1] = (float)bTx.getMyx(); tempTx[4] = (float)bTx.getMyy(); tempTx[7] = (float)bTx.getMyz();
        tempTx[2] = (float)bTx.getMzx(); tempTx[5] = (float)bTx.getMzy(); tempTx[8] = (float)bTx.getMzz();
        tempTx[9] = (float)bTx.getMxt(); tempTx[10] = (float)bTx.getMyt(); tempTx[11] = (float)bTx.getMzt();
    }
    
    protected void renderContent(Graphics g) {

        if (material == null) {
            return;
        }

        MeshView mView = getNativeObject(g.getResourceFactory());

        if (mView != null) {
            // TODO: 3D - Replace mView.setPos with setWorldTransform
            // mView.setWorldTransform(g.getTransformNoClone());
            setMatrixAs3x4TempTx(g.getTransformNoClone());
            mView.setPos(tempTx);
            g.draw3DObject(mView);
        }
    }

    @Override
    NodeType getNodeType() {
        return NodeType.NODE_3D;
    }

    @Override
    protected boolean hasOverlappingContents() {
        return false;
    }

    @Override
    public void release() {
        // TODO: 3D - Need to release native resources
//        System.err.println("NGShape3D: Need to release native resources");
    }
}
