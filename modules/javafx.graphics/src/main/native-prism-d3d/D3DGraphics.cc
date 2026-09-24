/*
 * Copyright (c) 2009, 2026, Oracle and/or its affiliates. All rights reserved.
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

#include "D3DPipeline.h"
#include "D3DPipelineManager.h"
#include "D3DResourceManager.h"
#include "D3DContext.h"

/*
 * This is Prism VertexBuffer format for the FloatBuffer passed to nFlush
 */
struct PrismSourceVertex {
    float x, y, z;
    float tu1, tv1;
    float tu2, tv2;
};

/*
 * Note: this method assumes that pVert, pSrcFloats and pSrcColors are not null
 */
void fillVB(PRISM_VERTEX_2D *pVert, PrismSourceVertex const *pSrcFloats, BYTE const *pSrcColors, UINT numVerts) {
    for (UINT i = 0; i < numVerts; i++) {

        pVert->x = pSrcFloats->x;
        pVert->y = pSrcFloats->y;
        pVert->z = pSrcFloats->z;

        pVert->color =
            (pSrcColors[3]<<24) + (pSrcColors[0]<<16) +
            (pSrcColors[1]<<8 ) +  pSrcColors[2];

        pVert->tu1 = pSrcFloats->tu1;
        pVert->tv1 = pSrcFloats->tv1;

        pVert->tu2 = pSrcFloats->tu2;
        pVert->tv2 = pSrcFloats->tv2;

        pSrcFloats++;
        pSrcColors+=4;
        pVert++;
    }
}

inline UINT align4(UINT x) {
    return (x+3) & ~3;
}

void D3DContext::stretchRect(IDirect3DSurface9* pSrcSurface,
                               int srcX0, int srcY0, int srcX1, int srcY1,
                               IDirect3DSurface9* pDstSurface,
                               int dstX0, int dstY0, int dstX1, int dstY1)
{
    RETURN_IF_NULL(pd3dDevice);

    HRESULT res;
    IDirect3DSurface9* pDst = (pDstSurface == NULL) ? currentSurface : pDstSurface;
    RECT srcRect = {srcX0, srcY0, srcX1, srcY1};
    RECT dstRect = {dstX0, dstY0, dstX1, dstY1};
    res = pd3dDevice->StretchRect(pSrcSurface, &srcRect, pDst, &dstRect, D3DTEXF_LINEAR);
    if (FAILED(res)) {
        DebugPrintD3DError(res, "D3DContext::stretchRect: error StretchRect");
    }
}

/*
 * Note: this method assumes that pSrcFloats and pSrcColors are not null and
 * numVerts is a positive number
 */
HRESULT D3DContext::drawIndexedQuads(PrismSourceVertex const *pSrcFloats, BYTE const *pSrcColors, int numVerts) {

    RETURN_STATUS_IF_NULL(pd3dDevice, E_FAIL);

    // pVertexBufferRes and pVertexBuffer is never null
    // it is checked in D3DContext::InitDevice
    IDirect3DVertexBuffer9 *pVertexBuffer = pVertexBufferRes->GetVertexBuffer();

    HRESULT res = BeginScene();
    RETURN_STATUS_IF_FAILED(res);

    UINT firstIndex = align4(pVertexBufferRes->GetFirstIndex());

    int numQuads = numVerts / 4;

    do {
        UINT quadsInBatch = min(MAX_BATCH_QUADS, numQuads);
        int vertsInBatch = quadsInBatch * 4;

        if ((firstIndex + vertsInBatch) > MAX_VERTICES) {
            firstIndex = 0;
        }

        DWORD dwLockFlags = firstIndex ? D3DLOCK_NOOVERWRITE : D3DLOCK_DISCARD;

        UINT lockIndex = firstIndex   * sizeof(PRISM_VERTEX_2D);
        UINT lockSize  = vertsInBatch * sizeof(PRISM_VERTEX_2D);

        PRISM_VERTEX_2D *pVert = 0;
        res = pVertexBuffer->Lock(lockIndex, lockSize, (void **)&pVert, dwLockFlags);
        if (SUCCEEDED(res)) {

            fillVB(pVert, pSrcFloats, pSrcColors, vertsInBatch);
            pSrcFloats += vertsInBatch;
            pSrcColors += vertsInBatch * 4;

            res = pVertexBuffer->Unlock();

#if defined PERF_COUNTERS
            D3DContext::FrameStats &stats = getStats();
            stats.numBufferLocks++;
            stats.numDrawCalls++;
            stats.numTrianglesDrawn += quadsInBatch * 2;
#endif

            res = pd3dDevice->DrawIndexedPrimitive(D3DPT_TRIANGLELIST, 0,
                firstIndex, numQuads * 4,
                (firstIndex / 4) * 6, quadsInBatch * 2);

            firstIndex += vertsInBatch;
            numQuads -= quadsInBatch;
        }
    } while (numQuads > 0 && SUCCEEDED(res));

    pVertexBufferRes->SetLastIndex(firstIndex);

    return res;
}
