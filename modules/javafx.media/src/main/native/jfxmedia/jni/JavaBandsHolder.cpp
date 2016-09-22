/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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

#include "JavaBandsHolder.h"
#include "JniUtils.h"

CJavaBandsHolder::CJavaBandsHolder()
{
}

CJavaBandsHolder::~CJavaBandsHolder()
{
    if (m_jvm != NULL) {
        CJavaEnvironment jenv(m_jvm);
        JNIEnv *pEnv = jenv.getEnvironment();

        if (pEnv) {
            if (m_Magnitudes) {
                pEnv->DeleteGlobalRef(m_Magnitudes);
                m_Magnitudes = NULL;
            }

            if (m_Phases) {
                pEnv->DeleteGlobalRef(m_Phases);
                m_Phases = NULL;
            }
        }
    }
}

bool CJavaBandsHolder::Init(JNIEnv* env, int bands, jfloatArray magnitudes, jfloatArray phases)
{
    env->GetJavaVM(&m_jvm);
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        m_jvm = NULL;
        return false;
    }

    m_Bands = bands;
    m_Magnitudes = (jfloatArray)env->NewGlobalRef(magnitudes);
    m_Phases = (jfloatArray)env->NewGlobalRef(phases);

    InitRef(this);

    return true;
}

void CJavaBandsHolder::UpdateBands(int size, const float* magnitudes, const float* phases)
{
    if (m_Bands != size || m_jvm == NULL)
        return;

    CJavaEnvironment jenv(m_jvm);
    JNIEnv *pEnv = jenv.getEnvironment();
    if (pEnv) {
        // use local references due to threading issues
        jfloatArray localMagnitudes = (jfloatArray)pEnv->NewLocalRef(m_Magnitudes);
        jfloatArray localPhases = (jfloatArray)pEnv->NewLocalRef(m_Phases);

        if (localMagnitudes && localPhases) {
            pEnv->SetFloatArrayRegion(localMagnitudes, 0, size, magnitudes);
            pEnv->SetFloatArrayRegion(localPhases, 0, size, phases);
        }

        pEnv->DeleteLocalRef(localMagnitudes);
        pEnv->DeleteLocalRef(localPhases);
    }
}
