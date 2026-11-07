/*
 * Copyright (c) 2025, 2026, Oracle and/or its affiliates. All rights reserved.
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

// doing this the classic way because HLSL compiler does not recognize #pragma once
#ifndef D3D12NI_SHADER_LIMITS_HPP
#define D3D12NI_SHADER_LIMITS_HPP

#ifndef STRINGIFY
#define STRINGIFY(x) #x
#endif //STRINGIFY

#ifndef STR
#define STR(x) STRINGIFY(x)
#endif // STR

#define D3D12NI_SHADER_LIMITS_MAX_VERTEX_CBV_DTABLE_ENTRIES 3

#define D3D12NI_SHADER_LIMITS_MAX_PIXEL_CBV_DTABLE_ENTRIES 3
#define D3D12NI_SHADER_LIMITS_MAX_PIXEL_SRV_DTABLE_ENTRIES 4

#define D3D12NI_SHADER_LIMITS_MAX_COMPUTE_SRV_DTABLE_ENTRIES 4
#define D3D12NI_SHADER_LIMITS_MAX_COMPUTE_UAV_DTABLE_ENTRIES 4

#endif // D3D12_SHADER_LIMITS_HPP
