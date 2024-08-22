/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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

#include "FontPlatformData.h"
#include "FontDescription.h"
#include "GraphicsContextJava.h"
#include "NotImplemented.h"

#include <wtf/Assertions.h>
#include <wtf/text/CString.h>
#include <wtf/text/WTFString.h>

namespace WebCore {

namespace {

RefPtr<RQRef> getJavaFont(const String& family, float size, bool italic, bool bold)
{
    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetMethodID(PG_GetGraphicsManagerClass(env),
        "getWCFont", "(Ljava/lang/String;ZZF)Lcom/sun/webkit/graphics/WCFont;");
    ASSERT(mid);

    JLObject wcFont(env->CallObjectMethod( PL_GetGraphicsManager(env), mid,
        (jstring)JLString(family.toJavaString(env)),
        bool_to_jbool(bold),
        bool_to_jbool(italic),
        jfloat(size)));

    WTF::CheckAndClearException(env);

    return RQRef::create(wcFont);
}
}

FontPlatformData::FontPlatformData(RefPtr<RQRef> font, float size)
    : m_jFont(font)
    , m_size(size)
{
}

std::unique_ptr<FontPlatformData> FontPlatformData::create(
        const FontDescription& fontDescription, const AtomString& family)
{
    RefPtr<RQRef> wcFont = getJavaFont(
            family,
            fontDescription.computedSize(),
            isItalic(fontDescription.italic()),
            fontDescription.weight() >= boldWeightValue());
    return !wcFont ? nullptr : std::make_unique<FontPlatformData>(wcFont, fontDescription.computedSize());
}

std::unique_ptr<FontPlatformData> FontPlatformData::derive(float scaleFactor) const
{
    ASSERT(m_jFont);
    float size = m_size * scaleFactor;

    JNIEnv* env = WTF::GetJavaEnv();
    static jmethodID createScaledMID = env->GetMethodID(
        PG_GetFontClass(env), "deriveFont", "(F)Lcom/sun/webkit/graphics/WCFont;");
    ASSERT(createScaledMID);

    JLObject wcFont(env->CallObjectMethod(*m_jFont, createScaledMID, size));
    WTF::CheckAndClearException(env);

    return std::make_unique<FontPlatformData>(RQRef::create(wcFont), size);
}

bool FontPlatformData::platformIsEqual(const FontPlatformData& other) const
{
    JNIEnv* env = WTF::GetJavaEnv();

    if (m_jFont == other.m_jFont) {
        return true;
    }
    if (!m_jFont || isHashTableDeletedValue() ||
        !other.m_jFont || other.isHashTableDeletedValue()) {
        return false;
    }

    static jmethodID compare_mID = env->GetMethodID(
        PG_GetFontClass(env), "equals", "(Ljava/lang/Object;)Z");
    ASSERT(compare_mID);

    jboolean res = env->CallBooleanMethod(*m_jFont, compare_mID, (jobject)(*other.m_jFont));
    WTF::CheckAndClearException(env);

    return bool_to_jbool(res);
}

unsigned FontPlatformData::hash() const
{
    JNIEnv* env = WTF::GetJavaEnv();

    if (!m_jFont || isHashTableDeletedValue()) {
        return (unsigned)-1;
    }

    static jmethodID hash_mID = env->GetMethodID(PG_GetFontClass(env), "hashCode", "()I");
    ASSERT(hash_mID);

    jint res = env->CallIntMethod(*m_jFont, hash_mID);
    WTF::CheckAndClearException(env);

    return res;
}

#ifndef NDEBUG
String FontPlatformData::description() const
{
    notImplemented();
    return "Java font"_s;
}
#endif //NDEBUG

String FontPlatformData::familyName() const
{
    // FIXME: Not implemented yet.
    return { };
}

}
