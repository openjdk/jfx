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

#include "D3D12NativeMeshView.hpp"

#define _USE_MATH_DEFINES
#include <math.h>

#include <com_sun_prism_d3d12_ni_D3D12NativeMeshView.h>


namespace D3D12 {

NativeMeshView::NativeMeshView(const NIPtr<NativeDevice>& nativeDevice)
    : mNativeDevice(nativeDevice)
    , mMesh()
    , mCullMode(D3D12_CULL_MODE_NONE)
    , mFillMode(D3D12_FILL_MODE_SOLID)
    , mMaterial()
    , mLightEnabled()
    , mLightsVS()
    , mColorsPS()
    , mLightsPS()
{
}

bool NativeMeshView::Init(const NIPtr<NativeMesh>& mesh)
{
    mMesh = mesh;

    return true;
}

void NativeMeshView::SetCullingMode(CullFace mode)
{
    switch (mode)
    {
    case CullFace::NONE: mCullMode = D3D12_CULL_MODE_NONE; break;
    case CullFace::BACK: mCullMode = D3D12_CULL_MODE_BACK; break;
    case CullFace::FRONT: mCullMode = D3D12_CULL_MODE_FRONT; break;
    }
}

void NativeMeshView::SetMaterial(const NIPtr<NativePhongMaterial>& material)
{
    mMaterial = material;
}

void NativeMeshView::SetWireframe(bool wireframe)
{
    mFillMode = wireframe ? D3D12_FILL_MODE_WIREFRAME : D3D12_FILL_MODE_SOLID;
}

void NativeMeshView::SetAmbientLight(float r, float g, float b)
{
    mColorsPS.ambientLight.r = r;
    mColorsPS.ambientLight.g = g;
    mColorsPS.ambientLight.b = b;
    mColorsPS.ambientLight.a = 1.0f;
}

void NativeMeshView::SetLight(int index, float x, float y, float z, float r, float g, float b,
                              float enabled, float ca, float la, float qa, float isAttenuated, float maxRange,
                              float dirX, float dirY, float dirZ, float innerAngle, float outerAngle, float falloff)
{
    mLightEnabled[index] = (enabled != 0.0f);


    // Vertex Shader light data

    mLightsVS[index].pos.x = x;
    mLightsVS[index].pos.y = y;
    mLightsVS[index].pos.z = z;
    mLightsVS[index].pos.w = 0.0f;

    mLightsVS[index].normDir.x = dirX;
    mLightsVS[index].normDir.y = dirY;
    mLightsVS[index].normDir.z = dirZ;
    mLightsVS[index].normDir.w = 0.0f;


    // Pixel Shader light data

    mLightsPS[index].color.r = r;
    mLightsPS[index].color.g = g;
    mLightsPS[index].color.b = b;
    mLightsPS[index].color.a = 1.0f;

    // { r=constant, g=linear, b=quadratic, a=on/off }
    mLightsPS[index].attenuation.r = ca;
    mLightsPS[index].attenuation.g = la;
    mLightsPS[index].attenuation.b = qa;
    mLightsPS[index].attenuation.a = isAttenuated;

    // { r=maxRange, _, _, _ }
    mLightsPS[index].maxRange.r = maxRange;
    mLightsPS[index].maxRange.g = 0.0f;
    mLightsPS[index].maxRange.b = 0.0f;
    mLightsPS[index].maxRange.a = 0.0f;

    mLightSpotFactors[index].innerAngle = innerAngle;
    mLightSpotFactors[index].outerAngle = outerAngle;
    mLightSpotFactors[index].falloff = falloff;

    // { r=cos(outer), g=cos(inner)-cos(outer), b=falloff, _ }
    // also converted to radians
    if (IsPointLight(index) || IsDirectionalLight(index))
    {
        mLightsPS[index].spotLightFactors.r = -1.0f;
        mLightsPS[index].spotLightFactors.g = 2.0f;
        mLightsPS[index].spotLightFactors.b = 0.0f;
        mLightsPS[index].spotLightFactors.a = 0.0f;
    }
    else
    {
        double cosInner = cos(innerAngle * M_PI / 180.0);
        double cosOuter = cos(outerAngle * M_PI / 180.0);
        mLightsPS[index].spotLightFactors.r = static_cast<float>(cosOuter);
        mLightsPS[index].spotLightFactors.g = static_cast<float>(cosInner - cosOuter);
        mLightsPS[index].spotLightFactors.b = falloff;
        mLightsPS[index].spotLightFactors.a = 0.0f;
    }
}



} // namespace D3D12


JNIEXPORT void JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeMeshView_nReleaseNativeObject
    (JNIEnv* env, jobject obj, jlong ptr)
{
    if (!ptr) return;

    D3D12::FreeNIObject<D3D12::NativeMeshView>(ptr);
}

JNIEXPORT void JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeMeshView_nSetCullingMode
    (JNIEnv* env, jobject obj, jlong ptr, jint mode)
{
    if (!ptr) return;
    if (mode < static_cast<jint>(D3D12::CullFace::NONE) || mode >= static_cast<jint>(D3D12::CullFace::MAX_ENUM)) return;

    D3D12::GetNIObject<D3D12::NativeMeshView>(ptr)->SetCullingMode(static_cast<D3D12::CullFace>(mode));
}

JNIEXPORT void JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeMeshView_nSetMaterial
    (JNIEnv* env, jobject obj, jlong ptr, jlong phongMaterialPtr)
{
    if (!ptr) return;
    if (!phongMaterialPtr) return;

    const D3D12::NIPtr<D3D12::NativePhongMaterial>& material = D3D12::GetNIObject<D3D12::NativePhongMaterial>(phongMaterialPtr);
    if (!material) return;

    D3D12::GetNIObject<D3D12::NativeMeshView>(ptr)->SetMaterial(material);
}

JNIEXPORT void JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeMeshView_nSetWireframe
    (JNIEnv* env, jobject obj, jlong ptr, jboolean wireframe)
{
    if (!ptr) return;

    D3D12::GetNIObject<D3D12::NativeMeshView>(ptr)->SetWireframe(wireframe);
}

JNIEXPORT void JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeMeshView_nSetAmbientLight
    (JNIEnv* env, jobject obj, jlong ptr, jfloat r, jfloat g, jfloat b)
{
    if (!ptr) return;

    D3D12::GetNIObject<D3D12::NativeMeshView>(ptr)->SetAmbientLight(r, g, b);
}

JNIEXPORT void JNICALL Java_com_sun_prism_d3d12_ni_D3D12NativeMeshView_nSetLight
    (JNIEnv* env, jobject obj, jlong ptr, jint index, jfloat x, jfloat y, jfloat z, jfloat r, jfloat g, jfloat b,
     jfloat enabled, jfloat ca, jfloat la, jfloat qa, jfloat isAttenuated, jfloat maxRange,
     jfloat dirX, jfloat dirY, jfloat dirZ, jfloat innerAngle, jfloat outerAngle, jfloat falloff)
{
    if (!ptr) return;
    if (index < 0 || index >= D3D12::Constants::MAX_LIGHTS)
    {
        D3D12NI_LOG_ERROR("Light index too high (max %d)", D3D12::Constants::MAX_LIGHTS);
        return;
    }

    D3D12::GetNIObject<D3D12::NativeMeshView>(ptr)->SetLight(index, x, y, z, r, g, b, enabled, ca, la, qa, isAttenuated, maxRange,
                                                             dirX, dirY, dirZ, innerAngle, outerAngle, falloff);
}
