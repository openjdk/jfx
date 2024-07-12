/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

#include "config.h"

#include "NotImplemented.h"

#include "BitmapImage.h"
#include "GraphicsContext.h"
#include "ImageObserver.h"
#include "PlatformJavaClasses.h"
#include "GraphicsContextJava.h"

#include "PlatformContextJava.h"
#include "RenderingQueue.h"
#include "SharedBuffer.h"
#include "Logging.h"

namespace WebCore {

IntSize PlatformImageNativeImageBackend::size() const
{
    if (!m_platformImage || !m_platformImage->getImage()) {
        return {};
    }

    JNIEnv* env = WTF::GetJavaEnv();
    static jmethodID midGetSize = env->GetMethodID(
        PG_GetImageFrameClass(env),
        "getSize",
        "()[I");
    ASSERT(midGetSize);
    JLocalRef<jintArray> jsize((jintArray)env->CallObjectMethod(
                        jobject(*m_platformImage->getImage().get()),
                        midGetSize));
    if (!jsize) {
        return {};
    }

    jint* size = (jint*)env->GetPrimitiveArrayCritical((jintArray)jsize, 0);
    IntSize frameSize(size[0], size[1]);
    env->ReleasePrimitiveArrayCritical(jsize, size, 0);
    return frameSize;
}

bool PlatformImageNativeImageBackend::hasAlpha() const
{
    // FIXME-java: Get alpha details from ImageMetadata class
    return true;
}

Color NativeImage::singlePixelSolidColor() const
{
    return {};
}

void NativeImage::clearSubimages()
{
    notImplemented();
}

DestinationColorSpace PlatformImageNativeImageBackend::colorSpace() const //TBD
{
    notImplemented();
    return DestinationColorSpace::SRGB();
}

}
