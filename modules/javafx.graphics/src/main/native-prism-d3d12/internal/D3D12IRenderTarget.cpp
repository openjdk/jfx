/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

#include "D3D12IRenderTarget.hpp"


namespace D3D12 {
namespace Internal {

void IRenderTarget::UpdateDirtyBBox(const BBox& bbox)
{
    if (mBBoxState != BBoxTrackingState::Enabled) return;

    if (!bbox.Valid())
    {
        // we drew something providing a not valid bbox - assume this RTT
        // will be used for more complex draws and disable tracking for this frame
        // this prevents situations like ex. underlined text being drawn
        // providing us with invalid dirty bbox when reused
        mDirtyBBox = BBox();
        mBBoxState = BBoxTrackingState::FrameDisabled;
        return;
    }

    // if current bbox is not valid, set it to this one and return
    if (!mDirtyBBox.Valid())
    {
        mDirtyBBox = bbox;
        return;
    }

    // current bbox and new one are separate or partially overlap
    // this means we dirty this RTT in a more complex way; we should
    // skip the clear opts entirely for visual consistency and try again
    // next frame (maybe this RTT will be reused differently)
    if (!mDirtyBBox.Inside(bbox) && !bbox.Inside(mDirtyBBox))
    {
        mDirtyBBox = BBox();
        mBBoxState = BBoxTrackingState::FrameDisabled;
        return;
    }

    // the region overwrite is easy enough that we can merge the bboxes and continue
    // attempting to optimize the clears; merge and leave
    mDirtyBBox.Merge(bbox);
}

} // namespace Internal
} // namespace D3D12
