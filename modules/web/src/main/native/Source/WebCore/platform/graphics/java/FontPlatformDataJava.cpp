/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "CString.h"
#include "FontPlatformData.h"
#include "GraphicsContextJava.h"
#include "NotImplemented.h"

#include <wtf/Assertions.h>
#include <wtf/text/WTFString.h>

#include <wtf/PassOwnPtr.h> // todo tav remove when building w/ pch

namespace WebCore {

PassRefPtr<RQRef> FontPlatformData::getJavaFont(const String& family, float size, bool italic, bool bold)
{
    JNIEnv* env = WebCore_GetJavaEnv();

    static jmethodID mid = env->GetMethodID(PG_GetGraphicsManagerClass(env),
        "getWCFont", "(Ljava/lang/String;ZZF)Lcom/sun/webkit/graphics/WCFont;");
    ASSERT(mid);

    JLObject wcFont(env->CallObjectMethod( PL_GetGraphicsManager(env), mid,
        (jstring)JLString(family.toJavaString(env)),
        bool_to_jbool(bold),
        bool_to_jbool(italic),
        jfloat(size)));

    CheckAndClearException(env);

    return RQRef::create(wcFont);
}

PassOwnPtr<FontPlatformData> FontPlatformData::create(
        const FontDescription& fontDescription, const AtomicString& family)
{
    FontWeight weight = fontDescription.weight();
    PassRefPtr<RQRef> wcFont = getJavaFont(
            family,
            fontDescription.computedSize(),
            fontDescription.italic(),
            (FontWeightBold <= weight) && (weight <= FontWeight900));
    return !wcFont ? nullptr : adoptPtr(new FontPlatformData(wcFont, fontDescription.computedSize()));
}

PassOwnPtr<FontPlatformData> FontPlatformData::derive(float scaleFactor) const
{
    ASSERT(m_jFont);
    float size = m_size * scaleFactor;

    JNIEnv* env = WebCore_GetJavaEnv();
    static jmethodID createScaledMID = env->GetMethodID(
        PG_GetFontClass(env), "deriveFont", "(F)Lcom/sun/webkit/graphics/WCFont;");
    ASSERT(createScaledMID);

    JLObject wcFont(env->CallObjectMethod(*m_jFont, createScaledMID, size));
    CheckAndClearException(env);

    return adoptPtr(new FontPlatformData(RQRef::create(wcFont), size));
}

jint FontPlatformData::getJavaFontID(const JLObject &font)
{
    JNIEnv* env = WebCore_GetJavaEnv();

    static jmethodID mid = env->GetStaticMethodID(PG_GetGraphicsManagerClass(env), "getFontRef",
        "(Lcom/sun/webkit/graphics/WCFont;)I");
    ASSERT(mid);

    jint res = env->CallStaticIntMethod(PG_GetGraphicsManagerClass(env), mid, (jobject)font);
    CheckAndClearException(env);

    return res;
}

bool FontPlatformData::operator==(const FontPlatformData& other) const
{
    JNIEnv* env = WebCore_GetJavaEnv();

    if (m_jFont == other.m_jFont) {
        return true;
    }
    if (!m_jFont || isHashTableDeletedValue() ||
        !other.m_jFont || other.isHashTableDeletedValue())
    {
        return false;
    }

    static jmethodID compare_mID = env->GetMethodID(
        PG_GetFontClass(env), "equals", "(Ljava/lang/Object;)Z");
    ASSERT(compare_mID);

    jboolean res = env->CallBooleanMethod(*m_jFont, compare_mID, (jobject)(*other.m_jFont));
    CheckAndClearException(env);

    return bool_to_jbool(res);
}

unsigned FontPlatformData::hash() const
{
    JNIEnv* env = WebCore_GetJavaEnv();

    if (!m_jFont || isHashTableDeletedValue()) {
        return (unsigned)-1;
    }

    static jmethodID hash_mID = env->GetMethodID(PG_GetFontClass(env), "hashCode", "()I");
    ASSERT(hash_mID);

    jint res = env->CallIntMethod(*m_jFont, hash_mID);
    CheckAndClearException(env);

    return res;
}

FontPlatformData& FontPlatformData::operator=(const FontPlatformData& fpd)
{
    // Check for self-assignment.
    if (this != &fpd) {
        FontPlatformData other(fpd);
        swap(other);
    }
    return *this;
}

#ifndef NDEBUG
String FontPlatformData::description() const
{
    notImplemented();
    return "Java font";
}
#endif //NDEBUG

}
