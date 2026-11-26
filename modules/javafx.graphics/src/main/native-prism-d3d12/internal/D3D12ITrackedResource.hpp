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

#pragma once

#include "../D3D12Common.hpp"


namespace D3D12 {
namespace Internal {

// This provides an interface to fetch ID3D12Resource pointer from backend objects.
// This allows us to fetch the resource inside the Render Thread when it's actually needed.
// Resolves situations like SwapChain having multiple buffers and switching between them
// after RenderThread presents a frame.
class ITrackedResource
{
public:
    virtual const D3D12ResourcePtr& GetD3D12Resource() const = 0;
    virtual D3D12_RESOURCE_STATES GetD3D12ResourceState(uint32_t subresource) const = 0;
    virtual void SetD3D12ResourceState(D3D12_RESOURCE_STATES newState, uint32_t subresource) = 0;
    virtual bool NeedsStateTransitions() const = 0;
};

} // namespace Internal
} // namespace D3D12
