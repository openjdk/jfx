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

#ifndef _GST_AUDIO_SPECTRUM_H_
#define _GST_AUDIO_SPECTRUM_H_

#include <PipelineManagement/AudioSpectrum.h>
#include <gst/gst.h>

class CGstAudioSpectrum : public CAudioSpectrum
{
public:
    CGstAudioSpectrum(GstElement* spectrum, bool enabled);
    virtual ~CGstAudioSpectrum();

    virtual bool      IsEnabled();
    virtual void      SetEnabled(bool isEnabled);

    virtual void      SetBands(int bands, CBandsHolder* updater);
    virtual size_t    GetBands();
    virtual void      UpdateBands(int size, const float* magnitudes, const float* phases);

    virtual double    GetInterval();
    virtual void      SetInterval(double interval);

    virtual int       GetThreshold();
    virtual void      SetThreshold(int threshold);

private:
    GstElement*            m_pSpectrum;
    volatile CBandsHolder* m_pHolder;
};

#endif // _GST_AUDIO_SPECTRUM_H_
