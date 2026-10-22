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

#include "D3D12NativePhongMaterial.hpp"

#include <com_sun_prism_d3d12_ni_D3D12NativePhongMaterial.h>


namespace D3D12 {

NativePhongMaterial::NativePhongMaterial(const NIPtr<NativeDevice>& nativeDevice)
    : mNativeDevice(nativeDevice)
{
}

bool NativePhongMaterial::Init()
{
    return true;
}

void NativePhongMaterial::SetDiffuseColor(float r, float g, float b, float a)
{
    mDiffuseColor.r = r;
    mDiffuseColor.g = g;
    mDiffuseColor.b = b;
    mDiffuseColor.a = a;
}

void NativePhongMaterial::SetSpecularColor(bool set, float r, float g, float b, float a)
{
    mSpecularColorSet = set;
    mSpecularColor.r = r;
    mSpecularColor.g = g;
    mSpecularColor.b = b;
    mSpecularColor.a = a;
}

void NativePhongMaterial::ClearTextureMap(TextureMapType type)
{
    mMaps[static_cast<size_t>(type)].reset();
}

void NativePhongMaterial::SetTextureMap(const NIPtr<NativeTexture>& map, TextureMapType type)
{
    mMaps[static_cast<size_t>(type)] = map;
}

} // namespace D3D12


JNIEXPORT void JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativePhongMaterial_nReleaseNativeObject
    (JNIEnv* env, jobject obj, jlong ptr)
{
    if (!ptr) return;

    D3D12::FreeNIObject<D3D12::NativePhongMaterial>(ptr);
}

JNIEXPORT void JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativePhongMaterial_nSetDiffuseColor
    (JNIEnv* env, jobject obj, jlong ptr, jfloat r, jfloat g, jfloat b, jfloat a)
{
    if (!ptr) return;

    D3D12::GetNIObject<D3D12::NativePhongMaterial>(ptr)->SetDiffuseColor(r, g, b, a);
}

JNIEXPORT void JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativePhongMaterial_nSetSpecularColor
    (JNIEnv* env, jobject obj, jlong ptr, jboolean set, jfloat r, jfloat g, jfloat b, jfloat a)
{
    if (!ptr) return;

    D3D12::GetNIObject<D3D12::NativePhongMaterial>(ptr)->SetSpecularColor(set, r, g, b, a);
}

JNIEXPORT void JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativePhongMaterial_nSetTextureMap
    (JNIEnv* env, jobject obj, jlong ptr, jlong texturePtr, jint mapType)
{
    if (!ptr) return;
    if (mapType < static_cast<jint>(D3D12::TextureMapType::DIFFUSE) || mapType >= static_cast<jint>(D3D12::TextureMapType::MAX_ENUM)) return;

    if (texturePtr == 0)
    {
        D3D12::GetNIObject<D3D12::NativePhongMaterial>(ptr)->ClearTextureMap(static_cast<D3D12::TextureMapType>(mapType));
    }
    else
    {
        const D3D12::NIPtr<D3D12::NativeTexture>& texture = D3D12::GetNIObject<D3D12::NativeTexture>(texturePtr);
        D3D12::GetNIObject<D3D12::NativePhongMaterial>(ptr)->SetTextureMap(texture, static_cast<D3D12::TextureMapType>(mapType));
    }
}
