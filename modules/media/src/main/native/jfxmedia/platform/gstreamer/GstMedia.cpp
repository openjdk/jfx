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

#include <com_sun_media_jfxmediaimpl_platform_gstreamer_GSTMedia.h>
#include <com_sun_media_jfxmedia_control_VideoFormat_FormatTypes.h>

#include <Common/ProductFlags.h>
#include <Common/VSMemory.h>
#include <MediaManagement/Media.h>
#include <MediaManagement/MediaManager.h>
#include <PipelineManagement/PipelineOptions.h>
#include <PipelineManagement/VideoFrame.h>
#include <Locator/LocatorStream.h>
#include <jni/JniUtils.h>
#include <jni/JavaInputStreamCallbacks.h>
#include <jfxmedia_errors.h>
#include <Utils/LowLevelPerf.h>

using namespace std;

//*************************************************************************************************
//********** com.sun.media.jfxmediaimpl.Media JNI support functions
//*************************************************************************************************

#ifdef __cplusplus
extern "C" {
#endif

    static jstring LocatorToString(JNIEnv *env, jobject locator)
    {
        static jmethodID mid_toString = 0;

        if (mid_toString == 0)
        {
            jclass klass = env->GetObjectClass(locator);

            mid_toString = env->GetMethodID(klass, "getStringLocation", "()Ljava/lang/String;");
            env->DeleteLocalRef(klass);
        }


        return (jstring)env->CallObjectMethod(locator, mid_toString);
    }

    static jint InitMedia(JNIEnv *env, CPipelineOptions* pOptions, jobject jLocator, jstring jContentType, jlong jSizeHint,
                          jlongArray jlMediaHandle)
    {
        CMedia*         pMedia = NULL;
        char*           pjContent = (char*)env->GetStringUTFChars(jContentType , NULL);
        jstring         jLocation = LocatorToString(env, jLocator);
        char*           pjLocation = (char*)env->GetStringUTFChars(jLocation , NULL);
        CMediaManager*  pManager = NULL;
        uint32_t        uErrCode = CMediaManager::GetInstance(&pManager);

        if (ERROR_NONE != uErrCode)
            return uErrCode;

        //***** pre-conditions
        if (NULL == pjContent)
        {
            return ERROR_MEMORY_ALLOCATION;
        }
        if (NULL == pjLocation)
        {
            env->ReleaseStringUTFChars(jContentType, pjContent);
            return ERROR_MEMORY_ALLOCATION;
        }
        if (NULL == pManager)
        {
            env->ReleaseStringUTFChars(jContentType, pjContent);
            env->ReleaseStringUTFChars(jLocation, pjLocation);
            return ERROR_MANAGER_NULL;
        }

        //***** Create a new native locator object
        CLocator *locator;
        CJavaInputStreamCallbacks *callbacks = new (nothrow) CJavaInputStreamCallbacks();
        if (NULL == callbacks)
            return ERROR_MEMORY_ALLOCATION;

        if (!callbacks->Init(env, jLocator))
        {
            env->ReleaseStringUTFChars(jContentType, pjContent);
            env->ReleaseStringUTFChars(jLocation, pjLocation);
            delete callbacks;
            return ERROR_MEDIA_CREATION;
        }

        locator = new(nothrow) CLocatorStream(callbacks, pjContent, pjLocation, (int64_t)jSizeHint);
        env->ReleaseStringUTFChars(jContentType, pjContent);
        env->ReleaseStringUTFChars(jLocation, pjLocation);

        if (NULL == locator)
            return ERROR_MEMORY_ALLOCATION;

        //***** Create the media object
        uErrCode  = pManager->CreatePlayer(locator, pOptions, &pMedia);

        //***** return
        if (ERROR_NONE == uErrCode)
        {
            if (CMedia::IsValid(pMedia))
            {
                jlong lMediaHandle = (jlong)ptr_to_jlong(pMedia);
                env->SetLongArrayRegion(jlMediaHandle, 0, 1, &lMediaHandle);
            }
            else
            {
                uErrCode = ERROR_MEDIA_INVALID;
            }
        }

        // Free locator
        if (locator != NULL)
            delete locator;

        // Clean up
        if (ERROR_NONE != uErrCode)
        {
            if (NULL != pMedia)
                delete pMedia;
        }

        return uErrCode;
    }

    /**
     * GSTMedia_gstInitNativeMedia()
     *
     * Creates a native media reference for the resource string.
     *
     * @return  Media reference.  This reference must be used when calling GSTMediaPlayer function.
     */
    JNIEXPORT jint JNICALL Java_com_sun_media_jfxmediaimpl_platform_gstreamer_GSTMedia_gstInitNativeMedia
    (JNIEnv *env, jobject obj, jobject jLocator, jstring jContentType, jlong jSizeHint, jlongArray jlMediaHandle)
    {
        LOWLEVELPERF_EXECTIMESTART("gstInitNativeMediaToSendToJavaPlayerStateEventPaused");
        LOWLEVELPERF_EXECTIMESTART("gstInitNativeMedia()");
        uint32_t result = InitMedia(env, NULL, jLocator, jContentType, jSizeHint, jlMediaHandle);
        LOWLEVELPERF_EXECTIMESTOP("gstInitNativeMedia()");

        return result;
    }

    /*
    * Class:     com_sun_media_jfxmediaimpl_platform_gstreamer_GSTMedia
    * Method:    gstDispose
    * Signature: (J)V
    */
    JNIEXPORT void JNICALL Java_com_sun_media_jfxmediaimpl_platform_gstreamer_GSTMedia_gstDispose
    (JNIEnv *env, jobject obj, jlong ref_media)
    {
        LOWLEVELPERF_EXECTIMESTART("gstDispose()");

        CMedia* pMedia = (CMedia*)jlong_to_ptr(ref_media);

        if (pMedia != NULL)
        {
            delete pMedia;
            pMedia = NULL;
        }

        LOWLEVELPERF_EXECTIMESTOP("gstDispose()");
    }

#ifdef __cplusplus
}
#endif
