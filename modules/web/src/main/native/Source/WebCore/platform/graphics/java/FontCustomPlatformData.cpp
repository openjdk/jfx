/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"
#include "FontCustomPlatformData.h"

#include "SharedBuffer.h"
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
        int size,
        bool bold,
        bool italic,
        FontOrientation,
        FontWidthVariant,
        FontRenderingMode)
{
    JNIEnv* env = WebCore_GetJavaEnv();

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
    CheckAndClearException(env);

    return FontPlatformData(RQRef::create(font), size);
}

std::unique_ptr<FontCustomPlatformData> createFontCustomPlatformData(SharedBuffer& buffer)
{
    JNIEnv* env = WebCore_GetJavaEnv();

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
    CheckAndClearException(env);

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
    CheckAndClearException(env);

    return data ? std::make_unique<FontCustomPlatformData>(data) : nullptr;
}

bool FontCustomPlatformData::supportsFormat(const String& format)
{
    return equalIgnoringCase(format, "truetype")
            || equalIgnoringCase(format, "opentype")
            || equalIgnoringCase(format, "woff");
}

}
