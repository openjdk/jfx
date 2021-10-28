/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include "GstAudioSpectrum.h"
#include <MediaManagement/Media.h>
#include <PipelineManagement/Pipeline.h>
#include <PipelineManagement/AudioSpectrum.h>
#include <jni/JavaBandsHolder.h>
#include <jni/JniUtils.h>

/************************************************************************
 *
 *************************************************************************/
void CBandsHolder::InitRef(CBandsHolder* ref)
{
    g_atomic_int_set(&ref->m_RefCounter, 1);
}

CBandsHolder* CBandsHolder::AddRef(CBandsHolder* ref)
{
    if (ref != NULL)
        g_atomic_int_add(&ref->m_RefCounter, 1);
    return ref;
}

void CBandsHolder::ReleaseRef(CBandsHolder* ref)
{
    if (ref != NULL && g_atomic_int_dec_and_test(&ref->m_RefCounter))
        delete ref;
}

/************************************************************************
 *
 *************************************************************************/
CGstAudioSpectrum::CGstAudioSpectrum(GstElement* pSpectrum, bool enabled)
{
    m_pSpectrum = GST_ELEMENT(gst_object_ref(pSpectrum));

    // Do send magnitude and phase infromation, off by default
    g_object_set(m_pSpectrum, "post-messages", enabled,
                              "message-magnitude", TRUE,
                              "message-phase", TRUE, NULL);
    g_atomic_pointer_set(&m_pHolder, NULL);
}

CGstAudioSpectrum::~CGstAudioSpectrum()
{
    CBandsHolder::ReleaseRef((CBandsHolder*)g_atomic_pointer_get(&m_pHolder));
    gst_object_unref(m_pSpectrum);
}

bool CGstAudioSpectrum::IsEnabled()
{
    gboolean post_messages;
    g_object_get(m_pSpectrum, "post-messages", &post_messages, NULL);
    return post_messages;
}

void CGstAudioSpectrum::SetEnabled(bool enabled)
{
    g_object_set(m_pSpectrum, "post-messages", enabled, NULL);
}

size_t CGstAudioSpectrum::GetBands()
{
    gint bands = 0;
    g_object_get(m_pSpectrum, "bands", &bands, NULL);
    return (size_t)bands;
}

void CGstAudioSpectrum::SetBands(int bands, CBandsHolder* holder)
{
    g_object_set(m_pSpectrum, "bands", bands, NULL);

    CBandsHolder *old_holder = (CBandsHolder*)g_atomic_pointer_get(&m_pHolder);
    g_atomic_pointer_compare_and_exchange((gpointer*)&m_pHolder, (gpointer)old_holder, (gpointer)holder);
    CBandsHolder::ReleaseRef(old_holder);
}

void CGstAudioSpectrum::UpdateBands(int size, const float* magnitudes, const float* phases)
{
    CBandsHolder *holder = CBandsHolder::AddRef((CBandsHolder*)g_atomic_pointer_get(&m_pHolder));
    holder->UpdateBands(size, magnitudes, phases);
    CBandsHolder::ReleaseRef(holder);
}

double CGstAudioSpectrum::GetInterval()
{
    guint64 interval;
    g_object_get(m_pSpectrum, "interval", &interval, NULL);
    return GST_TIME_AS_SECONDS((double)interval);
}

void CGstAudioSpectrum::SetInterval(double interval)
{
    guint64 value = (guint64)(interval * GST_SECOND);
    g_object_set(m_pSpectrum, "interval", value, NULL);
}

int CGstAudioSpectrum::GetThreshold()
{
    gint threshold;
    g_object_get(m_pSpectrum, "threshold", &threshold, NULL);
    return threshold;
}

void CGstAudioSpectrum::SetThreshold(int threshold)
{
    g_object_set(m_pSpectrum, "threshold", threshold, NULL);
}
