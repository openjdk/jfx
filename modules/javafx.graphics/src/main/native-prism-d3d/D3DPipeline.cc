/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "com_sun_prism_d3d_D3DPipeline.h"
#include "D3DPipelineManager.h"


// d3d9.dll library dynamic load
HMODULE hLibD3D9 = 0;
typedef IDirect3D9 * WINAPI FnDirect3DCreate9(UINT SDKVersion);
typedef HRESULT WINAPI FnDirect3DCreate9Ex(UINT SDKVersion, IDirect3D9Ex**);

FnDirect3DCreate9 * pD3D9FactoryFunction = 0;
FnDirect3DCreate9Ex * pD3D9FactoryExFunction = 0;

static jboolean checkAndClearException(JNIEnv *env) {
    if (!env->ExceptionCheck()) {
        return JNI_FALSE;
    }
    env->ExceptionClear();
    return JNI_TRUE;
}

void loadD3DLibrary() {
    wchar_t path[MAX_PATH];
    if (::GetSystemDirectory(path, sizeof(path) / sizeof(wchar_t)) != 0) {
        wcscat_s(path, MAX_PATH-1, L"\\d3d9.dll");
        hLibD3D9 = ::LoadLibrary(path);
    }
    if (hLibD3D9) {
        pD3D9FactoryFunction = (FnDirect3DCreate9*)GetProcAddress(hLibD3D9, "Direct3DCreate9");
        pD3D9FactoryExFunction = (FnDirect3DCreate9Ex*)GetProcAddress(hLibD3D9, "Direct3DCreate9Ex");
    }
}

void freeD3DLibrary() {
    if (hLibD3D9) {
        ::FreeLibrary(hLibD3D9);
        hLibD3D9 = 0;
        pD3D9FactoryFunction = 0;
        pD3D9FactoryExFunction = 0;
    }
}

IDirect3D9 * Direct3DCreate9() {
    return pD3D9FactoryFunction ? pD3D9FactoryFunction(D3D_SDK_VERSION) : 0;
}

IDirect3D9Ex * Direct3DCreate9Ex() {
    IDirect3D9Ex * pD3D = 0;
    HRESULT hr = pD3D9FactoryExFunction ? pD3D9FactoryExFunction(D3D_SDK_VERSION, &pD3D) : E_FAIL;
    return SUCCEEDED(hr) ? pD3D : 0;
}

#ifndef STATIC_BUILD
BOOL APIENTRY DllMain( HANDLE hModule,
                       DWORD  ul_reason_for_call,
                       LPVOID lpReserved)
{
    switch (ul_reason_for_call) {
    case DLL_PROCESS_ATTACH:
        loadD3DLibrary();
        break;
    case DLL_PROCESS_DETACH:
        freeD3DLibrary();
        break;
    }
    return TRUE;
}
#endif // STATIC_BUILD

struct ConfigJavaStaticClass : IConfig {
    JNIEnv *_env; jclass _psClass;
    ConfigJavaStaticClass(JNIEnv *env, jclass psClass)  :
    _env(env), _psClass(psClass) {}

    virtual int getInt(cstr name) {
        jfieldID id = _env->GetStaticFieldID(_psClass, name, "I");
        return id ? _env->GetStaticIntField(_psClass, id) : 0;
    }

    virtual bool getBool(cstr name) {
        jfieldID id = _env->GetStaticFieldID(_psClass, name, "Z");
        return id && _env->GetStaticBooleanField(_psClass, id);
    }

};

/*
 * Class:     com_sun_prism_d3d_D3DPipeline
 * Method:    nInit
 */

JNIEXPORT jboolean JNICALL Java_com_sun_prism_d3d_D3DPipeline_nInit
  (JNIEnv *env, jclass, jclass psClass)
{
    if (D3DPipelineManager::GetInstance()) {
        D3DPipelineManager::SetErrorMessage("Double D3DPipelineManager initialization");
        return false;
    }

    if (FAILED(D3DPipelineManager::CheckOSVersion())) {
        D3DPipelineManager::SetErrorMessage("Wrong operating system version");
        return false;
    }

#ifdef STATIC_BUILD
    loadD3DLibrary();
#endif // STATIC_BUILD

    TraceLn(NWT_TRACE_INFO, "D3DPipeline_nInit");
    D3DPipelineManager *pMgr = D3DPipelineManager::CreateInstance(ConfigJavaStaticClass(env, psClass));

    if (!pMgr && !D3DPipelineManager::GetErrorMessage()) {
        D3DPipelineManager::SetErrorMessage("Direct3D initialization failed");
    }

    return pMgr != 0;
}


