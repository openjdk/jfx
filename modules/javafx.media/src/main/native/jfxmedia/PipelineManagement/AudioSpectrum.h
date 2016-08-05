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

#ifndef _AUDIO_SPECTRUM_H_
#define _AUDIO_SPECTRUM_H_

#include <stdlib.h>

class IBandsUpdater
{
public:
    virtual void UpdateBands(int size, const float* magnitudes, const float* phases) = 0;
};

class CBandsHolder : public IBandsUpdater
{
public:
    virtual ~CBandsHolder() {}

    static CBandsHolder* AddRef(CBandsHolder* holder);
    static void          ReleaseRef(CBandsHolder* holder);

protected:
    static void          InitRef(CBandsHolder* holder);

private:
    volatile int m_RefCounter;
};

class CAudioSpectrum : public IBandsUpdater
{
public:
    virtual ~CAudioSpectrum() {}

    virtual bool       IsEnabled() = 0;
    virtual void       SetEnabled(bool isEnabled) = 0;

    virtual void       SetBands(int bands, CBandsHolder* holder) = 0;
    virtual size_t     GetBands() = 0;

    virtual double     GetInterval() = 0;
    virtual void       SetInterval(double interval) = 0;

    virtual int        GetThreshold() = 0;
    virtual void       SetThreshold(int threshold) = 0;
};

#endif // _AUDIO_SPECTRUM_H_
