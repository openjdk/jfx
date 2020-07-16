/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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

#include "FontCustomPlatformData.h"

#include "SharedBuffer.h"
#include "FontDescription.h"
#include "FontPlatformData.h"

namespace WebCore {

FontCustomPlatformData::FontCustomPlatformData(const JLObject& data)
    : m_data(data)
{
}

FontCustomPlatformData::~FontCustomPlatformData()
{
}

FontPlatformData FontCustomPlatformData::fontPlatformData(
        const FontDescription& fontDescription, bool bold, bool italic, const FontFeatureSettings&, FontSelectionSpecifiedCapabilities)
{
    JNIEnv* env = WTF::GetJavaEnv();

    int size = fontDescription.computedPixelSize();
    static jmethodID mid = env->GetMethodID(
            PG_GetFontCustomPlatformDataClass(env),
            "createFont",
            "(IZZ)Lcom/sun/webkit/graphics/WCFont;");
    ASSERT(mid);

    JLObject font(env->CallObjectMethod(
            m_data,
            mid,
            size,
            bool_to_jbool(bold),
            bool_to_jbool(italic)));
    WTF::CheckAndClearException(env);

    return FontPlatformData(RQRef::create(font), size);
}

std::unique_ptr<FontCustomPlatformData> createFontCustomPlatformData(SharedBuffer& buffer, const String& /* index */)
{
    JNIEnv* env = WTF::GetJavaEnv();

    static JGClass sharedBufferClass(env->FindClass(
            "com/sun/webkit/SharedBuffer"));
    ASSERT(sharedBufferClass);

    static jmethodID mid1 = env->GetStaticMethodID(
            sharedBufferClass,
            "fwkCreate",
            "(J)Lcom/sun/webkit/SharedBuffer;");
    ASSERT(mid1);

    JLObject sharedBuffer(env->CallStaticObjectMethod(
            sharedBufferClass,
            mid1,
            ptr_to_jlong(&buffer)));
    WTF::CheckAndClearException(env);

    static jmethodID mid2 = env->GetMethodID(
            PG_GetGraphicsManagerClass(env),
            "fwkCreateFontCustomPlatformData",
            "(Lcom/sun/webkit/SharedBuffer;)"
            "Lcom/sun/webkit/graphics/WCFontCustomPlatformData;");
    ASSERT(mid2);

    JLObject data(env->CallObjectMethod(
            PL_GetGraphicsManager(env),
            mid2,
            (jobject) sharedBuffer));
    WTF::CheckAndClearException(env);

    return data ? std::make_unique<FontCustomPlatformData>(data) : nullptr;
}

bool FontCustomPlatformData::supportsFormat(const String& format)
{
    return equalLettersIgnoringASCIICase(format, "truetype")
            || equalLettersIgnoringASCIICase(format, "opentype")
            || equalLettersIgnoringASCIICase(format, "woff");
}

}
