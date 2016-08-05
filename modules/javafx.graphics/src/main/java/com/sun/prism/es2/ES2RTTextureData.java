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

package com.sun.prism.es2;

import com.sun.prism.impl.PrismTrace;

class ES2RTTextureData extends ES2TextureData {
    private int fboID;
    private int dbID;
    private int rbID;

    ES2RTTextureData(ES2Context context, int texID, int fboID,
                     int w, int h, long size)
    {
        super(context, texID, size);
        this.fboID = fboID;
        PrismTrace.rttCreated(fboID, w, h, size);
    }

    public int getFboID() {
        return fboID;
    }

    public int getMSAARenderBufferID() {
        return this.rbID;
    }

    void setMSAARenderBufferID(int rbID) {
        // Texture ID and multisample render buffer are mutually excusive
        assert getTexID() == 0;
        this.rbID = rbID;
    }

    public int getDepthBufferID() {
        return dbID;
    }

    void setDepthBufferID(int dbID) {
        this.dbID = dbID;
    }

    @Override
    void traceDispose() {
        PrismTrace.rttDisposed(fboID);
    }

    @Override
    public void dispose() {
        super.dispose();
        if (fboID != 0) {
            context.getGLContext().deleteFBO(fboID);
            if (dbID != 0) {
                context.getGLContext().deleteRenderBuffer(dbID);
                dbID = 0;
            }
            if (rbID != 0) {
                context.getGLContext().deleteRenderBuffer(rbID);
                rbID = 0;
            }
            fboID = 0;
        }
    }
}
