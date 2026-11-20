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

#include "D3D12Common.hpp"

#include "internal/D3D12Config.hpp"
#include "internal/D3D12Debug.hpp"
#include "internal/D3D12Logger.hpp"

#include <com_sun_prism_d3d12_D3D12Pipeline.h>

#ifdef __cplusplus
extern "C"
{
#endif

JNIEXPORT jboolean JNICALL Java_com_sun_prism_d3d12_D3D12Pipeline_nInit
    (JNIEnv *env, jclass, jclass psClass)
{
    if (!D3D12::Internal::Config::Instance().LoadConfiguration(env, psClass))
    {
        return false;
    }

    if (!D3D12::Internal::Debug::Instance().Init())
    {
        D3D12NI_LOG_ERROR("Failed to initialize Debug facilities");
        return false;
    }

    return true;
}

#ifdef __cplusplus
}
#endif
