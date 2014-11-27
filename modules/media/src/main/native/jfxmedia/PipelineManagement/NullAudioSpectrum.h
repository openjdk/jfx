/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

#ifndef __JFXMedia__NullAudioSpectrum__
#define __JFXMedia__NullAudioSpectrum__

#include "AudioSpectrum.h"

/*
 * Audio spectrum class that does nothing, since the stack does not allow one to NOT exist.
 */

class CNullAudioSpectrum : public CAudioSpectrum {
public:
    CNullAudioSpectrum() : CAudioSpectrum(), mEnabled(false) {}
    virtual ~CNullAudioSpectrum() {}

    virtual bool IsEnabled() {
        return mEnabled;
    }

    virtual void SetEnabled(bool isEnabled) {
        mEnabled = isEnabled;
    }

    virtual void SetBands(int bands, CBandsHolder* holder) {
        mBandCount = bands;
        mBandsHolder = holder;
    }

    virtual size_t GetBands() {
        return (size_t)mBandCount;
    }

    virtual double GetInterval() {
        return mInterval;
    }

    virtual void SetInterval(double interval) {
        mInterval = interval;
    }

    virtual int GetThreshold() {
        return mThreshold;
    }

    virtual void SetThreshold(int threshold) {
        mThreshold = threshold;
    }

    virtual void UpdateBands(int size, const float* magnitudes, const float* phases) {
        // Do nothing...
    }

private:
    bool mEnabled;
    CBandsHolder *mBandsHolder;
    int mBandCount;
    double mInterval;
    int mThreshold;
};

#endif /* defined(__JFXMedia__NullAudioSpectrum__) */
