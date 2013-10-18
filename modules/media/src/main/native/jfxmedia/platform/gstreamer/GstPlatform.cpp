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

#include <com_sun_media_jfxmediaimpl_platform_gstreamer_GSTPlatform.h>

#include <Common/ProductFlags.h>
#include <Common/VSMemory.h>
#include <jfxmedia_errors.h>
#include <jni/JniUtils.h>
#include <MediaManagement/Media.h>
#include <MediaManagement/MediaManager.h>
#include <PipelineManagement/PipelineFactory.h>
#include <jni/Logger.h>
#include <jni/JavaPlayerEventDispatcher.h>
#include <jni/JavaMediaWarningListener.h>
#include <Utils/LowLevelPerf.h>

using namespace std;

//*************************************************************************************************
//********** com.sun.media.jfxmediaimpl.MediaManager JNI support functions
//*************************************************************************************************

#ifdef __cplusplus
extern "C" {
#endif

    /* Initialize the Java VM instance variable when the library is first loaded */
    JavaVM *g_pJVM;

    /*
     * Specify the require JNI version.
     */
    JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved)
    {
        g_pJVM = vm;
        return JNI_VERSION_1_2;
    }

    /**
     * gstInitPlatform()
     *
     * Initializes the native engine.
     *
     * @return Zero on success, non-zero error code on failure.
     */
    JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_gstreamer_GSTPlatform_gstInitPlatform
    (JNIEnv *env, jclass klass)
    {
        LOWLEVELPERF_EXECTIMESTART("gstInitPlatform()");
        LOWLEVELPERF_EXECTIMESTART("gstInitPlatformToVideoPreroll");

        uint32_t uErrorCode = ERROR_NONE;
        CMediaManager* pManager = NULL;

#if ENABLE_VISUAL_STUDIO_MEMORY_LEAKS_DETECTION && TARGET_OS_WIN32
        _CrtSetDbgFlag ( _CRTDBG_ALLOC_MEM_DF | _CRTDBG_LEAK_CHECK_DF);
        //_CrtSetBreakAlloc(0);
#endif // ENABLE_VISUAL_STUDIO_MEMORY_LEAKS_DETECTION

        LOGGER_LOGMSG(LOGGER_DEBUG, "Initializing GSTPlatform");

        uErrorCode = CMediaManager::GetInstance(&pManager);
        if (ERROR_NONE != uErrorCode)
            return uErrorCode;
        else if (NULL == pManager) // Should not happen
            return ERROR_MANAGER_NULL;

        CJavaMediaWarningListener* pWarningListener = new(nothrow) CJavaMediaWarningListener(env);
        if (NULL == pWarningListener)
            return ERROR_MEMORY_ALLOCATION;

        pManager->SetWarningListener(pWarningListener);

        LOWLEVELPERF_EXECTIMESTOP("gstInitPlatform()");

        return ERROR_NONE;
    }

#ifdef __cplusplus
}
#endif
