/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.prism.impl.BaseGraphicsResource;
import com.sun.prism.impl.Disposer;

/**
 * This class provides base functionality for tracking and releasing native
 * d3d-related resources.
 *
 * When a Direct3D resource (such as texture, swap chain or pixel shader) is
 * created at the native level it is added to the list of resources (see
 * D3DResourceManager.cc) on both native and java level.
 *
 * This is needed because if a d3d device is lost
 * and needs to be reset we must release all resources created in the default
 * pool first. We must have references to all allocated resources in order to
 * do that. In some cases we need to release all resources (when the device
 * needs to be released, which may happen when a monitor is added or removed).
 *
 * There are several different ways a resource could be disposed of:
 *  - explicit disposal (dispose() is called) - the resource is released by the
 * resource manager at the native level, disposer record is updated to reflect
 * that
 *  - resource became unreachable - then the disposer will eventulally call
 * dispose() for this resource
 *  - resource is disposed of at the native level when trying to reset the
 * device. In this case the native code will call appropriate method to mark
 * default pool or all resources as released (the release itself will happen
 * at the native level) - see {@link D3DResourceManager}
 *
 * In all these cases resource disposal happens on the same thread (the
 * Rendering Thread).
 *
 * Note that some d3d-related resources are not derived from this class - like
 * D3DTexture. This is a bit confusing. But they do use this class's disposer
 * record (they must).
 */
class D3DResource extends BaseGraphicsResource {

    protected final D3DRecord d3dResRecord;

    D3DResource(D3DRecord disposerRecord) {
        super(disposerRecord);
        this.d3dResRecord = disposerRecord;
    }

    @Override
    public void dispose() {
        d3dResRecord.dispose();
    }

    static class D3DRecord implements Disposer.Record {

        private final D3DContext context;
        private long pResource;
        private boolean isDefaultPool;

        D3DRecord(D3DContext context, long pResource) {
            this.context = context;
            this.pResource = pResource;
            if (pResource != 0L) {
                // only add to the list of resources if there's something to
                // dispose of
                context.getResourceFactory().addRecord(this);
                isDefaultPool = D3DResourceFactory.nIsDefaultPool(pResource);
            } else {
                isDefaultPool = false;
            }
        }

        long getResource() {
            return pResource;
        }

        D3DContext getContext() {
            return context;
        }

        boolean isDefaultPool() {
            return isDefaultPool;
        }

        protected void markDisposed() {
            pResource = 0L;
        }

        public void dispose() {
            if (pResource != 0L) {
                context.getResourceFactory().removeRecord(this);
                D3DResourceFactory.nReleaseResource(context.getContextHandle(),
                                                               pResource);
                pResource = 0L;

                // res is always S_OK, no need to validate anything here
                // context.validate(res);
            }
        }
    }
}