JNIEXPORT jstring JNICALL Java_com_sun_prism_d3d_D3DPipeline_nGetErrorMessage(JNIEnv *jEnv, jclass) {
    const char * msg = D3DPipelineManager::GetErrorMessage();
    return msg ? jEnv->NewStringUTF(msg) : 0;
}

/*
 * Class:     com_sun_prism_d3d_D3DPipeline
 * Method:    nDispose
 */

JNIEXPORT void JNICALL Java_com_sun_prism_d3d_D3DPipeline_nDispose(JNIEnv *pEnv, jclass)
{
    TraceLn(NWT_TRACE_INFO, "D3DPipeline_nDispose");
    if (D3DPipelineManager::GetInstance()) {
        D3DPipelineManager::DeleteInstance();
    }

#ifdef STATIC_BUILD
    freeD3DLibrary();
#endif // STATIC_BUILD
}


JNIEXPORT jint JNICALL Java_com_sun_prism_d3d_D3DPipeline_nGetAdapterOrdinal(JNIEnv *, jclass, jlong hMonitor) {
    D3DPipelineManager *pMgr = D3DPipelineManager::GetInstance();
    if (!pMgr) {
        return 0;
    }
    return pMgr->GetAdapterOrdinalByHmon(HMONITOR(hMonitor));
}

JNIEXPORT jint JNICALL Java_com_sun_prism_d3d_D3DPipeline_nGetAdapterCount(JNIEnv *, jclass) {
    D3DPipelineManager *pMgr = D3DPipelineManager::GetInstance();
    if (!pMgr) {
        return 0;
    }
    return pMgr->GetAdapterCount();
}

static const char jStringField[]  = "Ljava/lang/String;";

void setStringField(JNIEnv *env, jobject object, jclass clazz, const char *name, const char * string) {
    if (jobject jString = env->NewStringUTF(string)) {
        jfieldID id = env->GetFieldID(clazz, name, jStringField);
        if (checkAndClearException(env)) return;
        env->SetObjectField(object, id, jString);
        env->DeleteLocalRef(jString);
    }
}

void setIntField(JNIEnv *env, jobject object, jclass clazz, const char *name, int value) {
    jfieldID id = env->GetFieldID(clazz, name, "I");
    if (checkAndClearException(env)) return;
    env->SetIntField(object, id, value);
}

void fillMSAASupportInformation(JNIEnv *env, jobject object, jclass clazz, int max) {
    setIntField(env, object, clazz, "maxSamples", max);
}

void fillDriverInformation(JNIEnv *env, jobject object, jclass clazz, D3DADAPTER_IDENTIFIER9 &did, D3DCAPS9 &caps) {
    setStringField(env, object, clazz, "deviceDescription", did.Description);
    setStringField(env, object, clazz, "deviceName", did.DeviceName);
    setStringField(env, object, clazz, "driverName", did.Driver);
    setIntField(env, object, clazz, "vendorID", did.VendorId);
    setIntField(env, object, clazz, "deviceID", did.DeviceId);
    setIntField(env, object, clazz, "subSysId", did.SubSysId);
    setIntField(env, object, clazz, "product", HIWORD(did.DriverVersion.HighPart));
    setIntField(env, object, clazz, "version", LOWORD(did.DriverVersion.HighPart));
    setIntField(env, object, clazz, "subVersion", HIWORD(did.DriverVersion.LowPart));
    setIntField(env, object, clazz, "buildID", LOWORD(did.DriverVersion.LowPart));
    if (caps.PixelShaderVersion & 0xFFFF0000) {
        setIntField(env, object, clazz, "psVersionMajor",  (caps.PixelShaderVersion >> 8) & 0xFF);
        setIntField(env, object, clazz, "psVersionMinor",  (caps.PixelShaderVersion) & 0xFF);
    }

    // execute CheckForBadHardware to have valid string
    D3DPipelineManager::CheckForBadHardware(did);
    setStringField(env, object, clazz, "warningMessage", D3DPipelineManager::GetErrorMessage());
}

