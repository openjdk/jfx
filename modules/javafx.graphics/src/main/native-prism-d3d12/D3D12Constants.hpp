/*
 * Copyright (c) 2024, 2026, Oracle and/or its affiliates. All rights reserved.
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

#include <string>

namespace D3D12 {
namespace Constants {

const std::string PASSTHROUGH_VS_NAME("PassThroughVS");
const std::string PHONG_VS_NAME("Mtl1VS");
const std::string PHONG_PS_NAME("Mtl1PS");
const std::string MIPMAP_CS_NAME("MipmapCS");

const unsigned int MAX_BATCH_QUADS = 4096; // follows other backends and Prism
const unsigned int MAX_BATCH_VERTICES = MAX_BATCH_QUADS * 4;
const unsigned int MAX_TEXTURE_UNITS = 4; // see BaseShaderContext.java validateTextureOp()
const unsigned int MAX_LIGHTS = 3; // for now we support only up to 3 lights per mesh view
const unsigned int MAX_MSAA_SAMPLE_COUNT = 16; // limit for MSAA sample count, same as D3D9

} // namespace Constants
} // namespace D3D12