void fillOsVersionInformation(JNIEnv *env, jobject object, jclass clazz) {
    OSVERSIONINFO osInfo; osInfo.dwOSVersionInfoSize = sizeof(osInfo);
    if (GetVersionEx( &osInfo )) {
        setIntField(env, object, clazz, "osMajorVersion", osInfo.dwMajorVersion);
        setIntField(env, object, clazz, "osMinorVersion", osInfo.dwMinorVersion);
        setIntField(env, object, clazz, "osBuildNumber", osInfo.dwBuildNumber);
    }
}

inline IDirect3D9* addRef(IDirect3D9* i) {
    i->AddRef();
    return i;
}

int getMaxSampleSupport(IDirect3D9 *d3d9, UINT adapter) {
    int maxSamples = 0;
    if (SUCCEEDED(d3d9->CheckDeviceMultiSampleType(adapter,
                    D3DDEVTYPE_HAL , D3DFMT_X8R8G8B8, FALSE,
                    D3DMULTISAMPLE_2_SAMPLES, NULL))) {
        const int MAX_SAMPLES_SEARCH = D3DMULTISAMPLE_16_SAMPLES;
        maxSamples = D3DMULTISAMPLE_2_SAMPLES;
        // Typically even samples are used, thus checking only even samples to
        // save time
        for (int i = maxSamples; i <= MAX_SAMPLES_SEARCH; i += 2) {
            D3DMULTISAMPLE_TYPE msType = static_cast<D3DMULTISAMPLE_TYPE>(i);
            if (SUCCEEDED(d3d9->CheckDeviceMultiSampleType(adapter,
                    D3DDEVTYPE_HAL, D3DFMT_X8R8G8B8, FALSE,
                    msType, NULL))) {
                maxSamples = i;
            } else {
                break;
            }
        }
    }
    return maxSamples;
}

JNIEXPORT jobject JNICALL Java_com_sun_prism_d3d_D3DPipeline_nGetDriverInformation(JNIEnv *env, jclass, jint adapter, jobject obj) {

    if (!obj) {
        return 0;
    }

    // if there is D3DPipelineManager take ready D3D9 object, otherwise create new
    IDirect3D9 * d3d9 = D3DPipelineManager::GetInstance() ?
        addRef(D3DPipelineManager::GetInstance()->GetD3DObject()) : Direct3DCreate9();

    if (!d3d9) {
        return 0;
    }

    if (unsigned(adapter) >= d3d9->GetAdapterCount()) {
        d3d9->Release();
        return 0;
    }

    D3DADAPTER_IDENTIFIER9 d_id;
    D3DCAPS9 caps;
    if (FAILED(d3d9->GetAdapterIdentifier(adapter, 0, &d_id)) ||
        FAILED(d3d9->GetDeviceCaps(adapter, D3DDEVTYPE_HAL, &caps))) {
        d3d9->Release();
        return 0;
    }

    int maxSamples = getMaxSampleSupport(d3d9, adapter);

    if (jclass cls = env->GetObjectClass(obj)) {
        fillDriverInformation(env, obj, cls, d_id, caps);
        fillMSAASupportInformation(env, obj, cls, maxSamples);
        fillOsVersionInformation(env, obj, cls);
    }

    d3d9->Release();
    return obj;
}

JNIEXPORT jint JNICALL Java_com_sun_prism_d3d_D3DPipeline_nGetMaxSampleSupport(JNIEnv *env, jclass, jint adapter) {

    // if there is D3DPipelineManager take ready D3D9 object, otherwise create new
    IDirect3D9 * d3d9 = D3DPipelineManager::GetInstance() ?
        addRef(D3DPipelineManager::GetInstance()->GetD3DObject()) : Direct3DCreate9();

    if (!d3d9) {
        return 0;
    }

    if (unsigned(adapter) >= d3d9->GetAdapterCount()) {
        d3d9->Release();
        return 0;
    }

    int maxSamples = getMaxSampleSupport(d3d9, adapter);

    d3d9->Release();
    return maxSamples;
}
